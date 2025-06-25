package com.ppai.voicetotask.presentation.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs

/**
 * Modifier to detect swipe gestures for navigation
 */
@Composable
fun Modifier.swipeGesture(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    swipeThreshold: Float = 100f
): Modifier {
    val density = LocalDensity.current
    var totalDrag by remember { mutableStateOf(0f) }
    
    return this.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { 
                totalDrag = 0f
            },
            onDragEnd = {
                val threshold = with(density) { swipeThreshold.toDp().toPx() }
                when {
                    totalDrag > threshold -> onSwipeRight?.invoke()
                    totalDrag < -threshold -> onSwipeLeft?.invoke()
                }
            },
            onHorizontalDrag = { _, dragAmount ->
                totalDrag += dragAmount
            }
        )
    }
}

/**
 * Modifier for edge swipe detection (useful for back navigation)
 */
@Composable
fun Modifier.edgeSwipeGesture(
    onSwipeFromLeft: (() -> Unit)? = null,
    onSwipeFromRight: (() -> Unit)? = null,
    edgeWidth: Float = 20f,
    swipeThreshold: Float = 100f
): Modifier {
    val density = LocalDensity.current
    var startX by remember { mutableStateOf(0f) }
    var totalDrag by remember { mutableStateOf(0f) }
    
    return this.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { offset ->
                startX = offset.x
                totalDrag = 0f
            },
            onDragEnd = {
                val edge = with(density) { edgeWidth.toDp().toPx() }
                val threshold = with(density) { swipeThreshold.toDp().toPx() }
                
                when {
                    startX < edge && totalDrag > threshold -> onSwipeFromLeft?.invoke()
                    startX > size.width - edge && totalDrag < -threshold -> onSwipeFromRight?.invoke()
                }
            },
            onHorizontalDrag = { _, dragAmount ->
                totalDrag += dragAmount
            }
        )
    }
}