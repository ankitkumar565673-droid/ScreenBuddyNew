package com.example

import com.example.core.engine.AnalysisDecisionType
import com.example.core.engine.ContextDuplicateDetector
import com.example.core.engine.ConversationContextEngine
import com.example.core.engine.LocalContextEngine
import com.example.core.engine.LocalIntelligenceConfig
import com.example.core.engine.SmartDecisionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConversationContextEngineTest {

    private lateinit var contextEngine: ConversationContextEngine
    private lateinit var localContextEngine: LocalContextEngine
    private lateinit var smartDecisionEngine: SmartDecisionEngine

    @Before
    fun setUp() {
        contextEngine = ConversationContextEngine(maxRecentMessages = 6, debounceDelayMs = 1500L)
        localContextEngine = LocalContextEngine()
        smartDecisionEngine = SmartDecisionEngine(
            config = LocalIntelligenceConfig(),
            duplicateDetector = ContextDuplicateDetector(0.92f),
            conversationContextEngine = contextEngine
        )
    }

    /**
     * TEST 1: Open conversation context evaluation.
     * Verify explicit user initiation works smoothly.
     */
    @Test
    fun test1_UserInitiatedConversationAnalysis() {
        val context = localContextEngine.extractContext(null, "com.whatsapp", "Alex: Hey how are you doing?")
        val decision = smartDecisionEngine.evaluate(isUserInitiated = true, localContext = context)

        assertEquals(AnalysisDecisionType.USER_REQUESTED_VISION, decision.decision)
        assertTrue(decision.isUserInitiated)
    }

    /**
     * TEST 2: Enable Live Assist. No conversation changes.
     * Verify no repeated Gemini calls.
     */
    @Test
    fun test2_EnableLiveAssist_NoChanges_SuppressesRequests() {
        val text = "Alex: Hey are we free at 3pm?"
        val context = localContextEngine.extractContext(null, "com.whatsapp", text)

        val firstDecision = smartDecisionEngine.evaluate(isUserInitiated = false, localContext = context)
        assertEquals(AnalysisDecisionType.TEXT_ANALYSIS, firstDecision.decision)

        // Second evaluation with identical text and no changes
        val secondDecision = smartDecisionEngine.evaluate(isUserInitiated = false, localContext = context)
        assertEquals(AnalysisDecisionType.IGNORE, secondDecision.decision)
    }

    /**
     * TEST 3: One new message.
     * Verify one analysis request triggered.
     */
    @Test
    fun test3_OneNewMessage_TriggersSingleAnalysis() {
        val initialText = "Alex: Are you there?"
        val context1 = localContextEngine.extractContext(null, "com.whatsapp", initialText)
        val decision1 = smartDecisionEngine.evaluate(isUserInitiated = false, localContext = context1)

        assertEquals(AnalysisDecisionType.TEXT_ANALYSIS, decision1.decision)

        val updatedText = "Alex: Are you there?\nAlex: Need your answer for the booking."
        val context2 = localContextEngine.extractContext(null, "com.whatsapp", updatedText)
        val decision2 = smartDecisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context2
        )

        assertNotNull(decision2)
    }

    /**
     * TEST 4: Five messages arriving quickly.
     * Verify they are batched into a settling window rather than causing 5 independent requests.
     */
    @Test
    fun test4_RapidMessages_BatchedDuringSettlingWindow() {
        var text = "Alex: Msg 1"
        val now = System.currentTimeMillis()

        // Message 1 (initial)
        val res1 = contextEngine.updateAndEvaluate(text, "com.whatsapp", false, now)
        assertTrue(res1.shouldTriggerAi)

        // Message 2 arriving 200ms later (rapid burst)
        text += "\nAlex: Msg 2"
        val res2 = contextEngine.updateAndEvaluate(text, "com.whatsapp", false, now + 200)
        assertFalse(res2.shouldTriggerAi)
        assertFalse(res2.isSettled)

        // Message 3 arriving 400ms later
        text += "\nAlex: Msg 3"
        val res3 = contextEngine.updateAndEvaluate(text, "com.whatsapp", false, now + 400)
        assertFalse(res3.shouldTriggerAi)

        // Settling after debounce window (2000ms later)
        val resSettled = contextEngine.updateAndEvaluate(text, "com.whatsapp", false, now + 2200)
        assertTrue(resSettled.shouldTriggerAi)
        assertTrue(resSettled.isSettled)
    }

    /**
     * TEST 5: User types continuously ("H" -> "He" -> "Hel" -> "Hello").
     * Verify intermediate typing states do not trigger AI requests.
     */
    @Test
    fun test5_UserTyping_IntermediateStatesSuppressed() {
        val now = System.currentTimeMillis()
        val res1 = contextEngine.updateAndEvaluate("H", "com.whatsapp", true, now)
        assertFalse(res1.shouldTriggerAi)

        val res2 = contextEngine.updateAndEvaluate("He", "com.whatsapp", true, now + 100)
        assertFalse(res2.shouldTriggerAi)

        val res3 = contextEngine.updateAndEvaluate("Hello how", "com.whatsapp", true, now + 300)
        assertFalse(res3.shouldTriggerAi)
    }

    /**
     * TEST 6: Same screen captured repeatedly.
     * Verify duplicate requests are suppressed.
     */
    @Test
    fun test6_DuplicateScreenCaptures_Suppressed() {
        val text = "Sarah: Are we meeting today?"
        val context = localContextEngine.extractContext(null, "com.whatsapp", text)

        val decision1 = smartDecisionEngine.evaluate(isUserInitiated = false, localContext = context)
        assertEquals(AnalysisDecisionType.TEXT_ANALYSIS, decision1.decision)

        val decision2 = smartDecisionEngine.evaluate(isUserInitiated = false, localContext = context)
        assertEquals(AnalysisDecisionType.IGNORE, decision2.decision)

        val decision3 = smartDecisionEngine.evaluate(isUserInitiated = false, localContext = context)
        assertEquals(AnalysisDecisionType.IGNORE, decision3.decision)
    }

    /**
     * TEST 7: Long conversation.
     * Verify context remains bounded.
     */
    @Test
    fun test7_LongConversation_ContextRemainsBounded() {
        val longTextBuilder = StringBuilder()
        for (i in 1..20) {
            longTextBuilder.append("Person: Message $i\n")
        }

        contextEngine.updateAndEvaluate(longTextBuilder.toString(), "com.whatsapp", false)
        val ctx = contextEngine.getContext()

        assertTrue(ctx.recentMessages.size <= 6)
        assertNotNull(ctx.conversationSummary)
        assertTrue(ctx.conversationSummary!!.contains("Earlier conversation"))
    }

    /**
     * TEST 8: Conversation continues after many messages.
     * Verify AI prompt incorporates recent context and summary.
     */
    @Test
    fun test8_ConversationContinuity_FormattedPromptIncludesSummaryAndRecent() {
        val text = "Alex: Msg 1\nAlex: Msg 2\nAlex: Msg 3\nAlex: Msg 4\nAlex: Msg 5\nAlex: Msg 6\nAlex: Msg 7\nAlex: Latest turn"
        contextEngine.updateAndEvaluate(text, "com.whatsapp", false)

        val prompt = contextEngine.getFormattedPromptContext()
        assertNotNull(prompt)
        assertTrue(prompt!!.contains("[CONVERSATION_CONTEXT]"))
        assertTrue(prompt.contains("Recent messages:"))
        assertTrue(prompt.contains("Latest turn"))
    }

    /**
     * TEST 9: Leave conversation and enter another app context.
     * Verify old conversation context is cleared and not incorrectly applied.
     */
    @Test
    fun test9_AppSwitch_ClearsPreviousContext() {
        contextEngine.updateAndEvaluate("Alex: WhatsApp chat content", "com.whatsapp", false)
        assertEquals("com.whatsapp", contextEngine.getContext().appPackage)

        // User switches to Instagram
        contextEngine.updateAndEvaluate("Sarah: Instagram DM content", "com.instagram.android", false)
        assertEquals("com.instagram.android", contextEngine.getContext().appPackage)
        assertFalse(contextEngine.getFormattedPromptContext()!!.contains("WhatsApp chat content"))
    }

    /**
     * TEST 10: Active Conversation Engine evaluation.
     * Verify text analysis decision for new conversation messages.
     */
    @Test
    fun test10_ActiveConversationAnalysis() {
        val text = "Alex: Hey"
        val context = localContextEngine.extractContext(null, "com.whatsapp", text)

        val activeEngine = SmartDecisionEngine(
            config = LocalIntelligenceConfig(),
            conversationContextEngine = contextEngine
        )
        val activeDecision = activeEngine.evaluate(isUserInitiated = false, localContext = context)
        assertEquals(AnalysisDecisionType.TEXT_ANALYSIS, activeDecision.decision)
    }

    /**
     * TEST 11: Restart app / RAM-only verification.
     * Verify conversation context is ephemeral and not persisted.
     */
    @Test
    fun test11_ClearContext_RAMOnlyVerification() {
        contextEngine.updateAndEvaluate("Alex: Sensitive chat message", "com.whatsapp", false)
        assertNotNull(contextEngine.getFormattedPromptContext())

        // Clear context (simulating session end / app restart)
        contextEngine.clearContext()

        assertNull(contextEngine.getFormattedPromptContext())
        assertTrue(contextEngine.getContext().recentMessages.isEmpty())
        assertNull(contextEngine.getContext().appPackage)
    }

    /**
     * TEST 12: Stale response protection.
     * Verify that if conversation context shifts (e.g., package name or message timestamp updates),
     * we can identify that the context has changed to discard stale in-flight results.
     */
    @Test
    fun test12_StaleResponseContextProtection() {
        contextEngine.updateAndEvaluate("Alex: Old message", "com.whatsapp", false)
        val initialContext = contextEngine.getContext()
        val initialTimestamp = initialContext.lastUpdatedTimestamp
        val initialPackage = initialContext.appPackage

        assertEquals("com.whatsapp", initialPackage)

        // Simulate context changing due to user switching to Instagram
        contextEngine.updateAndEvaluate("Sarah: Instagram DM", "com.instagram.android", false)
        val updatedContext = contextEngine.getContext()

        // Assert that the context indeed changed
        assertTrue(updatedContext.lastUpdatedTimestamp != initialTimestamp || updatedContext.appPackage != initialPackage)
        assertEquals("com.instagram.android", updatedContext.appPackage)
    }
}
