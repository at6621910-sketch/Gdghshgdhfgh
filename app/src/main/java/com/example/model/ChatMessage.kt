package com.example.model

enum class SenderType {
    USER,
    JARVIS
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: SenderType,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

enum class AssistantState(val displayName: String, val hindiName: String) {
    READY("Assistant Ready", "Jarvis Tayyar Hai"),
    LISTENING("Listening...", "Sun raha hoon..."),
    THINKING("Thinking...", "AI soch raha hai..."),
    SPEAKING("Speaking...", "Bol raha hoon..."),
    STOPPED("Voice Stopped", "Awaaz band hui"),
    ERROR("Connection Required", "Error")
}
