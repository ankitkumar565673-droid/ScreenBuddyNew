package com.example.ui.companion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.UserIntent

/**
 * Menu Action types for ScreenBuddy (Level 8 Structured Intent System).
 */
enum class CompanionActionType(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val defaultPrompt: String,
    val intent: UserIntent,
    val isHighlighted: Boolean = false
) {
    CHAT_MODE(
        emoji = "✨",
        title = "Chat Mode",
        subtitle = "Auto smart replies with tone",
        defaultPrompt = "Chat Mode active. Automatically suggest context-aware conversation replies.",
        intent = UserIntent.REPLY,
        isHighlighted = true
    ),
    LOOK(
        emoji = "👀",
        title = "Look at my screen",
        subtitle = "AI summarizes what's visible",
        defaultPrompt = "What is happening on this screen? Give me the most useful information and key takeaways.",
        intent = UserIntent.GENERAL_QUESTION,
        isHighlighted = true
    ),
    REPLY(
        emoji = "💬",
        title = "What should I reply?",
        subtitle = "Smart conversation suggestions",
        defaultPrompt = "What should I reply to this conversation? Provide natural, context-aware options.",
        intent = UserIntent.REPLY,
        isHighlighted = true
    ),
    EXPLAIN(
        emoji = "📖",
        title = "Explain this",
        subtitle = "Explain in simple language",
        defaultPrompt = "Explain what is shown on this screen in simple language.",
        intent = UserIntent.EXPLAIN
    ),
    TRANSLATE(
        emoji = "🌐",
        title = "Translate",
        subtitle = "Translate visible text to English",
        defaultPrompt = "Identify the important visible text and translate it into English.",
        intent = UserIntent.TRANSLATE
    ),
    SUMMARIZE(
        emoji = "📝",
        title = "Summarize",
        subtitle = "Bullet points & key takeaways",
        defaultPrompt = "Summarize the important information visible on this screen in crisp bullet points.",
        intent = UserIntent.SUMMARIZE
    ),
    LIVE_VIEW(
        emoji = "🔴",
        title = "Live View",
        subtitle = "RAM-only visual stream",
        defaultPrompt = "What is visible on this screen?",
        intent = UserIntent.GENERAL_QUESTION
    )
}

/**
 * Compact Floating Assistant Menu for ScreenBuddy.
 */
@Composable
fun CompanionMenu(
    onActionSelected: (CompanionActionType) -> Unit,
    onCloseCompanion: () -> Unit,
    onDismissMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.92f)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0F172A), // Premium dark slate canvas
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFFD54F).copy(alpha = 0.7f),
                        Color(0xFFFFF59D).copy(alpha = 0.45f),
                        Color(0xFF1E293B)
                    )
                )
            ),
            shadowElevation = 16.dp,
            modifier = modifier
                .width(280.dp)
                .testTag("companion_floating_menu")
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Header with mini companion avatar & close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CuteCompanion(
                            state = CompanionState.HAPPY,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ScreenBuddy",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ready to assist",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismissMenu,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss menu",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action items
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    CompanionActionType.values().forEach { action ->
                        CompanionActionRow(
                            action = action,
                            onClick = { onActionSelected(action) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Privacy assurance note
                Text(
                    text = "🔒 Screen access happens only when you tap an option.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 9.5.sp,
                        lineHeight = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Close / Stop ScreenBuddy button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCloseCompanion)
                        .testTag("menu_action_close_companion")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    ) {
                        Text(
                            text = "✕ Stop ScreenBuddy",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanionActionRow(
    action: CompanionActionType,
    onClick: () -> Unit
) {
    val bgBrush = if (action.isHighlighted) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFD97706).copy(alpha = 0.28f),
                Color(0xFFF59E0B).copy(alpha = 0.18f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF1E293B).copy(alpha = 0.6f),
                Color(0xFF1E293B).copy(alpha = 0.35f)
            )
        )
    }

    val borderBrush = if (action.isHighlighted) {
        Brush.horizontalGradient(
            listOf(
                Color(0xFFFFD54F).copy(alpha = 0.7f),
                Color(0xFFFFF59D).copy(alpha = 0.5f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF334155).copy(alpha = 0.5f),
                Color(0xFF1E293B).copy(alpha = 0.3f)
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = CardDefaults.outlinedCardBorder().copy(brush = borderBrush),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("companion_action_${action.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(bgBrush)
                .padding(horizontal = 10.dp, vertical = 7.5.dp)
        ) {
            Text(text = action.emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontWeight = if (action.isHighlighted) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = action.subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (action.isHighlighted) Color(0xFF93C5FD) else Color(0xFF94A3B8),
                        fontSize = 9.5.sp
                    )
                )
            }
        }
    }
}
