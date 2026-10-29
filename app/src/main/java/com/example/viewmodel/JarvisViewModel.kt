package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.JarvisSpeechManager
import com.example.audio.JarvisVoiceRecognizer
import com.example.brain.JarvisBrain
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.model.SenderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JarvisUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = SenderType.JARVIS,
            text = "Namaste! Main aapka AI assistant hoon. Mujhse kuch bhi pooch sakte ho."
        )
    ),
    val assistantState: AssistantState = AssistantState.READY,
    val inputText: String = "",
    val audioLevel: Float = 0.2f,
    val isTtsSpeaking: Boolean = false,
    val speechRate: Float = 1.0f,
    val speechLanguage: String = "hi-IN",
    val autoSpeak: Boolean = true,
    val isGeminiConnected: Boolean = false,
    val customApiKey: String = "",
    val errorMessage: String? = null
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val brain = JarvisBrain(application.applicationContext)
    private val speechManager = JarvisSpeechManager(application.applicationContext)
    private var voiceRecognizer: JarvisVoiceRecognizer? = null

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    init {
        speechManager.isSpeakingCallback = { speaking ->
            _uiState.value = _uiState.value.copy(
                isTtsSpeaking = speaking,
                assistantState = if (speaking) AssistantState.SPEAKING else AssistantState.READY
            )
        }

        voiceRecognizer = JarvisVoiceRecognizer(
            context = application.applicationContext,
            onResult = { spokenText ->
                _uiState.value = _uiState.value.copy(
                    inputText = spokenText,
                    assistantState = AssistantState.READY
                )
                sendMessage(spokenText)
            },
            onError = { errorMsg ->
                _uiState.value = _uiState.value.copy(
                    assistantState = AssistantState.ERROR,
                    errorMessage = errorMsg
                )
            },
            onListeningChanged = { listening ->
                _uiState.value = _uiState.value.copy(
                    assistantState = if (listening) AssistantState.LISTENING else AssistantState.READY
                )
            },
            onRmsChanged = { rms ->
                _uiState.value = _uiState.value.copy(audioLevel = rms)
            }
        )

        _uiState.value = _uiState.value.copy(
            isGeminiConnected = brain.isGeminiConfigured
        )
    }

    fun onInputTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText)
    }

    fun sendMessage(textOverride: String? = null) {
        val query = (textOverride ?: _uiState.value.inputText).trim()
        if (query.isEmpty()) return

        // Stop any ongoing voice or speech
        speechManager.stop()
        voiceRecognizer?.stopListening()

        val userMessage = ChatMessage(sender = SenderType.USER, text = query)
        val updatedList = _uiState.value.messages + userMessage

        _uiState.value = _uiState.value.copy(
            messages = updatedList,
            inputText = "",
            assistantState = AssistantState.THINKING,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val reply = brain.processQuery(query)
                val botMessage = ChatMessage(sender = SenderType.JARVIS, text = reply)
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + botMessage,
                    assistantState = AssistantState.READY
                )

                if (_uiState.value.autoSpeak) {
                    speechManager.speak(
                        text = reply,
                        speechRate = _uiState.value.speechRate,
                        languagePreference = _uiState.value.speechLanguage
                    )
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    sender = SenderType.JARVIS,
                    text = "AI server connect nahi hai. Backend aur AI API setup karna baaki hai.",
                    isError = true
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + errorMessage,
                    assistantState = AssistantState.ERROR,
                    errorMessage = e.localizedMessage
                )
            }
        }
    }

    fun startVoiceInput() {
        speechManager.stop()
        _uiState.value = _uiState.value.copy(
            assistantState = AssistantState.LISTENING,
            errorMessage = null
        )
        voiceRecognizer?.startListening(languageCode = _uiState.value.speechLanguage)
    }

    fun stopVoice() {
        speechManager.stop()
        voiceRecognizer?.stopListening()
        _uiState.value = _uiState.value.copy(
            assistantState = AssistantState.STOPPED,
            isTtsSpeaking = false
        )
    }

    fun speakMessage(text: String) {
        speechManager.speak(
            text = text,
            speechRate = _uiState.value.speechRate,
            languagePreference = _uiState.value.speechLanguage
        )
    }

    fun setSpeechRate(rate: Float) {
        _uiState.value = _uiState.value.copy(speechRate = rate)
    }

    fun setSpeechLanguage(lang: String) {
        _uiState.value = _uiState.value.copy(speechLanguage = lang)
    }

    fun setAutoSpeak(auto: Boolean) {
        _uiState.value = _uiState.value.copy(autoSpeak = auto)
    }

    fun setCustomApiKey(key: String) {
        brain.customApiKey = key.trim()
        _uiState.value = _uiState.value.copy(
            customApiKey = key.trim(),
            isGeminiConnected = brain.isGeminiConfigured
        )
    }

    fun clearChat() {
        speechManager.stop()
        _uiState.value = _uiState.value.copy(
            messages = listOf(
                ChatMessage(
                    sender = SenderType.JARVIS,
                    text = "Namaste! Main aapka AI assistant hoon. Mujhse kuch bhi pooch sakte ho."
                )
            ),
            assistantState = AssistantState.READY
        )
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
        voiceRecognizer?.destroy()
    }
}
