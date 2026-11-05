package com.ultron.android.core.tools

import android.content.Context
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

enum class ToolCategory(val label: String) {
    ASSISTANT("Assistant & Persona"),
    PERSONAL("Personal & API"),
    MESSAGING("Messaging & Communication"),
    CONNECTORS("Connectors & Developer"),
    SYSTEM("Advanced & System"),
    DEVICE("Device & Hardware"),
    DEVELOPER("Developer & Productivity"),
    UTILITY("Utility & Media")
}

enum class RiskLevel { LOW, MEDIUM, HIGH }

data class ToolParam(
    val name: String,
    val type: String,          // "string", "number", "integer", "boolean"
    val description: String,
    val required: Boolean = false
)

sealed class ToolResult {
    data class Ok(val text: String) : ToolResult()
    data class Error(val message: String) : ToolResult()
    data class NeedsPermission(val permissions: List<String>) : ToolResult()
}

class ToolContext(
    val appContext: Context,
    /** User se haan/nahi poochta hai. true = allow. */
    val confirm: suspend (String) -> Boolean
)

interface Tool {
    val id: String
    val category: ToolCategory
    val title: String
    val description: String
    val params: List<ToolParam>
    val risk: RiskLevel get() = RiskLevel.LOW
    val permissions: List<String> get() = emptyList()

    suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult
}

fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
fun JsonObject.number(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull
fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
