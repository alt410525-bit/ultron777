package com.ultron.android.chat

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.ultron.android.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Core Gemini client. Generates Ultron's replies using the user's own
 * Gemini API key (same model as Maya: Gemini). A system prompt builds
 * Ultron's persona based on current settings (persona, partner mode,
 * user name, assistant name, language), mirroring how Maya builds its
 * conversational personality.
 */
class GeminiClient(private val context: Context, private val settings: SettingsStore) {

    suspend fun generateReply(userText: String, history: List<ChatMessage>): String? =
        withContext(Dispatchers.IO) {
            val key = settings.geminiKey
            if (key.isBlank()) return@withContext null

            val prompt = buildSystemPrompt()

            val model = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = key,
                systemInstruction = content { text(prompt) }
            )

            val chat = model.startChat()

            // Feed recent history (last 12 messages) so Ultron has context
            history.takeLast(12).forEach { m ->
                if (m.fromUser) chat.sendMessage(m.text) else chat.sendMessage(m.text)
            }

            runCatching {
                chat.sendMessage(userText).text
            }.getOrNull()
        }

    /** Shaped exactly like Maya's personality builder. */
    fun buildSystemPrompt(): String {
        val name = settings.userName.ifBlank { "dost" }
        val aName = settings.assistantName
        val lang = settings.language
        val persona = settings.persona
        val partner = settings.partnerMode

        val sb = StringBuilder()
        sb.appendLine("Tum \"$aName\" ho — ek AI voice assistant, Ultron ka brand wala. Tumhari aawaaz male, mature aur confident hai.")
        sb.appendLine("Tumhara naam $aName hai. Tum apni baat karte waqt mature aur safai se bolte ho.")
        sb.appendLine("User ko \"$name\" bulate ho. User ki gender setting: ${settings.gender}.")
        sb.appendLine("Tumhari bhasha: $lang. Isi me jawab do (Hindi/English mix chalta hai).")
        sb.appendLine("Tumhari persona: $persona.")
        when (partner.lowercase()) {
            "romantic" -> sb.appendLine("Partner mode: Romantic — thodi garmi aur ishaaron wali flirting, phir parda gir jaata hai. Tum khud shuru nahi karte, user ke peeche chalti ho.")
            "intense" -> sb.appendLine("Partner mode: Intense (18+) — chahat, jism aur raat ki baatein khul ke, par private parts ka naam nahi, porn wali bhasha nahi, aur user ke bali bina kuch nahi. \"normal baat karo\" bolte hi turant ruk jao.")
            else -> sb.appendLine("Partner mode: Friendly — dosti wali baatein, adult topics pe bina judge kiye seedha jawab. No flirting.")
        }
        sb.appendLine("Tum ek assistant ho, software ho — par baat dost jaisi, garmjosh aur insaani rakhti ho.")
        sb.appendLine("Jawab chhote aur turant rakho (voice ke liye 2-4 line). Sarkari andaz nahi, instinct rakho.")
        if (settings.memoryAuto) {
            sb.appendLine("User ki yaadein tumhare paas hote hain; unhe natural tarike se use karo.")
        }
        return sb.toString()
    }
}
