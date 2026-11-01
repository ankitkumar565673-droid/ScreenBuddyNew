package com.example.model

/**
 * Navigation destinations for ScreenBuddy.
 */
enum class AppScreen {
    HOME,
    ASSISTANT,
    SCREEN_ACCESS_INFO,
    SCREEN_ACCESS,
    SCREEN_ANALYSIS,
    DEMO_CONVERSATION
}

/**
 * Message sender type for the mock assistant chat.
 */
enum class MessageSender {
    USER,
    ROBOT,
    SYSTEM_INFO
}

/**
 * Represents a single chat message in the mock assistant conversation.
 * Designed simply so database or remote persistence can be plugged in later.
 */
data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: String = "Just now",
    val isHighlighted: Boolean = false
)

/**
 * Feature card item data displayed on the main home screen.
 */
data class FeatureItem(
    val title: String,
    val description: String,
    val emoji: String,
    val tag: String
)

/**
 * Preset sample questions that demonstrate future ScreenBuddy capabilities.
 */
data class SampleQuestion(
    val title: String,
    val query: String,
    val sampleResponse: String,
    val emoji: String
)
