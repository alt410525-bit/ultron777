package com.ultron.android.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Gemini REST API with function calling. Same key jo Settings → Personal mein hai. */
class GeminiRestProvider(
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash",
    private val http: OkHttpClient = OkHttpClient()
) : LlmProvider {

    override val name = "Gemini"

    override suspend fun chat(messages: List<LlmMessage>, tools: List<ToolSpec>): LlmReply =
        withContext(Dispatchers.IO) {
            val system = messages.filterIsInstance<LlmMessage.System>().joinToString("\n") { it.text }
            val conversation = messages.filterNot { it is LlmMessage.System }

            val body = buildJsonObject {
                if (system.isNotBlank()) {
                    putJsonObject("systemInstruction") {
                        putJsonArray("parts") { add(buildJsonObject { put("text", system) }) }
                    }
                }
                putJsonArray("contents") { conversation.forEach { add(it.toGemini()) } }
                if (tools.isNotEmpty()) {
                    putJsonArray("tools") {
                        add(buildJsonObject {
                            putJsonArray("functionDeclarations") { tools.forEach { add(it.toGemini()) } }
                        })
                    }
                }
            }

            val req = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .header("x-goog-api-key", apiKey)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw LlmException("Gemini HTTP ${resp.code}: ${raw.take(200)}")
                parse(Json.parseToJsonElement(raw).jsonObject)
            }
        }

    private fun parse(root: JsonObject): LlmReply {
        val parts = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?: return LlmReply.Text("")
        val text = parts.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }.joinToString("")
        val calls = parts.mapIndexedNotNull { i, el ->
            val fc = el.jsonObject["functionCall"]?.jsonObject ?: return@mapIndexedNotNull null
            val n = fc["name"]!!.jsonPrimitive.content
            ToolCall(
                id = "${n}_$i",
                name = n,
                args = fc["args"] as? JsonObject ?: JsonObject(emptyMap())
            )
        }
        return if (calls.isNotEmpty()) LlmReply.Calls(calls, text.ifBlank { null })
        else LlmReply.Text(text)
    }
}

private fun LlmMessage.toGemini(): JsonObject = when (this) {
    is LlmMessage.System -> error("system message alag se bheja jata hai")
    is LlmMessage.User -> buildJsonObject {
        put("role", "user")
        putJsonArray("parts") { add(buildJsonObject { put("text", text) }) }
    }
    is LlmMessage.Assistant -> buildJsonObject {
        put("role", "model")
        putJsonArray("parts") {
            if (!text.isNullOrBlank()) add(buildJsonObject { put("text", text.orEmpty()) })
            calls.forEach { c ->
                add(buildJsonObject {
                    putJsonObject("functionCall") {
                        put("name", c.name)
                        put("args", c.args)
                    }
                })
            }
        }
    }
    is LlmMessage.ToolOutput -> buildJsonObject {
        put("role", "user")
        putJsonArray("parts") {
            add(buildJsonObject {
                putJsonObject("functionResponse") {
                    put("name", name)
                    putJsonObject("response") { put("result", result) }
                }
            })
        }
    }
}

private fun ToolSpec.toGemini(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    if (params.isNotEmpty()) {
        putJsonObject("parameters") {
            put("type", "OBJECT")
            putJsonObject("properties") {
                params.forEach { p ->
                    putJsonObject(p.name) {
                        put("type", p.type.geminiType())
                        put("description", p.description)
                    }
                }
            }
            val required = params.filter { it.required }
            if (required.isNotEmpty()) {
                putJsonArray("required") { required.forEach { add(JsonPrimitive(it.name)) } }
            }
        }
    }
}

private fun String.geminiType(): String = when (this) {
    "number" -> "NUMBER"
    "integer" -> "INTEGER"
    "boolean" -> "BOOLEAN"
    else -> "STRING"
}
