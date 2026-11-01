package com.example.core.engine

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Level 10 Context Duplicate Detector.
 *
 * Responsibilities:
 * 1. Computes ephemeral in-memory fingerprints of screen text & visual state.
 * 2. Detects identical or near-duplicate screens to prevent sending repeated requests to AI.
 * 3. RAM-only: zero persistence, no logging of screen content.
 */
class ContextDuplicateDetector(
    private val similarityThreshold: Float = 0.92f
) {
    private var lastFingerprint: String? = null
    private var lastExtractedText: String? = null
    private var lastAnalyzedTime: Long = 0L

    /**
     * Resets the duplicate detector state.
     */
    @Synchronized
    fun reset() {
        lastFingerprint = null
        lastExtractedText = null
        lastAnalyzedTime = 0L
    }

    /**
     * Checks if the given context/text is a duplicate or near-duplicate of the recently analyzed context.
     *
     * @param context Current extracted [LocalContext].
     * @param changePercentage Delta percent from VisualChangeDetector.
     * @return True if classified as a duplicate that should be suppressed.
     */
    @Synchronized
    fun isDuplicate(context: LocalContext, changePercentage: Float = 0f): Boolean {
        val currentText = context.extractedText?.trim()

        // If no text and minimal visual change, it's duplicate visual state
        if (currentText.isNullOrBlank() && changePercentage < 5.0f && lastFingerprint != null) {
            return true
        }

        if (!currentText.isNullOrBlank() && !lastExtractedText.isNullOrBlank()) {
            val similarity = computeTextSimilarity(currentText, lastExtractedText!!)
            if (similarity >= similarityThreshold && changePercentage < 15.0f) {
                return true
            }
        }

        // Generate fingerprint
        val fingerprint = computeFingerprint(context.foregroundAppPackage, currentText)
        if (fingerprint == lastFingerprint && changePercentage < 10.0f) {
            return true
        }

        return false
    }

    /**
     * Records a newly sent analysis request to prevent duplicates.
     */
    @Synchronized
    fun markAnalyzed(context: LocalContext) {
        val currentText = context.extractedText?.trim()
        lastExtractedText = currentText
        lastFingerprint = computeFingerprint(context.foregroundAppPackage, currentText)
        lastAnalyzedTime = System.currentTimeMillis()
    }

    /**
     * Computes Normalized Levenshtein Distance similarity (0.0 .. 1.0).
     */
    fun computeTextSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isBlank() || s2.isBlank()) return 0.0f

        val len1 = s1.length
        val len2 = s2.length
        val maxLen = max(len1, len2)
        if (maxLen == 0) return 1.0f

        val dist = levenshteinDistance(s1.lowercase(Locale.ROOT), s2.lowercase(Locale.ROOT))
        return 1.0f - (dist.toFloat() / maxLen.toFloat())
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        var costs = IntArray(rhs.length + 1) { it }
        for (i in 1..lhs.length) {
            val newCosts = IntArray(rhs.length + 1)
            newCosts[0] = i
            for (j in 1..rhs.length) {
                val match = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                val costReplace = costs[j - 1] + match
                val costInsert = costs[j] + 1
                val costDelete = newCosts[j - 1] + 1
                newCosts[j] = min(min(costInsert, costDelete), costReplace)
            }
            costs = newCosts
        }
        return costs[rhs.length]
    }

    private fun computeFingerprint(pkg: String?, text: String?): String {
        val raw = "${pkg ?: "unknown"}:${text ?: "empty"}"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
