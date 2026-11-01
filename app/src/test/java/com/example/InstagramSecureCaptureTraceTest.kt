package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.core.capture.FrameValidationResult
import com.example.core.capture.FrameValidator
import com.example.core.capture.InvalidReason
import com.example.core.engine.LocalContextEngine
import com.example.core.ai.ImageOptimizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Diagnostic test tracing the 4-step MediaProjection screen capture lifecycle:
 * 1. Chrome -> capture (Valid)
 * 2. Instagram inbox -> capture (Valid)
 * 3. Instagram individual DM (FLAG_SECURE content) -> capture (Frame produced but ENTIRELY BLACK/BLANK)
 * 4. Return to Instagram inbox -> capture again (Valid)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InstagramSecureCaptureTraceTest {

    private val contextEngine = LocalContextEngine()

    @Test
    fun traceFourStepCaptureSequence() {
        // Step 1: Chrome -> capture
        val chromeBitmap = createSampleScreenBitmap(
            bgColor = Color.WHITE,
            headerColor = Color.rgb(66, 133, 244),
            title = "Google Chrome"
        )
        val chromeValidation = FrameValidator.validateFrame(chromeBitmap)
        assertTrue("Chrome frame should be valid", chromeValidation is FrameValidationResult.Valid)
        val chromeBase64 = ImageOptimizer.optimizeAndEncodeToBase64(chromeBitmap)
        assertTrue("Chrome image encoding should produce valid base64", chromeBase64.isNotEmpty())

        // Step 2: Instagram Inbox -> capture
        val inboxBitmap = createSampleScreenBitmap(
            bgColor = Color.rgb(18, 18, 18),
            headerColor = Color.BLACK,
            title = "Direct Messages"
        )
        val inboxValidation = FrameValidator.validateFrame(inboxBitmap)
        assertTrue("Instagram inbox frame should be valid", inboxValidation is FrameValidationResult.Valid)
        val inboxBase64 = ImageOptimizer.optimizeAndEncodeToBase64(inboxBitmap)
        assertTrue("Instagram inbox image encoding should produce valid base64", inboxBase64.isNotEmpty())

        // Step 3: Instagram Individual DM -> capture (FLAG_SECURE surface redacted by SurfaceFlinger)
        // MediaProjection produces an image frame (1080x2400), bitmap is created, image encoding succeeds,
        // but the pixel data rendered by SurfaceFlinger is entirely black/blank.
        val secureDmBitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        secureDmBitmap.eraseColor(Color.BLACK)

        val dmValidation = FrameValidator.validateFrame(secureDmBitmap)
        assertTrue("Individual DM secure frame must be rejected by FrameValidator", dmValidation is FrameValidationResult.Invalid)
        val invalidResult = dmValidation as FrameValidationResult.Invalid
        assertEquals(InvalidReason.NEARLY_BLACK, invalidResult.reason)
        assertEquals("This screen is protected by Android, so I can't read its visual content.", invalidResult.userFriendlyMessage)

        // Verify image encoding succeeds on the raw bitmap (Option B: frame is produced but is black/blank)
        val dmBase64 = ImageOptimizer.optimizeAndEncodeToBase64(secureDmBitmap)
        assertNotNull("Image encoding on black frame still produces Base64 payload", dmBase64)

        // Verify LocalContextEngine handles text/context extraction when visual frame is secure/blank
        val dmTextContext = contextEngine.extractContext(
            bitmap = secureDmBitmap,
            foregroundPackage = "com.instagram.android",
            providedText = "User: Hey, did you receive the photos? Sender: Yes, got them!"
        )
        assertTrue("Local text context can still be extracted even when visual frame is secure", dmTextContext.isConversationScreen)

        // Verify SmartDecisionEngine fallback behavior:
        val decisionEngine = com.example.core.engine.SmartDecisionEngine()

        // 3a. When text context is available -> TEXT_ANALYSIS fallback
        val textFallbackDecision = decisionEngine.evaluate(
            isUserInitiated = true,
            localContext = dmTextContext,
            frameValidation = dmValidation
        )
        assertEquals(com.example.core.engine.AnalysisDecisionType.TEXT_ANALYSIS, textFallbackDecision.decision)

        // 3b. When no text context is available -> PROTECTED_CONTENT decision
        val noTextContext = contextEngine.extractContext(
            bitmap = secureDmBitmap,
            foregroundPackage = "com.instagram.android",
            providedText = null
        )
        val protectedDecision = decisionEngine.evaluate(
            isUserInitiated = true,
            localContext = noTextContext,
            frameValidation = dmValidation
        )
        assertEquals(com.example.core.engine.AnalysisDecisionType.PROTECTED_CONTENT, protectedDecision.decision)
        assertEquals("This screen is protected by Android, so I can't read its visual content.", protectedDecision.reason)

        // Step 4: Return to Instagram Inbox -> capture again
        val returnInboxBitmap = createSampleScreenBitmap(
            bgColor = Color.rgb(18, 18, 18),
            headerColor = Color.BLACK,
            title = "Direct Messages"
        )
        val returnInboxValidation = FrameValidator.validateFrame(returnInboxBitmap)
        assertTrue("Return to inbox frame should be valid again", returnInboxValidation is FrameValidationResult.Valid)
    }

    private fun createSampleScreenBitmap(bgColor: Int, headerColor: Int, title: String): Bitmap {
        val bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(bgColor)
        val canvas = Canvas(bitmap)
        val headerPaint = Paint().apply { color = headerColor }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
        }
        canvas.drawRect(0f, 0f, 1080f, 200f, headerPaint)
        canvas.drawText(title, 50f, 120f, textPaint)
        return bitmap
    }
}
