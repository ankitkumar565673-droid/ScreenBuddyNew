package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.core.capture.FrameValidationResult
import com.example.core.capture.FrameValidator
import com.example.core.capture.InvalidReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FrameValidatorTest {

    @Test
    fun testNullOrRecycledBitmap() {
        val result = FrameValidator.validateFrame(null)
        assertTrue(result is FrameValidationResult.Invalid)
        assertEquals(
            "I can't see the screen yet — try again.",
            (result as FrameValidationResult.Invalid).userFriendlyMessage
        )
        assertEquals(InvalidReason.NULL_OR_RECYCLED, result.reason)

        val recycledBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        recycledBitmap.recycle()
        val recycledResult = FrameValidator.validateFrame(recycledBitmap)
        assertTrue(recycledResult is FrameValidationResult.Invalid)
        assertEquals(InvalidReason.NULL_OR_RECYCLED, (recycledResult as FrameValidationResult.Invalid).reason)
    }

    @Test
    fun testAllBlackBitmapIsRejected() {
        val blackBitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        blackBitmap.eraseColor(Color.BLACK)

        val result = FrameValidator.validateFrame(blackBitmap)
        assertTrue(result is FrameValidationResult.Invalid)
        val invalid = result as FrameValidationResult.Invalid
        assertEquals(InvalidReason.NEARLY_BLACK, invalid.reason)
        assertEquals("This screen is protected by Android, so I can't read its visual content.", invalid.userFriendlyMessage)
    }

    @Test
    fun testBlackBitmapWithRedRecordingIndicatorAtTopIsRejected() {
        val bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLACK)

        // Draw red recording pill in top status bar area (e.g. y = 30)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { color = Color.RED }
        canvas.drawCircle(540f, 40f, 20f, paint)

        val result = FrameValidator.validateFrame(bitmap)
        assertTrue(result is FrameValidationResult.Invalid)
        val invalid = result as FrameValidationResult.Invalid
        assertEquals("This screen is protected by Android, so I can't read its visual content.", invalid.userFriendlyMessage)
    }

    @Test
    fun testValidContentScreenIsPassed() {
        val validBitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        // Background: typical light chat background
        validBitmap.eraseColor(Color.WHITE)

        val canvas = Canvas(validBitmap)
        val chatBubblePaint = Paint().apply { color = Color.rgb(37, 211, 102) } // WhatsApp green
        val textPaint = Paint().apply { color = Color.BLACK }

        // Draw chat bubbles
        canvas.drawRect(50f, 300f, 800f, 450f, chatBubblePaint)
        canvas.drawRect(280f, 500f, 1030f, 650f, chatBubblePaint)
        canvas.drawRect(50f, 700f, 700f, 850f, chatBubblePaint)

        val result = FrameValidator.validateFrame(validBitmap)
        assertTrue(result is FrameValidationResult.Valid)
        val valid = result as FrameValidationResult.Valid
        assertTrue(valid.meanLuminance > 50f)
        assertTrue(valid.blackPixelPercent < 20f)
    }

    @Test
    fun testValidDarkModeScreenIsPassed() {
        val darkModeBitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
        // Dark theme background #121212
        darkModeBitmap.eraseColor(Color.rgb(18, 18, 18))

        val canvas = Canvas(darkModeBitmap)
        val cardPaint = Paint().apply { color = Color.rgb(45, 55, 72) }
        val accentPaint = Paint().apply { color = Color.rgb(99, 102, 241) }
        val textPaint = Paint().apply { color = Color.WHITE; textSize = 60f }

        // Draw cards, UI elements, and text
        canvas.drawRect(40f, 200f, 1040f, 600f, cardPaint)
        canvas.drawRect(40f, 650f, 1040f, 1050f, cardPaint)
        canvas.drawCircle(540f, 1300f, 150f, accentPaint)
        canvas.drawText("Dark Mode Chat", 80f, 300f, textPaint)
        canvas.drawText("Hello from ScreenBuddy", 80f, 750f, textPaint)

        val result = FrameValidator.validateFrame(darkModeBitmap)
        assertTrue(result is FrameValidationResult.Valid)
        val valid = result as FrameValidationResult.Valid
    }
}
