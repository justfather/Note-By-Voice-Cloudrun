package com.ppai.voicetotask.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import com.ppai.voicetotask.presentation.theme.*

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glassTint: Color = GlassWhite90,
    borderRadius: Dp = 20.dp,
    blurRadius: Dp = 0.dp,
    elevation: Dp = 8.dp,
    contentPadding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed) elevation / 2 else elevation,
        animationSpec = tween(150),
        label = "shadow"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(borderRadius),
                clip = false,
                ambientColor = ShadowLight,
                spotColor = ShadowLight
            )
            .clip(RoundedCornerShape(borderRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glassTint,
                        glassTint.copy(alpha = glassTint.alpha * 0.8f)
                    )
                )
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        // Glass overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        )
        
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    glassTint: Color = GlassWhite70,
    borderRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(borderRadius))
            .background(glassTint)
    ) {
        // Noise texture overlay for glass effect
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )
        
        content()
    }
}

@Composable
fun FloatingGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    gradient: List<Color> = listOf(GradientPurpleStart.copy(alpha = 0.3f), GradientPurpleEnd.copy(alpha = 0.2f)),
    borderRadius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offsetY"
    )
    
    Box(
        modifier = modifier
            .offset(y = offsetY)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(borderRadius),
                ambientColor = gradient[0].copy(alpha = 0.2f),
                spotColor = gradient[0].copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(borderRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = gradient,
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        // Glass overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(GlassWhite20)
        )
        
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}