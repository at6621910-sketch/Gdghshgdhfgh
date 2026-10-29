package com.example

import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.model.SenderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testChatMessageCreation() {
        val message = ChatMessage(
            sender = SenderType.JARVIS,
            text = "Namaste! Main aapka AI assistant hoon."
        )
        assertEquals(SenderType.JARVIS, message.sender)
        assertTrue(message.text.contains("Namaste"))
        assertNotNull(message.id)
    }

    @Test
    fun testAssistantStateDisplayNames() {
        assertEquals("Assistant Ready", AssistantState.READY.displayName)
        assertEquals("Listening...", AssistantState.LISTENING.displayName)
        assertEquals("Thinking...", AssistantState.THINKING.displayName)
        assertEquals("Speaking...", AssistantState.SPEAKING.displayName)
        assertEquals("Voice Stopped", AssistantState.STOPPED.displayName)
    }
}
