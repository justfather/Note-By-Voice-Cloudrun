package com.ppai.voicetotask.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset

// Animated visibility with fade and scale
@Composable
fun AnimatedVisibilityFadeScale(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        ) + scaleIn(
            initialScale = 0.9f,
            transformOrigin = TransformOrigin(0.5f, 0.5f),
            animationSpec = tween(
                durationMillis = 200,
                easing = FastOutSlowInEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 150,
                easing = FastOutSlowInEasing
            )
        ) + scaleOut(
            targetScale = 0.9f,
            transformOrigin = TransformOrigin(0.5f, 0.5f),
            animationSpec = tween(
                durationMillis = 150,
                easing = FastOutSlowInEasing
            )
        ),
        content = content
    )
}

// Animated FAB with scale animation
@Composable
fun AnimatedFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extended: Boolean = true,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (extended) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_scale"
    )
    
    if (extended) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier.scale(scale),
            icon = icon,
            text = text
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.scale(scale)
        ) {
            icon()
        }
    }
}

// Animated content with crossfade
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> AnimatedContent(
    targetState: T,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<T>.() -> ContentTransform = {
        fadeIn(animationSpec = tween(200)) with fadeOut(animationSpec = tween(200))
    },
    content: @Composable AnimatedVisibilityScope.(targetState: T) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = transitionSpec,
        content = content
    )
}

// Animated slide in from bottom
@Composable
fun AnimatedSlideInFromBottom(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200)
        ) + fadeOut(),
        content = content
    )
}

// Animated shimmer effect for loading
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    Box(
        modifier = modifier
            .shimmerBackground(translateAnim)
    )
}

// Extension function for shimmer background
private fun Modifier.shimmerBackground(translateAnim: Float): Modifier {
    return this // Simplified for now - would implement gradient animation
}

// Animated press effect
@Composable
fun rememberPressedScale(
    pressedScale: Float = 0.95f
): MutableState<Float> {
    return remember { mutableStateOf(1f) }
}

@Composable
fun Modifier.animatedPressedScale(
    scale: Float
): Modifier {
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressed_scale"
    )
    return this.scale(animatedScale)
}