package com.example.model

/**
 * Lifecycle states of the persistent Chat Mode assistant engine.
 */
enum class ChatEngineState(val label: String) {
    OFF("OFF"),
    ACTIVE_IDLE("ACTIVE_IDLE"),
    WAITING_INCOMING("WAITING_INCOMING"),
    PROCESSING("PROCESSING"),
    SUGGESTION_VISIBLE("SUGGESTION_VISIBLE"),
    USER_TYPING("USER_TYPING"),
    WAITING_NEXT_INCOMING("WAITING_NEXT_INCOMING")
}

/**
 * Ephemeral in-memory representation of a suggested smart reply in Chat Mode.
 * Strictly RAM-only: NEVER persisted to disk, preferences, or databases.
 */
data class ChatModeSuggestion(
    val primaryReply: String,
    val candidateReplies: List<String> = emptyList(),
    val tone: ChatTone = ChatTone.NATURAL,
    val customToneText: String? = null,
    val sourcePackage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isProtectedContentNotice: Boolean = false,
    val isError: Boolean = false
) {
    /**
     * All available candidate replies including primary.
     */
    val allOptions: List<String>
        get() {
            if (candidateReplies.isEmpty()) return listOf(primaryReply)
            return if (candidateReplies.contains(primaryReply)) {
                candidateReplies
            } else {
                listOf(primaryReply) + candidateReplies
            }
        }
}
