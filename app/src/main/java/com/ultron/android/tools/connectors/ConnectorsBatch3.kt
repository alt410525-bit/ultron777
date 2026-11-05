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
import java.util.concurrent.TimeUnit

private val httpC3: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

private fun ToolContext.githubTokenC3(): String? =
    ConnectorSecrets.get(appContext, ConnectorSecrets.GITHUB).takeIf { it.isNotBlank() }

private suspend fun githubGetC3(url: String, token: String?): JsonElement = withContext(Dispatchers.IO) {
    val req = Request.Builder()
        .url(url)
        .header("Accept", "application/vnd.github+json")
        .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
        .build()
    httpC3.newCall(req).execute().use { resp ->
        val raw = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) throw IllegalStateException("GitHub HTTP ${resp.code}: ${raw.take(120)}")
        Json.parseToJsonElement(raw)
    }
}

private suspend fun githubPostC3(url: String, token: String, body: JsonObject): JsonElement = withContext(Dispatchers.IO) {
    val req = Request.Builder()
        .url(url)
        .header("Accept", "application/vnd.github+json")
        .header("Authorization", "Bearer $token")
        .post(body.toString().toRequestBody("application/json".toMediaType()))
        .build()
    httpC3.newCall(req).execute().use { resp ->
        val raw = resp.body?.string().orEmpty()
        if (!resp.isSuccessful) throw IllegalStateException("GitHub HTTP ${resp.code}: ${raw.take(120)}")
        Json.parseToJsonElement(raw)
    }
}

private fun JsonObject.textC3(key: String): String =
    this[key]?.jsonPrimitive?.contentOrNull.orEmpty()

class GithubCommitHistoryTool : Tool {
    override val id = "github_commit_history"
    override val category = ToolCategory.CONNECTORS
    override val title = "GitHub commit history"
    override val description = "Kisi repo ke latest commits dikhao (default 5, max 10). Public repo ke liye token optional hai."
    override val params = listOf(
        ToolParam("owner", "string", "Repo owner username", required = true),
        ToolParam("repo", "string", "Repo name", required = true),
        ToolParam("count", "integer", "Kitne commits chahiye, 1 se 10", required = false)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val owner = args.str("owner") ?: return ToolResult.Error("owner nahi mila")
        val repo = args.str("repo") ?: return ToolResult.Error("repo nahi mila")
        val count = (args.int("count") ?: 5).coerceIn(1, 10)
        val arr = githubGetC3(
            "https://api.github.com/repos/$owner/$repo/commits?per_page=$count",
            ctx.githubTokenC3()
        ).jsonArray
        if (arr.isEmpty()) return ToolResult.Ok("Koi commit nahi mila")
        return ToolResult.Ok(arr.joinToString("\n") { el ->
            val o = el.jsonObject
            val commit = o["commit"]?.jsonObject
            val msg = commit?.textC3("message")?.lineSequence()?.firstOrNull().orEmpty()
            val author = commit?.get("author")?.jsonObject?.textC3("name").orEmpty()
            val sha = o.textC3("sha").take(7)
            "$sha $msg ($author)"
        })
    }
}

class GithubCommentIssueTool : Tool {
    override val id = "github_comment_issue"
    override val category = ToolCategory.CONNECTORS
    override val title = "GitHub issue par comment"
    override val description = "Kisi GitHub issue par comment likho. Pehle user se haan karwana zaroori hai. GitHub token chahiye."
    override val params = listOf(
        ToolParam("owner", "string", "Repo owner username", required = true),
        ToolParam("repo", "string", "Repo name", required = true),
        ToolParam("number", "integer", "Issue ka number", required = true),
        ToolParam("body", "string", "Comment ka text", required = true)
    )
    override val risk = RiskLevel.HIGH

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val token = ctx.githubTokenC3() ?: return ToolResult.Error("GitHub token set nahi hai")
        val owner = args.str("owner") ?: return ToolResult.Error("owner nahi mila")
        val repo = args.str("repo") ?: return ToolResult.Error("repo nahi mila")
        val number = args.int("number") ?: return ToolResult.Error("issue number nahi mila")
        val body = args.str("body")?.takeIf { it.isNotBlank() } ?: return ToolResult.Error("comment khali hai")
        val res = githubPostC3(
            "https://api.github.com/repos/$owner/$repo/issues/$number/comments",
            token,
            buildJsonObject { put("body", body) }
        )
        return ToolResult.Ok("Issue #$number par comment daal diya (${res.jsonObject.textC3("html_url")})")
    }
}

class GithubBuildFailuresTool : Tool {
    override val id = "github_build_failures"
    override val category = ToolCategory.CONNECTORS
    override val title = "Failed builds dekho"
    override val description = "Repo ke latest failed GitHub Actions runs dikhao. GitHub token chahiye."
    override val params = listOf(
        ToolParam("owner", "string", "Repo owner username", required = true),
        ToolParam("repo", "string", "Repo name", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val token = ctx.githubTokenC3() ?: return ToolResult.Error("GitHub token set nahi hai")
        val owner = args.str("owner") ?: return ToolResult.Error("owner nahi mila")
        val repo = args.str("repo") ?: return ToolResult.Error("repo nahi mila")
        val runs = githubGetC3(
            "https://api.github.com/repos/$owner/$repo/actions/runs?status=failure&per_page=5",
            token
        ).jsonObject["workflow_runs"]?.jsonArray.orEmpty()
        if (runs.isEmpty()) return ToolResult.Ok("Koi failed run nahi mila")
        return ToolResult.Ok(runs.joinToString("\n") { el ->
            val o = el.jsonObject
            "${o.textC3("name")} | branch: ${o.textC3("head_branch")} | ${o.textC3("created_at")}\n${o.textC3("html_url")}"
        })
    }
}
