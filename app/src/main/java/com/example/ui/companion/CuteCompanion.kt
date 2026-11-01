package com.example.ui.companion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * ScreenBuddy Cute AI Companion Composable (Level 9 Liquid-Gel Organic Polish).
 *
 * Design Characteristics:
 * - Cute, soft, living organic creature made from a soft gel/blob material.
 * - NO MOUTH (strictly expressive dark eyes with subtle highlights).
 * - Visibly larger, charming presence in the floating overlay.
 * - Substantially reduced bloom/glow (soft ambient presence, zero neon glare).
 * - Liquid-gel deformation: asymmetric squishy breathing, harmonic bezier perimeter wave, and spring physics.
 * - Zero-allocation rendering loop (cached Path objects) for buttery 60 FPS performance.
 */
@Composable
fun CuteCompanion(
    state: CompanionState = CompanionState.IDLE,
    size: Dp = 92.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_companion_motion")

    // 1. Organic vertical floating bob (-6px to +6px) with soft elastic timing
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -6.0f,
        targetValue = 6.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    CompanionState.THINKING -> 2000
                    CompanionState.EXCITED, CompanionState.HAPPY -> 1200
                    CompanionState.SLEEPY, CompanionState.SLEEPING -> 3800
                    else -> 2800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_vertical_bob"
    )

    // 2. Subtle horizontal gentle sway (-3.2px to +3.2px) out of phase for 8-figure organic drift
    val floatOffsetX by infiniteTransition.animateFloat(
        initialValue = -3.2f,
        targetValue = 3.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    CompanionState.EXCITED, CompanionState.HAPPY -> 1500
                    else -> 4100
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_horizontal_sway"
    )

    // 3. Body tilt angle (-3f to +3f degrees) synced smoothly with sway
    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = -3.0f,
        targetValue = 3.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    CompanionState.EXCITED, CompanionState.HAPPY -> 1400
                    CompanionState.CONFUSED -> 2600
                    else -> 3600
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_tilt"
    )

    // 4. Continuous linear phase (0..2π) for multi-harmonic liquid perimeter deformation
    val jellyPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    CompanionState.EXCITED, CompanionState.HAPPY -> 1800
                    CompanionState.THINKING -> 2400
                    CompanionState.SLEEPY, CompanionState.SLEEPING -> 4800
                    else -> 3400
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_jelly_phase"
    )

    // 5. Asymmetric Volume-Preserving Gel Breathing (breathScaleX out of phase with breathScaleY)
    val breathScaleX by infiniteTransition.animateFloat(
        initialValue = 0.982f,
        targetValue = 1.028f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    CompanionState.SLEEPY, CompanionState.SLEEPING -> 3600
                    CompanionState.THINKING -> 1900
                    CompanionState.EXCITED, CompanionState.HAPPY -> 1000
                    else -> 2600
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_breathing_x"
    )

    val breathScaleY by infiniteTransition.animateFloat(
        initialValue = 1.025f,
        targetValue = 0.975f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    CompanionState.SLEEPY, CompanionState.SLEEPING -> 3600
                    CompanionState.THINKING -> 1900
                    CompanionState.EXCITED, CompanionState.HAPPY -> 1000
                    else -> 2600
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_breathing_y"
    )

    // 6. Accessories float phase
    val accessoryPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_accessories"
    )

    // 7. Interactive Spring Physics (Tap Squash & Stretch + Error Wobble)
    val tapSquashX = remember { Animatable(1f) }
    val tapSquashY = remember { Animatable(1f) }
    val errorWobbleX = remember { Animatable(0f) }

    // 8. Smooth animated eye gaze tracking with low-stiffness springs
    val animGazeX = remember { Animatable(0f) }
    val animGazeY = remember { Animatable(0f) }

    // Procedural Natural Blinking
    var eyeBlinkProgress by remember { mutableFloatStateOf(1f) }

    // Blinking loop (smooth, non-robotic timing)
    LaunchedEffect(state) {
        if (state == CompanionState.SLEEPY || state == CompanionState.SLEEPING) {
            eyeBlinkProgress = 0f
            return@LaunchedEffect
        }

        while (isActive) {
            val delayMs = kotlin.random.Random.nextLong(3200, 6800)
            delay(delayMs)

            if (state == CompanionState.LOOKING || state == CompanionState.THINKING) {
                // Inquisitive double-blink
                eyeBlinkProgress = 0.08f
                delay(85)
                eyeBlinkProgress = 1f
                delay(105)
                eyeBlinkProgress = 0.08f
                delay(85)
                eyeBlinkProgress = 1f
            } else if (state == CompanionState.SURPRISED) {
                delay(2400)
                eyeBlinkProgress = 0.1f
                delay(85)
                eyeBlinkProgress = 1f
            } else {
                // Natural soft single blink
                eyeBlinkProgress = 0.04f
                delay(115)
                eyeBlinkProgress = 1f
            }
        }
    }

    // Dynamic Gaze Target shifts with gentle organic springs
    LaunchedEffect(state) {
        if (state == CompanionState.SLEEPY || state == CompanionState.SLEEPING) {
            animGazeX.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow))
            animGazeY.animateTo(0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessVeryLow))
            return@LaunchedEffect
        }

        if (state == CompanionState.ERROR) {
            // Trigger gentle error wobble animation
            launch {
                errorWobbleX.snapTo(-6f)
                errorWobbleX.animateTo(6f, tween(70))
                errorWobbleX.animateTo(-4f, tween(70))
                errorWobbleX.animateTo(4f, tween(70))
                errorWobbleX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
            }
        }

        while (isActive) {
            when (state) {
                CompanionState.LOOKING, CompanionState.READING -> {
                    // Scanning downward at the user's screen
                    val targetX = kotlin.random.Random.nextDouble(-0.18, 0.18).toFloat()
                    val targetY = 0.52f
                    launch { animGazeX.animateTo(targetX, spring(0.75f, Spring.StiffnessLow)) }
                    launch { animGazeY.animateTo(targetY, spring(0.75f, Spring.StiffnessLow)) }
                    delay(1700)
                }
                CompanionState.THINKING -> {
                    // Looking upward thoughtfully
                    launch { animGazeX.animateTo(0.32f, spring(0.75f, Spring.StiffnessLow)) }
                    launch { animGazeY.animateTo(-0.40f, spring(0.75f, Spring.StiffnessLow)) }
                    delay(2100)
                }
                CompanionState.SUGGESTING -> {
                    // Attentive friendly upward gaze ready to present
                    launch { animGazeX.animateTo(0f, spring(0.8f, Spring.StiffnessLow)) }
                    launch { animGazeY.animateTo(-0.15f, spring(0.8f, Spring.StiffnessLow)) }
                    delay(2200)
                }
                CompanionState.CONFUSED -> {
                    // Puzzled curious glance
                    launch { animGazeX.animateTo(0.28f, spring(0.75f, Spring.StiffnessLow)) }
                    launch { animGazeY.animateTo(-0.25f, spring(0.75f, Spring.StiffnessLow)) }
                    delay(2300)
                }
                CompanionState.SURPRISED -> {
                    // Centered attentive glance
                    launch { animGazeX.animateTo(0f, spring(0.8f, Spring.StiffnessLow)) }
                    launch { animGazeY.animateTo(0f, spring(0.8f, Spring.StiffnessLow)) }
                    delay(3000)
                }
                CompanionState.LIVE_VIEW -> {
                    // Attentive forward live stream focus
                    launch { animGazeX.animateTo(0f, spring(0.8f, Spring.StiffnessLow)) }
                    launch { animGazeY.animateTo(0.18f, spring(0.8f, Spring.StiffnessLow)) }
                    delay(2400)
                }
                CompanionState.ERROR -> {
                    // Downcast worried glance
                    launch { animGazeX.animateTo(-0.10f, spring(0.8f, Spring.StiffnessLow)) }
                    launch { animGazeY.animateTo(0.32f, spring(0.8f, Spring.StiffnessLow)) }
                    delay(2800)
                }
                else -> {
                    delay(kotlin.random.Random.nextLong(3000, 5600))
                    val choices = listOf(
                        Pair(0f, 0f),
                        Pair(0.20f, -0.03f),
                        Pair(-0.20f, -0.03f),
                        Pair(0f, 0.18f),
                        Pair(0.10f, 0.12f),
                        Pair(-0.10f, 0.12f)
                    )
                    val pick = choices.random()
                    launch { animGazeX.animateTo(pick.first, spring(0.7f, Spring.StiffnessLow)) }
                    launch { animGazeY.animateTo(pick.second, spring(0.7f, Spring.StiffnessLow)) }
                }
            }
        }
    }

    val stateDescription = when (state) {
        CompanionState.IDLE -> "ScreenBuddy is floating gently"
        CompanionState.LOOKING -> "ScreenBuddy is scanning your screen"
        CompanionState.READING -> "ScreenBuddy is reading conversation"
        CompanionState.THINKING -> "ScreenBuddy is processing"
        CompanionState.SUGGESTING -> "ScreenBuddy is preparing suggestions"
        CompanionState.HAPPY -> "ScreenBuddy has an answer ready!"
        CompanionState.SURPRISED -> "ScreenBuddy is amazed"
        CompanionState.CONFUSED -> "ScreenBuddy is curious"
        CompanionState.LIVE_VIEW -> "ScreenBuddy Live View active"
        CompanionState.SLEEPY, CompanionState.SLEEPING -> "ScreenBuddy is resting"
        CompanionState.ERROR -> "ScreenBuddy encountered an issue"
        CompanionState.EXCITED -> "ScreenBuddy is excited"
        CompanionState.LISTENING -> "ScreenBuddy is listening"
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = false, radius = size / 1.1f),
            onClick = {
                // Soft liquid gel spring squash-and-stretch on user tap
                coroutineScope.launch {
                    tapSquashX.animateTo(1.20f, animationSpec = tween(75, easing = FastOutSlowInEasing))
                    tapSquashY.animateTo(0.78f, animationSpec = tween(75, easing = FastOutSlowInEasing))
                    launch {
                        tapSquashX.animateTo(0.92f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                        tapSquashX.animateTo(1.05f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                        tapSquashX.animateTo(1f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                    }
                    launch {
                        tapSquashY.animateTo(1.12f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                        tapSquashY.animateTo(0.96f, animationSpec = tween(90, easing = FastOutSlowInEasing))
                        tapSquashY.animateTo(1f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                    }
                }
                onClick()
            }
        )
    } else {
        Modifier
    }

    // Cached Path objects for Zero-Allocation rendering loop
    val blobPath = remember { Path() }
    val highlightPath = remember { Path() }
    val qPath = remember { Path() }
    val dropPath = remember { Path() }
    val starPath = remember { Path() }
    val zPath = remember { Path() }

    val totalScaleX = tapSquashX.value * breathScaleX
    val totalScaleY = tapSquashY.value * breathScaleY

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .offset { IntOffset((floatOffsetX + errorWobbleX.value).roundToInt(), floatOffsetY.roundToInt()) }
            .rotate(tiltAngle)
            .scale(scaleX = totalScaleX, scaleY = totalScaleY)
            .semantics { contentDescription = stateDescription }
            .testTag("cute_companion_blob")
            .then(clickModifier)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

            // 1. Bottom soft ambient contact shadow
            drawContactShadow(center = center, width = canvasWidth, height = canvasHeight)

            // 2. Soft subtle outer glow outline (NO heavy bloom / neon flare)
            drawSubtleBlobGlow(center = center, width = canvasWidth, height = canvasHeight, state = state)

            // 3. Liquid-like gel droplet body with zero-allocation cached Path
            drawLiquidBlobBody(
                width = canvasWidth,
                height = canvasHeight,
                state = state,
                jellyPhase = jellyPhase,
                blobPath = blobPath,
                highlightPath = highlightPath
            )

            // 4. Soft rosy blush cheeks under the eyes
            drawCheekBlushes(center = center, width = canvasWidth, height = canvasHeight, state = state)

            // 5. Expressive big eyes (NO MOUTH) with subtle highlights & natural blinks
            drawBlobEyes(
                center = center,
                width = canvasWidth,
                height = canvasHeight,
                state = state,
                blinkProgress = eyeBlinkProgress,
                gazeX = animGazeX.value,
                gazeY = animGazeY.value,
                starPath = starPath
            )

            // 6. State accessories (thought bubble, question mark, red live dot, zZz, sweat drop)
            drawStateAccessories(
                center = center,
                width = canvasWidth,
                height = canvasHeight,
                state = state,
                phase = accessoryPhase,
                qPath = qPath,
                dropPath = dropPath,
                zPath = zPath
            )
        }
    }
}

/**
 * Draws soft ambient contact shadow below the companion.
 */
private fun DrawScope.drawContactShadow(
    center: Offset,
    width: Float,
    height: Float
) {
    val shadowWidth = width * 0.62f
    val shadowHeight = height * 0.12f
    val shadowTop = height * 0.89f

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x24000000),
                Color(0x0E000000),
                Color.Transparent
            ),
            center = Offset(center.x, shadowTop + shadowHeight / 2f),
            radius = shadowWidth / 2f
        ),
        topLeft = Offset(center.x - shadowWidth / 2f, shadowTop),
        size = Size(shadowWidth, shadowHeight)
    )
}

/**
 * Draws a subtle, soft, tight ambient glow outline.
 * Substantially reduced intensity to avoid neon bloom / washing out surrounding UI.
 */
private fun DrawScope.drawSubtleBlobGlow(
    center: Offset,
    width: Float,
    height: Float,
    state: CompanionState
) {
    val glowColor = when (state) {
        CompanionState.LIVE_VIEW -> Color(0xFFEF4444)
        CompanionState.ERROR -> Color(0xFFF87171)
        CompanionState.THINKING -> Color(0xFFFFD54F)
        CompanionState.HAPPY, CompanionState.EXCITED -> Color(0xFFFFEE58)
        CompanionState.SLEEPY, CompanionState.SLEEPING -> Color(0xFFFFF59D).copy(alpha = 0.10f)
        else -> Color(0xFFFFF59D) // Soft warm yellow
    }

    val glowAlpha = when (state) {
        CompanionState.HAPPY, CompanionState.EXCITED -> 0.20f
        CompanionState.SLEEPY, CompanionState.SLEEPING -> 0.08f
        else -> 0.14f // Gentle, understated ambient presence
    }

    val glowRadius = (width.coerceAtMost(height) / 2f) * 1.07f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = glowAlpha),
                glowColor.copy(alpha = glowAlpha * 0.30f),
                Color.Transparent
            ),
            center = Offset(center.x, center.y + height * 0.02f),
            radius = glowRadius
        ),
        radius = glowRadius,
        center = Offset(center.x, center.y + height * 0.02f)
    )
}

/**
 * Draws the organic liquid-like blob body using multi-harmonic bezier deformations.
 * Zero-allocation implementation reusing cached Path objects.
 */
private fun DrawScope.drawLiquidBlobBody(
    width: Float,
    height: Float,
    state: CompanionState,
    jellyPhase: Float,
    blobPath: Path,
    highlightPath: Path
) {
    // Multi-harmonic out-of-phase bezier offsets for continuous liquid gel movement
    val wave1 = sin(jellyPhase) * (width * 0.018f)
    val wave2 = cos(jellyPhase * 1.3f + 0.7f) * (height * 0.016f)
    val wave3 = sin(jellyPhase * 0.7f + 1.4f) * (width * 0.014f)

    val left = width * 0.13f - wave1
    val right = width * 0.87f + wave1
    val top = height * 0.09f + wave2
    val bottom = height * 0.91f - wave2

    blobPath.reset()
    blobPath.moveTo(width * 0.50f, top)

    // Top right tapering curve
    blobPath.cubicTo(
        width * 0.73f + wave3, top,
        right, height * 0.38f - wave2,
        right, height * 0.68f + wave2
    )

    // Bottom right rounded elastic base
    blobPath.cubicTo(
        right, bottom + height * 0.015f,
        width * 0.68f + wave1, bottom,
        width * 0.50f, bottom
    )

    // Bottom left rounded elastic base
    blobPath.cubicTo(
        width * 0.32f - wave1, bottom,
        left, bottom + height * 0.015f,
        left, height * 0.68f + wave2
    )

    // Left tapering curve returning to top
    blobPath.cubicTo(
        left, height * 0.38f - wave2,
        width * 0.27f - wave3, top,
        width * 0.50f, top
    )

    blobPath.close()

    // Soft warm yellow palette (#FFE082 base, #FFFDE7 top creamy highlight)
    val (topColor, midColor, bottomColor) = when (state) {
        CompanionState.ERROR -> Triple(Color(0xFFFFF176), Color(0xFFFFB74D), Color(0xFFFF8A65))
        CompanionState.SLEEPY, CompanionState.SLEEPING -> Triple(Color(0xFFFFFDE7), Color(0xFFFFF59D), Color(0xFFFFE082))
        else -> Triple(
            Color(0xFFFFFDE7), // Creamy soft light reflection
            Color(0xFFFFF59D), // Radiant warm yellow
            Color(0xFFFFE082)  // Soft warm yellow base
        )
    }

    // Fill soft jelly body
    drawPath(
        path = blobPath,
        brush = Brush.verticalGradient(
            colors = listOf(topColor, midColor, bottomColor),
            startY = top,
            endY = bottom
        )
    )

    // Soft inner rim stroke for depth
    drawPath(
        path = blobPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), Color(0xFFFFD54F).copy(alpha = 0.35f)),
            startY = top,
            endY = bottom
        ),
        style = Stroke(width = width * 0.032f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Primary gentle top-left specular highlight
    highlightPath.reset()
    highlightPath.moveTo(width * 0.28f, height * 0.26f)
    highlightPath.cubicTo(
        width * 0.32f, height * 0.15f,
        width * 0.44f, height * 0.13f,
        width * 0.51f, height * 0.15f
    )

    drawPath(
        path = highlightPath,
        color = Color.White.copy(alpha = 0.55f),
        style = Stroke(width = width * 0.042f, cap = StrokeCap.Round)
    )
}

/**
 * Draws cute soft rosy blush ovals under the companion's eyes.
 */
private fun DrawScope.drawCheekBlushes(
    center: Offset,
    width: Float,
    height: Float,
    state: CompanionState
) {
    if (state == CompanionState.ERROR) return

    val blushSpacing = width * 0.22f
    val blushCenterY = center.y + height * 0.06f
    val blushWidth = width * 0.10f
    val blushHeight = height * 0.05f

    val blushAlpha = when (state) {
        CompanionState.HAPPY, CompanionState.EXCITED -> 0.48f
        CompanionState.SURPRISED -> 0.38f
        CompanionState.SLEEPY, CompanionState.SLEEPING -> 0.22f
        else -> 0.30f
    }

    val blushColor = Color(0xFFFF8A80).copy(alpha = blushAlpha)

    // Left cheek blush
    drawOval(
        color = blushColor,
        topLeft = Offset(center.x - blushSpacing - blushWidth / 2f, blushCenterY - blushHeight / 2f),
        size = Size(blushWidth, blushHeight)
    )

    // Right cheek blush
    drawOval(
        color = blushColor,
        topLeft = Offset(center.x + blushSpacing - blushWidth / 2f, blushCenterY - blushHeight / 2f),
        size = Size(blushWidth, blushHeight)
    )
}

/**
 * Draws expressive big round eyes (NO MOUTH) with subtle soft highlights.
 */
private fun DrawScope.drawBlobEyes(
    center: Offset,
    width: Float,
    height: Float,
    state: CompanionState,
    blinkProgress: Float,
    gazeX: Float,
    gazeY: Float,
    starPath: Path
) {
    val eyeSpacing = width * 0.145f
    val eyeCenterY = center.y - height * 0.04f
    val leftEyeCenter = Offset(center.x - eyeSpacing, eyeCenterY)
    val rightEyeCenter = Offset(center.x + eyeSpacing, eyeCenterY)

    when (state) {
        CompanionState.HAPPY, CompanionState.EXCITED, CompanionState.SUGGESTING -> {
            val eyeRadius = width * 0.098f
            drawBlackEyeBase(center = leftEyeCenter, radius = eyeRadius, blinkProgress = blinkProgress)
            drawBlackEyeBase(center = rightEyeCenter, radius = eyeRadius, blinkProgress = blinkProgress)

            if (blinkProgress > 0.25f) {
                drawEyeSparkleStar(center = leftEyeCenter, size = eyeRadius * 1.25f, starPath = starPath)
                drawEyeSparkleStar(center = rightEyeCenter, size = eyeRadius * 1.25f, starPath = starPath)
            }
        }

        CompanionState.SLEEPY, CompanionState.SLEEPING -> {
            // Closed resting curved eyes (˘ ˘)
            val strokeWidth = width * 0.040f
            val arcSize = Size(width * 0.145f, height * 0.090f)

            drawArc(
                color = Color(0xFF0F172A),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(leftEyeCenter.x - arcSize.width / 2f, leftEyeCenter.y - arcSize.height / 2f),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF0F172A),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(rightEyeCenter.x - arcSize.width / 2f, rightEyeCenter.y - arcSize.height / 2f),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        CompanionState.SURPRISED -> {
            val eyeRadius = width * 0.110f
            drawRoundEye(
                center = leftEyeCenter,
                radius = eyeRadius,
                blinkProgress = blinkProgress,
                gazeX = 0f,
                gazeY = 0f,
                highlightScale = 1.15f
            )
            drawRoundEye(
                center = rightEyeCenter,
                radius = eyeRadius,
                blinkProgress = blinkProgress,
                gazeX = 0f,
                gazeY = 0f,
                highlightScale = 1.15f
            )
        }

        CompanionState.CONFUSED -> {
            drawRoundEye(
                center = leftEyeCenter,
                radius = width * 0.100f,
                blinkProgress = blinkProgress,
                gazeX = gazeX,
                gazeY = gazeY
            )
            drawRoundEye(
                center = Offset(rightEyeCenter.x, rightEyeCenter.y - height * 0.02f),
                radius = width * 0.080f,
                blinkProgress = blinkProgress,
                gazeX = gazeX,
                gazeY = gazeY
            )
        }

        CompanionState.LOOKING -> {
            val eyeRadius = width * 0.096f
            drawRoundEye(
                center = leftEyeCenter,
                radius = eyeRadius,
                blinkProgress = blinkProgress,
                gazeX = gazeX,
                gazeY = gazeY
            )
            drawRoundEye(
                center = rightEyeCenter,
                radius = eyeRadius,
                blinkProgress = blinkProgress,
                gazeX = gazeX,
                gazeY = gazeY
            )
        }

        CompanionState.THINKING -> {
            val eyeRadius = width * 0.094f
            drawRoundEye(
                center = leftEyeCenter,
                radius = eyeRadius,
                blinkProgress = blinkProgress,
                gazeX = gazeX,
                gazeY = gazeY
            )
            drawRoundEye(
                center = rightEyeCenter,
                radius = eyeRadius,
                blinkProgress = blinkProgress,
                gazeX = gazeX,
                gazeY = gazeY
            )
        }

        else -> {
            // IDLE, LIVE_VIEW, ERROR, EXCITED, LISTENING
            val eyeRadius = width * 0.095f
            drawRoundEye(
                center = leftEyeCenter,
                radius = eyeRadius,
                blinkProgress = blinkProgress,
                gazeX = gazeX,
                gazeY = gazeY
            )
            drawRoundEye(
                center = rightEyeCenter,
                radius = eyeRadius,
                blinkProgress = blinkProgress,
                gazeX = gazeX,
                gazeY = gazeY
            )
        }
    }
}

/**
 * Draws a solid round deep black pupil with soft, crisp white highlights.
 */
private fun DrawScope.drawRoundEye(
    center: Offset,
    radius: Float,
    blinkProgress: Float,
    gazeX: Float,
    gazeY: Float,
    highlightScale: Float = 1.0f
) {
    if (blinkProgress <= 0.08f) {
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = radius * 0.40f,
            cap = StrokeCap.Round
        )
        return
    }

    val eyeHeight = radius * 2f * blinkProgress
    val eyeWidth = radius * 2f

    // Deep black circular pupil (#0F172A)
    drawOval(
        color = Color(0xFF0F172A),
        topLeft = Offset(center.x - radius, center.y - eyeHeight / 2f),
        size = Size(eyeWidth, eyeHeight)
    )

    // Pupil shift with smooth gaze
    val maxShift = radius * 0.32f
    val pupilX = center.x + (gazeX * maxShift)
    val pupilY = center.y + (gazeY * maxShift * blinkProgress)

    // Primary subtle white shine (top-left)
    val primaryShineRadius = radius * 0.34f * highlightScale * blinkProgress
    if (primaryShineRadius > 0.5f) {
        drawCircle(
            color = Color.White,
            radius = primaryShineRadius,
            center = Offset(pupilX - radius * 0.28f, pupilY - radius * 0.28f * blinkProgress)
        )
    }

    // Secondary soft bounce light (bottom-right)
    val secondaryShineRadius = radius * 0.16f * highlightScale * blinkProgress
    if (secondaryShineRadius > 0.5f) {
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = secondaryShineRadius,
            center = Offset(pupilX + radius * 0.30f, pupilY + radius * 0.26f * blinkProgress)
        )
    }
}

/**
 * Helper to draw the base oval pupil for special eye states.
 */
private fun DrawScope.drawBlackEyeBase(
    center: Offset,
    radius: Float,
    blinkProgress: Float
) {
    if (blinkProgress <= 0.08f) {
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = radius * 0.40f,
            cap = StrokeCap.Round
        )
        return
    }

    val eyeHeight = radius * 2f * blinkProgress
    val eyeWidth = radius * 2f

    drawOval(
        color = Color(0xFF0F172A),
        topLeft = Offset(center.x - radius, center.y - eyeHeight / 2f),
        size = Size(eyeWidth, eyeHeight)
    )
}

/**
 * Draws a clean white 4-point star inside the eye for HAPPY/EXCITED state.
 */
private fun DrawScope.drawEyeSparkleStar(
    center: Offset,
    size: Float,
    starPath: Path
) {
    val half = size / 2f
    starPath.reset()
    starPath.moveTo(center.x, center.y - half)
    starPath.quadraticTo(center.x, center.y, center.x + half, center.y)
    starPath.quadraticTo(center.x, center.y, center.x, center.y + half)
    starPath.quadraticTo(center.x, center.y, center.x - half, center.y)
    starPath.quadraticTo(center.x, center.y, center.x, center.y - half)
    starPath.close()

    drawPath(path = starPath, color = Color.White)
    drawCircle(color = Color(0xFFFFF59D), radius = size * 0.18f, center = center)
}

/**
 * Draws state-specific accessories using cached zero-allocation Path objects.
 */
private fun DrawScope.drawStateAccessories(
    center: Offset,
    width: Float,
    height: Float,
    state: CompanionState,
    phase: Float,
    qPath: Path,
    dropPath: Path,
    zPath: Path
) {
    when (state) {
        CompanionState.THINKING -> {
            // Floating thought bubble (3 rising dots top-right)
            val bubbleBaseX = center.x + width * 0.34f
            val bubbleBaseY = center.y - height * 0.38f
            val bob = sin(phase * Math.PI.toFloat() * 2f) * (height * 0.032f)

            drawCircle(color = Color(0xFFFFF59D), radius = width * 0.026f, center = Offset(bubbleBaseX - width * 0.05f, bubbleBaseY + height * 0.12f + bob))
            drawCircle(color = Color(0xFFFFF59D), radius = width * 0.042f, center = Offset(bubbleBaseX - width * 0.01f, bubbleBaseY + height * 0.05f + bob))
            drawCircle(color = Color(0xFFFFF59D), radius = width * 0.068f, center = Offset(bubbleBaseX + width * 0.06f, bubbleBaseY - height * 0.03f + bob))
            drawCircle(color = Color.White, radius = width * 0.026f, center = Offset(bubbleBaseX + width * 0.04f, bubbleBaseY - height * 0.05f + bob))
        }

        CompanionState.SUGGESTING -> {
            // Floating spark of inspiration dots (top right)
            val sparkX = center.x + width * 0.32f
            val sparkY = center.y - height * 0.36f + (sin(phase * Math.PI.toFloat() * 2f) * height * 0.025f)
            val pulse = 0.85f + 0.15f * sin(phase * Math.PI.toFloat() * 2f)

            drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.4f), radius = width * 0.050f * pulse, center = Offset(sparkX, sparkY))
            drawCircle(color = Color(0xFF6366F1), radius = width * 0.032f, center = Offset(sparkX, sparkY))
            drawCircle(color = Color.White, radius = width * 0.014f, center = Offset(sparkX - width * 0.008f, sparkY - height * 0.008f))
        }

        CompanionState.CONFUSED -> {
            // Question mark (?) floating top-right
            val qX = center.x + width * 0.34f
            val qY = center.y - height * 0.40f + (sin(phase * Math.PI.toFloat() * 2f) * height * 0.032f)

            qPath.reset()
            qPath.moveTo(qX - width * 0.04f, qY - height * 0.03f)
            qPath.cubicTo(
                qX - width * 0.04f, qY - height * 0.08f,
                qX + width * 0.05f, qY - height * 0.08f,
                qX + width * 0.04f, qY - height * 0.03f
            )
            qPath.cubicTo(
                qX + width * 0.03f, qY,
                qX, qY + height * 0.01f,
                qX, qY + height * 0.04f
            )

            drawPath(
                path = qPath,
                color = Color(0xFFF59E0B),
                style = Stroke(width = width * 0.038f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = width * 0.020f,
                center = Offset(qX, qY + height * 0.075f)
            )
        }

        CompanionState.LIVE_VIEW -> {
            // Active live recording red indicator dot
            val dotX = center.x + width * 0.32f
            val dotY = center.y - height * 0.38f
            val pulse = 0.88f + 0.12f * sin(phase * Math.PI.toFloat() * 2f)

            // Red soft aura
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = 0.35f),
                radius = width * 0.065f * pulse,
                center = Offset(dotX, dotY)
            )
            // Solid red core
            drawCircle(
                color = Color(0xFFDC2626),
                radius = width * 0.038f,
                center = Offset(dotX, dotY)
            )
            // White specular core
            drawCircle(
                color = Color.White,
                radius = width * 0.013f,
                center = Offset(dotX - width * 0.01f, dotY - width * 0.01f)
            )
        }

        CompanionState.SLEEPY, CompanionState.SLEEPING -> {
            // Floating 'z Z z' letters rising top-right
            val zBaseX = center.x + width * 0.32f
            val zBaseY = center.y - height * 0.34f
            val drift = (phase * height * 0.14f)

            // Top big Z
            drawZLetter(
                topLeft = Offset(zBaseX + width * 0.05f, zBaseY - drift),
                size = width * 0.082f,
                color = Color(0xFFF59E0B).copy(alpha = (1f - phase * 0.6f).coerceIn(0.2f, 1f)),
                zPath = zPath
            )
            // Lower small z
            drawZLetter(
                topLeft = Offset(zBaseX - width * 0.02f, zBaseY + height * 0.08f - drift * 0.7f),
                size = width * 0.055f,
                color = Color(0xFFF59E0B).copy(alpha = (0.9f - phase * 0.4f).coerceIn(0.3f, 1f)),
                zPath = zPath
            )
        }

        CompanionState.ERROR -> {
            // Blue sweat droplet on top-right forehead
            val dropX = center.x + width * 0.30f
            val dropY = center.y - height * 0.30f

            dropPath.reset()
            dropPath.moveTo(dropX, dropY - height * 0.045f)
            dropPath.cubicTo(
                dropX + width * 0.038f, dropY - height * 0.01f,
                dropX + width * 0.038f, dropY + height * 0.035f,
                dropX, dropY + height * 0.035f
            )
            dropPath.cubicTo(
                dropX - width * 0.038f, dropY + height * 0.035f,
                dropX - width * 0.038f, dropY - height * 0.01f,
                dropX, dropY - height * 0.045f
            )
            dropPath.close()

            drawPath(path = dropPath, color = Color(0xFF38BDF8))
        }

        else -> {}
    }
}

/**
 * Helper to draw a crisp 'Z' letter for sleepy state using cached Path.
 */
private fun DrawScope.drawZLetter(
    topLeft: Offset,
    size: Float,
    color: Color,
    zPath: Path
) {
    zPath.reset()
    zPath.moveTo(topLeft.x, topLeft.y)
    zPath.lineTo(topLeft.x + size, topLeft.y)
    zPath.lineTo(topLeft.x, topLeft.y + size)
    zPath.lineTo(topLeft.x + size, topLeft.y + size)

    drawPath(path = zPath, color = color, style = Stroke(width = size * 0.24f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
