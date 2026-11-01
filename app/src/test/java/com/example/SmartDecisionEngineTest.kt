package com.example

import com.example.core.ai.UserIntent
import com.example.core.engine.AnalysisDecisionType
import com.example.core.engine.ContextDuplicateDetector
import com.example.core.engine.LocalContext
import com.example.core.engine.LocalContextEngine
import com.example.core.engine.LocalIntelligenceConfig
import com.example.core.engine.SmartDecisionEngine
import com.example.core.engine.VisualDiffResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmartDecisionEngineTest {

    private lateinit var contextEngine: LocalContextEngine
    private lateinit var duplicateDetector: ContextDuplicateDetector
    private lateinit var decisionEngine: SmartDecisionEngine

    @Before
    fun setUp() {
        contextEngine = LocalContextEngine()
        duplicateDetector = ContextDuplicateDetector(0.92f)
        decisionEngine = SmartDecisionEngine(
            config = LocalIntelligenceConfig(
                maxAiRequestsPerSession = 5
            ),
            duplicateDetector = duplicateDetector
        )
    }

    @Test
    fun testUserInitiatedRequestBypassesFilters() {
        val context = contextEngine.extractContext(null, "com.whatsapp", "Hello, are you free today?")
        val decision = decisionEngine.evaluate(
            isUserInitiated = true,
            localContext = context,
            isCooldownActive = true // Cooldown should be ignored for explicit user requests
        )

        assertEquals(AnalysisDecisionType.USER_REQUESTED_VISION, decision.decision)
        assertTrue(decision.isUserInitiated)
    }

    @Test
    fun testCooldownSuppressesAutomaticRequests() {
        val context = contextEngine.extractContext(null, "com.whatsapp", "Hello")
        val decision = decisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context,
            isCooldownActive = true
        )

        assertEquals(AnalysisDecisionType.IGNORE, decision.decision)
    }

    @Test
    fun testSessionQuotaLimitEnforced() {
        val context = contextEngine.extractContext(null, "com.whatsapp", "Hello")
        val decision = decisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context,
            sessionRequestCount = 10 // max is 5
        )

        assertEquals(AnalysisDecisionType.IGNORE, decision.decision)
    }

    @Test
    fun testConversationScreenPrefersTextAnalysis() {
        val chatText = "Me: Hey Alex! Are we meeting at 3 PM? Alex: Yes, 3 PM at the coffee shop."
        val context = contextEngine.extractContext(null, "com.whatsapp", chatText)
        val diff = VisualDiffResult(
            isMeaningfulChange = true,
            changePercentage = 25f,
            isMajorTransition = false
        )

        val decision = decisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context,
            visualDiffResult = diff,
            intent = UserIntent.REPLY
        )

        assertEquals(AnalysisDecisionType.TEXT_ANALYSIS, decision.decision)
        assertNotNull(decision.extractedTextContext)
    }

    @Test
    fun testDuplicateContextSuppressed() {
        val chatText = "Same chat message repeated."
        val context = contextEngine.extractContext(null, "com.whatsapp", chatText)
        val diff = VisualDiffResult(
            isMeaningfulChange = true,
            changePercentage = 12f,
            isMajorTransition = false
        )

        // First evaluation triggers TEXT_ANALYSIS and marks as analyzed
        val decision1 = decisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context,
            visualDiffResult = diff
        )
        assertEquals(AnalysisDecisionType.TEXT_ANALYSIS, decision1.decision)

        // Second evaluation with identical context must be IGNORE
        val decision2 = decisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context,
            visualDiffResult = diff
        )
        assertEquals(AnalysisDecisionType.IGNORE, decision2.decision)
    }

    @Test
    fun testKeyboardActivityIgnored() {
        val context = LocalContext(
            foregroundAppPackage = "com.whatsapp",
            isKeyboardActive = true,
            extractedText = null,
            textLength = 0,
            hasSufficientTextContext = false,
            isConversationScreen = true
        )
        val diff = VisualDiffResult(
            isMeaningfulChange = true,
            changePercentage = 8f,
            isMajorTransition = false
        )

        val decision = decisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context,
            visualDiffResult = diff
        )

        assertEquals(AnalysisDecisionType.IGNORE, decision.decision)
    }

    @Test
    fun testVisualAnalysisSelectedWhenTextInsufficient() {
        val context = LocalContext(
            foregroundAppPackage = "com.android.settings",
            isKeyboardActive = false,
            extractedText = null,
            textLength = 0,
            hasSufficientTextContext = false,
            isConversationScreen = false
        )
        val diff = VisualDiffResult(
            isMeaningfulChange = true,
            changePercentage = 30f,
            isMajorTransition = true
        )

        val decision = decisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context,
            visualDiffResult = diff,
            intent = UserIntent.EXPLAIN
        )

        assertEquals(AnalysisDecisionType.VISION_ANALYSIS, decision.decision)
    }

    @Test
    fun testProtectedFrameReturnsProtectedContentOrTextFallback() {
        val invalidFrame = com.example.core.capture.FrameValidationResult.Invalid(
            reason = com.example.core.capture.InvalidReason.NEARLY_BLACK,
            userFriendlyMessage = "This screen is protected by Android, so I can't read its visual content."
        )

        // Case A: No text context -> returns PROTECTED_CONTENT
        val noTextContext = LocalContext(
            foregroundAppPackage = "com.instagram.android",
            isKeyboardActive = false,
            extractedText = null,
            textLength = 0,
            hasSufficientTextContext = false,
            isConversationScreen = true
        )
        val decisionNoText = decisionEngine.evaluate(
            isUserInitiated = true,
            localContext = noTextContext,
            frameValidation = invalidFrame
        )
        assertEquals(AnalysisDecisionType.PROTECTED_CONTENT, decisionNoText.decision)
        assertEquals("This screen is protected by Android, so I can't read its visual content.", decisionNoText.reason)

        // Case B: Has text context -> returns TEXT_ANALYSIS fallback
        val textContext = LocalContext(
            foregroundAppPackage = "com.instagram.android",
            isKeyboardActive = false,
            extractedText = "Alex: See you soon!",
            textLength = 19,
            hasSufficientTextContext = true,
            isConversationScreen = true
        )
        val decisionWithText = decisionEngine.evaluate(
            isUserInitiated = true,
            localContext = textContext,
            frameValidation = invalidFrame
        )
        assertEquals(AnalysisDecisionType.TEXT_ANALYSIS, decisionWithText.decision)
        assertEquals("Alex: See you soon!", decisionWithText.extractedTextContext)
    }

    @Test
    fun testConcurrentRequestSuppression() {
        val context = contextEngine.extractContext(null, "com.whatsapp", "Hello, how are you?")
        
        // Mark request as in-flight
        decisionEngine.setRequestInFlight(true)
        
        // Automatic evaluation while in-flight -> should IGNORE
        val autoDecision = decisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context
        )
        assertEquals(AnalysisDecisionType.IGNORE, autoDecision.decision)
        assertEquals("A Gemini API request is already in progress", autoDecision.reason)
        
        // Explicit user action while in-flight -> should still bypass and run
        val manualDecision = decisionEngine.evaluate(
            isUserInitiated = true,
            localContext = context
        )
        assertEquals(AnalysisDecisionType.USER_REQUESTED_VISION, manualDecision.decision)
        
        // Reset in-flight state
        decisionEngine.setRequestInFlight(false)
        val afterDecision = decisionEngine.evaluate(
            isUserInitiated = false,
            localContext = context
        )
        assertTrue(afterDecision.decision != AnalysisDecisionType.IGNORE)
    }
}
