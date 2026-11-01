package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.core.capture.CaptureState
import com.example.core.capture.ScreenCaptureManager
import com.example.core.overlay.FloatingAssistantService
import com.example.ui.companion.CompanionState
import com.example.ui.companion.LottieCompanion
import com.example.ui.components.LevelBadge
import com.example.ui.components.RobotAvatar
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoEmeraldBg
import com.example.ui.theme.BentoEmeraldBorder
import com.example.ui.theme.BentoIndigo
import com.example.ui.theme.BentoIndigoBorder
import com.example.ui.theme.BentoIndigoLight
import com.example.ui.theme.BentoSlate100
import com.example.ui.theme.BentoSlate200
import com.example.ui.theme.BentoSlate400
import com.example.ui.theme.BentoSlate600
import com.example.ui.theme.BentoSlate800
import com.example.ui.theme.BentoSlate900

/**
 * Onboarding & Activation Hub for ScreenBuddy (Level 4: Real Floating Assistant).
 *
 * Flow:
 * 1. "Welcome to ScreenBuddy 🤖"
 * 2. "Your personal AI assistant for whatever is on your screen."
 * 3. Privacy Rule: "ScreenBuddy only looks at your screen when you ask it to."
 * 4. Two explicit required permissions:
 *    - Screen access (MediaProjection)
 *    - Display over other apps (SYSTEM_ALERT_WINDOW)
 * 5. "Start ScreenBuddy" button:
 *    - Launches floating assistant over active apps (e.g. Chrome)
 *    - Minimizes MainActivity
 *    - Displays draggable floating robot
 */
@Composable
fun HomeScreen(
    onStartFloatingAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    val captureManager = remember { ScreenCaptureManager.instance }
    val captureState by captureManager.captureState.collectAsState()
    val isAssistantRunning by FloatingAssistantService.isRunning.collectAsState()

    // Permission States
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val isScreenAccessGranted = captureManager.isCaptureGranted || captureState is CaptureState.Active

    // Refresh overlay permission state when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // MediaProjection permission launcher
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        captureManager.onPermissionResult(result.resultCode, result.data, context)
    }

    // Overlay settings launcher
    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        overlaySettingsLauncher.launch(intent)
    }

    fun requestScreenAccess() {
        val intent = captureManager.createScreenCaptureIntent(context)
        mediaProjectionLauncher.launch(intent)
    }

    fun startScreenBuddy() {
        if (!hasOverlayPermission) {
            requestOverlayPermission()
            return
        }
        if (!isScreenAccessGranted) {
            requestScreenAccess()
            return
        }

        // Start the FloatingAssistantService
        FloatingAssistantService.start(context)

        // Minimize MainActivity so the user lands back on Chrome / current app
        val activity = context as? Activity
        activity?.moveTaskToBack(true)
    }

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
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 600.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Level 7 Badge
            LevelBadge(
                currentLevel = 9,
                title = "Level 9 • Visual Change Engine",
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Companion Hero (Supports custom Lottie JSON animations with dynamic fallback)
            LottieCompanion(
                state = if (isAssistantRunning) CompanionState.HAPPY else CompanionState.IDLE,
                size = 180.dp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // App Greeting
            Text(
                text = "ScreenBuddy Companion 💙",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoSlate900,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("app_welcome_heading")
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Value Proposition
            Text(
                text = "Your personal AI companion for whatever is on your screen with live visual intelligence.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .testTag("app_value_prop")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Core Privacy Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = BentoIndigoLight,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(BentoIndigoBorder, BentoIndigo.copy(alpha = 0.2f)))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_guarantee_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BentoIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRIVACY GUARANTEE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoIndigo,
                                letterSpacing = 0.6.sp
                            )
                        )
                        Text(
                            text = "ScreenBuddy only looks at your screen when you ask it to. Never monitors continuously.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BentoSlate800,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Required Permissions Section
            Text(
                text = "Required Permissions",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoSlate900
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )

            // Permission 1: Screen Access
            PermissionCard(
                icon = Icons.Default.PhotoCamera,
                title = "1. Screen access",
                description = "Allows capturing a clean screenshot when you tap 'Look at my screen' or ask a question.",
                isGranted = isScreenAccessGranted,
                actionLabel = "Grant Screen Access",
                onAction = { requestScreenAccess() },
                testTag = "permission_screen_access"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission 2: Display Over Other Apps
            PermissionCard(
                icon = Icons.Default.Layers,
                title = "2. Display over other apps",
                description = "Allows the small floating ScreenBuddy robot to appear unobtrusively above Chrome and other apps.",
                isGranted = hasOverlayPermission,
                actionLabel = "Allow Overlay",
                onAction = { requestOverlayPermission() },
                testTag = "permission_overlay"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action: "Start ScreenBuddy"
            if (!isAssistantRunning) {
                Button(
                    onClick = { startScreenBuddy() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoSlate900),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_screenbuddy_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start ScreenBuddy",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            } else {
                // Assistant is already running
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = BentoEmeraldBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(BentoEmeraldBorder, BentoEmerald.copy(alpha = 0.2f)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BentoEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Floating Assistant is Active",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoEmerald
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "The floating robot is visible over other apps. Tap it anytime to capture screenshots or ask AI.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BentoSlate800,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val activity = context as? Activity
                                    activity?.moveTaskToBack(true)
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = BentoSlate900),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Minimize", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    FloatingAssistantService.stop(context)
                                },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Stop Assistant", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // How ScreenBuddy Works Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = CardDefaults.outlinedCardBorder(),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BentoIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "How to use ScreenBuddy",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoSlate900
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val steps = listOf(
                        "1. Grant the 2 permissions above and tap 'Start ScreenBuddy'.",
                        "2. ScreenBuddy minimizes and shows a cute, expressive companion floating over your apps.",
                        "3. Open Chrome, WhatsApp, Instagram, or any other app.",
                        "4. Tap the companion -> choose 'Look at my screen', 'What should I reply?', 'Explain this', 'Translate', or 'Summarize'.",
                        "5. ScreenBuddy hides itself momentarily to capture the clean underlying app.",
                        "6. Companion transitions through Looking -> Thinking -> Happy with answers and 1-tap copy."
                    )

                    steps.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isGranted) BentoEmeraldBg else MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (isGranted) listOf(BentoEmeraldBorder, BentoEmerald.copy(alpha = 0.3f))
                else listOf(BentoSlate200, BentoSlate100)
            )
        ),
        shadowElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (isGranted) BentoEmerald.copy(alpha = 0.15f) else BentoSlate100,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) BentoEmerald else BentoSlate800,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoSlate900
                        )
                    )
                }

                if (isGranted) {
                    Surface(
                        shape = CircleShape,
                        color = BentoEmerald
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Granted",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onAction,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

