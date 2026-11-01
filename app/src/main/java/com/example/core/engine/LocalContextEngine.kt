package com.example.core.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.core.capture.FrameValidationResult
import com.example.core.capture.FrameValidator
import com.example.core.diagnostics.ScreenBuddyDiagnostics
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Text extraction path category for Level 16 diagnostics.
 */
enum class TextExtractionMethod {
    ACCESSIBILITY,
    LOCAL_OCR,
    VISION_FALLBACK,
    NONE
}

/**
 * Extracted lightweight local context metadata.
 * Maintained ephemerally in RAM only — never persisted or logged.
 */
data class LocalContext(
    val foregroundAppPackage: String? = null,
    val isKeyboardActive: Boolean = false,
    val extractedText: String? = null,
    val textLength: Int = 0,
    val hasSufficientTextContext: Boolean = false,
    val isConversationScreen: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val extractionMethod: TextExtractionMethod = TextExtractionMethod.NONE
)

/**
 * Level 10 Local Context Engine.
 *
 * Responsibilities:
 * 1. Extract lightweight screen context (foreground package, visible text context, keyboard activity).
 * 2. Identify messaging and conversation screens (WhatsApp, Instagram, Messenger, Telegram, Slack, SMS).
 * 3. Determine if text context is sufficient for Gemini text analysis (avoiding continuous screenshots).
 * 4. Ephemeral RAM processing only: zero persistence, zero privacy leaks.
 */
class LocalContextEngine {

    private val knownMessagingPackages = setOf(
        "com.whatsapp",
        "com.instagram.android",
        "com.facebook.orca",
        "org.telegram.messenger",
        "com.google.android.apps.messaging",
        "com.slack",
        "com.discord",
        "com.microsoft.teams",
        "com.viber.voip",
        "com.snapchat.android"
    )

    /**
     * Extracts lightweight local context from a screen bitmap and metadata.
     *
     * @param bitmap In-memory screen capture bitmap (or null if unavailable).
     * @param foregroundPackage Current active foreground app package (or null if unavailable).
     * @param providedText Any text captured via Accessibility/node tree (or null).
     * @return [LocalContext] snapshot.
     */
    fun extractContext(
        bitmap: Bitmap?,
        foregroundPackage: String? = null,
        providedText: String? = null
    ): LocalContext {
        val pkg = foregroundPackage?.lowercase(Locale.ROOT)?.trim()
        val isKnownChatApp = pkg != null && knownMessagingPackages.any { pkg.contains(it) }

        val isKeyboardVisible = if (bitmap != null && !bitmap.isRecycled) {
            detectKeyboardActive(bitmap)
        } else false

        val extractionMethod: TextExtractionMethod
        val rawText = if (!providedText.isNullOrBlank()) {
            extractionMethod = TextExtractionMethod.ACCESSIBILITY
            providedText.trim()
        } else if (bitmap != null && !bitmap.isRecycled) {
            val ocr = extractLightweightText(bitmap)
            if (!ocr.isNullOrBlank() && !ocr.startsWith("Screen content visible")) {
                extractionMethod = TextExtractionMethod.LOCAL_OCR
                ocr
            } else {
                extractionMethod = TextExtractionMethod.VISION_FALLBACK
                ocr
            }
        } else {
            extractionMethod = TextExtractionMethod.NONE
            null
        }

        ScreenBuddyDiagnostics.recordExtraction(extractionMethod.name)

        val textLen = rawText?.length ?: 0
        val isPlaceholder = rawText != null && (rawText.startsWith("Screen content visible") || rawText.isBlank())

        val isTextSubstantial = !isPlaceholder && (textLen >= 20 || (rawText != null && isConversationStructure(rawText)))
        val isConversation = isKnownChatApp || (rawText != null && !isPlaceholder && isConversationStructure(rawText))

        return LocalContext(
            foregroundAppPackage = pkg,
            isKeyboardActive = isKeyboardVisible,
            extractedText = if (isPlaceholder) null else rawText,
            textLength = if (isPlaceholder) 0 else textLen,
            hasSufficientTextContext = isTextSubstantial,
            isConversationScreen = isConversation,
            extractionMethod = extractionMethod
        )
    }

    /**
     * Heuristic to check if text contains conversation / chat messaging cues.
     */
    fun isConversationStructure(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val chatKeywords = listOf(
            "type a message", "reply", "online", "active now", "delivered", "read",
            "sent", "chats", "direct", "message", "me:", "you:", "today", "yesterday",
            "pm", "am", "typing...", "whatsapp", "instagram"
        )
        var matches = 0
        for (kw in chatKeywords) {
            if (lower.contains(kw)) matches++
        }
        return matches >= 2
    }

    /**
     * Fast, lightweight visual check to estimate if an on-screen keyboard is visible
     * in the bottom third of the screen bitmap.
     */
    private fun detectKeyboardActive(bitmap: Bitmap): Boolean {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            if (width < 100 || height < 100) return false

            // Sample horizontal line in lower 20% of screen
            val sampleY = (height * 0.85f).toInt()
            val startX = (width * 0.1f).toInt()
            val endX = (width * 0.9f).toInt()
            val step = ((endX - startX) / 20).coerceAtLeast(1)

            var keyColorChanges = 0
            var prevLuma = -1

            for (x in startX until endX step step) {
                val pixel = bitmap.getPixel(x, sampleY)
                val luma = (0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)).toInt()
                if (prevLuma != -1 && abs(luma - prevLuma) > 25) {
                    keyColorChanges++
                }
                prevLuma = luma
            }

            // Keyboard keys produce high-frequency contrast boundaries
            keyColorChanges >= 8
        } catch (e: Exception) {
            false
        }
    }

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Fast local on-device OCR for in-memory bitmaps.
     * Extracts text lines directly on-device using MLKit in < 300ms without network transmission.
     */
    private fun extractLightweightText(bitmap: Bitmap): String? {
        return try {
            val validation = FrameValidator.validateFrame(bitmap)
            if (validation is FrameValidationResult.Invalid) {
                return null
            }
            if (bitmap.isRecycled) return null
            val width = bitmap.width
            val height = bitmap.height
            if (width < 100 || height < 100) return null

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val task = textRecognizer.process(inputImage)
            val result = Tasks.await(task, 1500L, TimeUnit.MILLISECONDS)
            val extracted = result.text.trim()
            if (extracted.isNotBlank()) {
                extracted
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d("LocalContextEngine", "MLKit OCR extraction fallback or error: ${e.message}")
            null
        }
    }
}
