package com.ultron.android.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps Android's SpeechRecognizer so the UI can subscribe to recogniser
 * state with coroutines. Mirrors how Maya captures Hindi/Hinglish voice.
 */
class VoiceRecognizer(context: Context) {

    private val appContext = context.applicationContext
    private val recognizer: SpeechRecognizer? =
        try { SpeechRecognizer.createSpeechRecognizer(appContext) } catch (e: Exception) { null }

    private val _state = MutableStateFlow<RecognizerState>(RecognizerState.Idle)
    val state: StateFlow<RecognizerState> = _state

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = RecognizerState.Listening()
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            (_state.value as? RecognizerState.Listening)?.volume = rmsdB
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = RecognizerState.Processing
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "Kuch samajh nahi aaya, phir try karo"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Abhi kuch suna nahi"
                SpeechRecognizer.ERROR_NETWORK -> "Internet chahiye"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission chahiye"
                else -> "Error $error. Dobara try karo."
            }
            _state.value = RecognizerState.Error(msg)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (text.isNullOrBlank()) {
                _state.value = RecognizerState.Error("Kuch suna nahi")
            } else {
                _state.value = RecognizerState.Result(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun start(localeTag: String = "hi-IN") {
        recognizer?.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        _state.value = RecognizerState.Idle
        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
    }
}

sealed class RecognizerState {
    object Idle : RecognizerState()
    class Listening(var volume: Float = 0f) : RecognizerState()
    object Processing : RecognizerState()
    data class Result(val text: String) : RecognizerState()
    data class Error(val message: String) : RecognizerState()
}
