package com.ultron.android.tools.assistant

import com.ultron.android.core.tools.*
import com.ultron.android.data.SettingsStore
import kotlinx.serialization.json.JsonObject

class AssistantSettingsGetTool(private val settings: SettingsStore) : Tool {
    override val id = "assistant_settings_get"
    override val category = ToolCategory.ASSISTANT
    override val title = "Assistant settings"
    override val description = "Ultron ki current bhasha, tone aur voice batao."
    override val params = emptyList<ToolParam>()

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult =
        ToolResult.Ok(
            "Bhasha: ${settings.language}, Tone: ${settings.partnerMode}, " +
                "Voice: ${settings.selectedVoice}, Persona: ${settings.persona}"
        )
}

class AssistantSetLanguageTool(private val settings: SettingsStore) : Tool {
    override val id = "assistant_set_language"
    override val category = ToolCategory.ASSISTANT
    override val title = "Bhasha badlo"
    override val description = "Ultron ki bhasha badlo. Allowed: Hindi, English, Hinglish."
    override val params = listOf(ToolParam("language", "string", "Hindi, English ya Hinglish", required = true))

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val raw = args.str("language")?.trim()?.lowercase() ?: return ToolResult.Error("language nahi mila")
        val normalized = when (raw) {
            "hindi" -> "Hindi"
            "english" -> "English"
            "hinglish" -> "Hinglish"
            else -> return ToolResult.Error("Ye bhasha abhi supported nahi hai")
        }
        settings.language = normalized
        return ToolResult.Ok("Bhasha ab $normalized hai")
    }
}

/**
 * Tone sirf Friendly ya Romantic. Intense (18+) mode yahan se kabhi nahi badla jayega,
 * wo Settings ke adult-confirm flow se hi hoga.
 */
class AssistantSetToneTool(private val settings: SettingsStore) : Tool {
    override val id = "assistant_set_tone"
    override val category = ToolCategory.ASSISTANT
    override val title = "Tone badlo"
    override val description = "Ultron ka tone badlo: friendly ya romantic. Intense mode yahan se nahi badalta."
    override val params = listOf(ToolParam("tone", "string", "friendly ya romantic", required = true))

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val tone = args.str("tone")?.trim()?.lowercase() ?: return ToolResult.Error("tone nahi mila")
        return when (tone) {
            "friendly", "romantic" -> {
                settings.partnerMode = tone
                ToolResult.Ok("Tone ab $tone hai")
            }
            "intense", "18+" -> ToolResult.Error("Intense mode Settings se hi badla ja sakta hai")
            else -> ToolResult.Error("Sirf friendly ya romantic set ho sakta hai")
        }
    }
}

class AssistantToolModule(private val settings: SettingsStore) : ToolModule {
    override val tools: List<Tool> = listOf(
        AssistantSettingsGetTool(settings),
        AssistantSetLanguageTool(settings),
        AssistantSetToneTool(settings)
    )
}
