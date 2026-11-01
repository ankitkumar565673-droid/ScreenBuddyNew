package com.example.core.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.core.ai.ScreenType
import com.example.core.ai.VisionAiClient
import com.example.core.ai.VisionAiResult
import com.example.core.capture.CaptureState
import com.example.core.capture.FrameValidator
import com.example.core.capture.ScreenCaptureManager
import com.example.core.diagnostics.ScreenBuddyDiagnostics
import com.example.core.engine.AnalysisDecisionType
import com.example.core.engine.AnalysisDecision
import com.example.core.engine.LocalContextEngine
import com.example.core.engine.LocalIntelligenceConfig
import com.example.core.engine.SmartDecisionEngine
import com.example.core.engine.VisualChangeDetector
import com.example.core.engine.VisualDiffResult
import com.example.core.engine.VisualEngineState
import com.example.ui.companion.CompanionActionType
import com.example.ui.companion.CompanionMenu
import com.example.ui.companion.CompanionResponseCard
import com.example.ui.companion.CompanionState
import com.example.model.ChatEngineState
import com.example.ui.companion.LottieCompanion
import com.example.ui.companion.CuteCompanion
import com.example.model.ChatTone
import com.example.model.ChatModeSuggestion
import com.example.ui.companion.ChatModeConfigCard
import com.example.ui.companion.ChatModeSuggestionCard
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
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot


/**
 * Overlay View States for Level 7 Cute AI Companion.
 */
enum class OverlayState {
    COLLAPSED,          // Small floating cute companion creature (approx 56dp)
    MENU,               // Compact assistant menu (Look at screen, Chat Mode, Reply, Explain, Translate, Summarize, Live View, Close)
    CHAT_MODE_CONFIG,   // Compact configuration panel before entering Chat Mode
    CHAT_MODE_ACTIVE,   // Active Chat Mode with smart reply card
    ACTION_RESPONSE,    // Responsive companion card for active AI query (Look, Reply, Explain, Translate, Summarize)
    SCREENSHOT_PREVIEW, // Screenshot thumbnail preview card (RAM-only)
    LIVE_VIEW_CONFIRM,  // Privacy consent confirmation before entering Live View
    LIVE_VIEW_ACTIVE,   // Active local live screen stream preview card
    ASK_AI              // Custom question AI card
}

/**
 * Pending latest state for Level 15 latest-state coalescing.
 */
data class PendingState(
    val bitmap: Bitmap?,
    val decision: AnalysisDecision,
    val appPackage: String?
)

/**
 * FloatingOverlayManager (Level 7: Expressive AI Companion & Assistant UI).
 *
 * Responsibilities:
 * - Creates and maintains the SYSTEM_ALERT_WINDOW overlay using TYPE_APPLICATION_OVERLAY.
 * - Renders the cute animated companion creature floating gently over third-party apps.
 * - Handles drag gestures with touch-slop detection and snaps smoothly to the nearest edge.
 * - Dispatches Level 7 actions: Look at screen, What should I reply?, Explain this, Translate, Summarize, Live View, and Close.
 * - Implements clean screen capture by hiding the overlay (View.GONE) during screenshot acquisition.
 * - Strict zero-persistence in-memory operation.
 */
class FloatingOverlayManager(
    private val service: FloatingAssistantService,
    private val coroutineScope: CoroutineScope
) {
    init {
        instance = this
    }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayRootView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // State
    private val _overlayState = mutableStateOf(OverlayState.COLLAPSED)
    val overlayState get() = _overlayState.value

    private val _currentAction = mutableStateOf(CompanionActionType.LOOK)
    val currentAction get() = _currentAction.value

    private val _lastCapturedBitmap = mutableStateOf<Bitmap?>(null)
    val lastCapturedBitmap get() = _lastCapturedBitmap.value

    // Chat Mode State (RAM-only)
    private val _isChatModeActive = mutableStateOf(false)
    val isChatModeActive get() = _isChatModeActive.value

    private val _isSuggestionVisible = mutableStateOf(false)
    val isSuggestionVisible get() = _isSuggestionVisible.value

    private val _chatEngineState = mutableStateOf(ChatEngineState.OFF)
    val chatEngineState get() = _chatEngineState.value

    private val _chatTone = mutableStateOf(ChatTone.NATURAL)
    val chatTone get() = _chatTone.value

    private val _customToneText = mutableStateOf<String?>(null)
    val customToneText get() = _customToneText.value

    private val _maxChatSuggestions = mutableStateOf(1)
    val maxChatSuggestions get() = _maxChatSuggestions.value

    private val _chatSuggestion = mutableStateOf<ChatModeSuggestion?>(null)
    val chatSuggestion get() = _chatSuggestion.value

    private val _isChatGenerating = mutableStateOf(false)
    val isChatGenerating get() = _isChatGenerating.value

    private var chatModeJob: Job? = null

    // Level 9 Visual Change Engine
    private val visualChangeDetector = VisualChangeDetector()
    private val _visualEngineState = mutableStateOf<VisualEngineState>(VisualEngineState.Idle)
    private val _liveAiResult = mutableStateOf<VisionAiResult?>(null)

    // Level 10 Local Intelligence & Smart Context Engine
    private val localContextEngine = LocalContextEngine()
    private val localIntelligenceConfig get() = LocalIntelligenceConfig()
    private val smartDecisionEngine = SmartDecisionEngine(localIntelligenceConfig)

    // Screen Dimensions & Companion Session Position
    private var screenWidth = 1080
    private var screenHeight = 1920
    private var displayDensity = 2.5f
    private var lastCompanionX = -1
    private var lastCompanionY = -1

    // Touch & Drag state
    private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // Live View Coroutine
    private var liveViewJob: Job? = null
    private var pendingLatestState: PendingState? = null

    fun initialize() {
        updateDisplayMetrics()
        createOverlayView()
    }

    private fun updateDisplayMetrics() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        displayDensity = metrics.density
    }

    private fun createOverlayView() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        if (lastCompanionX < 0) {
            lastCompanionX = screenWidth - 220
            lastCompanionY = screenHeight / 3
        }

        // Initial LayoutParams (Collapsed Cute Companion)
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lastCompanionX
            y = lastCompanionY
        }

        val composeView = ComposeView(service).apply {
            setViewTreeLifecycleOwner(service)
            setViewTreeViewModelStoreOwner(service)
            setViewTreeSavedStateRegistryOwner(service)

            setContent {
                MyApplicationTheme {
                    FloatingOverlayRoot(
                        state = _overlayState.value,
                        action = _currentAction.value,
                        capturedBitmap = _lastCapturedBitmap.value,
                        visualEngineState = _visualEngineState.value,
                        liveAiResult = _liveAiResult.value,
                        isChatModeActive = _isChatModeActive.value,
                        isSuggestionVisible = _isSuggestionVisible.value,
                        chatTone = _chatTone.value,
                        customTone = _customToneText.value,
                        maxSuggestions = _maxChatSuggestions.value,
                        chatSuggestion = _chatSuggestion.value,
                        isChatGenerating = _isChatGenerating.value,
                        onCompanionTap = {
                            toggleMenu()
                        },
                        onActionSelected = { selectedAction ->
                            handleCompanionAction(selectedAction)
                        },
                        onStartLiveViewConfirmed = {
                            startLiveViewActive()
                        },
                        onStopLiveViewClick = {
                            stopLiveView()
                            setOverlayState(OverlayState.COLLAPSED)
                        },
                        onEnableChatMode = { tone, customTone, count ->
                            startChatMode(tone, customTone, count)
                        },
                        onStopChatMode = {
                            stopChatMode()
                        },
                        onRegenerateChat = {
                            regenerateChatReply()
                        },
                        onChangeChatTone = {
                            setOverlayState(OverlayState.CHAT_MODE_CONFIG)
                        },
                        onDismissChatSuggestion = {
                            dismissChatSuggestion()
                        },
                        onCaptureScreen = {
                            captureUnderlyingScreen()
                        },
                        onDiscardScreenshot = {
                            _lastCapturedBitmap.value = null
                            ScreenCaptureManager.instance.discardScreenshot()
                            setOverlayState(OverlayState.COLLAPSED)
                        },
                        onDismissToCompanion = {
                            stopLiveView()
                            setOverlayState(OverlayState.COLLAPSED)
                        },
                        onCloseScreenBuddy = {
                            closeAssistant()
                        },
                        onDragDelta = { dx, dy ->
                            updatePositionByDelta(dx, dy)
                        }
                    )
                }
            }
        }

        // Touch listener that handles OUTSIDE touches for modal dismissal
        composeView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                val state = _overlayState.value
                if (state != OverlayState.COLLAPSED && state != OverlayState.CHAT_MODE_ACTIVE) {
                    stopLiveView()
                    setOverlayState(OverlayState.COLLAPSED)
                    return@setOnTouchListener true
                }
            }
            false
        }

        overlayRootView = composeView
        try {
            windowManager.addView(overlayRootView, layoutParams)
            Log.d(TAG, "Floating overlay view successfully added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view: ${e.message}", e)
        }
    }

    fun updatePositionByDelta(dx: Float, dy: Float) {
        val params = layoutParams ?: return
        updateDisplayMetrics()
        val companionSizePx = (92 * displayDensity).toInt()
        val minX = 0
        val maxX = (screenWidth - companionSizePx).coerceAtLeast(0)
        val minY = (24 * displayDensity).toInt()
        val maxY = (screenHeight - companionSizePx - 40).coerceAtLeast(minY)

        val newX = (params.x + dx.toInt()).coerceIn(minX, maxX)
        val newY = (params.y + dy.toInt()).coerceIn(minY, maxY)

        if (newX != params.x || newY != params.y) {
            params.x = newX
            params.y = newY
            lastCompanionX = newX
            lastCompanionY = newY
            try {
                windowManager.updateViewLayout(overlayRootView, params)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating window layout during drag: ${e.message}")
            }
        }
    }

    private fun toggleMenu() {
        if (_overlayState.value == OverlayState.COLLAPSED) {
            setOverlayState(OverlayState.MENU)
        } else {
            setOverlayState(OverlayState.COLLAPSED)
        }
    }

    fun setOverlayState(newState: OverlayState) {
        _overlayState.value = newState
        updateWindowFlagsForState(newState)
    }

    private fun updateWindowFlagsForState(state: OverlayState) {
        val params = layoutParams ?: return
        updateDisplayMetrics()

        if (state == OverlayState.ACTION_RESPONSE || state == OverlayState.ASK_AI || state == OverlayState.CHAT_MODE_CONFIG) {
            // Needs focus for typing follow-ups / prompt refinement / custom tone text input
            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            // Non-focusable so background touches pass through outside overlay bounds
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }

        val companionSizePx = (92 * displayDensity).toInt()
        if (state == OverlayState.COLLAPSED || state == OverlayState.CHAT_MODE_ACTIVE) {
            if (lastCompanionX < 0) {
                lastCompanionX = screenWidth - 220
                lastCompanionY = screenHeight / 3
            }
            val minX = 0
            val maxX = (screenWidth - companionSizePx).coerceAtLeast(0)
            val minY = (24 * displayDensity).toInt()
            val maxY = (screenHeight - companionSizePx - 40).coerceAtLeast(minY)

            params.x = lastCompanionX.coerceIn(minX, maxX)
            params.y = lastCompanionY.coerceIn(minY, maxY)
        } else {
            val cardWidthPx = (310 * displayDensity).toInt()
            val cardHeightPx = (460 * displayDensity).toInt()

            val anchorX = if (lastCompanionX > screenWidth / 2) {
                (lastCompanionX + companionSizePx - cardWidthPx).coerceIn(12, screenWidth - cardWidthPx - 12)
            } else {
                lastCompanionX.coerceIn(12, screenWidth - cardWidthPx - 12)
            }
            val anchorY = lastCompanionY.coerceIn(60, (screenHeight - cardHeightPx - 60).coerceAtLeast(60))

            params.x = anchorX
            params.y = anchorY
        }

        try {
            windowManager.updateViewLayout(overlayRootView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating window flags: ${e.message}")
        }
    }

    private fun snapToNearestEdge() {
        val params = layoutParams ?: return
        updateDisplayMetrics()

        val companionSizePx = (92 * displayDensity).toInt()
        val isCloserToLeft = (params.x + companionSizePx / 2) < (screenWidth / 2)

        val targetX = if (isCloserToLeft) 16 else (screenWidth - companionSizePx - 16)
        params.x = targetX
        params.y = params.y.coerceIn(80, screenHeight - 240)
        lastCompanionX = params.x
        lastCompanionY = params.y

        try {
            windowManager.updateViewLayout(overlayRootView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error snapping to edge: ${e.message}")
        }
    }

    /**
     * Dispatches user-selected companion action.
     */
    private fun handleCompanionAction(action: CompanionActionType) {
        if (action == CompanionActionType.CHAT_MODE) {
            setOverlayState(OverlayState.CHAT_MODE_CONFIG)
            return
        }
        if (action == CompanionActionType.LIVE_VIEW) {
            setOverlayState(OverlayState.LIVE_VIEW_CONFIRM)
            return
        }
        _currentAction.value = action
        coroutineScope.launch {
            val bitmap = captureUnderlyingScreen()
            _lastCapturedBitmap.value = bitmap
            setOverlayState(OverlayState.ACTION_RESPONSE)
        }
    }

    /**
     * Activates ScreenBuddy CHAT MODE.
     * Starts RAM-only context tracking and Smart Reply evaluation.
     */
    fun startChatMode(tone: ChatTone, customTone: String?, maxSuggestions: Int) {
        _chatTone.value = tone
        _customToneText.value = customTone
        _maxChatSuggestions.value = maxSuggestions
        _isChatModeActive.value = true
        _isSuggestionVisible.value = false
        _chatSuggestion.value = null
        _isChatGenerating.value = false
        _chatEngineState.value = ChatEngineState.ACTIVE_IDLE
        setOverlayState(OverlayState.COLLAPSED)

        visualChangeDetector.reset()
        smartDecisionEngine.duplicateDetector.reset()
        smartDecisionEngine.conversationContextEngine.clearContext()
        ScreenBuddyDiagnostics.recordMode("CHAT")
        ScreenBuddyDiagnostics.recordChatState("ACTIVE_IDLE")

        var sessionCount = 0
        chatModeJob?.cancel()
        chatModeJob = coroutineScope.launch {
            val captureManager = ScreenCaptureManager.instance

            while (isActive && _isChatModeActive.value) {
                try {
                    val latest = captureManager.getCurrentBitmap()
                    if (latest != null && !latest.isRecycled) {
                        // Avoid mutating _lastCapturedBitmap here to prevent Compose recomposition storm
                        visualChangeDetector.processFrame(latest)

                        // Extract local context & validate frame
                        val localContext = localContextEngine.extractContext(latest)
                        val frameValidation = FrameValidator.validateFrame(latest)

                        // 1. App package switch detection
                        val currentStoredPkg = smartDecisionEngine.conversationContextEngine.getContext().appPackage
                        if (localContext.foregroundAppPackage != null && currentStoredPkg != null &&
                            localContext.foregroundAppPackage != currentStoredPkg) {
                            ScreenBuddyDiagnostics.recordChatEvent("SCREEN_SWITCH")
                            smartDecisionEngine.conversationContextEngine.clearContext()
                            _chatSuggestion.value = null
                            _isSuggestionVisible.value = false
                            _chatEngineState.value = ChatEngineState.WAITING_INCOMING
                            ScreenBuddyDiagnostics.recordChatState("WAITING_INCOMING")
                        }

                        // 2. Keyboard / User typing check
                        if (localContext.isKeyboardActive) {
                            if (_chatEngineState.value != ChatEngineState.USER_TYPING) {
                                _chatEngineState.value = ChatEngineState.USER_TYPING
                                ScreenBuddyDiagnostics.recordChatState("USER_TYPING")
                                ScreenBuddyDiagnostics.recordChatEvent("USER_TYPING")
                            }
                            ScreenBuddyDiagnostics.recordAiGate("SUPPRESS", "USER_TYPING")
                            ScreenBuddyDiagnostics.recordSuppression("TYPING")
                        } else {
                            if (_chatEngineState.value == ChatEngineState.USER_TYPING) {
                                _chatEngineState.value = if (_isSuggestionVisible.value && _chatSuggestion.value != null) {
                                    ChatEngineState.SUGGESTION_VISIBLE
                                } else {
                                    ChatEngineState.WAITING_INCOMING
                                }
                                ScreenBuddyDiagnostics.recordChatState(_chatEngineState.value.name)
                            }

                            // 3. Evaluate context & changes
                            val decision = smartDecisionEngine.evaluate(
                                isUserInitiated = false,
                                localContext = localContext,
                                visualDiffResult = visualChangeDetector.lastDiffResult,
                                frameValidation = frameValidation,
                                isCooldownActive = visualChangeDetector.isCooldownActive(),
                                sessionRequestCount = sessionCount,
                                intent = com.example.core.ai.UserIntent.REPLY,
                                chatTone = _chatTone.value,
                                customTone = _customToneText.value,
                                isChatMode = true
                            )

                            when (decision.decision) {
                                AnalysisDecisionType.IGNORE -> {
                                    if (decision.isSuppressedByInFlight) {
                                        ScreenBuddyDiagnostics.recordAiGate("SUPPRESS", "IN_FLIGHT")
                                    } else {
                                        val reasonTag = when {
                                            decision.reason.contains("Duplicate", ignoreCase = true) -> "DUPLICATE"
                                            decision.reason.contains("outgoing", ignoreCase = true) ||
                                            decision.reason.contains("User sent", ignoreCase = true) -> {
                                                ScreenBuddyDiagnostics.recordChatEvent("USER_SENT")
                                                _chatSuggestion.value = null
                                                _isSuggestionVisible.value = false
                                                _chatEngineState.value = ChatEngineState.WAITING_NEXT_INCOMING
                                                ScreenBuddyDiagnostics.recordChatState("WAITING_NEXT_INCOMING")
                                                "NO_AI_NEEDED"
                                            }
                                            decision.reason.contains("typing", ignoreCase = true) -> "USER_TYPING"
                                            decision.reason.contains("batched", ignoreCase = true) ||
                                            decision.reason.contains("settling", ignoreCase = true) -> "DEBOUNCE"
                                            else -> "NO_MEANINGFUL_CHANGE"
                                        }
                                        ScreenBuddyDiagnostics.recordAiGate("SUPPRESS", reasonTag)
                                    }
                                }
                                AnalysisDecisionType.PROTECTED_CONTENT -> {
                                    Log.d(TAG, "[DIAGNOSTIC] [CHAT_MODE_PROTECTED] Protected screen detected")
                                    _chatSuggestion.value = ChatModeSuggestion(
                                        primaryReply = "Screen content is hidden/protected.",
                                        tone = _chatTone.value,
                                        isProtectedContentNotice = true
                                    )
                                    _isSuggestionVisible.value = true
                                    _chatEngineState.value = ChatEngineState.SUGGESTION_VISIBLE
                                    ScreenBuddyDiagnostics.recordChatState("SUGGESTION_VISIBLE")
                                }
                                AnalysisDecisionType.TEXT_ANALYSIS,
                                AnalysisDecisionType.VISION_ANALYSIS,
                                AnalysisDecisionType.USER_REQUESTED_VISION -> {
                                    ScreenBuddyDiagnostics.recordChatEvent("NEW_INCOMING")
                                    _chatEngineState.value = ChatEngineState.PROCESSING
                                    ScreenBuddyDiagnostics.recordChatState("PROCESSING")
                                    ScreenBuddyDiagnostics.recordAiGate("ALLOW", "NEW_INCOMING")

                                    executeChatModeAnalysis(latest, decision, localContext.foregroundAppPackage)
                                    sessionCount++
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Chat mode tick error: ${e.message}")
                }
                delay(visualChangeDetector.config.pollingIntervalMs)
            }
        }
    }

    /**
     * Dismisses the currently displayed smart reply suggestion without disabling Chat Mode.
     */
    fun dismissChatSuggestion() {
        ScreenBuddyDiagnostics.recordChatEvent("SUGGESTION_DISMISSED")
        _isSuggestionVisible.value = false
        _chatSuggestion.value = null
        _chatEngineState.value = ChatEngineState.WAITING_INCOMING
        ScreenBuddyDiagnostics.recordChatState("WAITING_INCOMING")
    }

    /**
     * Stops Chat Mode session completely and returns to collapsed companion.
     */
    fun stopChatMode() {
        ScreenBuddyDiagnostics.recordChatEvent("CHAT_MODE_STOPPED")
        _isChatModeActive.value = false
        _isSuggestionVisible.value = false
        _chatSuggestion.value = null
        _isChatGenerating.value = false
        _chatEngineState.value = ChatEngineState.OFF
        ScreenBuddyDiagnostics.recordChatState("OFF")
        chatModeJob?.cancel()
        chatModeJob = null
        if (_overlayState.value == OverlayState.CHAT_MODE_ACTIVE || _overlayState.value == OverlayState.CHAT_MODE_CONFIG) {
            setOverlayState(OverlayState.COLLAPSED)
        }
    }

    /**
     * Manually triggers a regeneration of smart reply for the current screen in Chat Mode.
     */
    fun regenerateChatReply() {
        val latest = _lastCapturedBitmap.value ?: ScreenCaptureManager.instance.getCurrentBitmap()
        val localContext = localContextEngine.extractContext(latest)
        val decision = smartDecisionEngine.evaluate(
            isUserInitiated = true,
            localContext = localContext,
            intent = com.example.core.ai.UserIntent.REPLY,
            chatTone = _chatTone.value,
            customTone = _customToneText.value,
            isChatMode = true
        )
        executeChatModeAnalysis(latest, decision, localContext.foregroundAppPackage)
    }

    private fun executeChatModeAnalysis(bitmap: Bitmap?, decision: AnalysisDecision, appPackage: String?) {
        val aiService = VisionAiClient.getService(service)
        val isVision = decision.decision == AnalysisDecisionType.VISION_ANALYSIS || 
                       decision.decision == AnalysisDecisionType.USER_REQUESTED_VISION
        val reqSeq = ScreenBuddyDiagnostics.nextRequestId()
        val modeStr = if (isVision) "VISION" else "TEXT"
        
        visualChangeDetector.markAnalysisStarted()
        _isChatGenerating.value = true
        _chatEngineState.value = ChatEngineState.PROCESSING
        ScreenBuddyDiagnostics.recordChatState("PROCESSING")
        ScreenBuddyDiagnostics.recordChatAiRequestStart(reqSeq)
        ScreenBuddyDiagnostics.recordChatAiInputMode(modeStr)

        val startTime = System.currentTimeMillis()

        coroutineScope.launch {
            smartDecisionEngine.setRequestInFlight(true)
            val requestTimestamp = smartDecisionEngine.conversationContextEngine.getContext().lastUpdatedTimestamp
            val requestPackage = smartDecisionEngine.conversationContextEngine.getContext().appPackage

            try {
                val effectivePrompt = CompanionActionType.CHAT_MODE.defaultPrompt
                
                // Hard timeout of 6500ms for fast chat reply generation
                val result = withTimeoutOrNull(6500L) {
                    withContext(Dispatchers.IO) {
                        aiService.analyzeScreenshot(
                            bitmap = if (isVision) bitmap else null,
                            question = effectivePrompt,
                            intent = com.example.core.ai.UserIntent.REPLY,
                            textContext = decision.extractedTextContext
                        )
                    }
                }

                val durationMs = System.currentTimeMillis() - startTime

                if (result == null) {
                    // AI Request Timed Out
                    ScreenBuddyDiagnostics.recordTimeout()
                    ScreenBuddyDiagnostics.recordChatAiRequestEnd(reqSeq, durationMs, false)
                    ScreenBuddyDiagnostics.recordChatTiming(
                        extractionMs = 50L,
                        duplicateCheckMs = 10L,
                        aiCallMs = durationMs,
                        totalMs = durationMs + 60L,
                        outcome = "TIMEOUT",
                        mode = modeStr
                    )
                    Log.w(TAG, "[CHAT-AI] Request #$reqSeq timed out after ${durationMs}ms")
                    
                    // Show error state with retry option rather than leaving user stuck
                    _chatSuggestion.value = ChatModeSuggestion(
                        primaryReply = "Couldn't generate reply in time",
                        candidateReplies = emptyList(),
                        tone = _chatTone.value,
                        customToneText = _customToneText.value,
                        sourcePackage = appPackage,
                        isError = true
                    )
                    _isSuggestionVisible.value = true
                    _chatEngineState.value = ChatEngineState.WAITING_NEXT_INCOMING
                    ScreenBuddyDiagnostics.recordChatState("WAITING_NEXT_INCOMING")
                    return@launch
                }

                val currentTimestamp = smartDecisionEngine.conversationContextEngine.getContext().lastUpdatedTimestamp
                val currentPackage = smartDecisionEngine.conversationContextEngine.getContext().appPackage

                if (requestPackage != null && currentPackage != null && requestPackage != currentPackage) {
                    ScreenBuddyDiagnostics.recordStaleResponseDiscarded(reqSeq, "APP_PACKAGE_CHANGED")
                    ScreenBuddyDiagnostics.recordChatAiRequestEnd(reqSeq, durationMs, false)
                    _chatEngineState.value = ChatEngineState.WAITING_NEXT_INCOMING
                    ScreenBuddyDiagnostics.recordChatState("WAITING_NEXT_INCOMING")
                    return@launch
                }
                if (currentTimestamp > requestTimestamp + 4000L) {
                    ScreenBuddyDiagnostics.recordStaleResponseDiscarded(reqSeq, "NEW_MESSAGES_ARRIVED")
                    ScreenBuddyDiagnostics.recordChatAiRequestEnd(reqSeq, durationMs, false)
                    _chatEngineState.value = ChatEngineState.WAITING_NEXT_INCOMING
                    ScreenBuddyDiagnostics.recordChatState("WAITING_NEXT_INCOMING")
                    return@launch
                }

                if (result is VisionAiResult.Success) {
                    ScreenBuddyDiagnostics.recordChatAiRequestEnd(reqSeq, durationMs, true)
                    ScreenBuddyDiagnostics.recordChatTiming(
                        extractionMs = 50L,
                        duplicateCheckMs = 10L,
                        aiCallMs = durationMs,
                        totalMs = durationMs + 60L,
                        outcome = "SUCCESS",
                        mode = modeStr
                    )

                    val structured = result.structuredResponse
                    val suggestedReplies = structured?.suggestedReplies ?: emptyList()
                    val candidateTexts = suggestedReplies.map { it.text }.filter { it.isNotBlank() }
                    val primary = candidateTexts.firstOrNull() ?: result.answer

                    _chatSuggestion.value = ChatModeSuggestion(
                        primaryReply = primary,
                        candidateReplies = candidateTexts.take(_maxChatSuggestions.value),
                        tone = _chatTone.value,
                        customToneText = _customToneText.value,
                        sourcePackage = appPackage,
                        isError = false
                    )
                    _isSuggestionVisible.value = true
                    _chatEngineState.value = ChatEngineState.SUGGESTION_VISIBLE
                    ScreenBuddyDiagnostics.recordChatState("SUGGESTION_VISIBLE")
                } else if (result is VisionAiResult.Error) {
                    ScreenBuddyDiagnostics.recordChatAiRequestEnd(reqSeq, durationMs, false)
                    ScreenBuddyDiagnostics.recordChatTiming(
                        extractionMs = 50L,
                        duplicateCheckMs = 10L,
                        aiCallMs = durationMs,
                        totalMs = durationMs + 60L,
                        outcome = "ERROR_${result.errorType}",
                        mode = modeStr
                    )
                    Log.w(TAG, "[CHAT-AI] Request #$reqSeq returned error: ${result.message}")

                    _chatSuggestion.value = ChatModeSuggestion(
                        primaryReply = result.message.ifBlank { "Couldn't generate reply" },
                        candidateReplies = emptyList(),
                        tone = _chatTone.value,
                        customToneText = _customToneText.value,
                        sourcePackage = appPackage,
                        isError = true
                    )
                    _isSuggestionVisible.value = true
                    _chatEngineState.value = ChatEngineState.WAITING_NEXT_INCOMING
                    ScreenBuddyDiagnostics.recordChatState("WAITING_NEXT_INCOMING")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Chat mode analysis error: ${e.message}")
                val durationMs = System.currentTimeMillis() - startTime
                ScreenBuddyDiagnostics.recordChatAiRequestEnd(reqSeq, durationMs, false)
                _chatSuggestion.value = ChatModeSuggestion(
                    primaryReply = "Couldn't generate reply",
                    candidateReplies = emptyList(),
                    tone = _chatTone.value,
                    customToneText = _customToneText.value,
                    sourcePackage = appPackage,
                    isError = true
                )
                _isSuggestionVisible.value = true
                _chatEngineState.value = ChatEngineState.WAITING_NEXT_INCOMING
                ScreenBuddyDiagnostics.recordChatState("WAITING_NEXT_INCOMING")
            } finally {
                smartDecisionEngine.setRequestInFlight(false)
                visualChangeDetector.markAnalysisFinished()
                _isChatGenerating.value = false
            }
        }
    }

    /**
     * Starts Live View stream following explicit user confirmation.
     * Level 10: Integrates Local Intelligence & Smart Context Engine (decides WHEN Gemini is needed).
     */
    private fun startLiveViewActive() {
        setOverlayState(OverlayState.LIVE_VIEW_ACTIVE)
        visualChangeDetector.reset()
        smartDecisionEngine.duplicateDetector.reset()
        smartDecisionEngine.conversationContextEngine.clearContext()
        _liveAiResult.value = null
        var sessionCount = 0

        liveViewJob?.cancel()
        liveViewJob = coroutineScope.launch {
            val captureManager = ScreenCaptureManager.instance

            while (isActive && _overlayState.value == OverlayState.LIVE_VIEW_ACTIVE) {
                val latest = captureManager.getCurrentBitmap()
                if (latest != null && !latest.isRecycled) {
                    _lastCapturedBitmap.value = latest
                    visualChangeDetector.processFrame(latest)
                    _visualEngineState.value = visualChangeDetector.engineState.value

                    // Level 10: Extract local context & validate frame
                    val localContext = localContextEngine.extractContext(latest)
                    val frameValidation = FrameValidator.validateFrame(latest)

                    // Level 10: Evaluate decision via Smart Decision Engine
                    val decision = smartDecisionEngine.evaluate(
                        isUserInitiated = false,
                        localContext = localContext,
                        visualDiffResult = visualChangeDetector.lastDiffResult,
                        frameValidation = frameValidation,
                        isCooldownActive = visualChangeDetector.isCooldownActive(),
                        sessionRequestCount = sessionCount
                    )

                    when (decision.decision) {
                        AnalysisDecisionType.IGNORE -> {
                            // If it's suppressed because a request is currently in flight, store it as the latest pending state
                            if (decision.isSuppressedByInFlight) {
                                Log.d(TAG, "[AI-GATE] LATEST_STATE_PENDING. Storing suppressed update as pending.")
                                ScreenBuddyDiagnostics.recordLatestStateActive("A")
                                if (pendingLatestState == null) {
                                    ScreenBuddyDiagnostics.recordPendingStateUpdated()
                                } else {
                                    ScreenBuddyDiagnostics.recordPendingStateReplaced()
                                }
                                pendingLatestState = PendingState(
                                    bitmap = latest,
                                    decision = decision.copy(decision = if (localContext.isConversationScreen || localContext.hasSufficientTextContext) AnalysisDecisionType.TEXT_ANALYSIS else AnalysisDecisionType.VISION_ANALYSIS),
                                    appPackage = localContext.foregroundAppPackage
                                )
                            }
                        }
                        AnalysisDecisionType.PROTECTED_CONTENT -> {
                            Log.d(TAG, "[DIAGNOSTIC] [PROTECTED_CONTENT] Suppressing Gemini API call due to protected screen content")
                            _liveAiResult.value = VisionAiResult.Error(
                                message = decision.reason,
                                canRetry = false,
                                errorType = "PROTECTED_CONTENT"
                            )
                        }
                        AnalysisDecisionType.TEXT_ANALYSIS,
                        AnalysisDecisionType.VISION_ANALYSIS,
                        AnalysisDecisionType.USER_REQUESTED_VISION -> {
                            executeLiveAnalysis(latest, decision, localContext.foregroundAppPackage)
                            sessionCount++
                        }
                    }
                }
                delay(visualChangeDetector.config.pollingIntervalMs)
            }
        }
    }

    private fun executeLiveAnalysis(bitmap: Bitmap?, decision: AnalysisDecision, appPackage: String?) {
        val aiService = VisionAiClient.getService(service)
        val isVision = decision.decision == AnalysisDecisionType.VISION_ANALYSIS || 
                       decision.decision == AnalysisDecisionType.USER_REQUESTED_VISION
        val reqSeq = ScreenBuddyDiagnostics.nextRequestId()
        
        visualChangeDetector.markAnalysisStarted()
        _visualEngineState.value = visualChangeDetector.engineState.value
        
        coroutineScope.launch {
            smartDecisionEngine.setRequestInFlight(true)
            val requestTimestamp = smartDecisionEngine.conversationContextEngine.getContext().lastUpdatedTimestamp
            val requestPackage = smartDecisionEngine.conversationContextEngine.getContext().appPackage
            
            try {
                if (isVision) {
                    Log.d(TAG, "[AI-GATE] VISION_ANALYSIS. Reason: ${decision.reason}")
                } else {
                    Log.d(TAG, "[AI-GATE] TEXT_ANALYSIS. Reason: ${decision.reason}")
                }
                
                val result = withContext(Dispatchers.IO) {
                    aiService.analyzeScreenshot(
                        bitmap = if (isVision) bitmap else null,
                        question = "What is on this screen? Provide brief summary and key insights.",
                        textContext = decision.extractedTextContext
                    )
                }
                
                if (isActive && _overlayState.value == OverlayState.LIVE_VIEW_ACTIVE) {
                    val currentContext = smartDecisionEngine.conversationContextEngine.getContext()
                    if (currentContext.lastUpdatedTimestamp != requestTimestamp || currentContext.appPackage != requestPackage) {
                        Log.d(TAG, "[AI-GATE] STALE_RESPONSE_DISCARDED. Context changed during API call.")
                        ScreenBuddyDiagnostics.recordStaleResponseDiscarded(reqSeq, "CONTEXT_CHANGED")
                    } else {
                        if (result is VisionAiResult.Error && (result.errorType == "RATE_LIMITED" || result.retryAfterSeconds > 0)) {
                            val cooldownMs = if (result.retryAfterSeconds > 0) result.retryAfterSeconds * 1000L else 30000L
                            visualChangeDetector.extendCooldown(cooldownMs)
                        }
                        _liveAiResult.value = result
                        
                        if (result is VisionAiResult.Success) {
                            val summaryText = result.structuredResponse?.summary ?: result.answer
                            if (summaryText.isNotBlank()) {
                                smartDecisionEngine.conversationContextEngine.seedContext(summaryText, requestPackage)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error performing Live View AI analysis: ${e.message}")
            } finally {
                smartDecisionEngine.setRequestInFlight(false)
                visualChangeDetector.markAnalysisFinished()
                _visualEngineState.value = visualChangeDetector.engineState.value
                
                // Level 15: Coalescing trigger after request finishes
                val pending = pendingLatestState
                pendingLatestState = null
                if (pending != null) {
                    val currentContext = smartDecisionEngine.conversationContextEngine.getContext()
                    if (currentContext.appPackage == pending.appPackage) {
                        Log.d(TAG, "[AI-GATE] Launching pending latest coalesced state")
                        ScreenBuddyDiagnostics.recordProcessingLatestState()
                        executeLiveAnalysis(pending.bitmap, pending.decision, pending.appPackage)
                    } else {
                        Log.d(TAG, "[AI-GATE] STALE_RESPONSE_DISCARDED. Discarding pending state because app package changed.")
                        ScreenBuddyDiagnostics.recordStaleResponseDiscarded(0L, "PACKAGE_CHANGED")
                    }
                }
            }
        }
    }

    private fun stopLiveView() {
        liveViewJob?.cancel()
        liveViewJob = null
        visualChangeDetector.reset()
        smartDecisionEngine.conversationContextEngine.clearContext()
        _visualEngineState.value = VisualEngineState.Idle
        _liveAiResult.value = null
    }

    /**
     * Core Level 6 Capture Primitive:
     * Completely hides overlay view, delays for display flush, grabs frame, and re-shows overlay.
     */
    suspend fun captureUnderlyingScreen(): Bitmap? {
        return withContext(Dispatchers.Main) {
            overlayRootView?.visibility = View.GONE
            delay(350)
            val bitmap = ScreenCaptureManager.instance.captureCleanFrame(100L)
            overlayRootView?.visibility = View.VISIBLE
            bitmap
        }
    }

    private fun closeAssistant() {
        stopLiveView()
        stopChatMode()
        service.stopSelf()
    }

    fun destroy() {
        stopLiveView()
        stopChatMode()
        overlayRootView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay: ${e.message}")
            }
        }
        overlayRootView = null
        _lastCapturedBitmap.value = null
    }

    companion object {
        private const val TAG = "FloatingOverlayMgr"
        var instance: FloatingOverlayManager? = null
    }
}

/**
 * Top-level Compose UI Container for the Floating Overlay.
 */
/**
 * Top-level Compose UI Container for the Floating Overlay (Level 7: Cute Companion).
 */
@Composable
private fun FloatingOverlayRoot(
    state: OverlayState,
    action: CompanionActionType,
    capturedBitmap: Bitmap?,
    visualEngineState: VisualEngineState,
    liveAiResult: VisionAiResult?,
    isChatModeActive: Boolean,
    isSuggestionVisible: Boolean,
    chatTone: ChatTone,
    customTone: String?,
    maxSuggestions: Int,
    chatSuggestion: ChatModeSuggestion?,
    isChatGenerating: Boolean,
    onCompanionTap: () -> Unit,
    onActionSelected: (CompanionActionType) -> Unit,
    onStartLiveViewConfirmed: () -> Unit,
    onStopLiveViewClick: () -> Unit,
    onEnableChatMode: (ChatTone, String?, Int) -> Unit,
    onStopChatMode: () -> Unit,
    onRegenerateChat: () -> Unit,
    onChangeChatTone: () -> Unit,
    onDismissChatSuggestion: () -> Unit,
    onCaptureScreen: suspend () -> Bitmap?,
    onDiscardScreenshot: () -> Unit,
    onDismissToCompanion: () -> Unit,
    onCloseScreenBuddy: () -> Unit,
    onDragDelta: (Float, Float) -> Unit
) {
    val isDraggable = state == OverlayState.COLLAPSED || state == OverlayState.CHAT_MODE_ACTIVE

    val dragModifier = if (isDraggable) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDragDelta(dragAmount.x, dragAmount.y)
            }
        }
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .then(dragModifier)
            .padding(4.dp)
    ) {
        when (state) {
            OverlayState.COLLAPSED, OverlayState.CHAT_MODE_ACTIVE -> {
                if (isChatModeActive && isSuggestionVisible && (chatSuggestion != null || isChatGenerating)) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChatModeSuggestionCard(
                            suggestion = chatSuggestion,
                            isLoading = isChatGenerating,
                            currentTone = chatTone,
                            onCopy = { },
                            onRegenerate = onRegenerateChat,
                            onChangeTone = onChangeChatTone,
                            onDismiss = onDismissChatSuggestion,
                            onStopChatMode = onStopChatMode
                        )

                        LottieCompanion(
                            state = if (isChatGenerating) CompanionState.THINKING else if (chatSuggestion != null) CompanionState.HAPPY else CompanionState.IDLE,
                            size = 82.dp,
                            onClick = onCompanionTap
                        )
                    }
                } else {
                    LottieCompanion(
                        state = CompanionState.IDLE,
                        size = 104.dp,
                        onClick = onCompanionTap
                    )
                }
            }

            OverlayState.MENU -> {
                CompanionMenu(
                    onActionSelected = onActionSelected,
                    onCloseCompanion = onCloseScreenBuddy,
                    onDismissMenu = onDismissToCompanion
                )
            }

            OverlayState.CHAT_MODE_CONFIG -> {
                ChatModeConfigCard(
                    initialTone = chatTone,
                    initialCustomTone = customTone ?: "",
                    initialMaxSuggestions = maxSuggestions,
                    onEnableChatMode = onEnableChatMode,
                    onDismiss = onDismissToCompanion
                )
            }

            OverlayState.ACTION_RESPONSE -> {
                CompanionResponseCard(
                    action = action,
                    initialBitmap = capturedBitmap,
                    onCaptureScreen = onCaptureScreen,
                    onClose = onDismissToCompanion
                )
            }

            OverlayState.LIVE_VIEW_CONFIRM -> {
                FloatingLiveViewConsentCard(
                    onStartLiveView = onStartLiveViewConfirmed,
                    onCancel = onDismissToCompanion
                )
            }

            OverlayState.LIVE_VIEW_ACTIVE -> {
                FloatingLiveViewActiveCard(
                    bitmap = capturedBitmap,
                    engineState = visualEngineState,
                    aiResult = liveAiResult,
                    onStop = onStopLiveViewClick
                )
            }

            OverlayState.SCREENSHOT_PREVIEW -> {
                FloatingScreenshotPreviewCard(
                    bitmap = capturedBitmap,
                    onAskAi = { onActionSelected(CompanionActionType.LOOK) },
                    onWhatShouldIReply = { onActionSelected(CompanionActionType.REPLY) },
                    onRecapture = { onActionSelected(CompanionActionType.LOOK) },
                    onDiscard = onDiscardScreenshot,
                    onClose = onDismissToCompanion
                )
            }

            OverlayState.ASK_AI -> {
                CompanionResponseCard(
                    action = CompanionActionType.LOOK,
                    initialBitmap = capturedBitmap,
                    onCaptureScreen = onCaptureScreen,
                    onClose = onDismissToCompanion
                )
            }
        }
    }
}

/**
 * Privacy Consent Confirmation Dialog for Live View (Section 6 Requirement).
 */
@Composable
private fun FloatingLiveViewConsentCard(
    onStartLiveView: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0B1120),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF38BDF8)))
        ),
        shadowElevation = 16.dp,
        modifier = Modifier
            .width(280.dp)
            .testTag("floating_live_view_consent_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CuteCompanion(state = CompanionState.CONFUSED, size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Live View?",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "ScreenBuddy will analyze your screen while Live View is active.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "🔒 Privacy: Stream stays in local RAM. Zero frames are stored on disk or uploaded without permission.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartLiveView,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp)
                        .testTag("start_live_view_confirm_button")
                ) {
                    Text("Start Live View", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onCancel,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(36.dp)
                        .testTag("cancel_live_view_button")
                ) {
                    Text("Cancel", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

/**
 * Active Live View Visual Assistant Card (Level 9: Visual Change Engine & Real-time AI stream).
 */
@Composable
private fun FloatingLiveViewActiveCard(
    bitmap: Bitmap?,
    engineState: VisualEngineState,
    aiResult: VisionAiResult?,
    onStop: () -> Unit
) {
    val companionState = when (engineState) {
        is VisualEngineState.Analyzing -> CompanionState.THINKING
        is VisualEngineState.ChangeDetected -> CompanionState.LOOKING
        else -> if (aiResult != null && aiResult is VisionAiResult.Success) CompanionState.HAPPY else CompanionState.LOOKING
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0B1120),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFF38BDF8)))
        ),
        shadowElevation = 16.dp,
        modifier = Modifier
            .width(290.dp)
            .testTag("floating_live_view_active_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with companion + LIVE VIEW badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CuteCompanion(state = companionState, size = 38.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE VIEW",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF4444),
                            letterSpacing = 0.8.sp,
                            fontSize = 13.sp
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "RAM-Only",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Level 9 Visual Engine Status Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when (engineState) {
                    is VisualEngineState.Analyzing -> Color(0xFF312E81)
                    is VisualEngineState.ChangeDetected -> Color(0xFF451A03)
                    else -> Color(0xFF1E293B)
                },
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        when (engineState) {
                            is VisualEngineState.Analyzing -> listOf(Color(0xFF818CF8), Color(0xFF6366F1))
                            is VisualEngineState.ChangeDetected -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            else -> listOf(Color(0xFF334155), Color(0xFF1E293B))
                        }
                    )
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    when (engineState) {
                        is VisualEngineState.Analyzing -> {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color(0xFF818CF8),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🧠 Analyzing screen changes...",
                                color = Color(0xFFC7D2FE),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is VisualEngineState.ChangeDetected -> {
                            Text(
                                text = "✨ Change detected (${engineState.deltaPercent.toInt()}%) • Settling...",
                                color = Color(0xFFFDE68A),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is VisualEngineState.Monitoring -> {
                            val statusText = if (engineState.isCooldownActive) {
                                "⏳ Cooldown active • Monitoring"
                            } else {
                                "🟢 Monitoring screen changes"
                            }
                            Text(
                                text = statusText,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp
                            )
                        }
                        else -> {
                            Text(
                                text = "🟢 Monitoring visual changes",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Screen Frame Preview
            if (bitmap != null && !bitmap.isRecycled) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Live Screen Frame",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                ) {
                    Text("Awaiting live frames...", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }

            // Real-time AI Insight Section (Level 10 Structured Result)
            if (aiResult != null && aiResult is VisionAiResult.Success) {
                val context = LocalContext.current
                val summaryText = aiResult.structuredResponse?.summary ?: aiResult.answer
                val insightText = aiResult.structuredResponse?.mainInsight
                val replies = aiResult.structuredResponse?.suggestedReplies ?: emptyList()
                var copiedIdx by remember { mutableStateOf<Int?>(null) }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = summaryText,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp
                        )
                        if (!insightText.isNullOrBlank() && insightText != summaryText) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = insightText,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }

                        if (replies.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "💬 Suggested Replies",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                replies.take(3).forEachIndexed { rIdx, replyItem ->
                                    val isCopied = copiedIdx == rIdx
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCopied) Color(0xFF064E3B) else Color(0xFF0F172A),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = ClipData.newPlainText("ScreenBuddy Reply", replyItem.text)
                                                clipboard?.setPrimaryClip(clip)
                                                copiedIdx = rIdx
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = replyItem.toneLabel,
                                                    color = if (isCopied) Color(0xFF34D399) else Color(0xFFF59E0B),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = replyItem.text,
                                                    color = Color.White,
                                                    fontSize = 10.5.sp,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copy reply",
                                                tint = if (isCopied) Color(0xFF34D399) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (aiResult != null && aiResult is VisionAiResult.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (aiResult.errorType == "RATE_LIMITED") "☕ AI taking a tiny break — resuming shortly" else aiResult.message,
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🔒 Privacy: Ephemeral RAM only. Zero disk persistence.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF64748B),
                    fontSize = 9.5.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onStop,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("stop_live_view_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop Live View", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}


/**
 * Collapsed Floating Robot Bubble (56dp with subtle breathing glow and ScreenBuddy brand).
 */
@Composable
private fun FloatingRobotBubble(
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "robot_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        shape = CircleShape,
        color = BentoSlate900,
        shadowElevation = 8.dp,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(BentoIndigo, Color(0xFF38BDF8))
            )
        ),
        modifier = Modifier
            .size(58.dp)
            .scale(pulseScale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("floating_robot_bubble")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            RobotAvatar(size = 42.dp, showGlow = false)
        }
    }
}

/**
 * Menu Action Card (Level 6: Look at Screen, Screenshot, Live View, Ask AI, What Should I Reply?, Close).
 */
@Composable
private fun FloatingMenuCard(
    onLookAtScreen: () -> Unit,
    onScreenshot: () -> Unit,
    onLiveView: () -> Unit,
    onAskAi: () -> Unit,
    onWhatShouldIReply: () -> Unit,
    onClose: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = BentoSlate900,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BentoIndigoBorder, Color(0xFF334155)))
        ),
        shadowElevation = 12.dp,
        modifier = Modifier
            .width(270.dp)
            .testTag("floating_menu_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RobotAvatar(size = 32.dp, showGlow = true)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SCREENBUDDY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoIndigo,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Collapse",
                        tint = BentoSlate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 1: "👀 Look at Screen" (Feature 1)
            MenuActionButton(
                emoji = "👀",
                title = "Look at Screen",
                subtitle = "AI summarizes and explains screen",
                onClick = onLookAtScreen,
                highlight = true,
                tag = "menu_action_look_at_screen"
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action 2: "📸 Screenshot" (Feature 2)
            MenuActionButton(
                emoji = "📸",
                title = "Screenshot",
                subtitle = "Capture clean underlying app",
                onClick = onScreenshot,
                highlight = false,
                tag = "menu_action_screenshot"
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action 3: "🔴 Live View" (Feature 3)
            MenuActionButton(
                emoji = "🔴",
                title = "Live View",
                subtitle = "Local screen stream (No AI/upload)",
                onClick = onLiveView,
                highlight = false,
                tag = "menu_action_live_view"
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action 4: "💬 Ask AI" (Feature 4)
            MenuActionButton(
                emoji = "💬",
                title = "Ask AI",
                subtitle = "Ask anything about your screen",
                onClick = onAskAi,
                highlight = false,
                tag = "menu_action_ask_ai"
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action 5: "💭 What Should I Reply?" (Feature 5)
            MenuActionButton(
                emoji = "💭",
                title = "What Should I Reply?",
                subtitle = "Smart suggested replies for chats",
                onClick = onWhatShouldIReply,
                highlight = true,
                tag = "menu_action_what_should_i_reply"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action 6: "✕ Close"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClose() }
                    .testTag("menu_action_close")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "✕ Stop ScreenBuddy",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    highlight: Boolean,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (highlight) BentoIndigoLight else Color(0xFF1E293B),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                if (highlight) listOf(BentoIndigoBorder, BentoIndigo) else listOf(Color(0xFF334155), Color(0xFF1E293B))
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (highlight) BentoIndigo else Color.White
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (highlight) BentoIndigo.copy(alpha = 0.8f) else BentoSlate400,
                        fontSize = 10.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Screenshot Preview Card (Shows clean screenshot in RAM with quick actions).
 */
@Composable
private fun FloatingScreenshotPreviewCard(
    bitmap: Bitmap?,
    onAskAi: () -> Unit,
    onWhatShouldIReply: () -> Unit,
    onRecapture: () -> Unit,
    onDiscard: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = BentoSlate900,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BentoIndigoBorder, Color(0xFF334155)))
        ),
        shadowElevation = 12.dp,
        modifier = Modifier
            .width(280.dp)
            .testTag("floating_screenshot_preview_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📸", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clean Screenshot",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                // RAM-only badge
                Surface(
                    shape = CircleShape,
                    color = BentoEmeraldBg
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = BentoEmerald,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "RAM only",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BentoEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thumbnail Image
            if (bitmap != null && !bitmap.isRecycled) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Underlying Screen",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Text("Acquiring clean frame...", color = BentoSlate400, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: "Ask AI about this"
            Button(
                onClick = onAskAi,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BentoIndigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("preview_ask_ai_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask AI about this screen", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action: "What Should I Reply?"
            OutlinedButton(
                onClick = onWhatShouldIReply,
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("preview_reply_button")
            ) {
                Text("💭 What Should I Reply?", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Secondary Actions: Retake / Discard
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onRecapture,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text("Retake", fontSize = 11.sp, color = Color.White)
                }

                OutlinedButton(
                    onClick = onDiscard,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    Text("Discard", fontSize = 11.sp, color = BentoSlate400)
                }
            }
        }
    }
}

/**
 * Live View Local Preview Card (Level 4: Private local screen preview).
 */
@Composable
private fun FloatingLiveViewCard(
    bitmap: Bitmap?,
    onStop: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = BentoSlate900,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFF334155)))
        ),
        shadowElevation = 12.dp,
        modifier = Modifier
            .width(280.dp)
            .testTag("floating_live_view_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with pulsing red dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔴", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Live Screen View",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1E293B)
                ) {
                    Text(
                        text = "Local Only",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BentoEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Screen Frame Preview
            if (bitmap != null && !bitmap.isRecycled) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Live Screen Frame",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Text("Awaiting live frames...", color = BentoSlate400, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🔒 Privacy: Stream stays in local RAM. Zero frames are transmitted or stored.",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BentoSlate400,
                    fontSize = 10.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onStop,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("stop_live_view_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop Live View", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

/**
 * Compact Ask AI Assistant Interaction Card (Level 5: Real Vision AI Interaction).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FloatingAskAiCard(
    initialBitmap: Bitmap?,
    onCaptureScreen: suspend () -> Bitmap?,
    onAskAnother: () -> Unit,
    onDiscard: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var userQuestion by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var analysisStartTime by remember { mutableLongStateOf(0L) }
    var elapsedTimeSeconds by remember { mutableLongStateOf(0L) }
    var activeJob by remember { mutableStateOf<Job?>(null) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    // Standard Quick Actions for Level 5
    val quickActions = listOf(
        "What should I reply?",
        "Explain this",
        "What does this mean?",
        "What should I click?",
        "Summarize this",
        "Translate this"
    )

    // Elapsed timer while analyzing
    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            analysisStartTime = System.currentTimeMillis()
            elapsedTimeSeconds = 0L
            while (isActive && isAnalyzing) {
                delay(1000)
                elapsedTimeSeconds = (System.currentTimeMillis() - analysisStartTime) / 1000
            }
        }
    }

    // Reset copied state after 2 seconds
    LaunchedEffect(copiedToClipboard) {
        if (copiedToClipboard) {
            delay(2000)
            copiedToClipboard = false
        }
    }

    fun submitQuery(query: String) {
        val q = query.trim().ifBlank { "What is on my screen?" }
        keyboardController?.hide()
        focusManager.clearFocus()
        errorMessage = null
        aiResponse = null

        activeJob?.cancel()
        activeJob = coroutineScope.launch {
            try {
                isCapturing = true
                val bitmap = initialBitmap ?: onCaptureScreen()
                isCapturing = false

                if (bitmap == null) {
                    errorMessage = "I couldn't capture the screen. Please try again."
                    return@launch
                }

                isAnalyzing = true
                val aiService = VisionAiClient.getService()
                val result = aiService.analyzeScreenshot(bitmap, q)

                isAnalyzing = false
                when (result) {
                    is VisionAiResult.Success -> {
                        aiResponse = result.answer
                    }
                    is VisionAiResult.Error -> {
                        errorMessage = result.message
                    }
                }
            } catch (e: Exception) {
                isCapturing = false
                isAnalyzing = false
                errorMessage = "Something went wrong while analyzing your screen."
            }
        }
    }

    fun cancelAnalysis() {
        activeJob?.cancel()
        activeJob = null
        isCapturing = false
        isAnalyzing = false
    }

    fun copyAnswerToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("ScreenBuddy Answer", text)
            clipboard.setPrimaryClip(clip)
            copiedToClipboard = true
        }
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = BentoSlate900,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BentoIndigoBorder, Color(0xFF334155)))
        ),
        shadowElevation = 14.dp,
        modifier = Modifier
            .width(310.dp)
            .testTag("floating_ask_ai_card")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RobotAvatar(size = 30.dp, showGlow = isAnalyzing || isCapturing)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Ask ScreenBuddy",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Only looks when you ask",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BentoSlate400,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = {
                        cancelAnalysis()
                        onClose()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BentoSlate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Answer Result
            if (aiResponse != null && !isAnalyzing && !isCapturing) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(BentoIndigo, Color(0xFF38BDF8)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BentoEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ScreenBuddy Answer",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoEmerald
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = aiResponse ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.testTag("ai_answer_text")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons: Copy, Ask again, Close
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { aiResponse?.let { copyAnswerToClipboard(it) } },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copiedToClipboard) BentoEmerald else BentoIndigo
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("copy_answer_button")
                    ) {
                        Icon(
                            imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (copiedToClipboard) "Copied!" else "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            aiResponse = null
                            userQuestion = ""
                            onAskAnother()
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("ask_again_button")
                    ) {
                        Text("Ask again", fontSize = 11.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onDiscard,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(0.8f)
                            .height(34.dp)
                            .testTag("close_answer_button")
                    ) {
                        Text("Close", fontSize = 11.sp, color = BentoSlate400)
                    }
                }
            } else if (isAnalyzing || isCapturing) {
                // Loading & Privacy State
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(BentoIndigoBorder, Color(0xFF334155)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        // Prominent Privacy Indicator
                        Surface(
                            shape = CircleShape,
                            color = BentoEmeraldBg,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(BentoEmeraldBorder, BentoEmerald))
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = BentoEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ScreenBuddy is looking at your screen",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BentoEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        CircularProgressIndicator(
                            color = BentoIndigo,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isCapturing) "Capturing clean screen..." else "Analyzing visible screen context...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        if (elapsedTimeSeconds >= 10) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ScreenBuddy is taking longer than expected...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoSlate400,
                                    fontSize = 10.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { cancelAnalysis() },
                            shape = CircleShape,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("cancel_analysis_button")
                        ) {
                            Text("Cancel", fontSize = 11.sp, color = BentoSlate400)
                        }
                    }
                }
            } else {
                // Query Input State
                Text(
                    text = "How can I help?",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = userQuestion,
                    onValueChange = { userQuestion = it },
                    placeholder = {
                        Text(
                            "Ask something about your screen...",
                            fontSize = 12.sp,
                            color = BentoSlate400
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (userQuestion.isNotBlank()) {
                            submitQuery(userQuestion)
                        }
                    }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BentoIndigo,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("floating_ai_text_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Prompt Chips (Level 5)
                Text(
                    text = "Quick suggestions",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BentoSlate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickActions.forEach { prompt ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                            ),
                            modifier = Modifier
                                .clickable {
                                    userQuestion = prompt
                                    submitQuery(prompt)
                                }
                                .testTag("quick_chip_${prompt.take(6)}")
                        ) {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoSlate200,
                                    fontSize = 10.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Submit Button
                Button(
                    onClick = { submitQuery(userQuestion) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoIndigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("floating_submit_ask_ai_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ask ScreenBuddy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Look at Screen AI Card (Feature 1: Instant AI Screen Summary & Breakdown).
 */
@Composable
private fun FloatingLookAtScreenCard(
    initialBitmap: Bitmap?,
    onCaptureScreen: suspend () -> Bitmap?,
    onAskFollowUp: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isAnalyzing by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var analysisStartTime by remember { mutableLongStateOf(0L) }
    var elapsedTimeSeconds by remember { mutableLongStateOf(0L) }
    var activeJob by remember { mutableStateOf<Job?>(null) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copiedToClipboard by remember { mutableStateOf(false) }

    fun runAnalysis() {
        errorMessage = null
        aiResponse = null

        activeJob?.cancel()
        activeJob = coroutineScope.launch {
            try {
                isCapturing = true
                val bitmap = initialBitmap ?: onCaptureScreen()
                isCapturing = false

                if (bitmap == null) {
                    errorMessage = "I couldn't capture the screen. Please try again."
                    return@launch
                }

                isAnalyzing = true
                val aiService = VisionAiClient.getService()
                val prompt = "Provide a clear, helpful summary of what is currently displayed on this screen. Identify the application or website, main topics, key points, and recommended actions if any."
                val result = aiService.analyzeScreenshot(bitmap, prompt)

                isAnalyzing = false
                when (result) {
                    is VisionAiResult.Success -> {
                        aiResponse = result.answer
                    }
                    is VisionAiResult.Error -> {
                        errorMessage = result.message
                    }
                }
            } catch (e: Exception) {
                isCapturing = false
                isAnalyzing = false
                errorMessage = "Something went wrong while analyzing your screen."
            }
        }
    }

    LaunchedEffect(Unit) {
        if (aiResponse == null && !isAnalyzing) {
            runAnalysis()
        }
    }

    // Elapsed timer while analyzing
    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            analysisStartTime = System.currentTimeMillis()
            elapsedTimeSeconds = 0L
            while (isActive && isAnalyzing) {
                delay(1000)
                elapsedTimeSeconds = (System.currentTimeMillis() - analysisStartTime) / 1000
            }
        }
    }

    // Reset copied state after 2 seconds
    LaunchedEffect(copiedToClipboard) {
        if (copiedToClipboard) {
            delay(2000)
            copiedToClipboard = false
        }
    }

    fun cancelAnalysis() {
        activeJob?.cancel()
        activeJob = null
        isCapturing = false
        isAnalyzing = false
    }

    fun copyAnswerToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("ScreenBuddy Summary", text)
            clipboard.setPrimaryClip(clip)
            copiedToClipboard = true
        }
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = BentoSlate900,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BentoIndigoBorder, Color(0xFF334155)))
        ),
        shadowElevation = 14.dp,
        modifier = Modifier
            .width(310.dp)
            .testTag("floating_look_at_screen_card")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RobotAvatar(size = 30.dp, showGlow = isAnalyzing || isCapturing)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "👀 Screen Analysis",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Looking at active screen",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BentoSlate400,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = {
                        cancelAnalysis()
                        onClose()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BentoSlate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isAnalyzing || isCapturing) {
                // Loading & Privacy State
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(BentoIndigoBorder, Color(0xFF334155)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BentoEmeraldBg,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(BentoEmeraldBorder, BentoEmerald))
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = BentoEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ScreenBuddy is looking at your screen",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BentoEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        CircularProgressIndicator(
                            color = BentoIndigo,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isCapturing) "Capturing clean screen..." else "Summarizing screen context...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        if (elapsedTimeSeconds >= 10) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ScreenBuddy is taking longer than expected...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoSlate400,
                                    fontSize = 10.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { cancelAnalysis() },
                            shape = CircleShape,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("cancel_look_at_screen_button")
                        ) {
                            Text("Cancel", fontSize = 11.sp, color = BentoSlate400)
                        }
                    }
                }
            } else if (aiResponse != null) {
                // AI Result Display
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(BentoIndigo, Color(0xFF38BDF8)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BentoEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Screen Overview",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoEmerald
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = aiResponse ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.testTag("look_at_screen_result_text")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Copy, Ask follow-up, Retake
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { aiResponse?.let { copyAnswerToClipboard(it) } },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copiedToClipboard) BentoEmerald else BentoIndigo
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("copy_look_at_screen_button")
                    ) {
                        Icon(
                            imageVector = if (copiedToClipboard) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (copiedToClipboard) "Copied!" else "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onAskFollowUp,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(34.dp)
                            .testTag("ask_follow_up_button")
                    ) {
                        Text("💬 Ask Follow-up", fontSize = 11.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { runAnalysis() },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(0.8f)
                            .height(34.dp)
                            .testTag("retake_look_at_screen_button")
                    ) {
                        Text("Retake", fontSize = 11.sp, color = BentoSlate400)
                    }
                }
            } else if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFCA5A5))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { runAnalysis() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Analysis", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data class representing a parsed smart reply option.
 */
private data class ParsedReplyOption(
    val toneLabel: String,
    val text: String
)

/**
 * Helper to parse reply suggestions into structured items.
 */
private fun parseReplySuggestions(rawResponse: String): List<ParsedReplyOption> {
    val lines = rawResponse.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    val parsed = mutableListOf<ParsedReplyOption>()

    for (line in lines) {
        val colonIdx = line.indexOf(':')
        if (colonIdx in 1..40) {
            val label = line.substring(0, colonIdx).trim().removePrefix("- ").removePrefix("* ").replace("**", "")
            val content = line.substring(colonIdx + 1).trim().removeSurrounding("\"")
            if (content.length > 2) {
                parsed.add(ParsedReplyOption(label, content))
            }
        }
    }
    return parsed
}

/**
 * What Should I Reply? Card (Feature 5: Smart Conversation Reply Suggestions).
 *
 * Requirements:
 * - Analyzes visible chat/message screen.
 * - Generates 3-4 distinct reply options with different tones (Friendly, Professional, Casual, Direct).
 * - Provides 1-tap "Copy" button for each option with instant visual feedback.
 * - Allows custom tone refinement.
 * - Never clicks Send or modifies external apps (Zero automated control).
 */
@Composable
private fun FloatingReplySuggestionsCard(
    initialBitmap: Bitmap?,
    onCaptureScreen: suspend () -> Bitmap?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var customToneInput by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var analysisStartTime by remember { mutableLongStateOf(0L) }
    var elapsedTimeSeconds by remember { mutableLongStateOf(0L) }
    var activeJob by remember { mutableStateOf<Job?>(null) }
    var rawAiResponse by remember { mutableStateOf<String?>(null) }
    var parsedReplies by remember { mutableStateOf<List<ParsedReplyOption>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var copiedIndex by remember { mutableStateOf<Int?>(null) }

    fun generateReplies(customInstruction: String? = null) {
        keyboardController?.hide()
        focusManager.clearFocus()
        errorMessage = null
        rawAiResponse = null
        parsedReplies = emptyList()

        activeJob?.cancel()
        activeJob = coroutineScope.launch {
            try {
                isCapturing = true
                val bitmap = initialBitmap ?: onCaptureScreen()
                isCapturing = false

                if (bitmap == null) {
                    errorMessage = "I couldn't capture the screen. Please try again."
                    return@launch
                }

                isAnalyzing = true
                val aiService = VisionAiClient.getService()
                val basePrompt = if (customInstruction.isNullOrBlank()) {
                    "You are ScreenBuddy analyzing the conversation, chat message, email, or discussion visible on screen. Suggest 3 to 4 distinct, natural reply options for the user with different tones. Format each option on its own line with a tone label, for example:\n😊 Friendly: [reply text]\n💼 Professional: [reply text]\n⚡ Casual & Quick: [reply text]\n🎯 Direct: [reply text]\n\nOnly output the suggestions without introductory or concluding filler."
                } else {
                    "You are ScreenBuddy analyzing the visible conversation. Suggest 3 to 4 distinct reply options with this specific instruction: '$customInstruction'. Format each option on its own line with a tone label like '😊 Friendly: ...' or '⚡ Quick: ...'."
                }

                val result = aiService.analyzeScreenshot(bitmap, basePrompt)

                isAnalyzing = false
                when (result) {
                    is VisionAiResult.Success -> {
                        rawAiResponse = result.answer
                        parsedReplies = parseReplySuggestions(result.answer)
                    }
                    is VisionAiResult.Error -> {
                        errorMessage = result.message
                    }
                }
            } catch (e: Exception) {
                isCapturing = false
                isAnalyzing = false
                errorMessage = "Something went wrong while generating replies."
            }
        }
    }

    LaunchedEffect(Unit) {
        if (rawAiResponse == null && !isAnalyzing) {
            generateReplies()
        }
    }

    // Elapsed timer while analyzing
    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            analysisStartTime = System.currentTimeMillis()
            elapsedTimeSeconds = 0L
            while (isActive && isAnalyzing) {
                delay(1000)
                elapsedTimeSeconds = (System.currentTimeMillis() - analysisStartTime) / 1000
            }
        }
    }

    // Reset copied state after 2 seconds
    LaunchedEffect(copiedIndex) {
        if (copiedIndex != null) {
            delay(2000)
            copiedIndex = null
        }
    }

    fun cancelAnalysis() {
        activeJob?.cancel()
        activeJob = null
        isCapturing = false
        isAnalyzing = false
    }

    fun copyToClipboard(index: Int, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("ScreenBuddy Suggested Reply", text)
            clipboard.setPrimaryClip(clip)
            copiedIndex = index
        }
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = BentoSlate900,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(BentoIndigoBorder, Color(0xFF334155)))
        ),
        shadowElevation = 14.dp,
        modifier = Modifier
            .width(310.dp)
            .testTag("floating_reply_suggestions_card")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RobotAvatar(size = 30.dp, showGlow = isAnalyzing || isCapturing)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "💭 Suggested Replies",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Tap Copy to paste into your chat",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BentoSlate400,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = {
                        cancelAnalysis()
                        onClose()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BentoSlate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isAnalyzing || isCapturing) {
                // Loading State
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(BentoIndigoBorder, Color(0xFF334155)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BentoEmeraldBg,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(BentoEmeraldBorder, BentoEmerald))
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = BentoEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Analyzing visible conversation",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BentoEmerald,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        CircularProgressIndicator(
                            color = BentoIndigo,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isCapturing) "Capturing clean chat screen..." else "Drafting smart reply options...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        if (elapsedTimeSeconds >= 10) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ScreenBuddy is taking longer than expected...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoSlate400,
                                    fontSize = 10.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { cancelAnalysis() },
                            shape = CircleShape,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Cancel", fontSize = 11.sp, color = BentoSlate400)
                        }
                    }
                }
            } else if (parsedReplies.isNotEmpty()) {
                // Structured Suggestions List
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    parsedReplies.forEachIndexed { index, option ->
                        val isThisCopied = copiedIndex == index

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(
                                    if (isThisCopied) listOf(BentoEmerald, BentoEmeraldBorder)
                                    else listOf(Color(0xFF334155), Color(0xFF1E293B))
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = option.toneLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isThisCopied) BentoEmerald else Color(0xFF38BDF8),
                                            fontSize = 11.sp
                                        )
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = if (isThisCopied) BentoEmerald else BentoIndigo,
                                        modifier = Modifier.clickable { copyToClipboard(index, option.text) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isThisCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isThisCopied) "Copied!" else "Copy",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = option.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White,
                                        lineHeight = 16.sp,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Refine tone input field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = customToneInput,
                        onValueChange = { customToneInput = it },
                        placeholder = {
                            Text("Refine tone (e.g. shorter, funnier)...", fontSize = 11.sp, color = BentoSlate400)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (customToneInput.isNotBlank()) {
                                generateReplies(customToneInput)
                            }
                        }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BentoIndigo,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (customToneInput.isNotBlank()) {
                                generateReplies(customToneInput)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(BentoIndigo, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Refine",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Privacy Notice
                Text(
                    text = "🔒 ScreenBuddy never automatically sends messages. You choose what to paste.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BentoSlate400,
                        fontSize = 9.5.sp,
                        lineHeight = 13.sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Regenerate, Retake
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { generateReplies() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BentoIndigo),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Regenerate", fontSize = 11.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = onClose,
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(0.8f)
                            .height(34.dp)
                    ) {
                        Text("Close", fontSize = 11.sp, color = BentoSlate400)
                    }
                }
            } else if (rawAiResponse != null) {
                // Fallback Display if structured parsing didn't match lines
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(BentoIndigo, Color(0xFF38BDF8)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = rawAiResponse ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                lineHeight = 18.sp
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
                        onClick = { copyToClipboard(0, rawAiResponse ?: "") },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copiedIndex == 0) BentoEmerald else BentoIndigo
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text(if (copiedIndex == 0) "Copied!" else "Copy All", fontSize = 11.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { generateReplies() },
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text("Regenerate", fontSize = 11.sp, color = Color.White)
                    }
                }
            } else if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFCA5A5))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { generateReplies() },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Suggestions", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}


