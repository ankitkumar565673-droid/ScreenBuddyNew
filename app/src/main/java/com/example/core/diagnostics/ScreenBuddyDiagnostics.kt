package com.example.core.diagnostics

import android.util.Log
import com.example.BuildConfig
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Level 16: Real-Device Smart Reply Diagnostics & Validation Manager.
 *
 * Responsibilities:
 * 1. Safe, lightweight in-memory counters for observing AI requests, extractions, and suppressions.
 * 2. Pure RAM lifecycle: zero disk persistence, zero database storage, zero SharedPreferences writes.
 * 3. Privacy strictness: NEVER logs screenshots, message content, or private conversation text.
 * 4. Logs safe metadata under prefix `[SB-DIAG]` for logcat/ADB validation on physical Android devices.
 * 5. Provides a DEBUG-only reset method for clean test and verification cycles.
 */
object ScreenBuddyDiagnostics {

    private const val TAG = "ScreenBuddyIntelligence"

    // Atomic in-memory counters (RAM-only, reset on app restart or reset())
    private val _totalAiRequests = AtomicInteger(0)
    private val _textRequests = AtomicInteger(0)
    private val _visionRequests = AtomicInteger(0)
    private val _duplicateSuppressions = AtomicInteger(0)
    private val _typingSuppressions = AtomicInteger(0)
    private val _debounceSuppressions = AtomicInteger(0)
    private val _inFlightSuppressions = AtomicInteger(0)
    private val _unchangedSuppressions = AtomicInteger(0)
    private val _noAiNeededSuppressions = AtomicInteger(0)
    private val _staleResponsesDiscarded = AtomicInteger(0)
    private val _pendingUpdates = AtomicInteger(0)
    private val _pendingReplaced = AtomicInteger(0)
    private val _pendingProcessed = AtomicInteger(0)
    private val _contextResets = AtomicInteger(0)
    private val _timeouts = AtomicInteger(0)
    private val _actualGeminiRequests = AtomicInteger(0)
    private val _promptTokensTotal = AtomicInteger(0)
    private val _candidateTokensTotal = AtomicInteger(0)

    private val extractionCounts = mutableMapOf<String, AtomicInteger>()
    private val _requestSequence = AtomicLong(0)

    @Volatile
    var lastExtractionMethod: String? = null
        private set

    @Volatile
    var lastAiRequest: String? = null
        private set

    @Volatile
    var lastSuppressionReason: String? = null
        private set

    @Volatile
    var lastStaleDiscardReason: String? = null
        private set

    @Volatile
    var lastMode: String? = null
        private set

    @Volatile
    var lastErrorCategory: String? = null
        private set

    val totalAiRequests: Int get() = _totalAiRequests.get()
    val actualGeminiRequests: Int get() = _actualGeminiRequests.get()
    val textRequests: Int get() = _textRequests.get()
    val promptTokensTotal: Int get() = _promptTokensTotal.get()
    val candidateTokensTotal: Int get() = _candidateTokensTotal.get()
    val visionRequests: Int get() = _visionRequests.get()
    val duplicateSuppressions: Int get() = _duplicateSuppressions.get()
    val typingSuppressions: Int get() = _typingSuppressions.get()
    val debounceSuppressions: Int get() = _debounceSuppressions.get()
    val inFlightSuppressions: Int get() = _inFlightSuppressions.get()
    val unchangedSuppressions: Int get() = _unchangedSuppressions.get()
    val noAiNeededSuppressions: Int get() = _noAiNeededSuppressions.get()
    val staleResponsesDiscarded: Int get() = _staleResponsesDiscarded.get()
    val timeoutsCount: Int get() = _timeouts.get()
    val pendingUpdatesCount: Int get() = _pendingUpdates.get()
    val pendingReplacedCount: Int get() = _pendingReplaced.get()
    val pendingProcessedCount: Int get() = _pendingProcessed.get()
    val contextResetsCount: Int get() = _contextResets.get()

    fun nextRequestId(): Long = _requestSequence.incrementAndGet()

    @Synchronized
    fun getExtractionCount(method: String): Int = extractionCounts[method]?.get() ?: 0

    fun getAiRequestCount(type: String): Int = when (type) {
        "TEXT" -> textRequests
        "VISION" -> visionRequests
        else -> totalAiRequests
    }

    fun getSuppressionCount(reason: String): Int = when (reason) {
        "DUPLICATE" -> duplicateSuppressions
        "TYPING" -> typingSuppressions
        "DEBOUNCE" -> debounceSuppressions
        "IN_FLIGHT" -> inFlightSuppressions
        "UNCHANGED" -> unchangedSuppressions
        "NO_AI_NEEDED" -> noAiNeededSuppressions
        else -> 0
    }

    /**
     * Resets all in-memory diagnostic counters (DEBUG only).
     * Does NOT affect user settings, API configuration, or normal app state.
     */
    @Synchronized
    fun reset() {
        _totalAiRequests.set(0)
        _actualGeminiRequests.set(0)
        _textRequests.set(0)
        _visionRequests.set(0)
        _duplicateSuppressions.set(0)
        _typingSuppressions.set(0)
        _debounceSuppressions.set(0)
        _inFlightSuppressions.set(0)
        _unchangedSuppressions.set(0)
        _noAiNeededSuppressions.set(0)
        _staleResponsesDiscarded.set(0)
        _timeouts.set(0)
        _pendingUpdates.set(0)
        _pendingReplaced.set(0)
        _pendingProcessed.set(0)
        _contextResets.set(0)
        _promptTokensTotal.set(0)
        _candidateTokensTotal.set(0)
        _requestSequence.set(0)
        extractionCounts.clear()
        lastExtractionMethod = null
        lastAiRequest = null
        lastSuppressionReason = null
        lastStaleDiscardReason = null
        lastMode = null
        lastErrorCategory = null
        logDiagnostic("DIAGNOSTIC_COUNTERS_RESET")
    }

    /**
     * Records token usage metrics safely without logging prompts or sensitive texts.
     */
    fun recordTokenUsage(promptTokens: Int, candidateTokens: Int) {
        if (promptTokens > 0) _promptTokensTotal.addAndGet(promptTokens)
        if (candidateTokens > 0) _candidateTokensTotal.addAndGet(candidateTokens)
        logDiagnostic("TOKEN_USAGE prompt=$promptTokens candidates=$candidateTokens")
    }

    /**
     * Records error category safely without exposing private content or credentials.
     */
    fun recordErrorCategory(category: String) {
        lastErrorCategory = category
        logDiagnostic("ERROR_CATEGORY=$category")
    }

    /**
     * Records active mode: CHAT or LIVE_VIEW.
     */
    fun recordMode(mode: String) {
        lastMode = mode
        logDiagnostic("MODE=$mode")
    }

    /**
     * Records text extraction path used: ACCESSIBILITY, LOCAL_OCR, VISION_FALLBACK, or NONE.
     */
    @Synchronized
    fun recordExtraction(method: String) {
        lastExtractionMethod = method
        extractionCounts.getOrPut(method) { AtomicInteger(0) }.incrementAndGet()
        logDiagnostic("EXTRACTION=$method")
    }

    /**
     * Records Chat AI input mode: [CHAT-AI] INPUT_MODE=TEXT or [CHAT-AI] INPUT_MODE=VISION
     */
    fun recordChatAiInputMode(mode: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[CHAT-AI] INPUT_MODE=$mode")
        }
    }

    /**
     * Records Chat AI Request Start.
     */
    fun recordChatAiRequestStart(requestId: Long) {
        _actualGeminiRequests.incrementAndGet()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[CHAT-AI] REQUEST_START id=$requestId")
        }
    }

    /**
     * Records Chat AI Request End.
     */
    fun recordChatAiRequestEnd(requestId: Long, durationMs: Long, isSuccess: Boolean = true) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[CHAT-AI] REQUEST_END id=$requestId duration_ms=$durationMs success=$isSuccess")
        }
    }

    /**
     * Records Chat AI In-flight request suppression.
     */
    fun recordChatAiInFlightSuppress() {
        _inFlightSuppressions.incrementAndGet()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[CHAT-AI] IN_FLIGHT_SUPPRESS")
        }
    }

    /**
     * Records Chat Mode timing breakdown strictly without logging sensitive content.
     */
    fun recordChatTiming(
        extractionMs: Long = 0L,
        parsingMs: Long = 0L,
        gateMs: Long = 0L,
        networkUploadMs: Long = 0L,
        backendWaitMs: Long = 0L,
        geminiMs: Long = 0L,
        responseParseMs: Long = 0L,
        uiUpdateMs: Long = 0L,
        duplicateCheckMs: Long = 0L,
        aiCallMs: Long = 0L,
        totalMs: Long = 0L,
        outcome: String = "SUCCESS",
        mode: String = "TEXT"
    ) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[CHAT-TIMING] extraction_ms=$extractionMs duplicate_ms=$duplicateCheckMs ai_call_ms=$aiCallMs total_ms=$totalMs mode=$mode outcome=$outcome")
        }
    }

    /**
     * Records request timeout occurrence.
     */
    fun recordTimeout() {
        _timeouts.incrementAndGet()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[CHAT-STATE] TIMEOUT")
        }
    }

    /**
     * Records AI decision modality: TEXT, VISION, or NONE.
     */
    fun recordAiRequest(type: String) {
        lastAiRequest = type
        when (type) {
            "TEXT" -> {
                _totalAiRequests.incrementAndGet()
                _textRequests.incrementAndGet()
            }
            "VISION" -> {
                _totalAiRequests.incrementAndGet()
                _visionRequests.incrementAndGet()
            }
        }
        logDiagnostic("AI_REQUEST=$type")
    }

    /**
     * Records suppression category: DUPLICATE, TYPING, DEBOUNCE, IN_FLIGHT, UNCHANGED, or NO_AI_NEEDED.
     */
    fun recordSuppression(reasonCategory: String) {
        lastSuppressionReason = reasonCategory
        lastAiRequest = "NONE"
        when (reasonCategory) {
            "DUPLICATE" -> _duplicateSuppressions.incrementAndGet()
            "TYPING" -> _typingSuppressions.incrementAndGet()
            "DEBOUNCE" -> _debounceSuppressions.incrementAndGet()
            "IN_FLIGHT" -> _inFlightSuppressions.incrementAndGet()
            "UNCHANGED" -> _unchangedSuppressions.incrementAndGet()
            "NO_AI_NEEDED" -> _noAiNeededSuppressions.incrementAndGet()
        }
        logDiagnostic("SUPPRESSED=$reasonCategory")
        logDiagnostic("AI_REQUEST=NONE")
    }

    /**
     * Records when a stale AI response is safely discarded.
     */
    fun recordStaleResponseDiscarded(requestId: Long = 0L, reason: String = "CONTEXT_CHANGED") {
        lastStaleDiscardReason = reason
        _staleResponsesDiscarded.incrementAndGet()
        logDiagnostic("STALE_RESPONSE_DISCARDED sequence=$requestId reason=$reason")
    }

    /**
     * Records when conversation context is reset (e.g. Chat A -> Chat B, WhatsApp -> Instagram).
     */
    fun recordConversationContextReset() {
        _contextResets.incrementAndGet()
        logDiagnostic("CONVERSATION_CONTEXT_RESET")
    }

    /**
     * Records Chat Mode state transitions (e.g. OFF, ACTIVE_IDLE, WAITING_INCOMING, PROCESSING, SUGGESTION_VISIBLE, USER_TYPING, WAITING_NEXT_INCOMING).
     */
    fun recordChatState(state: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[CHAT-STATE] $state")
        }
    }

    /**
     * Records Chat Mode lifecycle events (e.g. NEW_INCOMING, DUPLICATE, USER_TYPING, USER_SENT, SCREEN_SWITCH, SUGGESTION_DISMISSED, CHAT_MODE_STOPPED).
     */
    fun recordChatEvent(event: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[CHAT-EVENT] $event")
        }
    }

    /**
     * Records AI Gate evaluation decisions (e.g. ALLOW reason=NEW_INCOMING, SUPPRESS reason=DUPLICATE, SUPPRESS reason=USER_TYPING, SUPPRESS reason=NO_MEANINGFUL_CHANGE, SUPPRESS reason=IN_FLIGHT).
     */
    fun recordAiGate(action: String, reason: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[AI-GATE] $action reason=$reason")
        }
    }

    /**
     * Records active in-flight request during Level 15 latest-state coalescing.
     */
    fun recordLatestStateActive(activeId: Any) {
        logDiagnostic("REQUEST_ACTIVE=$activeId")
    }

    /**
     * Records when a pending state is stored during coalescing.
     */
    fun recordPendingStateUpdated() {
        _pendingUpdates.incrementAndGet()
        logDiagnostic("PENDING_STATE_UPDATED")
    }

    /**
     * Records when an existing pending state is replaced by a newer state during coalescing.
     */
    fun recordPendingStateReplaced() {
        _pendingReplaced.incrementAndGet()
        logDiagnostic("PENDING_STATE_REPLACED")
    }

    /**
     * Records when the settled latest state is dequeued for processing.
     */
    fun recordProcessingLatestState() {
        _pendingProcessed.incrementAndGet()
        logDiagnostic("PROCESSING_LATEST_STATE")
    }

    fun logDiagnostic(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[SB-DIAG] $message")
        }
    }
}
