package com.example.core.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Utility for preparing and optimizing screenshot Bitmaps for AI transmission.
 *
 * Requirements:
 * - Downscales large screen resolutions (e.g. 1440p/1080p) to max dimension 1280px.
 * - Compresses to JPEG at 80% quality to balance bandwidth, latency, and OCR readability.
 * - Encodes in-memory to Base64.
 * - Zero disk I/O, zero permanent storage.
 */
object ImageOptimizer {
    private const val TAG = "ImageOptimizer"
    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 80

    fun optimizeAndEncodeToBase64(bitmap: Bitmap?): String {
        if (bitmap == null || bitmap.isRecycled) {
            Log.e(TAG, "[IMAGE_ENCODING_FAILED] Provided screen bitmap is null or recycled")
            throw IllegalArgumentException("[IMAGE_ENCODING_FAILED] Provided screen bitmap is null or recycled")
        }
        val width = bitmap.width.coerceAtLeast(1)
        val height = bitmap.height.coerceAtLeast(1)
        val maxDim = max(width, height)

        val scaledBitmap = if (maxDim > MAX_DIMENSION) {
            val scale = MAX_DIMENSION.toFloat() / maxDim
            val targetWidth = (width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        val success = scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)

        if (scaledBitmap !== bitmap && !scaledBitmap.isRecycled) {
            try {
                scaledBitmap.recycle()
            } catch (ignored: Throwable) {}
        }

        if (!success) {
            Log.e(TAG, "[IMAGE_ENCODING_FAILED] Bitmap JPEG compression returned false")
            throw IllegalStateException("[IMAGE_ENCODING_FAILED] Failed to compress screen bitmap into JPEG")
        }

        val bytes = outputStream.toByteArray()
        if (bytes.isEmpty()) {
            Log.e(TAG, "[IMAGE_ENCODING_FAILED] Output stream produced empty byte array")
            throw IllegalStateException("[IMAGE_ENCODING_FAILED] Encoded image byte array is empty")
        }

        val base64Result = Base64.encodeToString(bytes, Base64.NO_WRAP)
        Log.d(TAG, "[DIAGNOSTIC] Image encoding result: SUCCESS (compressedBytes=${bytes.size}, base64Length=${base64Result.length})")
        return base64Result
    }
}
