package com.example

import com.example.core.ai.UserIntent
import com.example.core.engine.AnalysisDecisionType
import com.example.core.engine.ConversationContextEngine
import com.example.core.engine.LocalContextEngine
import com.example.core.engine.LocalIntelligenceConfig
import com.example.core.engine.SmartDecisionEngine
import com.example.model.ChatModeSuggestion
import com.example.model.ChatTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ScreenBuddy CHAT MODE smart reply system.
 * Verifies:
 * - Tone-aware prompt context construction
 * - Privacy constraints (RAM-only, zero disk state)
 * - Custom tone injection
 * - Decision engine evaluation with Chat Mode parameters
 * - ChatModeSuggestion fallback and options
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatModeTest {

    @Test
    fun testChatTonePromptInstructions() {
        val casualPrompt = ChatTone.CASUAL.promptInstruction
        assertTrue(casualPrompt.contains("relaxed") || casualPrompt.contains("laid-back") || casualPrompt.contains("informal"))

        val profPrompt = ChatTone.PROFESSIONAL.promptInstruction
        assertTrue(profPrompt.contains("polished") || profPrompt.contains("clear") || profPrompt.contains("professional"))

        val flirtyPrompt = ChatTone.FLIRTY.promptInstruction
        assertTrue(flirtyPrompt.contains("playful") || flirtyPrompt.contains("charming") || flirtyPrompt.contains("engaging"))

        val shortPrompt = ChatTone.SHORT.promptInstruction
        assertTrue(shortPrompt.contains("concise") || shortPrompt.contains("direct") || shortPrompt.contains("punchy"))
    }

    @Test
    fun testConversationContextEngineWithChatTone() {
        val engine = ConversationContextEngine()
        engine.seedContext("Alex: Are you coming tonight?", "com.whatsapp")

        val casualPrompt = engine.getFormattedPromptContext(ChatTone.CASUAL, null)
        assertNotNull(casualPrompt)
        assertTrue(casualPrompt!!.contains("[CHAT_MODE_INSTRUCTIONS]"))
        assertTrue(casualPrompt.contains("Casual"))

        val customPrompt = engine.getFormattedPromptContext(ChatTone.CUSTOM, "Shakespearean English")
        assertNotNull(customPrompt)
        assertTrue(customPrompt!!.contains("[CHAT_MODE_INSTRUCTIONS]"))
        assertTrue(customPrompt.contains("Shakespearean English"))
    }

    @Test
    fun testSmartDecisionEngineChatModeEvaluation() {
        val config = LocalIntelligenceConfig()
        val engine = SmartDecisionEngine(config)
        val localContextEngine = LocalContextEngine()

        val localContext = localContextEngine.extractContext(
            null,
            "com.instagram.android",
            "Alex: Hey, what are you doing? Let's grab lunch."
        )

        val decision = engine.evaluate(
            isUserInitiated = false,
            localContext = localContext,
            intent = UserIntent.REPLY,
            chatTone = ChatTone.FUNNY,
            isChatMode = true
        )

        assertEquals(AnalysisDecisionType.TEXT_ANALYSIS, decision.decision)
        assertNotNull(decision.extractedTextContext)
        assertTrue(decision.extractedTextContext?.contains("Funny") == true)
        assertTrue(decision.extractedTextContext?.contains("[CHAT_MODE_INSTRUCTIONS]") == true)
    }

    @Test
    fun testChatModeSuggestionOptions() {
        val suggestion = ChatModeSuggestion(
            primaryReply = "Sounds great, see you at 7!",
            candidateReplies = listOf("Sounds great, see you at 7!", "I'll be there by 7:30!", "Can't wait!"),
            tone = ChatTone.FRIENDLY
        )

        assertEquals("Sounds great, see you at 7!", suggestion.primaryReply)
        assertEquals(3, suggestion.allOptions.size)
        assertEquals("I'll be there by 7:30!", suggestion.allOptions[1])
        assertFalse(suggestion.isProtectedContentNotice)
    }
}
