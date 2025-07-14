package com.ppai.voicetotask.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.ppai.voicetotask.domain.model.Task
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showNote: Boolean = false,
    onAddToCalendar: ((Task) -> Unit)? = null
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right - could be used for complete/uncomplete
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left - delete
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance ->
            // Make it easier to swipe - only need to swipe 25% of the distance
            totalDistance * 0.25f
        }
    )
    
    val haptics = LocalHapticFeedback.current
    
    // Trigger haptic feedback when reaching dismiss threshold
    LaunchedEffect(dismissState.progress) {
        if (dismissState.progress > 0.5f) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false, // Disable right swipe
        enableDismissFromEndToStart = true, // Enable left swipe for delete
        backgroundContent = {
            val backgroundColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                },
                label = "background_color"
            )
            
            val scale by animateFloatAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.8f,
                label = "icon_scale"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.scale(scale),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    ) {
        TaskItem(
            task = task,
            onToggleComplete = onToggleComplete,
            showNote = showNote,
            onAddToCalendar = onAddToCalendar
        )
    }
}