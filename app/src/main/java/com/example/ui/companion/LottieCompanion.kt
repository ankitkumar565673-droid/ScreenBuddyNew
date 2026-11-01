package com.example.ui.companion

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Utility helper to determine available Lottie animation assets for character states.
 */
object LottieCompanionRegistry {

    fun hasAsset(context: Context, fileName: String): Boolean {
        return try {
            context.assets.open(fileName).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getLottieAssetForState(context: Context, state: CompanionState): String? {
        val stateSpecific = when (state) {
            CompanionState.THINKING -> "companion_thinking.json"
            CompanionState.HAPPY, CompanionState.EXCITED -> "companion_happy.json"
            CompanionState.SUGGESTING, CompanionState.LISTENING -> "companion_talking.json"
            CompanionState.SLEEPY, CompanionState.SLEEPING -> "companion_sleepy.json"
            else -> "companion_idle.json"
        }

        if (hasAsset(context, stateSpecific)) return stateSpecific
        if (hasAsset(context, "companion_idle.json")) return "companion_idle.json"
        if (hasAsset(context, "companion.json")) return "companion.json"
        return null
    }
}

/**
 * Universal Composable for playing Lottie JSON animations with state-based reactions.
 */
@Composable
fun LottieCompanion(
    state: CompanionState = CompanionState.IDLE,
    size: Dp = 88.dp,
    assetName: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val resolvedAsset = remember(state, assetName) {
        assetName ?: LottieCompanionRegistry.getLottieAssetForState(context, state)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = false, radius = size / 2),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(clickableModifier)
            .testTag("lottie_companion_character")
            .semantics {
                contentDescription = "Animated Companion State: ${state.name}"
            },
        contentAlignment = Alignment.Center
    ) {
        if (resolvedAsset != null) {
            val composition by rememberLottieComposition(LottieCompositionSpec.Asset(resolvedAsset))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                speed = when (state) {
                    CompanionState.THINKING -> 1.25f
                    CompanionState.HAPPY, CompanionState.EXCITED -> 1.2f
                    CompanionState.SLEEPY, CompanionState.SLEEPING -> 0.7f
                    else -> 1.0f
                },
                restartOnPlay = false
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Cute organic liquid-gel companion
            CuteCompanion(
                state = state,
                size = size,
                onClick = null
            )
        }
    }
}
