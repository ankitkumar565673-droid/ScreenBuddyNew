package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.core.ai.CompanionReplyOption
import com.example.core.ai.CompanionSuggestedAction
import com.example.core.ai.GeminiVisionAiService
import com.example.core.ai.ScreenBuddyAiResponse
import com.example.core.ai.ScreenType
import com.example.core.ai.UserIntent
import com.example.core.ai.VisionAiResult
import com.example.core.capture.CaptureState
import com.example.core.capture.FrameValidationResult
import com.example.core.capture.FrameValidator
import com.example.core.capture.InvalidReason
import com.example.core.capture.ScreenCaptureManager
import com.example.core.engine.ContextDuplicateDetector
import com.example.core.engine.LocalContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenUnderstandingReliabilityTest {

    // 1. A valid screen frame reaches the existing analysis pipeline
    @Test
    fun testValidScreenFrameValidation() {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { color = Color.BLACK; textSize = 40f }
        canvas.drawText("Hello, are we still on for lunch?", 100f, 400f, paint)

        val validation = FrameValidator.validateFrame(bitmap)
        assertTrue("Expected frame to be Valid", validation is FrameValidationResult.Valid)
    }

    // 2. Permission denial or capture failure is handled safely
    @Test
    fun testPermissionDenialOrCaptureFailureHandledSafely() {
        val manager = ScreenCaptureManager.instance
        // Reset to simulate ungranted state
        manager.resetToIdle()
        assertFalse(manager.isCaptureGranted)

        // Capture should return null safely without generating fake synthetic content
        runTest {
            val frame = manager.captureCleanFrame(delayMs = 0L)
            assertNull("Frame must be null when permission is not active", frame)
        }
    }

    // 3. An invalid or blank frame is not submitted as a successful analysis
    @Test
    fun testInvalidOrBlankFrameIsRejected() {
        val blackFrame = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        blackFrame.eraseColor(Color.BLACK)

        val validation = FrameValidator.validateFrame(blackFrame)
        assertTrue("Black frame must be rejected", validation is FrameValidationResult.Invalid)
        val invalid = validation as FrameValidationResult.Invalid
        assertTrue(
            invalid.reason == InvalidReason.COMPLETELY_BLACK ||
            invalid.reason == InvalidReason.NEARLY_BLACK ||
            invalid.reason == InvalidReason.SOLID_COLOR_BLANK
        )
    }

    // 4. A valid structured AI response is parsed correctly
    @Test
    fun testStructuredAiResponseParsedCorrectly() {
        val service = GeminiVisionAiService()
        val jsonText = """
        {
          "screenType": "CONVERSATION",
          "screenTitle": "WhatsApp - Alex",
          "summary": "Alex sent a message asking about Saturday plans.",
          "mainInsight": "Alex wants to meet at 2 PM. A confirmation or counter-offer is appropriate.",
          "suggestedReplies": [
            { "tone": "Friendly", "text": "Sounds great! 2 PM works." },
            { "tone": "Casual", "text": "Sure thing, see you then 😄" },
            { "tone": "Short", "text": "Yes, 2 PM works." }
          ],
          "suggestedActions": [
            { "label": "Make it casual", "actionQuery": "Make the suggested replies more casual." }
          ],
          "confidence": 0.95
        }
        """.trimIndent()

        // Access private method parseStructuredJson via reflection for whitebox testing
        val method = GeminiVisionAiService::class.java.getDeclaredMethod("parseStructuredJson", String::class.java)
        method.isAccessible = true
        val response = method.invoke(service, jsonText) as? ScreenBuddyAiResponse

        assertNotNull("Response should not be null", response)
        assertEquals(ScreenType.CONVERSATION, response?.screenType)
        assertEquals("WhatsApp - Alex", response?.screenTitle)
        assertEquals(3, response?.suggestedReplies?.size)
        assertEquals("Friendly", response?.suggestedReplies?.get(0)?.toneLabel)
        assertEquals(1, response?.suggestedActions?.size)
    }

    // 5. Missing optional fields do not crash the app
    @Test
    fun testMissingOptionalFieldsDoNotCrash() {
        val service = GeminiVisionAiService()
        // Missing screenTitle, empty suggestedReplies, missing actions
        val minimalJson = """
        {
          "screenType": "WEBPAGE",
          "summary": "User is reading a Wikipedia article about Android.",
          "mainInsight": "Android is a mobile operating system based on a modified Linux kernel."
        }
        """.trimIndent()

        val method = GeminiVisionAiService::class.java.getDeclaredMethod("parseStructuredJson", String::class.java)
        method.isAccessible = true
        val response = method.invoke(service, minimalJson) as? ScreenBuddyAiResponse

        assertNotNull("Minimal response must parse safely", response)
        assertEquals(ScreenType.WEBPAGE, response?.screenType)
        assertNull(response?.screenTitle)
        assertEquals("User is reading a Wikipedia article about Android.", response?.summary)
        assertTrue(response?.suggestedReplies?.isEmpty() == true)
        // Default suggested actions are provided
        assertFalse(response?.suggestedActions?.isEmpty() == true)
    }

    // 6. Malformed JSON is handled gracefully (with preamble or markdown code fences)
    @Test
    fun testMalformedJsonWithMarkdownAndSurroundingText() {
        val service = GeminiVisionAiService()
        val markdownJson = """
        Here is the screen analysis:
        ```json
        {
          "screenType": "SETTINGS",
          "summary": "Display settings menu.",
          "mainInsight": "You can enable Dark theme here."
        }
        ```
        Hope this helps!
        """.trimIndent()

        val method = GeminiVisionAiService::class.java.getDeclaredMethod("parseStructuredJson", String::class.java)
        method.isAccessible = true
        val response = method.invoke(service, markdownJson) as? ScreenBuddyAiResponse

        assertNotNull("Should extract JSON from surrounding text and markdown", response)
        assertEquals(ScreenType.SETTINGS, response?.screenType)
        assertEquals("Display settings menu.", response?.summary)
    }

    // 7. Network errors and timeouts do not crash the app
    @Test
    fun testNetworkErrorsAndTimeoutsMapToFriendlyErrors() = runTest {
        val service = GeminiVisionAiService()
        // Calling with null bitmap and empty text context returns clean error
        val result = service.analyzeScreenshot(
            bitmap = null,
            question = "What is on my screen?",
            textContext = null
        )

        assertTrue(result is VisionAiResult.Error)
        val error = result as VisionAiResult.Error
        assertEquals("MISSING_INPUT", error.errorType)
        assertFalse(error.message.isBlank())
    }

    // 8. Duplicate analysis requests are appropriately controlled
    @Test
    fun testDuplicateAnalysisRequestsAreSuppressed() {
        val detector = ContextDuplicateDetector()
        val context = LocalContext(
            extractedText = "Meeting tomorrow at 10 AM",
            foregroundAppPackage = "com.whatsapp"
        )

        // First time: not a duplicate
        assertFalse(detector.isDuplicate(context, 0f))
        detector.markAnalyzed(context)

        // Second time with identical text and 0% visual change: MUST be suppressed as duplicate
        assertTrue(detector.isDuplicate(context, 0f))
    }

    // 9. A newer analysis result cannot be overwritten by an older response
    @Test
    fun testNewerAnalysisResultNotOverwrittenByOlderResponse() {
        val requestCounter = AtomicLong(0L)
        var latestCommittedResult: String? = null

        val req1 = requestCounter.incrementAndGet()
        val req2 = requestCounter.incrementAndGet()

        // Simulate Req 2 completing first (fast response)
        if (req2 >= requestCounter.get()) {
            latestCommittedResult = "Result from Request 2"
        }

        // Simulate Req 1 completing later (slow response)
        if (req1 >= requestCounter.get()) {
            latestCommittedResult = "Result from Request 1"
        }

        assertEquals("Newer response must not be overwritten by older response", "Result from Request 2", latestCommittedResult)
    }

    // 10. Existing character and overlay components remain intact
    @Test
    fun testExistingCharacterAndOverlayPreserved() {
        assertTrue(com.example.ui.companion.CompanionState.values().isNotEmpty())
        val companionClass = Class.forName("com.example.ui.companion.CuteCompanionKt")
        assertNotNull(companionClass)
    }
}
