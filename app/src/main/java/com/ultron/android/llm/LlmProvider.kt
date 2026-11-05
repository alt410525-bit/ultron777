package com.ultron.android.llm

import com.ultron.android.core.tools.ToolParam
import kotlinx.serialization.json.JsonObject

data class ToolSpec(val name: String, val description: String, val params: List<ToolParam>)

data class ToolCall(val id: String, val name: String, val args: JsonObject)

sealed class LlmMessage {
    data class System(val text: String) : LlmMessage()
    data class User(val text: String) : LlmMessage()
    data class Assistant(val text: String?, val calls: List<ToolCall> = emptyList()) : LlmMessage()
    data class ToolOutput(val callId: String, val name: String, val result: String) : LlmMessage()
}

sealed class LlmReply {
    data class Text(val text: String) : LlmReply()
    data class Calls(val calls: List<ToolCall>, val preface: String?) : LlmReply()
}

class LlmException(msg: String) : Exception(msg)

interface LlmProvider {
    val name: String
    suspend fun chat(messages: List<LlmMessage>, tools: List<ToolSpec>): LlmReply
}
