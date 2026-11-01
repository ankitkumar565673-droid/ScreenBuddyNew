package com.example.core.engine

import com.example.core.diagnostics.ScreenBuddyDiagnostics
import java.util.Locale

/**
 * Level 12: Conversation Message Turn Model.
 * Ephemeral RAM-only representation of a message in an ongoing conversation.
 * Strictly RAM-only: NEVER persisted to disk, database, preferences, or logs.
 */
data class ConversationMessage(
    val sender: String? = null,
    val text: String,
    val isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Level 12: Short-Lived In-Memory Conversation Context Model.
 * Holds rolling message history, sender details, latest message, and compact summary.
 * Ephemeral RAM-only: NEVER written to storage or logs.
 */
data class ConversationContext(
    val appPackage: String? = null,
    val conversationHeader: String? = null,
    val recentMessages: List<ConversationMessage> = emptyList(),
    val latestMessage: ConversationMessage? = null,
    val conversationSummary: String? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val isSettled: Boolean = true,
    val isTypingActive: Boolean = false
)

/**
 * Result of evaluating conversation text changes and context updates.
 */
data class ConversationUpdateResult(
    val hasMeaningfulChange: Boolean,
    val isSettled: Boolean,
    val isDuplicate: Boolean,
    val shouldTriggerAi: Boolean,
    val formattedContextPrompt: String?,
    val decisionReason: String,
    val isUserSentMessage: Boolean = false
)

/**
 * Level 12: Conversation Context Engine.
 *
 * Responsibilities:
 * 1. Maintains a short-lived, RAM-only rolling context of active conversation threads.
 * 2. Parses extracted text into discrete messages and identifies speaker turns.
 * 3. Filters out intermediate typing noise ("H", "He", "Hello") and suppresses rapid un-settled message bursts via debouncing.
 * 4. Compresses older conversation history into a compact summary to keep prompts strictly bounded.
 * 5. Prevents unnecessary Gemini API requests by requiring settled, meaningful message changes before triggering AI.
 * 6. RAM-only: zero disk, DB, or preference persistence, zero private content logging.
 */
class ConversationContextEngine(
    private val maxRecentMessages: Int = 6,
    private val debounceDelayMs: Long = 1800L
) {
    private var activeContext: ConversationContext = ConversationContext()
    private var lastRawText: String? = null
    private var lastTypingCandidate: String? = null
    private var lastSettledTextTimestamp: Long = 0L

    private var previousContext: ConversationContext? = null
    private var previousRawText: String? = null
    private var previousSettledTextTimestamp: Long = 0L

    /**
     * Retrieves the current ephemeral in-memory conversation context.
     */
    @Synchronized
    fun getContext(): ConversationContext = activeContext

    /**
     * Rolls back conversation context to the state prior to the last updateAndEvaluate call.
     * Useful when an AI trigger is suppressed due to request-in-flight or rate limiting.
     */
    @Synchronized
    fun rollback() {
        activeContext = previousContext ?: activeContext
        lastRawText = previousRawText
        lastSettledTextTimestamp = previousSettledTextTimestamp
    }

    /**
     * Resets conversation context completely (e.g., when Live Assist stops, app package changes, or user clears context).
     */
    @Synchronized
    fun clearContext() {
        activeContext = ConversationContext()
        lastRawText = null
        lastTypingCandidate = null
        lastSettledTextTimestamp = 0L
        ScreenBuddyDiagnostics.recordConversationContextReset()
    }

    /**
     * Evaluates new extracted text and package context to update conversation state.
     *
     * @param rawText Extracted text content from the screen.
     * @param appPackage Package name of the foreground application.
     * @param isKeyboardActive True if an on-screen keyboard is visible (typing active).
     * @param currentTimeMs Current timestamp in milliseconds.
     * @return [ConversationUpdateResult] containing evaluation decisions and formatted context.
     */
    @Synchronized
    fun updateAndEvaluate(
        rawText: String?,
        appPackage: String?,
        isKeyboardActive: Boolean,
        currentTimeMs: Long = System.currentTimeMillis()
    ): ConversationUpdateResult {
        previousContext = activeContext
        previousRawText = lastRawText
        previousSettledTextTimestamp = lastSettledTextTimestamp

        if (rawText.isNullOrBlank()) {
            return ConversationUpdateResult(
                hasMeaningfulChange = false,
                isSettled = true,
                isDuplicate = true,
                shouldTriggerAi = false,
                formattedContextPrompt = null,
                decisionReason = "No text content available"
            )
        }

        val trimmedText = rawText.trim()
        val currentPackage = appPackage?.lowercase(Locale.ROOT)?.trim()

        // 1. Context switch check: Clear context if package changed
        if (currentPackage != null && activeContext.appPackage != null && currentPackage != activeContext.appPackage) {
            clearContext()
        }

        // 2. Typing / Intermediate state check
        val isTyping = isKeyboardActive || isIntermediateTyping(trimmedText, lastRawText)
        if (isTyping) {
            lastTypingCandidate = trimmedText
            activeContext = activeContext.copy(
                appPackage = currentPackage,
                isSettled = false,
                isTypingActive = true,
                lastUpdatedTimestamp = currentTimeMs
            )
            return ConversationUpdateResult(
                hasMeaningfulChange = false,
                isSettled = false,
                isDuplicate = false,
                shouldTriggerAi = false,
                formattedContextPrompt = null,
                decisionReason = "User actively typing; intermediate state suppressed"
            )
        }

        // 3. Duplicate text check (exact match on settled state)
        if (trimmedText == lastRawText && activeContext.isSettled) {
            return ConversationUpdateResult(
                hasMeaningfulChange = false,
                isSettled = true,
                isDuplicate = true,
                shouldTriggerAi = false,
                formattedContextPrompt = getFormattedPromptContext(),
                decisionReason = "Duplicate unchanged conversation text"
            )
        }

        // 4. Debounce check: Rapid message arrival batching
        val timeSinceLastUpdate = currentTimeMs - activeContext.lastUpdatedTimestamp
        val isSettlingWindowActive = lastRawText != null && timeSinceLastUpdate < debounceDelayMs

        val parsedMessages = parseMessagesFromText(trimmedText)
        var newMessagesDetected = extractNewMessages(parsedMessages, activeContext.recentMessages)

        // Intra-app conversation switch check: if completely new thread with 0 overlap with existing context
        if (activeContext.recentMessages.isNotEmpty() && parsedMessages.isNotEmpty() && newMessagesDetected.size == parsedMessages.size) {
            clearContext()
            activeContext = activeContext.copy(appPackage = currentPackage)
            newMessagesDetected = parsedMessages
        }

        if (newMessagesDetected.isEmpty() && trimmedText == lastRawText) {
            return ConversationUpdateResult(
                hasMeaningfulChange = false,
                isSettled = true,
                isDuplicate = true,
                shouldTriggerAi = false,
                formattedContextPrompt = getFormattedPromptContext(),
                decisionReason = "No new conversation messages detected"
            )
        }

        // If batching rapid incoming messages within settling window
        if (isSettlingWindowActive && newMessagesDetected.isNotEmpty()) {
            val updatedList = (activeContext.recentMessages + newMessagesDetected).distinctBy { it.text.trim().lowercase(Locale.ROOT) }
            activeContext = activeContext.copy(
                appPackage = currentPackage,
                recentMessages = updatedList,
                latestMessage = newMessagesDetected.lastOrNull(),
                lastUpdatedTimestamp = currentTimeMs,
                isSettled = false,
                isTypingActive = false
            )
            return ConversationUpdateResult(
                hasMeaningfulChange = true,
                isSettled = false,
                isDuplicate = false,
                shouldTriggerAi = false,
                formattedContextPrompt = null,
                decisionReason = "Rapid messages arriving; batched into settling window"
            )
        }

        // 5. Settled change: Maintain rolling context and update summary if bounded capacity exceeded
        val allMessages = (activeContext.recentMessages + newMessagesDetected).distinctBy { it.text.trim().lowercase(Locale.ROOT) }
        val (rollingRecent, updatedSummary) = buildRollingContext(allMessages, activeContext.conversationSummary)

        val isUserSent = newMessagesDetected.isNotEmpty() && newMessagesDetected.lastOrNull()?.isUser == true

        activeContext = ConversationContext(
            appPackage = currentPackage,
            recentMessages = rollingRecent,
            latestMessage = rollingRecent.lastOrNull(),
            conversationSummary = updatedSummary,
            lastUpdatedTimestamp = currentTimeMs,
            confidence = if (rollingRecent.isNotEmpty()) 0.95f else 0.7f,
            isSettled = true,
            isTypingActive = false
        )

        lastRawText = trimmedText
        lastSettledTextTimestamp = currentTimeMs

        if (isUserSent) {
            return ConversationUpdateResult(
                hasMeaningfulChange = true,
                isSettled = true,
                isDuplicate = false,
                shouldTriggerAi = false,
                formattedContextPrompt = getFormattedPromptContext(),
                decisionReason = "User sent outgoing message; waiting for other person to reply",
                isUserSentMessage = true
            )
        }

        return ConversationUpdateResult(
            hasMeaningfulChange = true,
            isSettled = true,
            isDuplicate = false,
            shouldTriggerAi = true,
            formattedContextPrompt = getFormattedPromptContext(),
            decisionReason = "New settled conversation context ready for AI analysis",
            isUserSentMessage = false
        )
    }

    /**
     * Identifies if current text represents an incremental typing state of previous text.
     */
    fun isIntermediateTyping(current: String, previous: String?): Boolean {
        if (previous == null) return false
        val c = current.trim()
        val p = previous.trim()

        if (c == p) return false

        // Check incremental character addition (e.g., "H" -> "He" -> "Hel" -> "Hello")
        if (c.startsWith(p) && (c.length - p.length) in 1..8) return true
        if (p.startsWith(c) && (p.length - c.length) in 1..8) return true

        return false
    }

    /**
     * Parses raw screen text line-by-line into discrete conversation turns.
     */
    fun parseMessagesFromText(text: String): List<ConversationMessage> {
        val normalizedText = text.replace(Regex("""(?<=[.!?])\s+([A-Za-z0-9_]{1,20}:\s+)"""), "\n$1")
        val lines = normalizedText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val messages = mutableListOf<ConversationMessage>()

        val ignoredCues = setOf(
            "type a message", "online", "active now", "delivered", "read", "sent",
            "chats", "message", "today", "yesterday", "typing..."
        )

        for (line in lines) {
            val lower = line.lowercase(Locale.ROOT)
            if (ignoredCues.any { lower == it || lower.startsWith("$it ") }) continue
            if (line.length < 2) continue

            // Parse "Sender: Message" format
            if (line.contains(":") && !line.startsWith("http")) {
                val parts = line.split(":", limit = 2)
                val sender = parts[0].trim()
                val body = parts[1].trim()
                if (body.isNotBlank() && sender.length < 25) {
                    val isUser = sender.equals("me", ignoreCase = true) || sender.equals("you", ignoreCase = true)
                    messages.add(ConversationMessage(sender = sender, text = body, isUser = isUser))
                    continue
                }
            }

            // Standalone message turn
            messages.add(ConversationMessage(sender = null, text = line, isUser = false))
        }

        return messages
    }

    /**
     * Extracts new messages from newParse that are not in existingMessages.
     */
    private fun extractNewMessages(
        newParse: List<ConversationMessage>,
        existingMessages: List<ConversationMessage>
    ): List<ConversationMessage> {
        val existingTexts = existingMessages.map { it.text.trim().lowercase(Locale.ROOT) }.toSet()
        return newParse.filter { !existingTexts.contains(it.text.trim().lowercase(Locale.ROOT)) }
    }

    /**
     * Bounded Rolling Context: Keeps last [maxRecentMessages] in [recentMessages] and condenses older ones into [conversationSummary].
     */
    private fun buildRollingContext(
        allMessages: List<ConversationMessage>,
        existingSummary: String?
    ): Pair<List<ConversationMessage>, String?> {
        if (allMessages.size <= maxRecentMessages) {
            return Pair(allMessages, existingSummary)
        }

        val olderMessages = allMessages.dropLast(maxRecentMessages)
        val recentMessages = allMessages.takeLast(maxRecentMessages)

        val olderSummaryText = olderMessages.joinToString("; ") { msg ->
            if (msg.sender != null) "${msg.sender}: ${msg.text}" else msg.text
        }.take(300)

        val combinedSummary = if (existingSummary.isNullOrBlank()) {
            "Earlier conversation: $olderSummaryText"
        } else {
            "$existingSummary | Subsequent: $olderSummaryText".take(400)
        }

        return Pair(recentMessages, combinedSummary)
    }

    /**
     * Formats bounded, compact conversation context prompt for Gemini.
     */
    @Synchronized
    fun getFormattedPromptContext(): String? {
        val ctx = activeContext
        if (ctx.recentMessages.isEmpty() && ctx.conversationSummary.isNullOrBlank()) {
            return null
        }

        val sb = StringBuilder()
        sb.append("[CONVERSATION_CONTEXT]\n")
        if (!ctx.conversationSummary.isNullOrBlank()) {
            sb.append("Summary of earlier conversation: ").append(ctx.conversationSummary).append("\n")
        }
        sb.append("Recent messages:\n")
        for (msg in ctx.recentMessages) {
            val prefix = when {
                msg.isUser -> "User: "
                !msg.sender.isNullOrBlank() -> "${msg.sender}: "
                else -> "Person: "
            }
            sb.append("- ").append(prefix).append(msg.text).append("\n")
        }
        if (ctx.latestMessage != null) {
            sb.append("Latest message to reply to: ").append(ctx.latestMessage.text).append("\n")
        }

        return sb.toString()
    }

    /**
     * Formats bounded, compact conversation context prompt with structured tone metadata for Chat Mode.
     */
    @Synchronized
    fun getFormattedPromptContext(
        tone: com.example.model.ChatTone,
        customToneText: String? = null
    ): String? {
        val basePrompt = getFormattedPromptContext() ?: return null
        val sb = StringBuilder(basePrompt)
        val effectiveToneName = if (tone == com.example.model.ChatTone.CUSTOM && !customToneText.isNullOrBlank()) {
            customToneText.trim()
        } else {
            tone.displayName
        }
        sb.append("\n[CHAT_MODE_INSTRUCTIONS]\n")
        sb.append("Requested Reply Tone: ").append(effectiveToneName).append(" (").append(tone.promptInstruction).append(")\n")
        sb.append("Format: Suggest a natural, human-sounding reply to the latest message. Do NOT add meta commentary or labels like 'Here is a response:'. Sound authentic to a human texting.\n")
        return sb.toString()
    }

    /**
     * Seeds the RAM-only conversation context with external/extracted text (e.g. from Vision AI OCR).
     */
    @Synchronized
    fun seedContext(rawText: String, appPackage: String?) {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return
        val pkg = appPackage?.lowercase(Locale.ROOT)?.trim()
        val parsed = parseMessagesFromText(trimmed)
        if (parsed.isEmpty()) return
        val (rolling, summary) = buildRollingContext(parsed, activeContext.conversationSummary)
        activeContext = ConversationContext(
            appPackage = pkg,
            recentMessages = rolling,
            latestMessage = rolling.lastOrNull(),
            conversationSummary = summary,
            lastUpdatedTimestamp = System.currentTimeMillis(),
            isSettled = true,
            isTypingActive = false
        )
        lastRawText = trimmed
        lastSettledTextTimestamp = System.currentTimeMillis()
    }
}
