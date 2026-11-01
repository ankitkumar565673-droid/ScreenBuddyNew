package com.example.core.engine

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Configuration for the Level 9 Visual Change Engine.
 * Highly configurable and easy to tune.
 */
data class VisualChangeConfig(
    /** Width of downsampled comparison thumbnail (48px provides optimal speed and noise rejection) */
    val sampleWidth: Int = 48,
    /** Height of downsampled comparison thumbnail */
    val sampleHeight: Int = 48,
    /** Per-pixel luminance difference threshold (0..255) to qualify as a pixel change */
    val pixelLumaThreshold: Int = 22,
    /** Percentage of total sample pixels (0..100) that must change to qualify as a meaningful change */
    val meaningfulChangeThresholdPercent: Float = 8.5f,
    /** Percentage threshold above which a sudden full-screen transition/navigation is detected */
    val majorTransitionThresholdPercent: Float = 35.0f,
    /** Minimum duration (ms) the screen must remain stable after motion stops before triggering AI */
    val settleDebounceMs: Long = 700L,
    /** Minimum interval (ms) between Gemini AI analyses to prevent API spam and respect quotas */
    val minAnalysisIntervalMs: Long = 5000L,
    /** Polling interval (ms) between screen frame comparisons */
    val pollingIntervalMs: Long = 800L
)

/**
 * Detailed result of a visual frame comparison.
 */
data class VisualDiffResult(
    val changedPixelsCount: Int = 0,
    val totalPixelsCount: Int = 2304,
    val changePercentage: Float,
    val isMeaningfulChange: Boolean,
    val isMajorTransition: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Current operating state of the Visual Change Engine.
 */
sealed class VisualEngineState {
    data object Idle : VisualEngineState()
    data class Monitoring(
        val lastDeltaPercent: Float = 0f,
        val isSettling: Boolean = false,
        val isCooldownActive: Boolean = false,
        val cooldownRemainingMs: Long = 0L
    ) : VisualEngineState()
    data class ChangeDetected(
        val deltaPercent: Float,
        val isMajorTransition: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    ) : VisualEngineState()
    data class Analyzing(
        val deltaPercent: Float,
        val startedAt: Long = System.currentTimeMillis()
    ) : VisualEngineState()
}

/**
 * Level 9 Visual Change Engine for ScreenBuddy Live View.
 *
 * Capabilities:
 * 1. Efficient downsampled perceptual luminance comparison (48x48 matrix).
 * 2. Reusable pixel buffers with zero heap allocations during frame diffing.
 * 3. Rejects micro-noise: blinking cursors, single-character typing, rendering artifacts, minor animations.
 * 4. Detects meaningful visual changes: screen navigation, dialogs open/close, major content changes, scrolling settling.
 * 5. Motion settling debounce: waits for fast scrolling to settle before analyzing the final screen state.
 * 6. Throttles AI analysis with cooldown protection to avoid spamming the Gemini proxy.
 * 7. Ephemeral in-memory only: zero disk I/O, no logging of private screen data.
 * 8. Releases and recycles temporary comparison bitmaps safely.
 */
class VisualChangeDetector(
    val config: VisualChangeConfig = VisualChangeConfig()
) {
    private val _engineState = MutableStateFlow<VisualEngineState>(VisualEngineState.Idle)
    val engineState: StateFlow<VisualEngineState> = _engineState.asStateFlow()

    // Reusable buffers to eliminate garbage collection pressure
    private val totalSamplePixels = config.sampleWidth * config.sampleHeight
    private val previousLumaBuffer = IntArray(totalSamplePixels) { -1 }
    private val currentLumaBuffer = IntArray(totalSamplePixels)
    private val pixelExtractBuffer = IntArray(totalSamplePixels)

    private var hasBaseline = false
    private var lastChangeDetectedTime = 0L
    private var lastMeaningfulDiffResult: VisualDiffResult? = null
    val lastDiffResult: VisualDiffResult? get() = lastMeaningfulDiffResult
    private var lastAnalyzedTimestamp = 0L
    private var pendingChangeAwaitingSettle = false

    fun isCooldownActive(): Boolean {
        return System.currentTimeMillis() - lastAnalyzedTimestamp < config.minAnalysisIntervalMs
    }

    /**
     * Resets the baseline and internal state. Call when starting a new Live View session.
     */
    @Synchronized
    fun reset() {
        hasBaseline = false
        for (i in 0 until totalSamplePixels) {
            previousLumaBuffer[i] = -1
        }
        lastChangeDetectedTime = 0L
        lastMeaningfulDiffResult = null
        pendingChangeAwaitingSettle = false
        _engineState.value = VisualEngineState.Idle
    }

    /**
     * Compares a newly captured screen frame against the previous baseline.
     *
     * @param screenBitmap The full-resolution in-memory screenshot bitmap.
     * @return [VisualDiffResult] indicating delta metrics, or null if baseline was just established.
     */
    @Synchronized
    fun processFrame(screenBitmap: Bitmap?): VisualDiffResult? {
        if (screenBitmap == null || screenBitmap.isRecycled) {
            return null
        }

        val currentTime = System.currentTimeMillis()

        // 1. Create temporary downsampled thumbnail (48x48)
        var thumbnail: Bitmap? = null
        try {
            thumbnail = Bitmap.createScaledBitmap(
                screenBitmap,
                config.sampleWidth,
                config.sampleHeight,
                true // bilinear filtering for smooth perceptual downsampling
            )

            // Extract pixels into buffer
            thumbnail.getPixels(
                pixelExtractBuffer,
                0,
                config.sampleWidth,
                0,
                0,
                config.sampleWidth,
                config.sampleHeight
            )

            // Convert RGB to perceptual Luminance (Y = 0.299R + 0.587G + 0.114B)
            for (i in 0 until totalSamplePixels) {
                val color = pixelExtractBuffer[i]
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                currentLumaBuffer[i] = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
            }
        } finally {
            // Immediately release thumbnail
            thumbnail?.recycle()
        }

        // 2. First frame initialization (establish baseline)
        if (!hasBaseline) {
            System.arraycopy(currentLumaBuffer, 0, previousLumaBuffer, 0, totalSamplePixels)
            hasBaseline = true
            _engineState.value = VisualEngineState.Monitoring(lastDeltaPercent = 0f)
            return null
        }

        // 3. Compute pixel differences against previous frame
        var changedPixels = 0
        for (i in 0 until totalSamplePixels) {
            val diff = abs(currentLumaBuffer[i] - previousLumaBuffer[i])
            if (diff >= config.pixelLumaThreshold) {
                changedPixels++
            }
        }

        val changePercentage = (changedPixels.toFloat() / totalSamplePixels.toFloat()) * 100f
        val isMeaningful = changePercentage >= config.meaningfulChangeThresholdPercent
        val isMajor = changePercentage >= config.majorTransitionThresholdPercent

        val diffResult = VisualDiffResult(
            changedPixelsCount = changedPixels,
            totalPixelsCount = totalSamplePixels,
            changePercentage = changePercentage,
            isMeaningfulChange = isMeaningful,
            isMajorTransition = isMajor,
            timestamp = currentTime
        )

        // 4. Update baseline buffer to current frame for the next delta step
        System.arraycopy(currentLumaBuffer, 0, previousLumaBuffer, 0, totalSamplePixels)

        // 5. State machine & Debounce logic
        val timeSinceLastAnalysis = currentTime - lastAnalyzedTimestamp
        val isCooldownActive = timeSinceLastAnalysis < config.minAnalysisIntervalMs
        val cooldownRemaining = if (isCooldownActive) config.minAnalysisIntervalMs - timeSinceLastAnalysis else 0L

        if (isMeaningful) {
            lastChangeDetectedTime = currentTime
            lastMeaningfulDiffResult = diffResult
            pendingChangeAwaitingSettle = true
            _engineState.value = VisualEngineState.ChangeDetected(
                deltaPercent = changePercentage,
                isMajorTransition = isMajor,
                timestamp = currentTime
            )
        } else if (pendingChangeAwaitingSettle) {
            // Screen was changing earlier and is now stable (scrolling or animation ended)
            val stableDuration = currentTime - lastChangeDetectedTime
            if (stableDuration >= config.settleDebounceMs) {
                _engineState.value = VisualEngineState.Monitoring(
                    lastDeltaPercent = changePercentage,
                    isSettling = false,
                    isCooldownActive = isCooldownActive,
                    cooldownRemainingMs = cooldownRemaining
                )
            } else {
                _engineState.value = VisualEngineState.Monitoring(
                    lastDeltaPercent = changePercentage,
                    isSettling = true,
                    isCooldownActive = isCooldownActive,
                    cooldownRemainingMs = cooldownRemaining
                )
            }
        } else {
            _engineState.value = VisualEngineState.Monitoring(
                lastDeltaPercent = changePercentage,
                isSettling = false,
                isCooldownActive = isCooldownActive,
                cooldownRemainingMs = cooldownRemaining
            )
        }

        return diffResult
    }

    /**
     * Determines whether the visual state should trigger a Gemini AI analysis right now.
     * Requires:
     * 1. A meaningful change occurred.
     * 2. The visual motion has settled (stable for >= [VisualChangeConfig.settleDebounceMs]).
     * 3. The cooldown period ([VisualChangeConfig.minAnalysisIntervalMs]) has elapsed.
     */
    @Synchronized
    fun shouldTriggerAnalysis(): Boolean {
        if (!pendingChangeAwaitingSettle) {
            return false
        }

        val currentTime = System.currentTimeMillis()
        val stableDuration = currentTime - lastChangeDetectedTime
        val timeSinceLastAnalysis = currentTime - lastAnalyzedTimestamp

        val isSettled = stableDuration >= config.settleDebounceMs
        val isCooldownElapsed = timeSinceLastAnalysis >= config.minAnalysisIntervalMs

        return isSettled && isCooldownElapsed
    }

    /**
     * Notifies the engine that an analysis has begun for the current settled change.
     */
    @Synchronized
    fun markAnalysisStarted() {
        val delta = lastMeaningfulDiffResult?.changePercentage ?: 0f
        lastAnalyzedTimestamp = System.currentTimeMillis()
        pendingChangeAwaitingSettle = false
        _engineState.value = VisualEngineState.Analyzing(deltaPercent = delta)
    }

    /**
     * Extends the cooldown (e.g. when a rate-limit 429 is encountered) to pause AI triggers.
     */
    @Synchronized
    fun extendCooldown(additionalMs: Long) {
        lastAnalyzedTimestamp = System.currentTimeMillis() + additionalMs - config.minAnalysisIntervalMs
        _engineState.value = VisualEngineState.Monitoring(
            lastDeltaPercent = 0f,
            isSettling = false,
            isCooldownActive = true,
            cooldownRemainingMs = additionalMs
        )
    }

    /**
     * Notifies the engine that analysis completed (or failed). Resumes monitoring.
     */
    @Synchronized
    fun markAnalysisFinished() {
        _engineState.value = VisualEngineState.Monitoring(
            lastDeltaPercent = 0f,
            isSettling = false,
            isCooldownActive = true,
            cooldownRemainingMs = config.minAnalysisIntervalMs
        )
    }
}
