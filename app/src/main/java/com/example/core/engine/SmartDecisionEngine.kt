package com.example.core.engine

import android.util.Log
import com.example.BuildConfig
import com.example.core.ai.ScreenType
import com.example.core.ai.UserIntent
import com.example.core.capture.FrameValidationResult
import com.example.core.capture.InvalidReason
import com.example.core.diagnostics.ScreenBuddyDiagnostics

/**
 * Categorized decisions produced by the Level 10 Smart Decision Engine.
 */
enum class AnalysisDecisionType {
    /** Suppress AI call (insignificant change, duplicate, animation, keyboard, cooldown) */
    IGNORE,
    /** Execute fast text/context Gemini analysis (no screenshot transmitted) */
    TEXT_ANALYSIS,
    /** Execute Gemini Vision AI analysis using temporary in-memory screenshot */
    VISION_ANALYSIS,
    /** User explicitly requested screen analysis via "Look at my screen" (bypasses auto filters) */
    USER_REQUESTED_VISION,
    /** Protected screen content (FLAG_SECURE or blank frame) where visual frame is unreadable */
    PROTECTED_CONTENT
}

/**
 * Result of Smart Decision Engine evaluation.
 */
data class AnalysisDecision(
    val decision: AnalysisDecisionType,
    val reason: String,
    val extractedTextContext: String? = null,
    val isUserInitiated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuppressedByInFlight: Boolean = false
)

/**
 * Level 10 Smart Decision Engine for ScreenBuddy.
 *
 * Responsibilities:
 * 1. Intelligently decides WHEN Gemini AI is actually needed.
 * 2. Chooses the minimum required AI input modality:
 *    - Prefers lightweight text/context analysis for conversations (WhatsApp / Instagram / DMs).
 *    - Selects Vision AI only when visual understanding or layout layout is strictly needed.
 * 3. Enforces session quotas, cooldowns, duplicate suppression, and keyboard noise filters.
 * 4. Respects user control ("Smart Live Assistance" toggle).
 * 5. Emits diagnostic logs in DEBUG mode under tag "ScreenBuddyIntelligence" (never logging private content).
 */
class SmartDecisionEngine(
    val config: LocalIntelligenceConfig = LocalIntelligenceConfig(),
    val duplicateDetector: ContextDuplicateDetector = ContextDuplicateDetector(config.duplicateSimilarityThreshold),
    val conversationContextEngine: ConversationContextEngine = ConversationContextEngine()
) {

    @Volatile
    private var isRequestInFlight = false

    @Synchronized
    fun setRequestInFlight(inFlight: Boolean) {
        isRequestInFlight = inFlight
    }

    @Synchronized
    fun isRequestInFlight(): Boolean = isRequestInFlight

    /**
     * Evaluates screen state, context, and intent to produce an [AnalysisDecision].
     *
     * @param isUserInitiated True if user explicitly tapped "Look at my screen" or action button.
     * @param localContext Extracted local context metadata.
     * @param visualDiffResult Diff metrics from VisualChangeDetector (or null).
     * @param frameValidation Result of validating the captured frame (or null).
     * @param isCooldownActive True if API rate-limit or min-interval cooldown is running.
     * @param sessionRequestCount Current count of AI calls in this session.
     * @param intent Optional user intent.
     * @param chatTone Optional user-configured reply tone in Chat Mode.
     * @param customTone Optional custom tone prompt text.
     * @param isChatMode True if evaluating during active Chat Mode.
     */
    fun evaluate(
        isUserInitiated: Boolean = false,
        localContext: LocalContext,
        visualDiffResult: VisualDiffResult? = null,
        frameValidation: FrameValidationResult? = null,
        isCooldownActive: Boolean = false,
        sessionRequestCount: Int = 0,
        intent: UserIntent? = null,
        chatTone: com.example.model.ChatTone? = null,
        customTone: String? = null,
        isChatMode: Boolean = false
    ): AnalysisDecision {
        if (isChatMode) {
            ScreenBuddyDiagnostics.recordMode("CHAT")
        }

        // 0. Frame Validation Safeguard & Smart Fallback
        if (frameValidation is FrameValidationResult.Invalid) {
            val isProtectedFrame = frameValidation.reason == InvalidReason.NEARLY_BLACK ||
                    frameValidation.reason == InvalidReason.COMPLETELY_BLACK ||
                    frameValidation.reason == InvalidReason.SOLID_COLOR_BLANK

            if (isProtectedFrame) {
                // If text context is available through legitimate Android APIs, use that text!
                if (!localContext.extractedText.isNullOrBlank() || localContext.hasSufficientTextContext) {
                    logDiagnostic("PROTECTED_CONTENT_TEXT_FALLBACK", "Protected screen frame detected; falling back to local text context")
                    duplicateDetector.markAnalyzed(localContext)
                    ScreenBuddyDiagnostics.recordAiRequest("TEXT")
                    return AnalysisDecision(
                        decision = AnalysisDecisionType.TEXT_ANALYSIS,
                        reason = "Protected visual frame; falling back to extracted text context",
                        extractedTextContext = localContext.extractedText?.take(config.maxTextContextLength),
                        isUserInitiated = isUserInitiated
                    )
                }

                // If neither visual frame nor text context is available -> Graceful protected content state
                logDiagnostic("PROTECTED_CONTENT_NO_CONTEXT", "Protected screen frame detected; zero visual or text context available")
                ScreenBuddyDiagnostics.recordSuppression("NO_AI_NEEDED")
                return AnalysisDecision(
                    decision = AnalysisDecisionType.PROTECTED_CONTENT,
                    reason = frameValidation.userFriendlyMessage,
                    extractedTextContext = null,
                    isUserInitiated = isUserInitiated
                )
            }
        }

        // 1. Explicit User Request ("Look at my screen") vs Chat Mode User Request
        if (isUserInitiated) {
            // In Chat Mode or for reply generation, prefer lightweight text-first path if text is present
            if ((isChatMode || intent == UserIntent.REPLY) && (!localContext.extractedText.isNullOrBlank() || localContext.hasSufficientTextContext)) {
                val effectiveTextPrompt = if (chatTone != null) {
                    conversationContextEngine.getFormattedPromptContext(tone = chatTone, customToneText = customTone)
                        ?: localContext.extractedText?.take(config.maxTextContextLength)
                } else {
                    localContext.extractedText?.take(config.maxTextContextLength)
                }
                logDiagnostic("USER_REQUESTED_CHAT_TEXT", "User triggered chat reply; selecting fast text-first analysis")
                duplicateDetector.markAnalyzed(localContext)
                ScreenBuddyDiagnostics.recordAiRequest("TEXT")
                return AnalysisDecision(
                    decision = AnalysisDecisionType.TEXT_ANALYSIS,
                    reason = "User requested chat reply with available text context",
                    extractedTextContext = effectiveTextPrompt,
                    isUserInitiated = true
                )
            }

            logDiagnostic("USER_REQUESTED_ANALYSIS", "User explicitly triggered screen analysis")
            ScreenBuddyDiagnostics.recordAiRequest("VISION")
            return AnalysisDecision(
                decision = AnalysisDecisionType.USER_REQUESTED_VISION,
                reason = "User explicitly requested screen analysis",
                extractedTextContext = localContext.extractedText,
                isUserInitiated = true
            )
        }

        // 3. Cooldown check
        if (isCooldownActive) {
            logDiagnostic("DEBOUNCE_SUPPRESSED", "Cooldown active — suppressing automatic AI trigger")
            ScreenBuddyDiagnostics.recordSuppression("DEBOUNCE")
            return AnalysisDecision(
                decision = AnalysisDecisionType.IGNORE,
                reason = "Cooldown period active"
            )
        }

        // 4. Session Quota safeguard
        if (sessionRequestCount >= config.maxAiRequestsPerSession) {
            logDiagnostic("DEBOUNCE_SUPPRESSED", "Session AI limit reached (${config.maxAiRequestsPerSession})")
            ScreenBuddyDiagnostics.recordSuppression("DEBOUNCE")
            return AnalysisDecision(
                decision = AnalysisDecisionType.IGNORE,
                reason = "Session AI request cap reached (${config.maxAiRequestsPerSession})"
            )
        }

        // 5. Insignificant change check
        val changeDelta = visualDiffResult?.changePercentage ?: 0f
        val isMeaningfulVisual = visualDiffResult?.isMeaningfulChange ?: false

        if (visualDiffResult != null && !isMeaningfulVisual && !localContext.hasSufficientTextContext) {
            logDiagnostic("DEBOUNCE_SUPPRESSED", "Visual delta below threshold ($changeDelta%)")
            ScreenBuddyDiagnostics.recordSuppression("UNCHANGED")
            return AnalysisDecision(
                decision = AnalysisDecisionType.IGNORE,
                reason = "Insignificant visual change ($changeDelta%)"
            )
        }

        // 6. Keyboard / Input noise filter
        if (localContext.isKeyboardActive && (visualDiffResult == null || !visualDiffResult.isMajorTransition)) {
            logDiagnostic("DEBOUNCE_SUPPRESSED", "Keyboard/typing movement ignored")
            ScreenBuddyDiagnostics.recordSuppression("TYPING")
            return AnalysisDecision(
                decision = AnalysisDecisionType.IGNORE,
                reason = "Keyboard input noise ignored"
            )
        }

        // 7. Duplicate / Near-duplicate context suppression
        if (duplicateDetector.isDuplicate(localContext, changeDelta)) {
            logDiagnostic("DUPLICATE_SUPPRESSED", "Duplicate context detected — suppressing request")
            ScreenBuddyDiagnostics.recordSuppression("DUPLICATE")
            return AnalysisDecision(
                decision = AnalysisDecisionType.IGNORE,
                reason = "Duplicate screen context suppressed"
            )
        }

        // 7.5 Level 12 Conversation Context Engine Evaluation & Gating
        val conversationResult = conversationContextEngine.updateAndEvaluate(
            rawText = localContext.extractedText,
            appPackage = localContext.foregroundAppPackage,
            isKeyboardActive = localContext.isKeyboardActive
        )

        if (localContext.isConversationScreen && !isUserInitiated) {
            if (!conversationResult.shouldTriggerAi) {
                logDiagnostic("DEBOUNCE_SUPPRESSED", "Conversation gated: ${conversationResult.decisionReason}")
                val reasonCategory = if (conversationResult.decisionReason.contains("typing", ignoreCase = true)) {
                    "TYPING"
                } else if (conversationResult.decisionReason.contains("Duplicate", ignoreCase = true) ||
                    conversationResult.decisionReason.contains("No new", ignoreCase = true)) {
                    "DUPLICATE"
                } else if (conversationResult.decisionReason.contains("settling", ignoreCase = true) ||
                    conversationResult.decisionReason.contains("Debounce", ignoreCase = true) ||
                    conversationResult.decisionReason.contains("batched", ignoreCase = true)) {
                    "DEBOUNCE"
                } else {
                    "NO_AI_NEEDED"
                }
                ScreenBuddyDiagnostics.recordSuppression(reasonCategory)
                return AnalysisDecision(
                    decision = AnalysisDecisionType.IGNORE,
                    reason = conversationResult.decisionReason
                )
            }
        }

        logDiagnostic("LOCAL_CHANGE", "Meaningful screen change detected ($changeDelta%)")

        val effectiveTextPrompt = if (chatTone != null) {
            conversationContextEngine.getFormattedPromptContext(tone = chatTone, customToneText = customTone)
                ?: conversationResult.formattedContextPrompt
                ?: localContext.extractedText?.take(config.maxTextContextLength)
        } else {
            conversationResult.formattedContextPrompt
                ?: localContext.extractedText?.take(config.maxTextContextLength)
        }

        // Prevents rapid back-to-back concurrent automatic requests while an API call is still in progress.
        // We evaluate first so that conversation state, duplicates, and context are properly tracked.
        if (isRequestInFlight() && !isUserInitiated) {
            logDiagnostic("REQUEST_IN_FLIGHT", "An API request is already in progress; marking as suppressed by in-flight")
            conversationContextEngine.rollback()
            ScreenBuddyDiagnostics.recordSuppression("IN_FLIGHT")
            return AnalysisDecision(
                decision = AnalysisDecisionType.IGNORE,
                reason = "A Gemini API request is already in progress",
                extractedTextContext = effectiveTextPrompt,
                isSuppressedByInFlight = true
            )
        }

        // 8. Modality Decision: Text Analysis vs Vision Analysis
        val isTextPreferred = localContext.isConversationScreen ||
                localContext.hasSufficientTextContext ||
                intent == UserIntent.REPLY ||
                intent == UserIntent.SUMMARIZE

        if (isTextPreferred && !effectiveTextPrompt.isNullOrBlank()) {
            logDiagnostic("TEXT_ANALYSIS_SELECTED", "Selected text-based Gemini analysis with ConversationContext")
            duplicateDetector.markAnalyzed(localContext)
            ScreenBuddyDiagnostics.recordAiRequest("TEXT")
            return AnalysisDecision(
                decision = AnalysisDecisionType.TEXT_ANALYSIS,
                reason = "Conversation context sufficient for analysis",
                extractedTextContext = effectiveTextPrompt
            )
        }

        // 9. Vision Analysis required
        logDiagnostic("VISION_ANALYSIS_SELECTED", "Selected Vision AI analysis")
        duplicateDetector.markAnalyzed(localContext)
        ScreenBuddyDiagnostics.recordAiRequest("VISION")
        return AnalysisDecision(
            decision = AnalysisDecisionType.VISION_ANALYSIS,
            reason = "Visual understanding required for screen state",
            extractedTextContext = effectiveTextPrompt
        )
    }

    /**
     * Safely logs diagnostic states in debug mode without exposing private text or images.
     */
    private fun logDiagnostic(event: String, detail: String) {
        if (BuildConfig.DEBUG) {
            Log.d("ScreenBuddyIntelligence", "[AI-GATE] $event. $detail")
        }
    }
}
