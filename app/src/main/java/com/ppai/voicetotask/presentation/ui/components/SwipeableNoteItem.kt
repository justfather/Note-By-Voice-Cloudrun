package com.ppai.voicetotask.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

enum class DismissDirection {
    StartToEnd,
    EndToStart
}

@Composable
fun SwipeableNoteItem(
    onDismissed: (DismissDirection) -> Unit,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableIntStateOf(0) }
    val dismissThreshold = 0.5f
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 300),
        label = "offset animation"
    )
    
    val progress = if (width > 0) (animatedOffsetX / width).coerceIn(-1f, 1f) else 0f
    val dismissDirection = when {
        progress > 0 -> DismissDirection.StartToEnd
        progress < 0 -> DismissDirection.EndToStart
        else -> null
    }
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            progress > 0 -> Color.Green.copy(alpha = progress.absoluteValue)
            progress < 0 -> Color.Red.copy(alpha = progress.absoluteValue)
            else -> Color.Transparent
        },
        label = "background color"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (progress.absoluteValue > 0.1f) 1f else 0.5f,
        label = "icon scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                width = size.width
            }
    ) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = when (dismissDirection) {
                DismissDirection.StartToEnd -> Alignment.CenterStart
                DismissDirection.EndToStart -> Alignment.CenterEnd
                null -> Alignment.Center
            }
        ) {
            if (progress.absoluteValue > 0.1f) {
                Icon(
                    imageVector = when (dismissDirection) {
                        DismissDirection.StartToEnd -> Icons.Default.Archive
                        DismissDirection.EndToStart -> Icons.Default.Delete
                        null -> Icons.Default.Delete
                    },
                    contentDescription = when (dismissDirection) {
                        DismissDirection.StartToEnd -> "Archive"
                        DismissDirection.EndToStart -> "Delete"
                        null -> null
                    },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .scale(iconScale),
                    tint = Color.White
                )
            }
        }
        
        // Foreground content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .alpha(1f - progress.absoluteValue * 0.3f)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX += delta
                    },
                    onDragStopped = {
                        coroutineScope.launch {
                            val shouldDismiss = progress.absoluteValue > dismissThreshold
                            if (shouldDismiss && dismissDirection != null) {
                                // Animate to fully dismissed position
                                offsetX = if (progress > 0) width.toFloat() else -width.toFloat()
                                onDismissed(dismissDirection)
                            } else {
                                // Animate back to original position
                                offsetX = 0f
                            }
                        }
                    }
                )
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = if (progress.absoluteValue > 0) 4.dp else 0.dp
            ) {
                content()
            }
        }
    }
}