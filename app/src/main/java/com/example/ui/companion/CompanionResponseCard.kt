package com.example.ui.companion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.capture.FrameValidationResult
import com.example.core.capture.FrameValidator
import com.example.core.ai.CompanionReplyOption
import com.example.core.ai.CompanionSuggestedAction
import com.example.core.ai.ScreenBuddyAiResponse
import com.example.core.ai.ScreenType
import com.example.core.ai.UserIntent
import com.example.core.ai.VisionAiClient
import com.example.core.ai.VisionAiResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Structured AI Response Card for ScreenBuddy (Level 8).
 *
 * Capabilities:
 * - Detects and displays visual screen type tag (e.g., 💬 Conversation, 📝 Form, 🐞 Error / Code, 🌐 Webpage).
 * - Multi-tone reply options (Friendly, Professional, Quick, Playful) with 1-tap clipboard copy.
 * - Dynamic suggested follow-up actions (interactive chips triggering subsequent analysis).
 * - Short-lived in-memory conversational context for follow-up questions.
 * - Reactive cute companion emotional state transitions.
 * - Strict RAM-only ephemeral visual processing.
 */
/**
 * Single turn in an ephemeral text follow-up session.
 */
data class CompanionFollowUpTurn(val question: String, val answer: String)

@Composable
fun CompanionResponseCard(
    action: CompanionActionType,
    initialBitmap: Bitmap?,
    onCaptureScreen: suspend () -> Bitmap?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val actionChipsScrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var companionState by remember { mutableStateOf(CompanionState.LOOKING) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var elapsedTimeSeconds by remember { mutableLongStateOf(0L) }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    var structuredResponse by remember { mutableStateOf<ScreenBuddyAiResponse?>(null) }
    var rawAnswerText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRateLimited by remember { mutableStateOf(false) }
    var cooldownSecondsRemaining by remember { mutableIntStateOf(0) }

    // Follow-up conversation turns (in-memory only)
    val conversationTurns = remember { mutableStateListOf<CompanionFollowUpTurn>() }

    // Ephemeral in-memory context (never written to disk or database)
    var inMemoryContext by remember { mutableStateOf<String?>(null) }

    var followUpInput by rememberSaveable { mutableStateOf("") }
    var copiedReplyIndex by remember { mutableStateOf<Int?>(null) }
    var mainTextCopied by remember { mutableStateOf(false) }

    var activeRequestId by remember { mutableLongStateOf(0L) }
    var sessionClearedNotice by remember { mutableStateOf(false) }

    fun clearSession() {
        activeJob?.cancel()
        ++activeRequestId
        isAnalyzing = false
        isCapturing = false
        conversationTurns.clear()
        inMemoryContext = null
        structuredResponse = null
        rawAnswerText = null
        followUpInput = ""
        errorMessage = null
        companionState = CompanionState.HAPPY
        sessionClearedNotice = true
        com.example.core.diagnostics.ScreenBuddyDiagnostics.recordConversationContextReset()
    }

    fun closeCard() {
        activeJob?.cancel()
        ++activeRequestId
        isAnalyzing = false
        isCapturing = false
        onClose()
    }

    fun buildRecentContextString(resp: ScreenBuddyAiResponse?, turns: List<CompanionFollowUpTurn>): String {
        val sb = StringBuilder()
        if (resp != null) {
            sb.appendLine("=== [SCREEN-DERIVED VISUAL CONTEXT] ===")
            sb.appendLine("Screen Category: ${resp.screenType.displayName}")
            if (!resp.screenTitle.isNullOrBlank()) {
                sb.appendLine("Screen Title / Chat Partner: ${resp.screenTitle}")
            }
            if (resp.summary.isNotBlank()) {
                sb.appendLine("Screen Summary: ${resp.summary}")
            }
            if (resp.mainInsight.isNotBlank()) {
                sb.appendLine("Screen Insight: ${resp.mainInsight}")
            }
            if (resp.suggestedReplies.isNotEmpty()) {
                sb.appendLine("=== [PREVIOUSLY SUGGESTED REPLIES DRAFTED BY SCREENBUDDY] ===")
                sb.appendLine("(Note: These are AI-suggested candidate replies drafted for the user. They are NOT incoming messages from the contact.)")
                resp.suggestedReplies.forEachIndexed { i, r ->
                    sb.appendLine("Option ${i + 1} [${r.toneLabel}]: \"${r.text}\"")
                }
            }
        }
        if (turns.isNotEmpty()) {
            sb.appendLine("=== [RECENT CONVERSATION TURNS] ===")
            turns.takeLast(4).forEachIndexed { idx, t ->
                sb.appendLine("Turn ${idx + 1} User: ${t.question}")
                sb.appendLine("Turn ${idx + 1} ScreenBuddy: ${t.answer}")
            }
        }
        return sb.toString().trim()
    }

    fun executeAnalysis(
        customPrompt: String? = null,
        customIntent: UserIntent? = null,
        forceFreshCapture: Boolean = false
    ) {
        if (isAnalyzing || isCapturing) {
            com.example.core.diagnostics.ScreenBuddyDiagnostics.recordSuppression("IN_FLIGHT")
            return
        }
        if (cooldownSecondsRemaining > 0 && !forceFreshCapture) {
            return
        }

        keyboardController?.hide()
        focusManager.clearFocus()
        errorMessage = null
        sessionClearedNotice = false

        val isTextFollowUp = customPrompt != null && !forceFreshCapture && (structuredResponse != null || inMemoryContext != null)
        val requestId = ++activeRequestId
        activeJob?.cancel()
        activeJob = coroutineScope.launch {
            try {
                com.example.core.diagnostics.ScreenBuddyDiagnostics.recordChatAiRequestStart(requestId)
                val effectivePrompt = customPrompt ?: action.defaultPrompt
                val effectiveIntent = customIntent ?: if (isTextFollowUp && (customPrompt.contains("reply", ignoreCase = true) || customPrompt.contains("option", ignoreCase = true))) {
                    UserIntent.REPLY
                } else {
                    action.intent
                }

                val aiService = VisionAiClient.getService(context)

                val result = if (isTextFollowUp) {
                    // Fast text-only follow-up: No screenshot re-capture, no buffer re-transmission
                    isAnalyzing = true
                    companionState = CompanionState.THINKING
                    val richContext = buildRecentContextString(structuredResponse, conversationTurns)

                    aiService.analyzeScreenshot(
                        bitmap = null,
                        question = effectivePrompt,
                        intent = effectiveIntent,
                        recentContext = richContext
                    )
                } else {
                    // Screen-grounded visual analysis
                    isCapturing = true
                    companionState = CompanionState.LOOKING
                    val bitmap = if (forceFreshCapture) {
                        onCaptureScreen() ?: initialBitmap
                    } else {
                        initialBitmap ?: onCaptureScreen()
                    }
                    isCapturing = false

                    if (!isActive || activeRequestId != requestId) return@launch

                    if (bitmap == null || bitmap.isRecycled) {
                        companionState = CompanionState.ERROR
                        errorMessage = "I can't see the screen yet — please ensure screen capture is active and try again."
                        return@launch
                    }

                    val validation = FrameValidator.validateFrame(bitmap)
                    if (validation is FrameValidationResult.Invalid) {
                        companionState = CompanionState.CONFUSED
                        errorMessage = validation.userFriendlyMessage
                        return@launch
                    }

                    isAnalyzing = true
                    companionState = CompanionState.THINKING

                    aiService.analyzeScreenshot(
                        bitmap = bitmap,
                        question = effectivePrompt,
                        intent = effectiveIntent,
                        recentContext = inMemoryContext
                    )
                }

                if (!isActive || activeRequestId != requestId) {
                    com.example.core.diagnostics.ScreenBuddyDiagnostics.recordStaleResponseDiscarded(requestId, "REQUEST_CANCELLED_OR_SUPERSEDED")
                    return@launch
                }
                isAnalyzing = false

                when (result) {
                    is VisionAiResult.Success -> {
                        isRateLimited = false
                        cooldownSecondsRemaining = 0
                        val resp = result.structuredResponse
                        val answerText = resp?.mainInsight?.ifBlank { resp.summary } ?: result.answer

                        if (isTextFollowUp) {
                            // Append turn to conversation history
                            conversationTurns.add(CompanionFollowUpTurn(effectivePrompt, answerText))
                            rawAnswerText = answerText
                            // Handle reply updates, explanations, or topic shifts
                            if (resp != null) {
                                if (resp.suggestedReplies.isNotEmpty()) {
                                    structuredResponse = resp.copy(
                                        suggestedReplies = resp.suggestedReplies,
                                        mainInsight = answerText
                                    )
                                } else {
                                    val isExplanation = effectivePrompt.contains("why", ignoreCase = true) ||
                                            effectivePrompt.contains("explain", ignoreCase = true) ||
                                            effectivePrompt.contains("reason", ignoreCase = true)
                                    if (isExplanation && structuredResponse?.suggestedReplies?.isNotEmpty() == true) {
                                        // Keep existing replies visible while displaying the reasoning
                                        structuredResponse = structuredResponse?.copy(mainInsight = answerText)
                                    } else {
                                        // Topic shift / unrelated query: clear old replies so they don't linger under unrelated answer
                                        structuredResponse = resp.copy(
                                            suggestedReplies = emptyList(),
                                            mainInsight = answerText
                                        )
                                    }
                                }
                            }
                            companionState = CompanionState.HAPPY
                            inMemoryContext = buildRecentContextString(structuredResponse, conversationTurns)
                        } else {
                            if (resp != null) {
                                structuredResponse = resp
                                rawAnswerText = resp.mainInsight
                                companionState = when {
                                    resp.confidence < 0.60f || resp.screenType == ScreenType.UNKNOWN -> CompanionState.CONFUSED
                                    resp.screenType == ScreenType.CONVERSATION -> CompanionState.HAPPY
                                    else -> CompanionState.HAPPY
                                }
                                conversationTurns.clear()
                                inMemoryContext = buildRecentContextString(resp, conversationTurns)
                            } else {
                                rawAnswerText = result.answer
                                companionState = CompanionState.HAPPY
                                conversationTurns.clear()
                                inMemoryContext = "Previous Query: $effectivePrompt\nAI Insight: ${result.answer.take(300)}"
                            }
                        }
                    }
                    is VisionAiResult.Error -> {
                        val rateLimited = result.errorType == "RATE_LIMITED" || result.retryAfterSeconds > 0
                        isRateLimited = rateLimited
                        if (rateLimited) {
                            companionState = CompanionState.SLEEPY
                            cooldownSecondsRemaining = if (result.retryAfterSeconds > 0) result.retryAfterSeconds else 30
                            errorMessage = result.message.ifBlank { "AI is taking a tiny break ☕ Please try again shortly." }
                        } else {
                            companionState = CompanionState.ERROR
                            errorMessage = result.message
                            cooldownSecondsRemaining = 0
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cooperative coroutine cancellation: rethrow so structured concurrency works correctly
                throw e
            } catch (e: Exception) {
                if (activeRequestId == requestId) {
                    isCapturing = false
                    isAnalyzing = false
                    companionState = CompanionState.ERROR
                    errorMessage = "ScreenBuddy couldn't complete the analysis. Please try again."
                }
            }
        }
    }

    LaunchedEffect(action) {
        executeAnalysis()
    }

    // Elapsed timer during analysis
    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            val startTime = System.currentTimeMillis()
            elapsedTimeSeconds = 0L
            while (isActive && isAnalyzing) {
                delay(1000)
                elapsedTimeSeconds = (System.currentTimeMillis() - startTime) / 1000
            }
        }
    }

    // Reset copied indicators
    LaunchedEffect(mainTextCopied) {
        if (mainTextCopied) {
            delay(2000)
            mainTextCopied = false
        }
    }
    LaunchedEffect(copiedReplyIndex) {
        if (copiedReplyIndex != null) {
            delay(2000)
            copiedReplyIndex = null
        }
    }

    // Live Cooldown countdown ticker
    LaunchedEffect(isRateLimited, cooldownSecondsRemaining) {
        if (cooldownSecondsRemaining > 0) {
            while (isActive && cooldownSecondsRemaining > 0) {
                delay(1000)
                cooldownSecondsRemaining -= 1
            }
        }
    }

    fun copyTextToClipboard(text: String, replyIndex: Int? = null) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("ScreenBuddy", text)
            clipboard.setPrimaryClip(clip)
            if (replyIndex != null) {
                copiedReplyIndex = replyIndex
            } else {
                mainTextCopied = true
            }
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.92f)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0B1120),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF6366F1).copy(alpha = 0.7f),
                        Color(0xFF38BDF8).copy(alpha = 0.4f),
                        Color(0xFF1E293B)
                    )
                )
            ),
            shadowElevation = 16.dp,
            modifier = modifier
                .width(320.dp)
                .testTag("companion_response_card")
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header with reactive cute companion & action title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CuteCompanion(
                            state = companionState,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = action.emoji,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = action.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 13.5.sp
                                    )
                                )
                            }
                            Text(
                                text = when (companionState) {
                                    CompanionState.LOOKING -> "Looking at screen..."
                                    CompanionState.THINKING -> "Analyzing..."
                                    CompanionState.HAPPY -> "Analysis ready"
                                    CompanionState.CONFUSED -> "Uncertain context"
                                    CompanionState.ERROR -> "Couldn't complete"
                                    else -> "AI Companion"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (companionState == CompanionState.ERROR) Color(0xFFF87171) else Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { clearSession() },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("companion_clear_session_button"),
                            enabled = !isAnalyzing
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Clear Session",
                                tint = if (conversationTurns.isNotEmpty() || structuredResponse != null) Color(0xFF38BDF8) else Color(0xFF64748B),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { closeCard() },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detected Screen Category Tag & Privacy Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val currentScreenType = structuredResponse?.screenType ?: ScreenType.UNKNOWN
                    val screenTitle = structuredResponse?.screenTitle

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.testTag("screen_category_tag")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = currentScreenType.emoji,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (!screenTitle.isNullOrBlank()) "${currentScreenType.displayName} • $screenTitle" else currentScreenType.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "RAM only",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                // Session cleared banner notice (Level 13)
                if (sessionClearedNotice) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF38BDF8)))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_cleared_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✨", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Session cleared. Clean slate for a new topic!",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            IconButton(
                                onClick = { sessionClearedNotice = false },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isAnalyzing || isCapturing) {
                    // Loading State
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF38BDF8)))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF38BDF8),
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isCapturing) "Capturing clean screen..." else "ScreenBuddy is analyzing your screen...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )

                            if (elapsedTimeSeconds >= 7) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Analyzing visual details...",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    activeJob?.cancel()
                                    isAnalyzing = false
                                    isCapturing = false
                                    companionState = CompanionState.IDLE
                                },
                                shape = CircleShape,
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Cancel", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                } else if (structuredResponse != null || rawAnswerText != null) {
                    val response = structuredResponse
                    val replies = response?.suggestedReplies ?: emptyList()
                    val summaryText = response?.summary
                    val mainAnswer = response?.mainInsight ?: rawAnswerText ?: ""

                    // 1. Summary box (if distinct and present)
                    if (!summaryText.isNullOrBlank() && summaryText != mainAnswer) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(9.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "💡",
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = summaryText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }

                    // 2. Main Insight / Answer card
                    if (mainAnswer.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E293B),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF38BDF8)))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = mainAnswer,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White,
                                        lineHeight = 17.5.sp,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.testTag("companion_response_text")
                                 )
                            }
                        }
                    }

                    // 3. Multi-Tone Reply Suggestions (when screen is a Conversation)
                    if (replies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Suggested Replies",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "Tap card or button to copy",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 9.5.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            replies.forEachIndexed { index, option ->
                                val isCopied = copiedReplyIndex == index
                                val toneLower = option.toneLabel.lowercase()
                                val (toneBadgeColor, toneBorderColor, toneIcon) = when {
                                    toneLower.contains("friendly") -> Triple(Color(0xFF38BDF8), Color(0xFF0284C7), "😊")
                                    toneLower.contains("casual") -> Triple(Color(0xFFF59E0B), Color(0xFFD97706), "😎")
                                    toneLower.contains("funny") || toneLower.contains("playful") -> Triple(Color(0xFFEC4899), Color(0xFFDB2777), "😄")
                                    toneLower.contains("confident") -> Triple(Color(0xFFA855F7), Color(0xFF7E22CE), "💪")
                                    toneLower.contains("prof") || toneLower.contains("formal") -> Triple(Color(0xFF818CF8), Color(0xFF4F46E5), "💼")
                                    toneLower.contains("short") || toneLower.contains("quick") -> Triple(Color(0xFF34D399), Color(0xFF059669), "⚡")
                                    toneLower.contains("empath") || toneLower.contains("warm") || toneLower.contains("supportive") -> Triple(Color(0xFFF472B6), Color(0xFFDB2777), "❤️")
                                    toneLower.contains("polite") || toneLower.contains("respect") -> Triple(Color(0xFF6EE7B7), Color(0xFF059669), "🙏")
                                    toneLower.contains("clarif") -> Triple(Color(0xFFFCD34D), Color(0xFFD97706), "❓")
                                    else -> Triple(Color(0xFF38BDF8), Color(0xFF4F46E5), "✨")
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isCopied) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF1E293B),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(
                                            if (isCopied) listOf(Color(0xFF10B981), Color(0xFF34D399))
                                            else listOf(toneBorderColor.copy(alpha = 0.7f), Color(0xFF1E293B))
                                        )
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { copyTextToClipboard(option.text, index) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = toneIcon,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = option.toneLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCopied) Color(0xFF34D399) else toneBadgeColor,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }

                                            Surface(
                                                shape = CircleShape,
                                                color = if (isCopied) Color(0xFF10B981) else Color(0xFF334155),
                                                modifier = Modifier.clickable {
                                                    copyTextToClipboard(option.text, index)
                                                }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                        contentDescription = "Copy reply",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = if (isCopied) "Copied!" else "Copy",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(5.dp))

                                        Text(
                                            text = option.text,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                lineHeight = 16.5.sp,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🔒 ScreenBuddy suggests replies only. Never types or sends automatically.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 9.5.sp,
                                lineHeight = 13.sp
                            ),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    } else if (action == CompanionActionType.REPLY) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF38BDF8).copy(alpha = 0.5f), Color(0xFF6366F1).copy(alpha = 0.3f))
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("no_replies_banner")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💬", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "No incoming message detected",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Make sure a chat conversation is visible on screen, then tap Re-analyze. Or type what you'd like to reply below.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                )
                            }
                        }
                    }

                    // Follow-up conversation turns (text chat history)
                    if (conversationTurns.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Conversation",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().testTag("conversation_turns_list")
                        ) {
                            conversationTurns.forEach { turn ->
                                // User question bubble
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                                        color = Color(0xFF4F46E5).copy(alpha = 0.7f),
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    ) {
                                        Text(
                                            text = turn.question,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontSize = 11.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                // Companion response bubble
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                                        color = Color(0xFF1E293B),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = Brush.linearGradient(
                                                listOf(Color(0xFF6366F1).copy(alpha = 0.5f), Color(0xFF38BDF8).copy(alpha = 0.3f))
                                            )
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { copyTextToClipboard(turn.answer) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = turn.answer,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color.White,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 16.sp
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy answer",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Dynamic Suggested Follow-up Actions (1-tap analysis triggers & refinement chips)
                    val baseActions = response?.suggestedActions ?: emptyList()
                    val actions = if (baseActions.isNotEmpty()) {
                        baseActions
                    } else if (response?.screenType == ScreenType.CONVERSATION || replies.isNotEmpty()) {
                        listOf(
                            CompanionSuggestedAction("Make it shorter", "Make the suggested replies shorter and more concise."),
                            CompanionSuggestedAction("More confident", "Use a more confident and direct tone for the suggested replies."),
                            CompanionSuggestedAction("Give me another option", "Suggest 3 different fresh reply options for this conversation."),
                            CompanionSuggestedAction("Explain suggestions", "Explain why you suggested these replies."),
                            CompanionSuggestedAction("Make it more casual", "Make the suggested replies more casual and relaxed."),
                            CompanionSuggestedAction("Make it more polite", "Make the suggested replies more polite and respectful.")
                        )
                    } else {
                        emptyList()
                    }

                    if (actions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (replies.isNotEmpty()) "Refine & Explore" else "Suggested Actions",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(actionChipsScrollState)
                        ) {
                            actions.forEach { actionItem ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E293B),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(
                                            listOf(Color(0xFF6366F1).copy(alpha = 0.6f), Color(0xFF38BDF8).copy(alpha = 0.4f))
                                        )
                                    ),
                                    modifier = Modifier
                                        .clickable(enabled = !isAnalyzing && !isCapturing) {
                                            executeAnalysis(customPrompt = actionItem.actionQuery, customIntent = UserIntent.REPLY)
                                        }
                                        .testTag("suggested_action_chip")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "✨",
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = actionItem.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons: Copy Main, Try Another, Close
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { copyTextToClipboard(mainAnswer) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (mainTextCopied) Color(0xFF10B981) else Color(0xFF4F46E5)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("companion_copy_button")
                        ) {
                            Icon(
                                imageVector = if (mainTextCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (mainTextCopied) "Copied!" else "Copy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = { executeAnalysis(forceFreshCapture = true) },
                            enabled = !isAnalyzing && !isCapturing,
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(34.dp)
                                .testTag("companion_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-analyze", fontSize = 11.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = { closeCard() },
                            shape = CircleShape,
                            modifier = Modifier
                                .weight(0.7f)
                                .height(34.dp)
                        ) {
                            Text("Close", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                } else if (errorMessage != null) {
                    if (isRateLimited) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFF38BDF8)))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rate_limit_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CuteCompanion(
                                        state = CompanionState.SLEEPY,
                                        size = 32.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "AI is taking a tiny break ☕",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (cooldownSecondsRemaining > 0) {
                                                "Ready in ~${cooldownSecondsRemaining}s"
                                            } else {
                                                "Ready to try again!"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            if (cooldownSecondsRemaining <= 0) {
                                                executeAnalysis(forceFreshCapture = true)
                                            }
                                        },
                                        enabled = cooldownSecondsRemaining <= 0 && !isAnalyzing,
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4F46E5),
                                            disabledContainerColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .testTag("companion_retry_button")
                                    ) {
                                        Text(
                                            text = if (cooldownSecondsRemaining > 0) {
                                                "Try again (${cooldownSecondsRemaining}s)"
                                            } else {
                                                "Try again"
                                            },
                                            color = if (cooldownSecondsRemaining > 0) Color(0xFF64748B) else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = onClose,
                                        shape = CircleShape,
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Close", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF450A0A).copy(alpha = 0.6f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { executeAnalysis(forceFreshCapture = true) },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Try again", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // In-Memory Follow-up Context Input Bar
                if (!isAnalyzing && (structuredResponse != null || rawAnswerText != null)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = followUpInput,
                            onValueChange = { followUpInput = it },
                            placeholder = {
                                Text(
                                    text = "Ask follow-up question...",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (followUpInput.isNotBlank()) {
                                    executeAnalysis(customPrompt = followUpInput)
                                    followUpInput = ""
                                }
                            }),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF38BDF8),
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedPlaceholderColor = Color(0xFF94A3B8),
                                unfocusedPlaceholderColor = Color(0xFF94A3B8)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("follow_up_input_field")
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (followUpInput.isNotBlank() && !isAnalyzing) {
                                    executeAnalysis(customPrompt = followUpInput)
                                    followUpInput = ""
                                }
                            },
                            enabled = !isAnalyzing && followUpInput.isNotBlank(),
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    if (!isAnalyzing && followUpInput.isNotBlank()) Color(0xFF4F46E5) else Color(0xFF334155),
                                    CircleShape
                                )
                                .testTag("follow_up_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send follow-up",
                                tint = if (!isAnalyzing && followUpInput.isNotBlank()) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
