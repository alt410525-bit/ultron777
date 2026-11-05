package com.ultron.android.tools.developer

import com.ultron.android.core.tools.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

private val D = ToolCategory.DEVELOPER
private val pretty = Json { prettyPrint = true }

private fun hash(algo: String, input: String): String =
    MessageDigest.getInstance(algo).digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

class DeveloperToolModule : ToolModule {
    override val tools: List<Tool> = listOf(
        SpecTool(
            id = "dev_base64_encode", category = D, title = "Base64 encode",
            description = "Text ko Base64 mein encode karo.",
            params = listOf(ToolParam("text", "string", "Text", required = true))
        ) { args, _ ->
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            ToolResult.Ok(Base64.getEncoder().encodeToString(t.toByteArray(Charsets.UTF_8)))
        },
        SpecTool(
            id = "dev_base64_decode", category = D, title = "Base64 decode",
            description = "Base64 string ko wapas text mein decode karo.",
            params = listOf(ToolParam("text", "string", "Base64 string", required = true))
        ) { args, _ ->
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            ToolResult.Ok(String(Base64.getDecoder().decode(t.trim()), Charsets.UTF_8))
        },
        SpecTool(
            id = "dev_url_encode", category = D, title = "URL encode",
            description = "Text ko URL-safe percent encoding mein badlo.",
            params = listOf(ToolParam("text", "string", "Text", required = true))
        ) { args, _ ->
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            ToolResult.Ok(URLEncoder.encode(t, "UTF-8"))
        },
        SpecTool(
            id = "dev_url_decode", category = D, title = "URL decode",
            description = "Percent-encoded URL text ko normal text mein badlo.",
            params = listOf(ToolParam("text", "string", "Encoded text", required = true))
        ) { args, _ ->
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            ToolResult.Ok(URLDecoder.decode(t, "UTF-8"))
        },
        SpecTool(
            id = "dev_hash", category = D, title = "Hash generator",
            description = "Text ka hash nikalo: md5, sha1, sha256 ya sha512.",
            params = listOf(
                ToolParam("text", "string", "Text", required = true),
                ToolParam("algorithm", "string", "md5, sha1, sha256 ya sha512 (default sha256)")
            )
        ) { args, _ ->
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            val algo = when (args.str("algorithm")?.lowercase() ?: "sha256") {
                "md5" -> "MD5"
                "sha1" -> "SHA-1"
                "sha256" -> "SHA-256"
                "sha512" -> "SHA-512"
                else -> return@SpecTool ToolResult.Error("algorithm md5, sha1, sha256 ya sha512 ho")
            }
            ToolResult.Ok("$algo: ${hash(algo, t)}")
        },
        SpecTool(
            id = "dev_json_format", category = D, title = "JSON formatter",
            description = "JSON ko indent karke sundar format mein dikhao.",
            params = listOf(ToolParam("json", "string", "JSON text", required = true))
        ) { args, _ ->
            val j = args.str("json") ?: return@SpecTool ToolResult.Error("json chahiye")
            val el: JsonElement = Json.parseToJsonElement(j)
            ToolResult.Ok(pretty.encodeToString(JsonElement.serializer(), el))
        },
        SpecTool(
            id = "dev_json_validate", category = D, title = "JSON validator",
            description = "Check karo JSON valid hai ya nahi, aur galti ho to batao.",
            params = listOf(ToolParam("json", "string", "JSON text", required = true))
        ) { args, _ ->
            val j = args.str("json") ?: return@SpecTool ToolResult.Error("json chahiye")
            try {
                Json.parseToJsonElement(j)
                ToolResult.Ok("JSON valid hai.")
            } catch (e: Exception) {
                ToolResult.Ok("JSON invalid hai: ${e.message}")
            }
        },
        SpecTool(
            id = "dev_uuid", category = D, title = "UUID generator",
            description = "Ek ya zyada random UUID generate karo.",
            params = listOf(ToolParam("count", "integer", "Kitne UUID (1-20, default 1)"))
        ) { args, _ ->
            val n = (args.int("count") ?: 1).coerceIn(1, 20)
            ToolResult.Ok(List(n) { UUID.randomUUID().toString() }.joinToString("\n"))
        },
        SpecTool(
            id = "dev_regex_test", category = D, title = "Regex tester",
            description = "Regex pattern ko text par test karo aur matches dikhao.",
            params = listOf(
                ToolParam("pattern", "string", "Regex pattern", required = true),
                ToolParam("text", "string", "Test text", required = true)
            )
        ) { args, _ ->
            val p = args.str("pattern") ?: return@SpecTool ToolResult.Error("pattern chahiye")
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            val matches = Regex(p).findAll(t).map { it.value }.toList()
            ToolResult.Ok(
                if (matches.isEmpty()) "Koi match nahi mila."
                else "${matches.size} match(es):\n" + matches.joinToString("\n")
            )
        },
        SpecTool(
            id = "dev_text_diff", category = D, title = "Text diff",
            description = "Do text ke beech line-wise difference dikhao (jo sirf A mein, jo sirf B mein).",
            params = listOf(
                ToolParam("a", "string", "Pehla text", required = true),
                ToolParam("b", "string", "Doosra text", required = true)
            )
        ) { args, _ ->
            val a = (args.str("a") ?: return@SpecTool ToolResult.Error("a chahiye")).lines()
            val b = (args.str("b") ?: return@SpecTool ToolResult.Error("b chahiye")).lines()
            val onlyA = a.filter { it !in b }
            val onlyB = b.filter { it !in a }
            if (onlyA.isEmpty() && onlyB.isEmpty()) ToolResult.Ok("Dono texts same hain.")
            else ToolResult.Ok(
                buildString {
                    onlyA.forEach { appendLine("- $it") }
                    onlyB.forEach { appendLine("+ $it") }
                }.trimEnd()
            )
        }
    )
}
