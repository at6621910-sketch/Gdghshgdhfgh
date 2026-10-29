package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class JarvisSpeechManager(
    private val context: Context,
    private val onInitComplete: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val tag = "JarvisSpeech"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    var isSpeakingCallback: ((Boolean) -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize TTS", e)
            onInitComplete(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupUtteranceListener()
            Log.d(tag, "TextToSpeech initialized successfully")
            onInitComplete(true)
        } else {
            isInitialized = false
            Log.e(tag, "TTS init error: $status")
            onInitComplete(false)
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeakingCallback?.invoke(true)
            }

            override fun onDone(utteranceId: String?) {
                isSpeakingCallback?.invoke(false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeakingCallback?.invoke(false)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeakingCallback?.invoke(false)
                Log.e(tag, "TTS utterance error: $errorCode for id $utteranceId")
            }
        })
    }

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        languagePreference: String = "auto"
    ) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "TTS not ready")
            return
        }

        stop()

        val locale = when (languagePreference.lowercase()) {
            "hi", "hindi", "hi-in" -> Locale.forLanguageTag("hi-IN")
            "en", "english", "en-us" -> Locale.US
            "en-in" -> Locale.forLanguageTag("en-IN")
            else -> {
                // Auto-detect based on text content
                if (containsDevanagari(text) || isHindiPhonetic(text)) {
                    Locale.forLanguageTag("hi-IN")
                } else {
                    Locale.forLanguageTag("en-IN")
                }
            }
        }

        val available = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (available >= TextToSpeech.LANG_AVAILABLE) {
            tts?.language = locale
        } else {
            tts?.language = Locale.US
        }

        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        tts?.setPitch(1.0f)

        val utteranceId = "jarvis_reply_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            isSpeakingCallback?.invoke(false)
        } catch (e: Exception) {
            Log.e(tag, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(tag, "Error shutting down TTS", e)
        }
    }

    private fun containsDevanagari(text: String): Boolean {
        for (char in text) {
            val block = Character.UnicodeBlock.of(char)
            if (block == Character.UnicodeBlock.DEVANAGARI) {
                return true
            }
        }
        return false
    }

    private fun isHindiPhonetic(text: String): Boolean {
        val lower = text.lowercase()
        val hindiKeywords = listOf(
            "namaste", "aap", "kaise", "haan", "nahi", "kya", "kyu", "kyun",
            "hoon", "hai", "mujhe", "tum", "batao", "shukriya", "dhanyawad",
            "theek", "haal", "kuch", "sawaal", "soch", "raha"
        )
        return hindiKeywords.any { lower.contains(it) }
    }
}
