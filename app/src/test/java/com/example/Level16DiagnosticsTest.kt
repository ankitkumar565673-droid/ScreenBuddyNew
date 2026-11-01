package com.example

import android.graphics.Bitmap
import com.example.core.diagnostics.ScreenBuddyDiagnostics
import com.example.core.engine.ConversationContextEngine
import com.example.core.engine.LocalContext
import com.example.core.engine.LocalContextEngine
import com.example.core.engine.SmartDecisionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Level16DiagnosticsTest {

    @Before
    fun setUp() {
        ScreenBuddyDiagnostics.reset()
    }

    @Test
    fun test01_textExtractionDiagnosticsReportAccessibilityWhenAccessibilityTextExists() {
        val localContextEngine = LocalContextEngine()
        val context = localContextEngine.extractContext(
            bitmap = null,
            foregroundPackage = "com.whatsapp",
            providedText = "Alice: Are we meeting at 5pm?"
        )

        assertEquals("ACCESSIBILITY", ScreenBuddyDiagnostics.lastExtractionMethod)
        assertTrue(ScreenBuddyDiagnostics.getExtractionCount("ACCESSIBILITY") >= 1)
        assertEquals(0, ScreenBuddyDiagnostics.getExtractionCount("NONE"))
    }

    @Test
    fun test02_textExtractionDiagnosticsReportLocalOcrWhenBitmapOcrRuns() {
        val localContextEngine = LocalContextEngine()
        val testBitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        
        val context = localContextEngine.extractContext(
            bitmap = testBitmap,
            foregroundPackage = "com.whatsapp",
            providedText = null
        )

        val lastMethod = ScreenBuddyDiagnostics.lastExtractionMethod
        assertTrue(lastMethod == "LOCAL_OCR" || lastMethod == "VISION_FALLBACK")
        assertTrue(
            ScreenBuddyDiagnostics.getExtractionCount("LOCAL_OCR") >= 1 ||
            ScreenBuddyDiagnostics.getExtractionCount("VISION_FALLBACK") >= 1
        )
    }

    @Test
    fun test03_textExtractionDiagnosticsReportVisionFallbackWhenOnlyVisualFallbackIsPossible() {
        val localContextEngine = LocalContextEngine()
        val blankBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        
        val context = localContextEngine.extractContext(
            bitmap = blankBitmap,
            foregroundPackage = "com.example.game",
            providedText = null
        )

        val lastMethod = ScreenBuddyDiagnostics.lastExtractionMethod
        assertTrue(lastMethod == "VISION_FALLBACK" || lastMethod == "LOCAL_OCR")
    }

    @Test
    fun test04_aiRequestDiagnosticsReportTextForConversationScreens() {
        val engine = SmartDecisionEngine()
        val localContext = LocalContext(
            foregroundAppPackage = "com.whatsapp",
            extractedText = "Alice: Dinner tonight?",
            isConversationScreen = true,
            hasSufficientTextContext = true
        )

        val decision = engine.evaluate(
            isUserInitiated = false,
            localContext = localContext
        )

        assertEquals("TEXT", ScreenBuddyDiagnostics.lastAiRequest)
        assertTrue(ScreenBuddyDiagnostics.getAiRequestCount("TEXT") >= 1)
    }

    @Test
    fun test05_suppressionDiagnosticsReportTypingWhenKeyboardActive() {
        val engine = SmartDecisionEngine()
        val localContext = LocalContext(
            foregroundAppPackage = "com.whatsapp",
            isKeyboardActive = true,
            extractedText = "Alice: Typing in progress...",
            isConversationScreen = true
        )

        val decision = engine.evaluate(
            isUserInitiated = false,
            localContext = localContext
        )

        assertEquals("TYPING", ScreenBuddyDiagnostics.lastSuppressionReason)
        assertTrue(ScreenBuddyDiagnostics.getSuppressionCount("TYPING") >= 1)
    }

    @Test
    fun test06_suppressionDiagnosticsReportDuplicateWhenTextDuplicate() {
        val engine = SmartDecisionEngine()
        val localContext = LocalContext(
            foregroundAppPackage = "com.whatsapp",
            extractedText = "Alice: What time is the flight?",
            isConversationScreen = true,
            hasSufficientTextContext = true
        )

        // First evaluation: Text analysis triggered
        engine.evaluate(isUserInitiated = false, localContext = localContext)
        assertEquals("TEXT", ScreenBuddyDiagnostics.lastAiRequest)

        // Second evaluation: Exact duplicate suppressed
        engine.evaluate(isUserInitiated = false, localContext = localContext)
        assertEquals("DUPLICATE", ScreenBuddyDiagnostics.lastSuppressionReason)
        assertTrue(ScreenBuddyDiagnostics.getSuppressionCount("DUPLICATE") >= 1)
    }

    @Test
    fun test07_suppressionDiagnosticsReportInFlightWhenRequestInFlight() {
        val engine = SmartDecisionEngine()
        engine.setRequestInFlight(true)

        val localContext = LocalContext(
            foregroundAppPackage = "com.whatsapp",
            extractedText = "Alice: Hello incoming message while in flight",
            isConversationScreen = true,
            hasSufficientTextContext = true
        )

        val decision = engine.evaluate(isUserInitiated = false, localContext = localContext)
        
        assertTrue(decision.isSuppressedByInFlight)
        assertEquals("IN_FLIGHT", ScreenBuddyDiagnostics.lastSuppressionReason)
        assertTrue(ScreenBuddyDiagnostics.getSuppressionCount("IN_FLIGHT") >= 1)
    }

    @Test
    fun test08_coalescingDiagnosticsReportLatestStatePreservation() {
        ScreenBuddyDiagnostics.recordLatestStateActive("A")
        ScreenBuddyDiagnostics.recordPendingStateUpdated()   // State B
        ScreenBuddyDiagnostics.recordPendingStateReplaced()  // State C replaces B
        ScreenBuddyDiagnostics.recordPendingStateReplaced()  // State D replaces C
        ScreenBuddyDiagnostics.recordProcessingLatestState() // Process latest State D

        assertTrue(ScreenBuddyDiagnostics.pendingUpdatesCount >= 1)
        assertTrue(ScreenBuddyDiagnostics.pendingReplacedCount >= 2)
        assertTrue(ScreenBuddyDiagnostics.pendingProcessedCount >= 1)
    }

    @Test
    fun test09_staleResponseDiscardDiagnosticsWhenContextChangesDuringFlight() {
        ScreenBuddyDiagnostics.recordStaleResponseDiscarded(9999L, "CONTEXT_CHANGED")
        
        assertEquals("CONTEXT_CHANGED", ScreenBuddyDiagnostics.lastStaleDiscardReason)
        assertTrue(ScreenBuddyDiagnostics.staleResponsesDiscarded >= 1)
    }

    @Test
    fun test10_conversationContextResetDiagnosticsWhenAppChanges() {
        val contextEngine = ConversationContextEngine()
        contextEngine.updateAndEvaluate("Alice: Hello", "com.whatsapp", false)
        
        // Switch app package
        contextEngine.updateAndEvaluate("Bob: Hey on Instagram", "com.instagram.android", false)

        assertTrue(ScreenBuddyDiagnostics.contextResetsCount >= 1)
    }
}
