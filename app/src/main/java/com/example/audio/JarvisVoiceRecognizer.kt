package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class JarvisVoiceRecognizer(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onRmsChanged: (Float) -> Unit
) : RecognitionListener {

    private val tag = "JarvisRecognizer"
    private var speechRecognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@JarvisVoiceRecognizer)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to create SpeechRecognizer", e)
            }
        } else {
            Log.w(tag, "SpeechRecognizer is not available on this device")
        }
    }

    fun startListening(languageCode: String = "hi-IN") {
        stopListening()

        if (speechRecognizer == null) {
            initRecognizer()
        }

        if (speechRecognizer == null) {
            onError("Speech recognition service is not available on this device.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "en-US"))
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            onListeningChanged(true)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start listening", e)
            isListening = false
            onListeningChanged(false)
            onError("Microphone error: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(tag, "Error stopping SpeechRecognizer", e)
            }
            isListening = false
            onListeningChanged(false)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(tag, "Error destroying SpeechRecognizer", e)
        }
        isListening = false
        onListeningChanged(false)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(tag, "Ready for speech")
        isListening = true
        onListeningChanged(true)
    }

    override fun onBeginningOfSpeech() {
        Log.d(tag, "Speech beginning")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Normalize roughly between 0.0 and 1.0 (typical range -2 to 10 dB)
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1.0f)
        onRmsChanged(normalized)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(tag, "End of speech")
        isListening = false
        onListeningChanged(false)
    }

    override fun onError(error: Int) {
        isListening = false
        onListeningChanged(false)
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client speech error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Speech recognition error ($error)"
        }
        Log.w(tag, "Speech recognition error: $message")
        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            onError(message)
        }
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        onListeningChanged(false)
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0]
            Log.d(tag, "Speech recognized: $spokenText")
            onResult(spokenText)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        // Can be used for live transcription feedback
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
