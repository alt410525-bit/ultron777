package com.ultron.android.tools.connectors

import com.ultron.android.core.tools.*
import com.ultron.android.data.ConnectorSecrets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val http = OkHttpClient()

private suspend fun getJson(url: String, headers: Map<String, String> = emptyMap()): JsonElement =
    withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.build()
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}: ${raw.take(120)}")
            Json.parseToJsonElement(raw)
        }
    }

private suspend fun postJson(url: String, headers: Map<String, String>, body: JsonObject): JsonElement =
    withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).apply {
            headers.forEach { (k, v) -> header(k, v) }
        }.post(body.toString().toRequestBody("application/json".toMediaType())).build()
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}: ${raw.take(120)}")
            Json.parseToJsonElement(raw)
        }
    }

private fun JsonElement.s(key: String): String =
    jsonObject[key]?.jsonPrimitive?.contentOrNull.orEmpty()

private fun githubHeaders(token: String?): Map<String, String> = buildMap {
    put("Accept", "application/vnd.github+json")
    if (!token.isNullOrBlank()) put("Authorization", "Bearer $token")
}

private fun ToolContext.token(key: String): String? =
    ConnectorSecrets.get(appContext, key).takeIf { it.isNotBlank() }

// ---------- GitHub ----------

class GithubRepoInfoTool : Tool {
    override val id = "github_repo_info"
    override val category = ToolCategory.CONNECTORS
    override val title = "GitHub repo info"
    override val description = "Kisi GitHub repo ki details batao: description, stars, open issues."
    override val params = listOf(
        ToolParam("owner", "string", "Repo owner username", required = true),
        ToolParam("repo", "string", "Repo name", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val owner = args.str("owner") ?: return ToolResult.Error("owner nahi mila")
        val repo = args.str("repo") ?: return ToolResult.Error("repo nahi mila")
        val o = getJson(
            "https://api.github.com/repos/$owner/$repo",
            githubHeaders(ctx.token(ConnectorSecrets.GITHUB))
        )
        return ToolResult.Ok(
            "${o.s("full_name")}: ${o.s("description").ifBlank { "description nahi hai" }}. " +
                "Stars ${o.s("stargazers_count")}, open issues ${o.s("open_issues_count")}"
        )
    }
}

class GithubListIssuesTool : Tool {
    override val id = "github_list_issues"
    override val category = ToolCategory.CONNECTORS
    override val title = "GitHub issues"
    override val description = "Kisi GitHub repo ke open issues ki list dikhao."
    override val params = listOf(
        ToolParam("owner", "string", "Repo owner username", required = true),
        ToolParam("repo", "string", "Repo name", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val owner = args.str("owner") ?: return ToolResult.Error("owner nahi mila")
        val repo = args.str("repo") ?: return ToolResult.Error("repo nahi mila")
        val arr = getJson(
            "https://api.github.com/repos/$owner/$repo/issues?state=open&per_page=5",
            githubHeaders(ctx.token(ConnectorSecrets.GITHUB))
        ).jsonArray
        if (arr.isEmpty()) return ToolResult.Ok("Koi open issue nahi hai")
        return ToolResult.Ok(arr.joinToString("\n") { "#${it.s("number")} ${it.s("title")}" })
    }
}

class GithubListPrsTool : Tool {
    override val id = "github_list_prs"
    override val category = ToolCategory.CONNECTORS
    override val title = "GitHub pull requests"
    override val description = "Kisi GitHub repo ke open pull requests ki list dikhao."
    override val params = listOf(
        ToolParam("owner", "string", "Repo owner username", required = true),
        ToolParam("repo", "string", "Repo name", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val owner = args.str("owner") ?: return ToolResult.Error("owner nahi mila")
        val repo = args.str("repo") ?: return ToolResult.Error("repo nahi mila")
        val arr = getJson(
            "https://api.github.com/repos/$owner/$repo/pulls?state=open&per_page=5",
            githubHeaders(ctx.token(ConnectorSecrets.GITHUB))
        ).jsonArray
        if (arr.isEmpty()) return ToolResult.Ok("Koi open PR nahi hai")
        return ToolResult.Ok(arr.joinToString("\n") { "#${it.s("number")} ${it.s("title")}" })
    }
}

class GithubCreateIssueTool : Tool {
    override val id = "github_create_issue"
    override val category = ToolCategory.CONNECTORS
    override val title = "GitHub issue banao"
    override val description = "Kisi GitHub repo mein naya issue banao. Pehle user se haan karwana zaroori hai. GitHub token chahiye."
    override val params = listOf(
        ToolParam("owner", "string", "Repo owner username", required = true),
        ToolParam("repo", "string", "Repo name", required = true),
        ToolParam("title", "string", "Issue ka title", required = true),
        ToolParam("body", "string", "Issue ka detail (optional)", required = false)
    )
    override val risk = RiskLevel.HIGH

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val token = ctx.token(ConnectorSecrets.GITHUB)
            ?: return ToolResult.Error("GitHub token set nahi hai")
        val owner = args.str("owner") ?: return ToolResult.Error("owner nahi mila")
        val repo = args.str("repo") ?: return ToolResult.Error("repo nahi mila")
        val title = args.str("title") ?: return ToolResult.Error("title nahi mila")
        val body = buildJsonObject {
            put("title", title)
            args.str("body")?.let { put("body", it) }
        }
        val created = postJson(
            "https://api.github.com/repos/$owner/$repo/issues",
            githubHeaders(token),
            body
        )
        return ToolResult.Ok("Issue #${created.s("number")} bana diya: $title")
    }
}

// ---------- Telegram ----------

class TelegramSendTool : Tool {
    override val id = "telegram_send_message"
    override val category = ToolCategory.CONNECTORS
    override val title = "Telegram message"
    override val description = "Telegram bot se kisi chat mein message bhejo. Pehle user se haan karwana zaroori hai. Bot token chahiye."
    override val params = listOf(
        ToolParam("chat_id", "string", "Telegram chat ID", required = true),
        ToolParam("text", "string", "Message ka text", required = true)
    )
    override val risk = RiskLevel.HIGH

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val token = ctx.token(ConnectorSecrets.TELEGRAM)
            ?: return ToolResult.Error("Telegram bot token set nahi hai")
        val chatId = args.str("chat_id") ?: return ToolResult.Error("chat_id nahi mila")
        val text = args.str("text") ?: return ToolResult.Error("text nahi mila")
        postJson(
            "https://api.telegram.org/bot$token/sendMessage",
            emptyMap(),
            buildJsonObject {
                put("chat_id", chatId)
                put("text", text)
            }
        )
        return ToolResult.Ok("Telegram par message bhej diya")
    }
}

// ---------- Notion ----------

class NotionSearchTool : Tool {
    override val id = "notion_search"
    override val category = ToolCategory.CONNECTORS
    override val title = "Notion search"
    override val description = "Notion mein pages ya databases dhoondho. Notion integration token chahiye."
    override val params = listOf(
        ToolParam("query", "string", "Kya dhoondhna hai", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val token = ctx.token(ConnectorSecrets.NOTION)
            ?: return ToolResult.Error("Notion token set nahi hai")
        val query = args.str("query") ?: return ToolResult.Error("query nahi mila")
        val res = postJson(
            "https://api.notion.com/v1/search",
            mapOf(
                "Authorization" to "Bearer $token",
                "Notion-Version" to "2022-06-28"
            ),
            buildJsonObject {
                put("query", query)
                put("page_size", 5)
            }
        ).jsonObject["results"]?.jsonArray.orEmpty()
        if (res.isEmpty()) return ToolResult.Ok("Notion mein kuch nahi mila")
        return ToolResult.Ok(res.joinToString("\n") { notionTitle(it) })
    }

    private fun notionTitle(item: JsonElement): String {
        val obj = item.jsonObject
        val props = obj["properties"]?.jsonObject
        val titleText = props?.values?.firstNotNullOfOrNull { p ->
            val o = p.jsonObject
            if (o["type"]?.jsonPrimitive?.contentOrNull == "title") {
                o["title"]?.jsonArray?.joinToString("") {
                    it.jsonObject["plain_text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                }
            } else null
        }
        val kind = obj["object"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return "${titleText?.ifBlank { null } ?: "(bina title)"} [$kind]"
    }
}

class ConnectorToolModule : ToolModule {
    override val tools: List<Tool> = listOf(
        GithubRepoInfoTool(),
        GithubListIssuesTool(),
        GithubListPrsTool(),
        GithubCreateIssueTool(),
        TelegramSendTool(),
        NotionSearchTool()
    )
}
