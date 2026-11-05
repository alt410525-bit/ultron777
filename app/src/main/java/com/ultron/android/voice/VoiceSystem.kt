package com.ultron.android.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

enum class VoiceMode { IDLE, LISTENING, SPEAKING }

data class VoiceState(
    val mode: VoiceMode = VoiceMode.IDLE,
    val partial: String = "",
    val volume: Float = 0f,
    val error: String? = null
)

data class VoiceSettings(
    /** true: ek baar mic dabane ke baad har jawab ke baad khud sunna shuru. */
    val continuous: Boolean = true,
    /** true: Ultron bolte waqt user bol sake aur Ultron ruk jaye. Earphones pe best. */
    val bargeIn: Boolean = false,
    val rate: Float = 1f,
    val pitch: Float = 1f,
    /** true: Iron Man jaisi processing (gehra EQ + room reverb) lagegi. */
    val cinematic: Boolean = true
)

/**
 * STT + TTS ek hi jagah.
 * - STT: SpeechRecognizer, partial results, RMS volume, continuous restart.
 * - TTS: sentence-by-sentence. Cinematic ON ho to har sentence pehle file mein banta hai,
 *   phir MediaPlayer par EQ + reverb ke saath bajta hai. OFF ho to seedha TTS bolta hai.
 * Saare SpeechRecognizer, TTS aur MediaPlayer calls main thread par hote hain.
 */
class VoiceSystem(private val context: Context) {

    private val main = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(VoiceState())
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _finals = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val finals: SharedFlow<String> = _finals.asSharedFlow()

    var settings = VoiceSettings()
    var languageTag: String = "en-IN"

    // STT
    private var recognizer: SpeechRecognizer? = null
    private var listenWanted = false
    private var recognizing = false
    private var emptyCycles = 0

    // TTS + playback
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val queue = ArrayDeque<String>()
    private var speaking = false
    private var player: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var reverb: PresetReverb? = null

    init {
        tts = TextToSpeech(context) { status ->
            main.post {
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    applyTtsSettings()
                    tts?.setOnUtteranceProgressListener(ttsProgress)
                    drain()
                }
            }
        }
    }

    // ---------- Public API ----------

    fun applyLanguage(tag: String) {
        languageTag = tag
        main.post { applyTtsSettings() }
    }

    fun updateSettings(s: VoiceSettings) {
        settings = s
        main.post { applyTtsSettings() }
    }

    fun startListening() {
        main.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.value = _state.value.copy(error = "Speech recognition is not available on this phone.")
                return@post
            }
            listenWanted = true
            emptyCycles = 0
            if (speaking && !settings.bargeIn) {
                _state.value = _state.value.copy(mode = VoiceMode.SPEAKING)
                return@post
            }
            beginRecognition()
        }
    }

    fun stopListening() {
        main.post {
            listenWanted = false
            if (recognizing) recognizer?.stopListening()
            else if (!speaking) _state.value = _state.value.copy(mode = VoiceMode.IDLE, volume = 0f)
        }
    }

    fun speak(text: String) {
        val parts = splitSentences(text)
        if (parts.isEmpty()) return
        main.post {
            queue.addAll(parts)
            drain()
        }
    }

    fun interrupt() {
        main.post {
            queue.clear()
            stopPlayback()
            if (speaking) tts?.stop()
            speaking = false
            if (listenWanted) beginRecognition()
            else _state.value = _state.value.copy(mode = VoiceMode.IDLE, volume = 0f)
        }
    }

    fun release() {
        main.post {
            stopPlayback()
            recognizer?.destroy()
            recognizer = null
            tts?.stop()
            tts?.shutdown()
            tts = null
        }
    }

    // ---------- STT ----------

    private fun beginRecognition() {
        if (recognizing) return
        val sr = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        recognizing = true
        if (!speaking) _state.value = _state.value.copy(mode = VoiceMode.LISTENING, partial = "", error = null)
        sr.startListening(intent)
    }

    private fun cancelRecognizer() {
        recognizer?.cancel()
        recognizing = false
    }

    private fun firstResult(bundle: Bundle?): String =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()

    private fun onRecognitionEnded() {
        recognizing = false
        _state.value = _state.value.copy(volume = 0f, partial = "")
        if (listenWanted && settings.continuous) {
            if (emptyCycles >= 3) {
                listenWanted = false
                emptyCycles = 0
                _state.value = _state.value.copy(
                    mode = VoiceMode.IDLE,
                    error = "I did not hear anything. Tap the orb to try again."
                )
                return
            }
            main.postDelayed({
                if (listenWanted && !recognizing && !(speaking && !settings.bargeIn)) beginRecognition()
            }, 400)
        } else {
            listenWanted = false
            if (!speaking) _state.value = _state.value.copy(mode = VoiceMode.IDLE)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onRmsChanged(rmsdB: Float) {
            _state.value = _state.value.copy(volume = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = firstResult(partialResults)
            if (text.isBlank()) return
            _state.value = _state.value.copy(partial = text)
            if (speaking && settings.bargeIn && text.trim().split(" ").size >= 2) interrupt()
        }

        override fun onResults(results: Bundle?) {
            val text = firstResult(results).trim()
            if (text.isNotEmpty()) {
                emptyCycles = 0
                _finals.tryEmit(text)
            } else {
                emptyCycles++
            }
            onRecognitionEnded()
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    emptyCycles++
                    null
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "Network issue. Offline voice may not be installed."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> null
                else -> "Voice error ($error)."
            }
            if (msg != null) _state.value = _state.value.copy(error = msg)
            onRecognitionEnded()
        }
    }

    // ---------- TTS settings ----------

    private fun applyTtsSettings() {
        val engine = tts ?: return
        val locale = Locale.forLanguageTag(languageTag)
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.forLanguageTag("en-IN"))
        }
        // Deep male voice: British (en-GB) ko pehle, warna koi bhi male voice
        val candidates = engine.voices
            ?.filter { it.locale.language == locale.language }
            ?.filter { it.name.contains("male", ignoreCase = true) && !it.name.contains("female", ignoreCase = true) }
            .orEmpty()
        val best = candidates.firstOrNull { it.locale.country == "GB" } ?: candidates.firstOrNull()
        best?.let { engine.voice = it }
        engine.setSpeechRate(settings.rate)
        engine.setPitch(settings.pitch)
    }

    // ---------- TTS queue ----------

    private fun drain() {
        if (speaking || !ttsReady) return
        val next = queue.removeFirstOrNull()
        if (next == null) {
            onSpeechFinished()
            return
        }
        speaking = true
        if (!settings.bargeIn && recognizing) cancelRecognizer()
        _state.value = _state.value.copy(mode = VoiceMode.SPEAKING, partial = "", volume = 0f)

        if (settings.cinematic) {
            // File mein banao, phir processing ke saath bajao
            val file = File(context.cacheDir, "ultron_${System.nanoTime()}.wav")
            val rc = tts?.synthesizeToFile(next, null, file, "syn|" + file.absolutePath)
            if (rc != TextToSpeech.SUCCESS) {
                tts?.speak(next, TextToSpeech.QUEUE_FLUSH, null, "pb|" + System.nanoTime())
            }
        } else {
            tts?.speak(next, TextToSpeech.QUEUE_FLUSH, null, "pb|" + System.nanoTime())
        }
    }

    private fun onSpeechFinished() {
        speaking = false
        if (listenWanted) beginRecognition()
        else _state.value = _state.value.copy(mode = VoiceMode.IDLE, volume = 0f)
    }

    private val ttsProgress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
            main.post {
                val id = utteranceId.orEmpty()
                if (id.startsWith("syn|")) {
                    playProcessed(File(id.removePrefix("syn|")))
                } else {
                    speaking = false
                    drain()
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            main.post { speaking = false; drain() }
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            main.post { if (!interrupted) { speaking = false; drain() } }
        }
    }

    // ---------- Cinematic playback (EQ + reverb) ----------

    private fun playProcessed(file: File) {
        if (!speaking) { file.delete(); return }
        stopPlayback()
        val mp = MediaPlayer()
        try {
            mp.setDataSource(file.absolutePath)
            mp.prepare()
            applyCinematicEffects(mp)
            mp.setOnCompletionListener {
                main.post {
                    stopPlayback()
                    file.delete()
                    speaking = false
                    drain()
                }
            }
            mp.setOnErrorListener { _, _, _ ->
                main.post { stopPlayback(); file.delete(); speaking = false; drain() }
                true
            }
            player = mp
            mp.start()
        } catch (e: Exception) {
            mp.release()
            file.delete()
            speaking = false
            drain()
        }
    }

    private fun applyCinematicEffects(mp: MediaPlayer) {
        val sid = mp.audioSessionId
        // EQ: low end ko bhari karo, high ko thoda kaato (gehri, robotic feel)
        try {
            equalizer = Equalizer(0, sid).apply {
                enabled = true
                for (band in 0 until numberOfBands.toInt()) {
                    val b = band.toShort()
                    val freq = getCenterFreq(b)            // milliHertz
                    val gain: Short = when {
                        freq < 300_000 -> 700                // 100-300 Hz: +7 dB
                        freq < 1_500_000 -> 250              // ~1 kHz: +2.5 dB
                        freq > 4_000_000 -> -400             // 4 kHz+: -4 dB
                        else -> 0
                    }.toShort()
                    setBandLevel(b, gain)
                }
            }
        } catch (e: Exception) { equalizer = null }

        // Reverb: chhota-sa bada room (aux effect, hal ka level)
        try {
            reverb = PresetReverb(0, 0).apply {
                preset = PresetReverb.PRESET_MEDIUMROOM
                enabled = true
            }
            mp.attachAuxEffect(reverb!!.id)
            mp.setAuxEffectSendLevel(0.2f)
        } catch (e: Exception) { reverb = null }
    }

    private fun stopPlayback() {
        player?.let {
            try { it.stop() } catch (e: Exception) {}
            it.release()
        }
        player = null
        equalizer?.release()
        equalizer = null
        reverb?.release()
        reverb = null
    }

    private fun splitSentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?\u0964\\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
