package com.ultron.android.core.tools

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

/**
 * Ek lambda se banne wala tool. Har naya tool sirf ek SpecTool { args, ctx -> ... } block hai.
 *
 * - `permissions` mein sirf RUNTIME permissions daalo (jaise Manifest.permission.READ_CONTACTS).
 *   Special permissions (WRITE_SETTINGS, notification policy) tool ke andar check karo.
 * - Exception aaye to ToolResult.Error ban jaata hai, crash nahi hota.
 */
class SpecTool(
    override val id: String,
    override val category: ToolCategory,
    override val title: String,
    override val description: String,
    override val params: List<ToolParam> = emptyList(),
    override val risk: RiskLevel = RiskLevel.LOW,
    override val permissions: List<String> = emptyList(),
    private val run: suspend (JsonObject, ToolContext) -> ToolResult
) : Tool {

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(ctx.appContext, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) return ToolResult.NeedsPermission(missing)
        return try {
            run(args, ctx)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            ToolResult.Error("Permission ki wajah se ye kaam nahi ho paya: ${e.message}")
        } catch (e: Exception) {
            ToolResult.Error(e.message ?: "Kuch galat ho gaya")
        }
    }
}

/** Ek module ki list ko registry mein jodne ke liye helper. */
fun specModule(vararg tools: Tool): ToolModule = object : ToolModule {
    override val tools: List<Tool> = tools.toList()
}
