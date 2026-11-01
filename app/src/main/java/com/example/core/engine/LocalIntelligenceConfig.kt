package com.example.core.engine

/**
 * Level 10 Configuration for ScreenBuddy Local Intelligence & Smart Context Engine.
 *
 * Centralized, configurable safeguards for API usage, request limits, duplicate thresholds,
 * and smart assistance settings.
 */
data class LocalIntelligenceConfig(
    /** Maximum AI requests allowed per session before auto-throttling */
    val maxAiRequestsPerSession: Int = 30,
    /** Minimum interval (ms) between AI calls */
    val minAnalysisIntervalMs: Long = 4000L,
    /** Maximum length of extracted text context to pass to AI */
    val maxTextContextLength: Int = 2000,
    /** Maximum image dimension (px) when vision analysis is required */
    val maxImageDimension: Int = 1080,
    /** Maximum image byte size allowed in RAM */
    val maxImageBytes: Int = 3 * 1024 * 1024,
    /** Threshold (0.0 .. 1.0) above which text or visual context is classified as a duplicate */
    val duplicateSimilarityThreshold: Float = 0.92f
)
