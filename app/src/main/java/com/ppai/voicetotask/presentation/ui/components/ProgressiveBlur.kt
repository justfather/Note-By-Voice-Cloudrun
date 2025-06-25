package com.ppai.voicetotask.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Progressive blur overlay for focus mode
 */
@Composable
fun ProgressiveBlurOverlay(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val blur by animateDpAsState(
        targetValue = if (isActive) blurRadius else 0.dp,
        animationSpec = tween(300),
        label = "blur"
    )
    
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blur)
        ) {
            content()
        }
        
        if (isActive) {
            // Gradient overlay for additional depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.2f)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Focus mode container that highlights specific content
 */
@Composable
fun FocusModeContainer(
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    focusedContent: @Composable () -> Unit,
    backgroundContent: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Background content with progressive blur
        ProgressiveBlurOverlay(
            isActive = isFocused,
            modifier = Modifier.fillMaxSize()
        ) {
            backgroundContent()
        }
        
        // Focused content on top
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                focusedContent()
            }
        }
    }
}

/**
 * Radial progressive blur that increases from center
 */
@Composable
fun RadialProgressiveBlur(
    modifier: Modifier = Modifier,
    maxBlur: Dp = 20.dp,
    centerContent: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        // Multiple layers with increasing blur
        for (i in 3 downTo 0) {
            val blurAmount = (maxBlur.value * (i / 3f)).dp
            val alpha = 1f - (i * 0.2f)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurAmount)
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = alpha * 0.05f)
                    )
            ) {
                if (i == 0) {
                    centerContent()
                }
            }
        }
    }
}