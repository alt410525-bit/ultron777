package com.ultron.android.tools.personal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.MediaStore
import com.ultron.android.core.tools.*
import com.ultron.android.data.ConnectorSecrets
import com.ultron.android.data.SettingsStore
import kotlinx.serialization.json.JsonObject

class ProfileGetTool(private val settings: SettingsStore) : Tool {
    override val id = "personal_profile_get"
    override val category = ToolCategory.PERSONAL
    override val title = "Profile dekho"
    override val description = "User ka naam, assistant ka naam, bhasha aur persona batao."
    override val params = emptyList<ToolParam>()

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult =
        ToolResult.Ok(
            "User: ${settings.userName.ifBlank { "set nahi" }}, " +
                "Assistant: ${settings.assistantName}, " +
                "Bhasha: ${settings.language}, " +
                "Persona: ${settings.persona}"
        )
}

class ProfileSetNameTool(private val settings: SettingsStore) : Tool {
    override val id = "personal_profile_set_name"
    override val category = ToolCategory.PERSONAL
    override val title = "Naam badlo"
    override val description = "User ka naam set ya badal do jo Ultron use karega."
    override val params = listOf(ToolParam("name", "string", "User ka naam", required = true))

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val name = args.str("name")?.trim()?.takeIf { it.isNotEmpty() }
            ?: return ToolResult.Error("naam nahi mila")
        settings.userName = name
        return ToolResult.Ok("Theek hai, ab main tumhe $name bulaunga")
    }
}

class ApiKeysStatusTool(private val settings: SettingsStore) : Tool {
    override val id = "personal_api_keys_status"
    override val category = ToolCategory.PERSONAL
    override val title = "API keys ka status"
    override val description = "Batao kaun si API keys set hain (Gemini, GitHub, Telegram, Notion). Keys ki value kabhi nahi bolni."
    override val params = emptyList<ToolParam>()

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val app = ctx.appContext
        val rows = listOf(
            "Gemini" to settings.geminiKey.isNotBlank(),
            "GitHub" to ConnectorSecrets.get(app, ConnectorSecrets.GITHUB).isNotBlank(),
            "Telegram" to ConnectorSecrets.get(app, ConnectorSecrets.TELEGRAM).isNotBlank(),
            "Notion" to ConnectorSecrets.get(app, ConnectorSecrets.NOTION).isNotBlank()
        )
        val text = rows.joinToString("\n") { (name, set) -> "$name: ${if (set) "set hai" else "set nahi"}" }
        return ToolResult.Ok(text + "\nOpenRouter, Groq aur Claude abhi app mein store nahi hote.")
    }
}

private fun playMusicSearch(ctx: ToolContext, settings: SettingsStore, query: String): ToolResult {
    val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
        putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
        putExtra(MediaStore.EXTRA_MEDIA_TITLE, query)
        putExtra("query", query)
        // musicApp mein package name ho to usi app mein kholo
        settings.musicApp.takeIf { it.contains('.') }?.let { setPackage(it) }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        ctx.appContext.startActivity(intent)
        ToolResult.Ok("\"$query\" music app mein search kar diya")
    } catch (e: ActivityNotFoundException) {
        ToolResult.Error("Music app nahi mila. Settings → Personal mein music app set karo.")
    }
}

class MusicSearchTool(private val settings: SettingsStore) : Tool {
    override val id = "music_play_search"
    override val category = ToolCategory.PERSONAL
    override val title = "Gaana chalao"
    override val description = "Music app mein gaana ya artist search karke chalu karo. Naam do."
    override val params = listOf(ToolParam("query", "string", "Gaane ya artist ka naam", required = true))

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val query = args.str("query")?.takeIf { it.isNotBlank() } ?: return ToolResult.Error("query nahi mila")
        return playMusicSearch(ctx, settings, query)
    }
}

class MusicFavoriteTool(private val settings: SettingsStore) : Tool {
    override val id = "music_play_favorite"
    override val category = ToolCategory.PERSONAL
    override val title = "Favorite gaana chalao"
    override val description = "User ka favorite gaana chalao. Ye Settings → Personal mein set hona chahiye."
    override val params = emptyList<ToolParam>()

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        if (settings.favoriteSong.isBlank()) return ToolResult.Error("Favorite song set nahi hai")
        return playMusicSearch(ctx, settings, settings.favoriteSong)
    }
}

class PersonalToolModule(private val settings: SettingsStore) : ToolModule {
    override val tools: List<Tool> = listOf(
        ProfileGetTool(settings),
        ProfileSetNameTool(settings),
        ApiKeysStatusTool(settings),
        MusicSearchTool(settings),
        MusicFavoriteTool(settings)
    )
}
