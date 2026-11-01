package com.example.core.ai

import android.graphics.Bitmap

/**
 * Visual screen categories detected by ScreenBuddy AI (Level 8).
 */
enum class ScreenType(val emoji: String, val displayName: String) {
    CONVERSATION("💬", "Conversation"),
    WEBPAGE("🌐", "Webpage"),
    FORM("📝", "Form"),
    CODE_OR_ERROR("🐞", "Error / Code"),
    FEED("📱", "Feed"),
    DOCUMENT("📄", "Document"),
    SETTINGS("⚙️", "Settings"),
    MEDIA("🎬", "Media"),
    UNKNOWN("👀", "Screen");

    companion object {
        fun fromString(value: String?): ScreenType {
            if (value.isNullOrBlank()) return UNKNOWN
            val clean = value.trim().uppercase()
            return when {
                clean == "CONVERSATION" || clean == "CHAT" || clean == "MESSAGING" ||
                clean == "MESSAGES" || clean == "MESSAGE" || clean == "DIRECT_MESSAGE" ||
                clean == "DM" || clean == "SMS" || clean == "TEXT" || clean == "THREAD" ||
                clean.contains("CHAT") || clean.contains("CONVERSATION") ||
                clean.contains("MESSAG") -> CONVERSATION

                clean == "WEBPAGE" || clean == "WEB" || clean == "BROWSER" || clean == "WEBSITE" ||
                clean.contains("WEB") || clean.contains("BROWSER") -> WEBPAGE

                clean == "FORM" || clean == "INPUT" || clean == "LOGIN" || clean == "SIGNUP" -> FORM

                clean == "CODE_OR_ERROR" || clean == "ERROR" || clean == "CODE" || clean == "CRASH" ||
                clean.contains("ERROR") || clean.contains("CRASH") -> CODE_OR_ERROR

                clean == "SETTINGS" || clean == "SETTING" || clean == "PREFERENCES" || clean == "CONFIG" -> SETTINGS

                clean == "DOCUMENT" || clean == "DOC" || clean == "PDF" || clean == "NOTES" -> DOCUMENT

                clean == "MEDIA" || clean == "VIDEO" || clean == "IMAGE" || clean == "PHOTO" -> MEDIA

                clean == "FEED" || clean == "TIMELINE" -> FEED

                else -> try {
                    valueOf(clean)
                } catch (e: Exception) {
                    UNKNOWN
                }
            }
        }
    }
}

/**
 * Structured user intent guiding visual analysis (Level 8).
 */
enum class UserIntent {
    GENERAL_QUESTION,
    EXPLAIN,
    REPLY,
    SUMMARIZE,
    TRANSLATE,
    TROUBLESHOOT,
    LEARN,
    COMPARE,
    EXTRACT,
    NAVIGATE
}

/**
 * Smart suggested reply option for conversation screens.
 */
data class CompanionReplyOption(
    val toneLabel: String,
    val text: String
)

/**
 * Contextual follow-up action suggested by ScreenBuddy.
 */
data class CompanionSuggestedAction(
    val label: String,
    val actionQuery: String
)

/**
 * Structured AI response produced by ScreenBuddy backend (Level 8).
 */
data class ScreenBuddyAiResponse(
    val screenType: ScreenType = ScreenType.UNKNOWN,
    val screenTitle: String? = null,
    val summary: String = "",
    val mainInsight: String = "",
    val suggestedReplies: List<CompanionReplyOption> = emptyList(),
    val suggestedActions: List<CompanionSuggestedAction> = emptyList(),
    val confidence: Float = 0.90f
)

/**
 * Result of a Vision AI Screen Analysis request.
 */
sealed interface VisionAiResult {
    data class Success(
        val answer: String,
        val structuredResponse: ScreenBuddyAiResponse? = null
    ) : VisionAiResult

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val errorType: String = "GENERIC",
        val retryAfterSeconds: Int = 0
    ) : VisionAiResult
}

/**
 * Clean abstraction for multimodal Vision AI analysis.
 * UI components interact only with this interface, never with specific network or provider implementations.
 */
interface VisionAiService {
    /**
     * Analyzes a screen image or text context alongside a user query, intent, and optional short-lived context.
     */
    suspend fun analyzeScreenshot(
        bitmap: Bitmap? = null,
        question: String,
        intent: UserIntent? = null,
        recentContext: String? = null,
        textContext: String? = null
    ): VisionAiResult
}
