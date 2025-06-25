package com.ppai.voicetotask.presentation.ui.recording

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ppai.voicetotask.presentation.viewmodel.RecordingViewModel
import com.ppai.voicetotask.util.PermissionHandler
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.ppai.voicetotask.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalRecordingScreen(
    onRecordingComplete: (String?) -> Unit,
    onCancel: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var hasPermission by remember { mutableStateOf(false) }
    
    PermissionHandler(
        permission = Manifest.permission.RECORD_AUDIO,
        onPermissionResult = { isGranted ->
            hasPermission = isGranted
            if (!isGranted) {
                // Permission denied, go back
                onCancel()
            }
        }
    ) {
        // Main content below
    }
    
    LaunchedEffect(uiState.isComplete, uiState.savedNoteId) {
        if (uiState.isComplete && uiState.savedNoteId != null) {
            delay(100)
            onRecordingComplete(uiState.savedNoteId)
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Close button at top
            IconButton(
                onClick = {
                    viewModel.cancelRecording()
                    onCancel()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            when {
                uiState.isProcessing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Processing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(48.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Timer display - ultra minimal
                        Text(
                            text = formatMinimalTime(uiState.recordingDuration),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = 72.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Minimal record button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(120.dp)
                        ) {
                            if (uiState.isRecording) {
                                MinimalPulse()
                            }
                            
                            Surface(
                                onClick = {
                                    if (uiState.isRecording) {
                                        viewModel.stopRecording()
                                    } else {
                                        viewModel.startRecording()
                                    }
                                },
                                shape = CircleShape,
                                color = if (uiState.isRecording) 
                                    MaterialTheme.colorScheme.onSurface 
                                else 
                                    MaterialTheme.colorScheme.surface,
                                border = if (!uiState.isRecording) 
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline) 
                                else null,
                                modifier = Modifier.size(80.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isRecording) {
                                        // Stop icon - just a square
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Minimal status text
                        Text(
                            text = if (uiState.isRecording) "tap to stop" else "tap to record",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalPulse() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .alpha(alpha)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = CircleShape
            )
    )
}

private fun formatMinimalTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}