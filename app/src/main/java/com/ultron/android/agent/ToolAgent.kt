package com.ultron.android.agent

import com.ultron.android.core.tools.*
import com.ultron.android.llm.*
import kotlinx.coroutines.CancellationException

class ToolAgent(
    private val llm: LlmProvider,
    private val registry: ToolRegistry,
    private val ctx: ToolContext,
    private val maxSteps: Int = 6
) {
    /** history mein System message pehle se ho sakta hai. Ye user message add karke reply deta hai. */
    suspend fun reply(history: MutableList<LlmMessage>, userText: String): String {
        history += LlmMessage.User(userText)
        val specs = registry.select(userText).map { ToolSpec(it.id, it.description, it.params) }

        repeat(maxSteps) {
            when (val r = llm.chat(history, specs)) {
                is LlmReply.Text -> {
                    history += LlmMessage.Assistant(r.text)
                    return r.text
                }
                is LlmReply.Calls -> {
                    history += LlmMessage.Assistant(r.preface, r.calls)
                    r.calls.forEach { call ->
                        history += LlmMessage.ToolOutput(call.id, call.name, runTool(call))
                    }
                }
            }
        }
        return "Ye kaam thoda bada ho gaya. Ise chhote steps mein batao."
    }

    private suspend fun runTool(call: ToolCall): String {
        val tool = registry.find(call.name) ?: return "Error: tool '${call.name}' nahi mila"

        if (tool.risk == RiskLevel.HIGH &&
            !ctx.confirm("Ultron ${tool.title} karna chahta hai.")
        ) return "User ne mana kar diya."

        return try {
            when (val r = tool.execute(call.args, ctx)) {
                is ToolResult.Ok -> r.text
                is ToolResult.Error -> "Error: ${r.message}"
                is ToolResult.NeedsPermission -> "Permission chahiye: ${r.permissions.joinToString()}"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
