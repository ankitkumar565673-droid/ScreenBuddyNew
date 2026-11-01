package com.example.core

/**
 * ============================================================================
 * SCREENBUDDY FUTURE ARCHITECTURE ROADMAP
 * ============================================================================
 * 
 * This file outlines the architectural contracts for ScreenBuddy's multi-level
 * roadmap. In this Level 1 release, these interfaces serve as the architectural
 * blueprint and placeholder contracts.
 * 
 * Level 1 (Current): Visual experience, basic navigation, and mock UI.
 * Level 2 (Upcoming): Android MediaProjection screen capture.
 * Level 3 (Upcoming): Vision AI API integration (Gemini Vision / on-device).
 * Level 4 (Upcoming): Floating assistant overlay (SYSTEM_ALERT_WINDOW).
 * Level 5 (Upcoming): Rich screen and accessibility context understanding.
 * Level 6 (Upcoming): Optional user-authorized actions.
 * Level 7 (Upcoming): Privacy controls and production hardening.
 */

/**
 * Level 2 Contract: Screen Capture Management.
 * Will handle MediaProjection token, virtual display, and frame extraction.
 */
interface ScreenCaptureManager {
    /** Whether screen capture permission is currently granted by user */
    val isCaptureGranted: Boolean

    /** Captures the current visible frame (stub for Level 2) */
    fun captureCurrentFrame(): ByteArray?
}

/**
 * Level 3 Contract: Vision AI Service.
 * Will send captured bitmap/context to AI and return structured guidance.
 */
interface VisionAiService {
    /** Analyzes captured screen with user query (stub for Level 3) */
    suspend fun analyzeScreen(imageBytes: ByteArray, query: String): String
}

/**
 * Level 4 Contract: Floating Assistant Overlay.
 * Will render the friendly robot as an on-screen floating bubble / pill.
 */
interface FloatingOverlayManager {
    /** Shows the floating robot assistant over other apps (stub for Level 4) */
    fun showOverlay()

    /** Hides the floating overlay */
    fun hideOverlay()
}

/**
 * Level 5 Contract: Context & Hierarchy Analyzer.
 * Will extract text, UI node semantics, and accessibility tree data.
 */
interface ScreenContextAnalyzer {
    /** Extracts text and semantics from active screen (stub for Level 5) */
    fun extractScreenSemantics(): Map<String, String>
}

/**
 * Level 6 Contract: User-Authorized Action Executor.
 * Will perform explicit user-approved interactions (e.g. clicking or typing).
 */
interface UserActionExecutor {
    /** Performs an explicit, user-authorized tap at coordinates (stub for Level 6) */
    fun executeTap(x: Float, y: Float): Boolean
}

/**
 * Level 7 Contract: Privacy & Data Security Guard.
 * Ensures zero unsolicited capture, ephemeral in-memory processing, and redactions.
 */
interface PrivacyGuard {
    /** Validates that a capture request has explicit user consent */
    fun verifyUserConsent(): Boolean

    /** Redacts sensitive PII areas (passwords, credit cards) before sending */
    fun redactSensitiveAreas(imageBytes: ByteArray): ByteArray
}
