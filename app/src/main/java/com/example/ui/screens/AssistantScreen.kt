package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.capture.CaptureState
import com.example.core.capture.ScreenCaptureManager
import com.example.core.overlay.FloatingAssistantService
import com.example.ui.components.LevelBadge
import com.example.ui.components.RobotAvatar
import com.example.ui.theme.BentoBlueBg
import com.example.ui.theme.BentoBlueBorder
import com.example.ui.theme.BentoBluePrimary
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoEmeraldBg
import com.example.ui.theme.BentoEmeraldBorder
import com.example.ui.theme.BentoIndigo
import com.example.ui.theme.BentoIndigoBorder
import com.example.ui.theme.BentoIndigoDark
import com.example.ui.theme.BentoIndigoLight
import com.example.ui.theme.BentoSlate100
import com.example.ui.theme.BentoSlate200
import com.example.ui.theme.BentoSlate400
import com.example.ui.theme.BentoSlate600
import com.example.ui.theme.BentoSlate800
import com.example.ui.theme.BentoSlate900

/**
 * Assistant UI Panel (Level 3) styled with Bento Grid aesthetic.
 *
 * Core Capabilities:
 * - "👀 Look at my screen" (triggers capture and opens Screen Analysis Screen)
 * - "🤖 Floating Assistant" toggle (enables floating bubble over Chrome / other apps)
 * - "✍️ Ask a question" (leads to Screen Analysis / Vision AI)
 * - Quick query chips to immediately analyze screen
 */
@Composable
fun AssistantScreen(
    onLookAtScreenClick: () -> Unit,
    onAskQuestionClick: (String?) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val captureManager = remember { ScreenCaptureManager.instance }
    val captureState by captureManager.captureState.collectAsState()
    val isFloatingRunning by FloatingAssistantService.isRunning.collectAsState()

    var showOverlayPermissionPrompt by remember { mutableStateOf(false) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            FloatingAssistantService.start(context)
        }
    }

    val quickQuestions = listOf(
        "What is on my screen?",
        "What should I reply?",
        "What does this mean?",
        "What should I do here?",
        "Explain this screen.",
        "Why am I getting this error?"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .widthIn(max = 600.dp)
        ) {
            // Top Navigation & Dismiss Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                LevelBadge(title = "Level 3 • Assistant Panel")

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .testTag("top_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Assistant",
                        tint = BentoSlate600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Bento Assistant Card
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                border = CardDefaults.outlinedCardBorder(),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("assistant_panel_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Grab Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(48.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(BentoSlate200)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Header Row with Robot Avatar + Greeting
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RobotAvatar(
                            size = 54.dp,
                            showGlow = false,
                            showOnlineBadge = true
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "SCREENBUDDY AI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BentoIndigo,
                                    letterSpacing = 1.2.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hi! What can I help with?",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoSlate900
                                ),
                                modifier = Modifier.testTag("assistant_greeting_text")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bento Action 1: "👀 Look at my screen"
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BentoIndigoLight,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BentoIndigoBorder)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLookAtScreenClick() }
                            .testTag("look_at_screen_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                            Text(text = "👀", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Look at my screen",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoIndigoDark
                                    )
                                )
                                Text(
                                    text = if (captureState is CaptureState.Active) "Screen access active • Ready to analyze" else "Capture & analyze current screen",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BentoSlate600,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bento Action 2: Floating Assistant Toggle Card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isFloatingRunning) BentoEmeraldBg else BentoSlate100,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isFloatingRunning) BentoEmeraldBorder else BentoSlate200
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isFloatingRunning) {
                                    FloatingAssistantService.stop(context)
                                } else {
                                    if (Settings.canDrawOverlays(context)) {
                                        FloatingAssistantService.start(context)
                                    } else {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        overlayPermissionLauncher.launch(intent)
                                    }
                                }
                            }
                            .testTag("floating_assistant_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Text(text = "🤖", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Floating Assistant",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFloatingRunning) BentoEmerald else BentoSlate900
                                    )
                                )
                                Text(
                                    text = if (isFloatingRunning) "Floating over other apps • Tap to stop" else "Use ScreenBuddy while viewing Chrome",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BentoSlate600,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }

                            Switch(
                                checked = isFloatingRunning,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (Settings.canDrawOverlays(context)) {
                                            FloatingAssistantService.start(context)
                                        } else {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            overlayPermissionLauncher.launch(intent)
                                        }
                                    } else {
                                        FloatingAssistantService.stop(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BentoEmerald,
                                    uncheckedThumbColor = BentoSlate400,
                                    uncheckedTrackColor = BentoSlate200
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bento Action 3: "✍️ Ask a question"
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BentoSlate100,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BentoSlate200)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAskQuestionClick(null) }
                            .testTag("ask_question_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Text(text = "✍️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Ask a question",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoSlate800
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bento Action 4: "Close"
                    TextButton(
                        onClick = onCloseClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("close_assistant_button")
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = BentoSlate400
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Question Suggestions in Bento Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "Quick screen queries",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoSlate600,
                        letterSpacing = 0.5.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                quickQuestions.forEach { query ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = CardDefaults.outlinedCardBorder(),
                        shadowElevation = 0.5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onAskQuestionClick(query) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "💬",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = query,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BentoSlate900,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


