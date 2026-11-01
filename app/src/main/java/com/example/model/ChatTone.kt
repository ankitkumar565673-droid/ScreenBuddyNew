package com.example.model

/**
 * Supported conversation reply tones for ScreenBuddy CHAT MODE.
 * Strictly ephemeral RAM-only configuration.
 */
enum class ChatTone(
    val displayName: String,
    val promptInstruction: String,
    val emoji: String
) {
    NATURAL(
        displayName = "Auto / Natural",
        promptInstruction = "natural, balanced, context-appropriate human conversational tone",
        emoji = "✨"
    ),
    CASUAL(
        displayName = "Casual",
        promptInstruction = "relaxed, informal, modern, and friendly texting style",
        emoji = "😎"
    ),
    PROFESSIONAL(
        displayName = "Professional",
        promptInstruction = "polite, clear, professional, well-structured, and business-appropriate",
        emoji = "💼"
    ),
    FRIENDLY(
        displayName = "Friendly",
        promptInstruction = "warm, upbeat, helpful, enthusiastic, and kind",
        emoji = "😊"
    ),
    FUNNY(
        displayName = "Funny",
        promptInstruction = "witty, playful, humorous, and clever with good-natured humor",
        emoji = "😂"
    ),
    FLIRTY(
        displayName = "Flirty",
        promptInstruction = "charming, playful, subtle, witty, and engaging",
        emoji = "😉"
    ),
    SHORT(
        displayName = "Short",
        promptInstruction = "very concise, direct, 1-6 words only, punchy",
        emoji = "⚡"
    ),
    POLITE(
        displayName = "Polite",
        promptInstruction = "respectful, courteous, considerate, and gracious",
        emoji = "🙏"
    ),
    CONFIDENT(
        displayName = "Confident",
        promptInstruction = "assertive, decisive, clear, and reassuring",
        emoji = "💪"
    ),
    CUSTOM(
        displayName = "Custom",
        promptInstruction = "custom tone specified by user",
        emoji = "🎨"
    );

    companion object {
        fun fromString(name: String?): ChatTone {
            if (name.isNullOrBlank()) return NATURAL
            val trimmed = name.trim()
            return values().firstOrNull {
                it.name.equals(trimmed, ignoreCase = true) ||
                        it.displayName.equals(trimmed, ignoreCase = true)
            } ?: NATURAL
        }
    }
}
