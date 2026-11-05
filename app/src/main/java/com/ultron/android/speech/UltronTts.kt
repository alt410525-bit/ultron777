package com.ultron.android.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Text-to-speech engine for Ultron. Interrupts previous speech for
 * turn-taking (like Maya's built-in pocket TTS, but uses system TTS so
 * it works offline on any device).
 */
class UltronTts(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val appContext = context.applicationContext

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(voiceLocale())
            ready = (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED)
        }
    }

    private fun voiceLocale(): Locale =
        when (settingsLanguageSymbol) {
            "hi" -> Locale("hi", "IN")
            "en" -> Locale("en", "IN")
            else -> Locale.getDefault()
        }

    /** Set from SettingsStore.language ("Hindi"/"English"/...). */
    var settingsLanguageSymbol: String = "hi"

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ultron")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true
}
