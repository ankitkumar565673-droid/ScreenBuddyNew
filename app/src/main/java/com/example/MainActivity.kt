package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.core.capture.CaptureState
import com.example.core.capture.ScreenCaptureManager
import com.example.core.overlay.FloatingAssistantService
import com.example.model.AppScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.DemoConversationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScreenAccessInfoScreen
import com.example.ui.screens.ScreenAccessScreen
import com.example.ui.screens.ScreenAnalysisScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ScreenBuddy Main Activity (Level 3: Full Multimodal Vision AI Assistant).
 */
class MainActivity : ComponentActivity() {

    private var targetNavigationScreen: AppScreen? = null
    private var initialAnalysisQuestion: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntentExtras(intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ScreenBuddyApp(
                    initialScreen = targetNavigationScreen ?: AppScreen.HOME,
                    initialQuestion = initialAnalysisQuestion,
                    onMinimizeAndCaptureUnderlyingScreen = { question ->
                        minimizeAndCapture(question)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras(intent)
    }

    private fun handleIntentExtras(intent: Intent?) {
        val destination = intent?.getStringExtra(EXTRA_NAVIGATE_TO)
        if (destination == DESTINATION_ANALYSIS) {
            targetNavigationScreen = AppScreen.SCREEN_ANALYSIS
            initialAnalysisQuestion = intent.getStringExtra(EXTRA_INITIAL_QUESTION)
        }
    }

    /**
     * Minimizes ScreenBuddy to reveal Chrome or other underlying apps,
     * delays 350ms for clean display compositing, captures frame, and re-launches ScreenBuddy into Analysis.
     */
    private fun minimizeAndCapture(question: String?) {
        moveTaskToBack(true)
        val captureManager = ScreenCaptureManager.instance
        window.decorView.postDelayed({
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAVIGATE_TO, DESTINATION_ANALYSIS)
                if (question != null) {
                    putExtra(EXTRA_INITIAL_QUESTION, question)
                }
            }
            startActivity(intent)
        }, 350)
    }

    companion object {
        const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
        const val EXTRA_INITIAL_QUESTION = "extra_initial_question"
        const val DESTINATION_ANALYSIS = "destination_analysis"
    }
}

@Composable
fun ScreenBuddyApp(
    initialScreen: AppScreen = AppScreen.HOME,
    initialQuestion: String? = null,
    onMinimizeAndCaptureUnderlyingScreen: (String?) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val captureManager = remember { ScreenCaptureManager.instance }
    val captureState by captureManager.captureState.collectAsState()

    var currentScreen by remember { mutableStateOf(initialScreen) }
    var activeQuestion by remember { mutableStateOf(initialQuestion) }

    // MediaProjection permission launcher
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        captureManager.onPermissionResult(result.resultCode, result.data, context)
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            currentScreen = AppScreen.SCREEN_ANALYSIS
        }
    }

    // React to external intent triggers (e.g. from FloatingAssistantService)
    LaunchedEffect(initialScreen, initialQuestion) {
        if (initialScreen == AppScreen.SCREEN_ANALYSIS) {
            currentScreen = AppScreen.SCREEN_ANALYSIS
            activeQuestion = initialQuestion
        }
    }

    // Handle Android system back button cleanly
    BackHandler(enabled = currentScreen != AppScreen.HOME) {
        currentScreen = when (currentScreen) {
            AppScreen.SCREEN_ANALYSIS -> AppScreen.ASSISTANT
            AppScreen.SCREEN_ACCESS -> AppScreen.ASSISTANT
            AppScreen.DEMO_CONVERSATION -> AppScreen.ASSISTANT
            AppScreen.SCREEN_ACCESS_INFO -> AppScreen.ASSISTANT
            AppScreen.ASSISTANT -> AppScreen.HOME
            AppScreen.HOME -> AppScreen.HOME
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (slideInHorizontally { width -> width / 3 } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
            },
            label = "screen_transition",
            modifier = Modifier.padding(innerPadding)
        ) { screen ->
            when (screen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        onStartFloatingAssistant = {
                            FloatingAssistantService.start(context)
                            val activity = context as? android.app.Activity
                            activity?.moveTaskToBack(true)
                        }
                    )
                }

                AppScreen.ASSISTANT -> {
                    AssistantScreen(
                        onLookAtScreenClick = {
                            if (captureManager.isCaptureActive()) {
                                // If capture session active, minimize momentarily to capture underlying app (e.g. Chrome)
                                onMinimizeAndCaptureUnderlyingScreen("What is on my screen?")
                            } else {
                                // Request permission
                                val intent = captureManager.createScreenCaptureIntent(context)
                                mediaProjectionLauncher.launch(intent)
                            }
                        },
                        onAskQuestionClick = { query ->
                            activeQuestion = query
                            currentScreen = AppScreen.SCREEN_ANALYSIS
                        },
                        onCloseClick = {
                            currentScreen = AppScreen.HOME
                        }
                    )
                }

                AppScreen.SCREEN_ACCESS_INFO -> {
                    ScreenAccessInfoScreen(
                        onContinueClick = {
                            val intent = captureManager.createScreenCaptureIntent(context)
                            mediaProjectionLauncher.launch(intent)
                        },
                        onBackClick = {
                            currentScreen = AppScreen.ASSISTANT
                        }
                    )
                }

                AppScreen.SCREEN_ACCESS -> {
                    ScreenAccessScreen(
                        onBackClick = {
                            currentScreen = AppScreen.ASSISTANT
                        }
                    )
                }

                AppScreen.SCREEN_ANALYSIS -> {
                    ScreenAnalysisScreen(
                        initialQuestion = activeQuestion,
                        onBackClick = {
                            currentScreen = AppScreen.ASSISTANT
                        },
                        onRecaptureClick = {
                            if (captureManager.isCaptureActive()) {
                                onMinimizeAndCaptureUnderlyingScreen(activeQuestion)
                            } else {
                                val intent = captureManager.createScreenCaptureIntent(context)
                                mediaProjectionLauncher.launch(intent)
                            }
                        }
                    )
                }

                AppScreen.DEMO_CONVERSATION -> {
                    DemoConversationScreen(
                        onBackToHomeClick = {
                            currentScreen = AppScreen.HOME
                        }
                    )
                }
            }
        }
    }
}
