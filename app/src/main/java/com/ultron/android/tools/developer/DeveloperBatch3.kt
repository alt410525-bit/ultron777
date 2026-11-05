package com.ultron.android.tools.developer

import com.ultron.android.core.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

private val httpDev3: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

class CodeCleanTool : Tool {
    override val id = "dev_code_clean"
    override val category = ToolCategory.DEVELOPER
    override val title = "Code clipboard clean"
    override val description = "Copied code se shell prompt ($ ya >) aur ``` fences hatao, trailing spaces saaf karo."
    override val params = listOf(
        ToolParam("text", "string", "Copied text", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val raw = args.str("text") ?: return ToolResult.Error("text nahi mila")
        val cleaned = raw.lines()
            .map { it.trimEnd() }
            .filterNot { it.trim().startsWith("```") }
            .map { it.replaceFirst(Regex("^\\s*(\\$ |> )"), "") }
            .joinToString("\n")
            .trim()
        return ToolResult.Ok(cleaned.ifBlank { "Clean karne ke baad kuch nahi bacha" })
    }
}

class WebhookTestTool : Tool {
    override val id = "dev_webhook_test"
    override val category = ToolCategory.DEVELOPER
    override val title = "Webhook test"
    override val description = "Ek https webhook URL par JSON POST bhejo aur response status dikhao. Pehle user se haan karwana zaroori hai."
    override val params = listOf(
        ToolParam("url", "string", "https:// wala webhook URL", required = true),
        ToolParam("payload", "string", "JSON body (optional, default ping)", required = false)
    )
    override val risk = RiskLevel.HIGH

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val url = args.str("url")?.trim() ?: return ToolResult.Error("url nahi mila")
        if (!url.startsWith("https://")) return ToolResult.Error("Sirf https:// URL allowed hai")
        val payload = args.str("payload")?.takeIf { it.isNotBlank() } ?: "{\"ping\":true}"
        return try {
            val (code, body) = withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url(url)
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()
                httpDev3.newCall(req).execute().use { resp ->
                    resp.code to resp.body?.string().orEmpty().take(200)
                }
            }
            ToolResult.Ok("Status $code. Response: $body")
        } catch (e: Exception) {
            ToolResult.Error("Webhook tak pahunch nahi paye: ${e.message}")
        }
    }
}

class PortStatusTool : Tool {
    override val id = "dev_port_status"
    override val category = ToolCategory.DEVELOPER
    override val title = "Localhost port check"
    override val description = "Phone ke localhost (127.0.0.1) par kisi port par server chal raha hai ya nahi, ye check karo."
    override val params = listOf(
        ToolParam("port", "integer", "Port number, 1 se 65535", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val port = args.int("port") ?: return ToolResult.Error("port nahi mila")
        if (port !in 1..65535) return ToolResult.Error("port 1 se 65535 ke beech hona chahiye")
        val open = withContext(Dispatchers.IO) {
            try {
                Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 500) }
                true
            } catch (e: Exception) {
                false
            }
        }
        return ToolResult.Ok(
            if (open) "Port $port par kuch chal raha hai" else "Port $port par kuch nahi chal raha"
        )
    }
}

private fun notesDirDev3(ctx: ToolContext): File =
    File(ctx.appContext.filesDir, "notes").apply { mkdirs() }

private fun noteFileDev3(ctx: ToolContext, name: String): File {
    val safe = name.lowercase().replace(Regex("[^a-z0-9_-]+"), "-").trim('-').take(60).ifBlank { "note" }
    return File(notesDirDev3(ctx), "$safe.md")
}

class NoteSaveTool : Tool {
    override val id = "dev_note_save"
    override val category = ToolCategory.DEVELOPER
    override val title = "Markdown note save"
    override val description = "Ek markdown note app ke andar save karo. Same naam par purana note overwrite ho jayega."
    override val params = listOf(
        ToolParam("name", "string", "Note ka naam", required = true),
        ToolParam("text", "string", "Note ka content (markdown)", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val name = args.str("name")?.takeIf { it.isNotBlank() } ?: return ToolResult.Error("naam nahi mila")
        val text = args.str("text") ?: return ToolResult.Error("text nahi mila")
        val file = noteFileDev3(ctx, name)
        withContext(Dispatchers.IO) { file.writeText(text) }
        return ToolResult.Ok("Note save ho gaya: ${file.name}")
    }
}

class NoteReadTool : Tool {
    override val id = "dev_note_read"
    override val category = ToolCategory.DEVELOPER
    override val title = "Markdown note padho"
    override val description = "App mein save kiya hua note naam se padhkar sunao."
    override val params = listOf(
        ToolParam("name", "string", "Note ka naam", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val name = args.str("name")?.takeIf { it.isNotBlank() } ?: return ToolResult.Error("naam nahi mila")
        val file = noteFileDev3(ctx, name)
        if (!file.exists()) return ToolResult.Error("ye note nahi mila")
        val text = withContext(Dispatchers.IO) { file.readText() }
        return ToolResult.Ok(text.take(3000))
    }
}

class JsonToKotlinTool : Tool {
    override val id = "dev_json_to_kotlin"
    override val category = ToolCategory.DEVELOPER
    override val title = "JSON se Kotlin data class"
    override val description = "Ek JSON object se Kotlin data class ka code banao. Sirf top-level fields, nested objects ko Any maana jata hai."
    override val params = listOf(
        ToolParam("json", "string", "JSON object (curly braces wala)", required = true),
        ToolParam("className", "string", "Class ka naam (optional, default Root)", required = false)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val raw = args.str("json") ?: return ToolResult.Error("json nahi mila")
        val root = try {
            Json.parseToJsonElement(raw) as? JsonObject
        } catch (e: Exception) {
            return ToolResult.Error("JSON valid nahi hai")
        } ?: return ToolResult.Error("JSON ek object hona chahiye { ... }")

        val className = (args.str("className") ?: "Root")
            .filter { it.isLetterOrDigit() }
            .ifBlank { "Root" }
            .replaceFirstChar { it.uppercaseChar() }

        val sb = StringBuilder("data class $className(\n")
        root.forEach { (key, value) ->
            sb.append("    val ${propName(key)}: ${kotlinType(value)},\n")
        }
        sb.append(")")
        return ToolResult.Ok(sb.toString())
    }

    private fun propName(key: String): String {
        val parts = key.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "field"
        val name = parts.first().replaceFirstChar { it.lowercaseChar() } +
            parts.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
        return if (name[0].isDigit()) "_$name" else name
    }

    private fun kotlinType(v: JsonElement): String = when (v) {
        is JsonNull -> "String?"
        is JsonPrimitive -> when {
            v.isString -> "String"
            v.booleanOrNull != null -> "Boolean"
            v.longOrNull != null -> "Long"
            else -> "Double"
        }
        is JsonArray -> if (v.isEmpty()) "List<Any>" else "List<${kotlinType(v.first())}>"
        is JsonObject -> "Any"
    }
}
