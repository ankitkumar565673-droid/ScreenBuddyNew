package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.capture.CaptureState
import com.example.core.capture.ScreenCaptureManager
import com.example.ui.components.LevelBadge
import com.example.ui.theme.BentoBlueBg
import com.example.ui.theme.BentoBlueBorder
import com.example.ui.theme.BentoBluePrimary
import com.example.ui.theme.BentoIndigo
import com.example.ui.theme.BentoIndigoLight
import com.example.ui.theme.BentoSlate100
import com.example.ui.theme.BentoSlate200
import com.example.ui.theme.BentoSlate400
import com.example.ui.theme.BentoSlate600
import com.example.ui.theme.BentoSlate800
import com.example.ui.theme.BentoSlate900

/**
 * ScreenAccessScreen (Level 2: Real Android MediaProjection Screen Capture).
 *
 * Implements the explicit user consent flow and real MediaProjection screen capture.
 *
 * Before permission:
 * - Title: "Screen access"
 * - Description: "ScreenBuddy will only look at your screen when you explicitly choose to use this feature."
 * - Privacy points:
 *   ✓ You control when ScreenBuddy looks.
 *   ✓ ScreenBuddy does not continuously monitor your screen.
 *   ✓ Your screenshot stays on this device in Level 2.
 * - Button: "Allow screen access"
 *
 * After permission granted:
 * - Show: "Screen access is active"
 * - Visual indicator: "🔴 ScreenBuddy is viewing your screen"
 * - Display captured screenshot from memory
 * - Buttons:
 *   - "↻ Capture again"
 *   - "🗑 Discard screenshot"
 *   - "⏹ Stop screen access"
 *
 * After stopping:
 * - Show: "Screen access stopped"
 */
@Composable
fun ScreenAccessScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val captureManager = remember { ScreenCaptureManager.instance }
    val captureState by captureManager.captureState.collectAsState()
    val scrollState = rememberScrollState()

    // Activity Result Launcher for system MediaProjection consent dialog
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        captureManager.onPermissionResult(result.resultCode, result.data, context)
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
            // Top Navigation Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        if (captureState is CaptureState.Active) {
                            captureManager.stopCapture(context)
                        }
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .testTag("screen_access_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BentoSlate600,
                        modifier = Modifier.size(18.dp)
                    )
                }

                LevelBadge(
                    title = if (captureState is CaptureState.Active) "Level 2 • Live Capture" else "Level 2 • Privacy & Consent"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = captureState) {
                is CaptureState.Active -> {
                    // Active Capture Session View
                    ActiveCaptureContent(
                        state = state,
                        onCaptureAgain = { captureManager.captureAgain() },
                        onDiscardScreenshot = { captureManager.discardScreenshot() },
                        onStopScreenAccess = { captureManager.stopCapture(context) }
                    )
                }

                is CaptureState.PermissionDenied -> {
                    // Permission Denied View
                    PermissionDeniedContent(
                        onRequestAgain = {
                            val intent = captureManager.createScreenCaptureIntent(context)
                            mediaProjectionLauncher.launch(intent)
                        },
                        onBack = onBackClick
                    )
                }

                is CaptureState.Stopped -> {
                    // Stopped State View
                    StoppedCaptureContent(
                        onAllowAccess = {
                            val intent = captureManager.createScreenCaptureIntent(context)
                            mediaProjectionLauncher.launch(intent)
                        },
                        onBack = onBackClick
                    )
                }

                is CaptureState.Error -> {
                    // Error State View
                    ErrorCaptureContent(
                        message = state.message,
                        onRetry = {
                            val intent = captureManager.createScreenCaptureIntent(context)
                            mediaProjectionLauncher.launch(intent)
                        },
                        onBack = onBackClick
                    )
                }

                CaptureState.Idle, CaptureState.PermissionRequested -> {
                    // Initial Consent & Educational View
                    InitialConsentContent(
                        onRequestPermission = {
                            val intent = captureManager.createScreenCaptureIntent(context)
                            mediaProjectionLauncher.launch(intent)
                        },
                        onBack = onBackClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Initial Consent View (Before permission is requested/granted).
 */
@Composable
private fun InitialConsentContent(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    val privacyPoints = listOf(
        Pair("You control when ScreenBuddy looks.", Icons.Default.TouchApp),
        Pair("ScreenBuddy does not continuously monitor your screen.", Icons.Default.VisibilityOff),
        Pair("Your screenshot stays on this device in Level 2.", Icons.Default.Lock)
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder(),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("screen_access_info_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Shield Icon Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BentoBlueBg)
                    .border(1.5.dp, BentoBlueBorder, RoundedCornerShape(20.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.GppGood,
                    contentDescription = "Security Verified",
                    tint = BentoBluePrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Heading: "Screen access"
            Text(
                text = "Screen access",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoSlate900
                ),
                modifier = Modifier.testTag("screen_access_title")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Subheading
            Text(
                text = "ScreenBuddy will only look at your screen when you explicitly choose to use this feature.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BentoSlate600,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .testTag("screen_access_subheading")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Three Privacy Points in Bento Inner Container
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    privacyPoints.forEachIndexed { index, (pointText, icon) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(BentoBlueBg)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = BentoBluePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = pointText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = BentoSlate800,
                                    fontSize = 13.5.sp
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notice Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BentoIndigoLight,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "🔒",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pressing allow will show Android's native screen-capture prompt. Capture occurs only in temporary RAM.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BentoIndigo,
                            lineHeight = 16.sp,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Primary Button: "Allow screen access"
            Button(
                onClick = onRequestPermission,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoSlate900
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("allow_screen_access_button")
            ) {
                Text(
                    text = "Allow screen access",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cancel / Back Button
            OutlinedButton(
                onClick = onBack,
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = BentoSlate600
                ),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cancel_screen_access_button")
            ) {
                Text(
                    text = "Not now",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BentoSlate600,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * Active Capture Session View.
 */
@Composable
private fun ActiveCaptureContent(
    state: CaptureState.Active,
    onCaptureAgain: () -> Unit,
    onDiscardScreenshot: () -> Unit,
    onStopScreenAccess: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder(),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_screen_capture_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Live Status Indicator Pill
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEE2E2),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))
                ),
                modifier = Modifier.testTag("active_status_indicator")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = "🔴", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ScreenBuddy is viewing your screen",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title: "Screen access is active"
            Text(
                text = "Screen access is active",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoSlate900
                ),
                modifier = Modifier.testTag("screen_access_active_title")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Local frame capture via MediaProjection API",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = BentoSlate600
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Screenshot Display Area
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BentoSlate100,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("screenshot_container")
            ) {
                if (state.bitmap != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Image(
                            bitmap = state.bitmap.asImageBitmap(),
                            contentDescription = "Captured Screen",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 340.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, BentoSlate200, RoundedCornerShape(14.dp))
                                .testTag("captured_screenshot_image")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "RAM Bitmap: ${state.bitmap.width} × ${state.bitmap.height}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoSlate600,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            Text(
                                text = "Frame #${state.captureCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoIndigo,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = BentoIndigo,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Awaiting screen frame...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BentoSlate600,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: "↻ Capture again"
                Button(
                    onClick = onCaptureAgain,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoSlate900
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("capture_again_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "↻ Capture again",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                // Button 2: "🗑 Discard screenshot"
                OutlinedButton(
                    onClick = onDiscardScreenshot,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BentoSlate800
                    ),
                    border = CardDefaults.outlinedCardBorder(),
                    enabled = state.bitmap != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("discard_screenshot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = BentoSlate600,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🗑 Discard screenshot",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.bitmap != null) BentoSlate800 else BentoSlate400
                        )
                    )
                }

                // Button 3: "⏹ Stop screen access"
                Button(
                    onClick = onStopScreenAccess,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("stop_screen_access_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⏹ Stop screen access",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Assurance Footer
            Text(
                text = "Screenshot remains strictly in temporary RAM. No files created.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BentoSlate400,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * Permission Denied View.
 */
@Composable
private fun PermissionDeniedContent(
    onRequestAgain: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder(),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_denied_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF2F2))
                    .border(1.dp, Color(0xFFFECACA), CircleShape)
            ) {
                Text(text = "✋", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Screen access was not granted.",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoSlate900
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("permission_denied_message")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "ScreenBuddy cannot view your screen without Android MediaProjection consent. You can grant access anytime.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BentoSlate600,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRequestAgain,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoSlate900
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("try_again_permission_button")
            ) {
                Text(
                    text = "Allow screen access",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                shape = CircleShape,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("denied_back_button")
            ) {
                Text(
                    text = "Back to assistant",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BentoSlate600,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * Stopped Capture View.
 */
@Composable
private fun StoppedCaptureContent(
    onAllowAccess: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder(),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stopped_capture_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(BentoSlate100)
                    .border(1.dp, BentoSlate200, CircleShape)
            ) {
                Text(text = "⏹", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Screen access stopped",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = BentoSlate900
                ),
                modifier = Modifier.testTag("screen_access_stopped_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "All capture hardware resources, virtual displays, and memory buffers have been completely released.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BentoSlate600,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAllowAccess,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoSlate900
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("restart_screen_access_button")
            ) {
                Text(
                    text = "Allow screen access",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                shape = CircleShape,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("stopped_back_button")
            ) {
                Text(
                    text = "Back to assistant",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = BentoSlate600,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

/**
 * Error Capture View.
 */
@Composable
private fun ErrorCaptureContent(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = CardDefaults.outlinedCardBorder(),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("error_capture_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Capture Error",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoSlate900
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = BentoSlate600,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onRetry,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BentoSlate900),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Retry screen access", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                shape = CircleShape,
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Back", color = BentoSlate600)
            }
        }
    }
}
