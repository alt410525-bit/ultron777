package com.ultron.android.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ultron.android.agent.ToolAgent
import com.ultron.android.core.tools.ToolContext
import com.ultron.android.core.tools.ToolRegistry
import com.ultron.android.data.SettingsStore
import com.ultron.android.llm.GeminiRestProvider
import com.ultron.android.llm.LlmMessage
import com.ultron.android.tools.buildToolRegistry
import com.ultron.android.voice.VoiceMode
import com.ultron.android.voice.VoiceSettings
import com.ultron.android.voice.VoiceSystem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface UiPhase {
    object Idle : UiPhase
    object Listening : UiPhase
    object Thinking : UiPhase
    object Speaking : UiPhase
}

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    val settings = SettingsStore(app)
    private val gemini = GeminiClient(app, settings)
    private val voice = VoiceSystem(app)
    private val registry: ToolRegistry = buildToolRegistry(settings)
    private val llmHistory = mutableListOf<LlmMessage>()
    private var pendingConfirm: CompletableDeferred<Boolean>? = null
    private val toolContext = ToolContext(app) { question -> askConfirm(question) }
    private var responseJob: Job? = null
    private var thinking = false
    private var lastError: String? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _phase = MutableStateFlow<UiPhase>(UiPhase.Idle)
    val phase: StateFlow<UiPhase> = _phase.asStateFlow()

    private val _micError = MutableStateFlow<String?>(null)
    val micError: StateFlow<String?> = _micError.asStateFlow()

    private val _recognizerActive = MutableStateFlow(false)
    val recognizerActive: StateFlow<Boolean> = _recognizerActive.asStateFlow()

    private val _recognizerVolume = MutableStateFlow(0f)
    val recognizerVolume: StateFlow<Float> = _recognizerVolume.asStateFlow()

    init {
        // continuous = true: ek baar mic dabane ke baad baat chalti rahegi
        // bargeIn = false: earphones ke bina echo se bachne ke liye. Settings se on kar sakte ho.
        voice.settings = VoiceSettings(continuous = true, bargeIn = false, rate = 0.9f, pitch = 0.6f)

        viewModelScope.launch {
            voice.finals.collect { text -> onUserSpeech(text) }
        }
        viewModelScope.launch {
            voice.state.collect { s ->
                _recognizerActive.value = s.mode == VoiceMode.LISTENING
                _recognizerVolume.value = s.volume
                if (s.error != null && s.error != lastError) {
                    lastError = s.error
                    _micError.value = s.error
                }
                if (s.error == null) lastError = null
                updatePhase()
            }
        }
    }

    /** Orb / mic dabaya. Agar Ultron bol raha hai to use rok kar sunna shuru karta hai (barge-in). */
    fun startListening() {
        _micError.value = null
        voice.applyLanguage(languageTag(settings.language))
        if (voice.state.value.mode == VoiceMode.SPEAKING) voice.interrupt()
        voice.startListening()
    }

    /** Mic band karo aur Ultron ko chup karao. */
    fun stopSession() {
        responseJob?.cancel()
        thinking = false
        pendingConfirm?.complete(false)
        pendingConfirm = null
        voice.stopListening()
        voice.interrupt()
        updatePhase()
    }

    fun sendText(text: String) {
        if (text.isNotBlank()) onUserSpeech(text.trim())
    }

    fun clearMicError() { _micError.value = null }

    fun clearChat() {
        stopSession()
        _messages.value = emptyList()
        llmHistory.clear()
    }

    private fun onUserSpeech(text: String) {
        addMsg(text, fromUser = true)

        // Ultron ne haan/nahi poocha hai to ye jawab usi ke liye hai
        pendingConfirm?.let { answer ->
            answer.complete(isYes(text))
            return
        }

        // User ne beech mein bola: Ultron ka bolna rok do
        if (voice.state.value.mode == VoiceMode.SPEAKING) voice.interrupt()

        responseJob?.cancel()
        thinking = true
        updatePhase()
        responseJob = viewModelScope.launch {
            val reply = runAgent(text)
            thinking = false
            if (reply.isNullOrBlank()) {
                updatePhase()
                if (_micError.value == null) {
                    _micError.value = "Ultron ne jawab nahi diya. Gemini key check karo (Settings → Personal)."
                }
                return@launch
            }
            addMsg(reply, fromUser = false)
            voice.speak(reply)
            updatePhase()
        }
    }

    private suspend fun runAgent(text: String): String? {
        if (settings.geminiKey.isBlank()) return null
        llmHistory.removeAll { it is LlmMessage.System }
        llmHistory.add(0, LlmMessage.System(gemini.buildSystemPrompt()))
        val agent = ToolAgent(GeminiRestProvider(settings.geminiKey), registry, toolContext)
        return try {
            agent.reply(llmHistory, text).also { trimHistory() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _micError.value = "AI se connect nahi ho paaya: ${e.message}"
            null
        }
    }

    /** Tool ke liye haan/nahi: bolkar ya likhkar dono chalte hain. */
    private suspend fun askConfirm(question: String): Boolean {
        val answer = CompletableDeferred<Boolean>()
        pendingConfirm = answer
        val prompt = "$question Haan ya nahi bolo."
        addMsg(prompt, fromUser = false)
        voice.speak(prompt)
        voice.startListening() // jawab ke liye mic khud on
        val ok = withTimeoutOrNull(30_000) { answer.await() } ?: false
        pendingConfirm = null
        return ok
    }

    private fun isYes(text: String): Boolean {
        val tokens = text.lowercase().split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }.toSet()
        val yes = setOf("haan", "ha", "yes", "kar", "karo", "do", "bhejo", "theek", "ok", "okay", "sahi")
        val no = setOf("nahi", "mat", "no", "cancel", "ruko", "rehne")
        return tokens.any { it in yes } && tokens.none { it in no }
    }

    private fun trimHistory() {
        val limit = 40
        while (llmHistory.count { it !is LlmMessage.System } > limit) {
            val idx = llmHistory.indexOfFirst { it !is LlmMessage.System }
            if (idx < 0) break
            llmHistory.removeAt(idx)
            while (idx < llmHistory.size && llmHistory[idx] !is LlmMessage.User &&
                llmHistory[idx] !is LlmMessage.System
            ) {
                llmHistory.removeAt(idx)
            }
        }
    }

    private fun languageTag(lang: String): String = when (lang.lowercase()) {
        "hindi" -> "hi-IN"
        else -> "en-IN"
    }

    private fun updatePhase() {
        val mode = voice.state.value.mode
        _phase.value = when {
            thinking -> UiPhase.Thinking
            mode == VoiceMode.SPEAKING -> UiPhase.Speaking
            mode == VoiceMode.LISTENING -> UiPhase.Listening
            else -> UiPhase.Idle
        }
    }

    private fun addMsg(text: String, fromUser: Boolean) {
        _messages.value = _messages.value + ChatMessage(
            id = System.currentTimeMillis(),
            text = text,
            fromUser = fromUser
        )
    }

    override fun onCleared() {
        super.onCleared()
        voice.release()
    }
}
