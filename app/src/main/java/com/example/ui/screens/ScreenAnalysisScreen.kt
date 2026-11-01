package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.VisionAiClient
import com.example.core.ai.VisionAiResult
import com.example.core.capture.CaptureState
import com.example.core.capture.ScreenCaptureManager
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
import com.example.ui.theme.BentoIndigoLight
import com.example.ui.theme.BentoSlate100
import com.example.ui.theme.BentoSlate200
import com.example.ui.theme.BentoSlate400
import com.example.ui.theme.BentoSlate600
import com.example.ui.theme.BentoSlate800
import com.example.ui.theme.BentoSlate900
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ScreenAnalysisScreen (Level 3: Live Multimodal AI Screen Assistant).
 *
 * Requirements:
 * - Displays in-memory screenshot thumbnail with RAM-only indicator.
 * - Text field for user question.
 * - Quick suggestion chips:
 *   - "👀 What is on my screen?"
 *   - "💬 What should I reply?"
 *   - "💡 Explain this screen"
 *   - "👆 What button should I press?"
 *   - "📝 Summarize this"
 *   - "🌐 Translate this"
 * - Sends question + RAM screenshot to GeminiVisionAiService.
 * - Displays AI answer card with formatted text.
 * - "Ask another question" and "Discard screenshot" actions.
 * - Strict privacy guarantee: no disk storage, RAM-only lifecycle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenAnalysisScreen(
    initialQuestion: String? = null,
    onBackClick: () -> Unit,
    onRecaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val captureManager = remember { ScreenCaptureManager.instance }
    val captureState by captureManager.captureState.collectAsState()

    val currentBitmap = captureManager.getCurrentBitmap()
        ?: (captureState as? CaptureState.Active)?.bitmap

    var userQuestion by remember { mutableStateOf(initialQuestion ?: "") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val aiService = remember { VisionAiClient.getService(context) }

    var analysisJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var activeRequestId by remember { mutableLongStateOf(0L) }

    fun performAnalysis(question: String) {
        if (isAnalyzing) return
        val query = question.trim().ifBlank { "What is on my screen?" }
        val bitmap = captureManager.getCurrentBitmap()
            ?: (captureState as? CaptureState.Active)?.bitmap

        if (bitmap == null || bitmap.isRecycled) {
            errorMessage = "No screenshot available in memory. Please capture your screen first."
            return
        }

        isAnalyzing = true
        errorMessage = null
        aiResponse = null
        keyboardController?.hide()

        val reqId = ++activeRequestId
        analysisJob?.cancel()
        analysisJob = coroutineScope.launch {
            try {
                val resolvedIntent = if (query.contains("reply", ignoreCase = true) || query.contains("respond", ignoreCase = true)) {
                    com.example.core.ai.UserIntent.REPLY
                } else null
                val result = aiService.analyzeScreenshot(bitmap, query, resolvedIntent)
                if (!isActive || activeRequestId != reqId) return@launch
                when (result) {
                    is VisionAiResult.Success -> {
                        isAnalyzing = false
                        val resp = result.structuredResponse
                        val baseInsight = resp?.mainInsight?.ifBlank { result.answer } ?: result.answer
                        val repliesText = if (resp != null && resp.suggestedReplies.isNotEmpty()) {
                            "\n\nSuggested Replies:\n" + resp.suggestedReplies.joinToString("\n") { "• [${it.toneLabel}] ${it.text}" }
                        } else ""
                        aiResponse = "$baseInsight$repliesText".trim()
                    }
                    is VisionAiResult.Error -> {
                        isAnalyzing = false
                        errorMessage = result.message
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (activeRequestId == reqId) {
                    isAnalyzing = false
                    errorMessage = "Analysis error: ${e.message}"
                }
            }
        }
    }

    // Auto-trigger analysis if an initial question was provided (e.g. from floating assistant)
    LaunchedEffect(initialQuestion) {
        if (!initialQuestion.isNullOrBlank() && currentBitmap != null && aiResponse == null && !isAnalyzing) {
            userQuestion = initialQuestion
            performAnalysis(initialQuestion)
        }
    }

    val quickQuestions = listOf(
        "👀 What is on my screen?",
        "💬 What should I reply?",
        "💡 Explain this screen",
        "👆 What button should I press?",
        "📝 Summarize this",
        "🌐 Translate this"
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
                .padding(horizontal = 18.dp, vertical = 16.dp)
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
                        keyboardController?.hide()
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .testTag("analysis_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BentoSlate600,
                        modifier = Modifier.size(18.dp)
                    )
                }

                LevelBadge(title = "Level 3 • Vision AI")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Bento Analysis Card
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = CardDefaults.outlinedCardBorder(),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("screen_analysis_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header with Robot Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RobotAvatar(
                                size = 44.dp,
                                showGlow = isAnalyzing,
                                showOnlineBadge = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SCREENBUDDY AI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BentoIndigo,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = "Screen Analysis",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSlate900
                                    )
                                )
                            }
                        }

                        // RAM Only Badge
                        Surface(
                            shape = CircleShape,
                            color = BentoEmeraldBg,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(BentoEmeraldBorder)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = BentoEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "RAM only",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BentoEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Screenshot Thumbnail Container
                    if (currentBitmap != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BentoSlate100,
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("analysis_screenshot_preview")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Image(
                                    bitmap = currentBitmap.asImageBitmap(),
                                    contentDescription = "Captured Screen for Analysis",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 140.dp, max = 220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, BentoSlate200, RoundedCornerShape(12.dp))
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = BentoSlate400,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Screenshot in volatile memory",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = BentoSlate600,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }

                                    TextButton(
                                        onClick = onRecaptureClick,
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = BentoIndigo,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Retake",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = BentoIndigo,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty / Awaiting Screen State
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BentoSlate100,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(text = "👀", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No screen captured yet",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSlate800
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap below to capture your active screen or return to the assistant.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BentoSlate600,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onRecaptureClick,
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoSlate900),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text("Capture screen now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Question Input Section
                    Text(
                        text = "What would you like help with?",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoSlate900
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userQuestion,
                        onValueChange = { userQuestion = it },
                        placeholder = {
                            Text(
                                text = "e.g., What should I reply here?",
                                style = MaterialTheme.typography.bodyMedium.copy(color = BentoSlate400)
                            )
                        },
                        trailingIcon = {
                            if (userQuestion.isNotBlank()) {
                                IconButton(onClick = { userQuestion = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = BentoSlate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            performAnalysis(userQuestion)
                        }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoIndigo,
                            unfocusedBorderColor = BentoSlate200,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("question_text_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Suggestion Chips
                    Text(
                        text = "Quick suggestions",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BentoSlate600,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickQuestions.forEach { chipQuery ->
                            val cleanQuery = chipQuery.substringAfter(" ")
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (userQuestion == cleanQuery) BentoIndigoLight else BentoSlate100,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (userQuestion == cleanQuery) BentoIndigoBorder else BentoSlate200
                                    )
                                ),
                                modifier = Modifier
                                    .clickable {
                                        userQuestion = cleanQuery
                                        performAnalysis(cleanQuery)
                                    }
                                    .testTag("quick_chip_${cleanQuery.take(10)}")
                            ) {
                                Text(
                                    text = chipQuery,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (userQuestion == cleanQuery) BentoIndigo else BentoSlate800,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Button: [ Ask ScreenBuddy ]
                    Button(
                        onClick = { performAnalysis(userQuestion) },
                        enabled = !isAnalyzing && currentBitmap != null,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BentoSlate900,
                            disabledContainerColor = BentoSlate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("ask_screenbuddy_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Analyzing screen...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ask ScreenBuddy",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Privacy Indicator
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔒 Screenshot used only for this request",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BentoSlate400,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Loading / Result / Error State Display
            if (isAnalyzing) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = BentoIndigoLight,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BentoIndigoBorder)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analysis_loading_card")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        RobotAvatar(size = 56.dp, showGlow = true)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ScreenBuddy is analyzing your screen...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoSlate900
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Processing visual elements and composing answer",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BentoSlate600
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Error Display
            if (errorMessage != null && !isAnalyzing) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFFEF2F2),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analysis_error_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unable to analyze",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage ?: "Unknown error occurred",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF7F1D1D)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { performAnalysis(userQuestion) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = BentoSlate900),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("Retry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Success Result Display
            if (aiResponse != null && !isAnalyzing) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BentoIndigoBorder)
                    ),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analysis_result_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Response Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RobotAvatar(size = 36.dp, showGlow = false)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ScreenBuddy",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BentoSlate900
                                        )
                                    )
                                    Text(
                                        text = "Screen Assistant Answer",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = BentoIndigo,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BentoEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // User Question Quote
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BentoSlate100,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "“${userQuestion.ifBlank { "What is on my screen?" }}”",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BentoSlate800,
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Formatted AI Answer Text
                        Text(
                            text = aiResponse ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = BentoSlate900,
                                lineHeight = 22.sp
                            ),
                            modifier = Modifier.testTag("ai_response_text")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Controls: Ask Another / Discard
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    userQuestion = ""
                                    aiResponse = null
                                },
                                shape = CircleShape,
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("ask_another_button")
                            ) {
                                Text(
                                    text = "Ask another",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = BentoSlate800,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    captureManager.discardScreenshot()
                                    onBackClick()
                                },
                                shape = CircleShape,
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("discard_and_exit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = BentoSlate600,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Discard",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = BentoSlate600,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
