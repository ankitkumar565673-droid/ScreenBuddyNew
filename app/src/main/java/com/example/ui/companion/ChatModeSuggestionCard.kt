package com.example.ui.companion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatModeSuggestion
import com.example.model.ChatTone
import kotlinx.coroutines.delay

/**
 * Floating Translucent Smart Reply Suggestion Card for ScreenBuddy CHAT MODE.
 *
 * Features:
 * - Ultra-compact, non-intrusive floating card design
 * - Smart reply text with 1-tap clipboard copy
 * - Subtle AI watermark badge in bottom-right corner
 * - Multi-reply candidate switcher (1-3 replies)
 * - Quick tone switcher and regenerate actions
 * - Turn OFF Chat Mode button
 * - Ephemeral RAM-only state
 */
@Composable
fun ChatModeSuggestionCard(
    suggestion: ChatModeSuggestion?,
    isLoading: Boolean,
    currentTone: ChatTone,
    onCopy: (String) -> Unit,
    onRegenerate: () -> Unit,
    onChangeTone: () -> Unit,
    onDismiss: () -> Unit,
    onStopChatMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedOptionIndex by remember(suggestion) { mutableIntStateOf(0) }
    var isCopied by remember { mutableStateOf(false) }

    val options = suggestion?.allOptions ?: emptyList()
    val activeReplyText = if (options.isNotEmpty()) {
        options.getOrElse(selectedOptionIndex) { options[0] }
    } else {
        suggestion?.primaryReply ?: ""
    }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000L)
            isCopied = false
        }
    }

    Surface(
        modifier = modifier
            .widthIn(min = 220.dp, max = 280.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFFB74D).copy(alpha = 0.8f),
                        Color(0xFF38BDF8).copy(alpha = 0.6f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("chat_mode_suggestion_card"),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.94f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Header Bar: Mode & Tone Info + Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onChangeTone() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("✨", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${currentTone.emoji} ${currentTone.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Turn OFF Chat Mode Button
                    IconButton(
                        onClick = onStopChatMode,
                        modifier = Modifier
                            .size(22.dp)
                            .testTag("stop_chat_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Chat Mode",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Dismiss current suggestion card
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(22.dp)
                            .testTag("dismiss_chat_suggestion_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body: Reply Content or Loading State
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFFB74D)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preparing reply...",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            } else if (suggestion?.isError == true) {
                // Error / Timeout State: Honest failure indication with Retry
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2D1515).copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = activeReplyText.ifBlank { "Couldn't generate reply" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.testTag("chat_mode_error_text")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap retry to try again",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color(0xFFF87171)
                    )
                }
            } else if (activeReplyText.isNotBlank()) {
                // Reply Text Box with tap-to-copy
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ScreenBuddy Reply", activeReplyText))
                            isCopied = true
                            onCopy(activeReplyText)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = activeReplyText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF8FAFC),
                            lineHeight = 16.sp,
                            modifier = Modifier.testTag("chat_mode_reply_text")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // AI Indicator badge in bottom right corner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF334155).copy(alpha = 0.7f),
                                modifier = Modifier.testTag("chat_mode_ai_badge")
                            ) {
                                Text(
                                    text = "AI",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD54F),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Watching conversation for new messages...",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Multiple candidates selector (if 2 or 3 options available)
            if (options.size > 1 && !isLoading) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    options.forEachIndexed { index, _ ->
                        val isSelected = selectedOptionIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFFFFB74D) else Color(0xFF1E293B))
                                .clickable { selectedOptionIndex = index }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("candidate_option_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Option ${index + 1}",
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1E1B4B) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Toolbar (Copy, Regenerate, Tone)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (suggestion?.isError == true) {
                    // Retry Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444))
                            .clickable { onRegenerate() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("retry_chat_reply_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Retry",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Copy Button (Primary Quick Action)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCopied) Color(0xFF10B981) else Color(0xFFFFB74D))
                            .clickable(enabled = activeReplyText.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ScreenBuddy Reply", activeReplyText))
                                isCopied = true
                                onCopy(activeReplyText)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("copy_chat_reply_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = if (isCopied) Color.White else Color(0xFF1E1B4B),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCopied) "Copied!" else "Copy",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCopied) Color.White else Color(0xFF1E1B4B)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Tone Change Action
                    IconButton(
                        onClick = onChangeTone,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("change_tone_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Change Tone",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Regenerate Action
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("regenerate_chat_reply_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
