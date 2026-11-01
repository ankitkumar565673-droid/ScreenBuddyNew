package com.example.core.capture

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log

/**
 * Result of validating a captured screen frame.
 */
sealed class FrameValidationResult {
    data class Valid(
        val meanLuminance: Float,
        val blackPixelPercent: Float,
        val variance: Float
    ) : FrameValidationResult()

    data class Invalid(
        val reason: InvalidReason,
        val userFriendlyMessage: String,
        val diagnosticCode: String = reason.diagnosticCode
    ) : FrameValidationResult()
}

enum class InvalidReason(val diagnosticCode: String) {
    NULL_OR_RECYCLED("CAPTURE_RETURNED_NULL"),
    ZERO_DIMENSIONS("CAPTURE_INVALID_DIMENSIONS"),
    COMPLETELY_BLACK("FRAME_COMPLETELY_BLACK"),
    NEARLY_BLACK("FRAME_NEARLY_BLACK"),
    SOLID_COLOR_BLANK("FRAME_SOLID_BLANK"),
    CORRUPTED("FRAME_CORRUPTED")
}

/**
 * Level 10 Frame Validator for ScreenBuddy.
 *
 * Ensures that captured screen frames are valid, non-blank, and have meaningful content
 * BEFORE any transmission to Gemini or the backend.
 *
 * Catches:
 * 1. Completely black frames (e.g. when Android's MediaProjection is restricted to "Single App"
 *    and user is viewing a different app).
 * 2. Unrendered virtual display buffers (where buffer is all zeros except top status bar / recording pill).
 * 3. Blank solid-color frames with zero content variance.
 * 4. Recycled or invalid bitmaps.
 *
 * Zero disk I/O, purely in-memory.
 */
object FrameValidator {
    private const val TAG = "FrameValidator"

    // Default sampling grid size
    private const val SAMPLE_COLS = 32
    private const val SAMPLE_ROWS = 32

    // Thresholds
    private const val BLACK_LUMA_THRESHOLD = 14 // Pixels with luma < 14 are considered near-black
    private const val MAX_ALLOWABLE_BLACK_PERCENT = 96.0f // If >96% of content area is black, frame is invalid
    private const val MIN_CONTENT_MEAN_LUMA = 2.0f // Mean luminance must be at least 2.0
    private const val MIN_VARIANCE_FOR_SOLID_DARK = 0.5f // Dark solid frames have variance near 0

    /**
     * Inspects the bitmap and determines whether it is usable for AI analysis.
     *
     * @param bitmap The in-memory screenshot bitmap.
     * @param excludeTopPercent Percentage of the top of the screen to ignore (e.g. 5% for status bar & recording pill).
     * @return [FrameValidationResult] indicating validity or failure reason.
     */
    fun validateFrame(
        bitmap: Bitmap?,
        excludeTopPercent: Float = 0.06f
    ): FrameValidationResult {
        if (bitmap == null || bitmap.isRecycled) {
            Log.e(TAG, "[CAPTURE_RETURNED_NULL] Frame bitmap is null or recycled")
            return FrameValidationResult.Invalid(
                reason = InvalidReason.NULL_OR_RECYCLED,
                userFriendlyMessage = "I can't see the screen yet — try again.",
                diagnosticCode = "CAPTURE_RETURNED_NULL"
            )
        }

        val width = bitmap.width
        val height = bitmap.height

        if (width <= 0 || height <= 0) {
            Log.e(TAG, "[CAPTURE_INVALID_DIMENSIONS] Frame has invalid dimensions: ${width}x${height}")
            return FrameValidationResult.Invalid(
                reason = InvalidReason.ZERO_DIMENSIONS,
                userFriendlyMessage = "I can't see the screen yet — try again.",
                diagnosticCode = "CAPTURE_INVALID_DIMENSIONS"
            )
        }

        try {
            val startY = (height * excludeTopPercent).toInt().coerceIn(0, height - 1)
            val usableHeight = height - startY
            if (usableHeight <= 0) {
                Log.e(TAG, "[FRAME_CORRUPTED] Usable frame height is invalid: $usableHeight")
                return FrameValidationResult.Invalid(
                    reason = InvalidReason.CORRUPTED,
                    userFriendlyMessage = "I can't see the screen yet — try again.",
                    diagnosticCode = "FRAME_CORRUPTED"
                )
            }

            var blackPixelCount = 0
            var totalSampled = 0
            var sumLuma = 0.0
            val lumaSamples = FloatArray(SAMPLE_COLS * SAMPLE_ROWS)

            val stepX = (width / (SAMPLE_COLS + 1)).coerceAtLeast(1)
            val stepY = (usableHeight / (SAMPLE_ROWS + 1)).coerceAtLeast(1)

            for (row in 1..SAMPLE_ROWS) {
                val y = (startY + row * stepY).coerceAtMost(height - 1)
                for (col in 1..SAMPLE_COLS) {
                    val x = (col * stepX).coerceAtMost(width - 1)
                    val pixel = bitmap.getPixel(x, y)

                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    // Standard Rec. 601 perceptual luminance
                    val luma = 0.299f * r + 0.587f * g + 0.114f * b

                    lumaSamples[totalSampled] = luma
                    sumLuma += luma

                    if (luma < BLACK_LUMA_THRESHOLD) {
                        blackPixelCount++
                    }
                    totalSampled++
                }
            }

            if (totalSampled == 0) {
                Log.e(TAG, "[FRAME_CORRUPTED] Zero pixels sampled")
                return FrameValidationResult.Invalid(
                    reason = InvalidReason.CORRUPTED,
                    userFriendlyMessage = "I can't see the screen yet — try again.",
                    diagnosticCode = "FRAME_CORRUPTED"
                )
            }

            val meanLuma = (sumLuma / totalSampled).toFloat()
            val blackPercent = (blackPixelCount.toFloat() / totalSampled.toFloat()) * 100f

            // Calculate sample variance
            var sumSqDiff = 0.0
            for (i in 0 until totalSampled) {
                val diff = lumaSamples[i] - meanLuma
                sumSqDiff += diff * diff
            }
            val variance = (sumSqDiff / totalSampled).toFloat()

            // 1. Check for completely black or nearly black frame
            if (blackPercent >= MAX_ALLOWABLE_BLACK_PERCENT || meanLuma < MIN_CONTENT_MEAN_LUMA) {
                Log.w(TAG, "[FRAME_NEARLY_BLACK] Frame rejected: blackPercent=$blackPercent%, meanLuma=$meanLuma, variance=$variance")
                return FrameValidationResult.Invalid(
                    reason = InvalidReason.NEARLY_BLACK,
                    userFriendlyMessage = "This screen is protected by Android, so I can't read its visual content.",
                    diagnosticCode = "FRAME_NEARLY_BLACK"
                )
            }

            // 2. Check for solid-color dark blank frames
            if (meanLuma < 2.5f && variance < MIN_VARIANCE_FOR_SOLID_DARK) {
                Log.w(TAG, "[FRAME_SOLID_BLANK] Frame rejected: dark solid blank frame, meanLuma=$meanLuma, variance=$variance")
                return FrameValidationResult.Invalid(
                    reason = InvalidReason.SOLID_COLOR_BLANK,
                    userFriendlyMessage = "This screen is protected by Android, so I can't read its visual content.",
                    diagnosticCode = "FRAME_SOLID_BLANK"
                )
            }

            return FrameValidationResult.Valid(
                meanLuminance = meanLuma,
                blackPixelPercent = blackPercent,
                variance = variance
            )
        } catch (e: Exception) {
            Log.e(TAG, "[FRAME_CORRUPTED] Error validating frame: ${e.message}", e)
            return FrameValidationResult.Invalid(
                reason = InvalidReason.CORRUPTED,
                userFriendlyMessage = "I can't see the screen yet — try again.",
                diagnosticCode = "FRAME_CORRUPTED"
            )
        }
    }
}
