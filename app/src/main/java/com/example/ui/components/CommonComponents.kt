package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.ui.theme.BentoBlueBg
import com.example.ui.theme.BentoBlueBorder
import com.example.ui.theme.BentoBluePrimary
import com.example.ui.theme.BentoBlueSubtext
import com.example.ui.theme.BentoBlueText
import com.example.ui.theme.BentoIndigo
import com.example.ui.theme.BentoIndigoBorder
import com.example.ui.theme.BentoIndigoDark
import com.example.ui.theme.BentoIndigoLight
import com.example.ui.theme.BentoSlate100
import com.example.ui.theme.BentoSlate200
import com.example.ui.theme.BentoSlate600
import com.example.ui.theme.BentoSlate900
import com.example.ui.theme.MintSoft

/**
 * Robot Avatar component in Bento Grid styling (squircle indigo container).
 */
@Composable
fun RobotAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showGlow: Boolean = true,
    showOnlineBadge: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "robot_glow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (showGlow) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size + 16.dp)
            .testTag("robot_avatar")
    ) {
        // Outer ambient glow ring
        if (showGlow) {
            Box(
                modifier = Modifier
                    .size(size + 12.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                BentoIndigo.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Bento Squircle Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .shadow(6.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(BentoIndigo)
                .border(
                    width = 1.5.dp,
                    color = BentoIndigoDark,
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_robot_avatar),
                contentDescription = "ScreenBuddy Robot Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size - 4.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
        }

        // Online status badge
        if (showOnlineBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp)
                    .size(size / 4)
                    .clip(CircleShape)
                    .background(MintSoft)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

/**
 * Privacy Card required by Level 1 specification, styled in Bento Grid theme:
 * - "col-span-2 bg-blue-50 border border-blue-100 rounded-3xl p-4 flex flex-col gap-1"
 * - Shield icon, uppercase tracking 'PRIVACY FIRST'
 * - Text: "ScreenBuddy only looks at your screen when you explicitly ask it to."
 */
@Composable
fun PrivacyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = BentoBlueBg,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(BentoBlueBorder, BentoBlueBorder.copy(alpha = 0.8f))
            )
        ),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("privacy_first_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🛡️",
                    fontSize = 18.sp
                )
                Text(
                    text = "PRIVACY FIRST",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoBlueText,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "ScreenBuddy only looks at your screen when you explicitly ask it to.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BentoBlueSubtext,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Bento Feature Card for displaying core capabilities in a 2x2 Bento Grid.
 */
@Composable
fun BentoFeatureCard(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.outlineVariant,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            )
        ),
        shadowElevation = 1.dp,
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("feature_card_${title.replace(" ", "_").lowercase()}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BentoIndigoLight)
            ) {
                Text(
                    text = emoji,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    ),
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Standard FeatureCard for list display or compatibility.
 */
@Composable
fun FeatureCard(
    emoji: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder(),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("feature_card_${title.replace(" ", "_").lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BentoIndigoLight)
            ) {
                Text(
                    text = emoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

/**
 * Privacy point item for the Screen Access educational screen.
 */
@Composable
fun PrivacyPointItem(
    index: Int,
    text: String,
    icon: ImageVector = Icons.Default.CheckCircle,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("privacy_point_$index")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BentoBlueBg)
                .border(1.dp, BentoBlueBorder, RoundedCornerShape(10.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BentoBluePrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 21.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Chat bubble for the mock assistant conversation.
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isRobot = message.sender == MessageSender.ROBOT
    val isSystem = message.sender == MessageSender.SYSTEM_INFO

    if (isSystem) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("system_info_bubble")
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BentoIndigoLight,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BentoIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = BentoIndigoDark
                        )
                    )
                }
            }
        }
        return
    }

    Row(
        horizontalArrangement = if (isRobot) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag(if (isRobot) "robot_chat_bubble" else "user_chat_bubble")
    ) {
        if (isRobot) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BentoIndigo)
                    .border(1.dp, BentoIndigoDark, RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_robot_avatar),
                    contentDescription = "Robot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isRobot) 6.dp else 20.dp,
                bottomEnd = if (isRobot) 20.dp else 6.dp
            ),
            color = if (isRobot) {
                MaterialTheme.colorScheme.surface
            } else {
                BentoSlate900
            },
            border = if (isRobot) CardDefaults.outlinedCardBorder() else null,
            shadowElevation = if (isRobot) 1.dp else 0.dp,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isRobot) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            Color.White
                        },
                        lineHeight = 21.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isRobot) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        } else {
                            Color.White.copy(alpha = 0.7f)
                        },
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

/**
 * Level Badge indicator with Bento styling.
 */
@Composable
fun LevelBadge(
    currentLevel: Int = 1,
    title: String = "Level 1: Visual Prototype",
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BentoIndigoLight,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(BentoIndigoBorder, BentoIndigoBorder))
        ),
        modifier = modifier.testTag("level_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = BentoIndigo,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoIndigoDark,
                    letterSpacing = 0.3.sp
                )
            )
        }
    }
}

