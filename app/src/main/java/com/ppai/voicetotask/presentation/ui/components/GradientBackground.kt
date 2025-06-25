package com.ppai.voicetotask.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.ppai.voicetotask.presentation.theme.*

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    
    val gradientColors = if (isLight) {
        listOf(
            Color(0xFFFAF9FF), // Very light purple
            Color(0xFFFFFFFF), // White
            Color(0xFFF5F3FF)  // Light purple
        )
    } else {
        listOf(
            Color(0xFF0A0A0F),
            Color(0xFF15151F),
            Color(0xFF1A1525)
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Add noise texture overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        ),
                        radius = 1000f,
                        center = Offset(0.5f, 0.3f)
                    )
                )
        )
        
        content()
    }
}

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    
    val gradientColors = if (isLight) {
        listOf(
            GradientPurpleStart.copy(alpha = 0.4f),
            GradientBlueStart.copy(alpha = 0.3f),
            GradientPinkStart.copy(alpha = 0.2f),
            BackgroundGradientEnd
        )
    } else {
        listOf(
            DarkGradientPurpleStart,
            Color(0xFF1A1A2E),
            Color(0xFF16213E),
            DarkBackgroundGradientEnd
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.sweepGradient(
                    colors = gradientColors,
                    center = Offset(0.5f, 0.5f)
                )
            )
    ) {
        content()
    }
}