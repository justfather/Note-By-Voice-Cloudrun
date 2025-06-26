package com.ppai.voicetotask.presentation.ui.recording

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ppai.voicetotask.data.ads.AdManager
import com.ppai.voicetotask.presentation.theme.RecordingRed
import com.ppai.voicetotask.presentation.theme.RecordingPulse
import com.ppai.voicetotask.presentation.ui.paywall.PaywallScreen
import com.ppai.voicetotask.presentation.viewmodel.RecordingViewModel
import com.ppai.voicetotask.util.PermissionHandler
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import android.util.Log
import androidx.compose.ui.res.stringResource
import com.ppai.voicetotask.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onRecordingComplete: (String?) -> Unit,
    onCancel: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var hasPermission by remember { mutableStateOf(false) }
    val adManager = remember { viewModel.getAdManager() }
    
    PermissionHandler(
        permission = Manifest.permission.RECORD_AUDIO,
        onPermissionResult = { isGranted ->
            android.util.Log.d("RecordingScreen", "Permission result: $isGranted")
            hasPermission = isGranted
            if (isGranted) {
                // Start recording immediately when permission is granted
                android.util.Log.d("RecordingScreen", "Starting recording after permission grant")
                viewModel.startRecording()
            } else {
                // Permission denied, go back
                android.util.Log.w("RecordingScreen", "Recording permission denied")
                // Permission denied, show log message
                onCancel()
            }
        }
    ) {
        // Main content
    }
    
    LaunchedEffect(uiState.isComplete, uiState.savedNoteId) {
        Log.d("RecordingScreen", "LaunchedEffect triggered - isComplete: ${uiState.isComplete}, savedNoteId: ${uiState.savedNoteId}")
        if (uiState.isComplete && uiState.savedNoteId != null) {
            Log.d("RecordingScreen", "Recording complete, navigating to note detail with ID: ${uiState.savedNoteId}")
            // Add a small delay to ensure the note is saved to database
            delay(100)
            onRecordingComplete(uiState.savedNoteId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelRecording()
                        onCancel()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.showingAd -> {
                    // Show ad for free users
                    LaunchedEffect(Unit) {
                        activity?.let { act ->
                            adManager.showInterstitialAd(
                                activity = act,
                                onAdDismissed = {
                                    viewModel.onAdDismissed()
                                }
                            )
                        } ?: viewModel.onAdDismissed() // Skip ad if no activity
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Please wait...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                uiState.isProcessing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Processing your recording...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        // Show saved note ID if available (for debugging)
                        if (uiState.savedNoteId != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Note saved! Navigating...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        // Timer display
                        Text(
                            text = formatTime(uiState.recordingDuration),
                            style = MaterialTheme.typography.displayMedium,
                            textAlign = TextAlign.Center
                        )
                        
                        // Recording indicator with pulse animation
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isRecording && !uiState.isPaused) {
                                PulsingCircle()
                            }
                            
                            // Record/Stop button
                            FilledIconButton(
                                onClick = {
                                    android.util.Log.d("RecordingScreen", "Stop/Start button clicked - isRecording: ${uiState.isRecording}, isProcessing: ${uiState.isProcessing}")
                                    if (uiState.isRecording) {
                                        android.util.Log.d("RecordingScreen", "Stopping recording")
                                        viewModel.stopRecording()
                                    } else {
                                        android.util.Log.d("RecordingScreen", "Starting recording")
                                        viewModel.startRecording()
                                    }
                                },
                                enabled = !uiState.isProcessing,
                                modifier = Modifier.size(80.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (uiState.isRecording) RecordingRed else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (uiState.isRecording) "Stop" else "Record",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        
                        // Pause/Resume button (only shown when recording)
                        if (uiState.isRecording) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedIconButton(
                                    onClick = {
                                        if (uiState.isPaused) {
                                            viewModel.resumeRecording()
                                        } else {
                                            viewModel.pauseRecording()
                                        }
                                    },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        
                        // Instructions
                        Text(
                            text = when {
                                uiState.isRecording && uiState.isPaused -> "Recording paused. Tap resume to continue."
                                uiState.isRecording && uiState.isResuming -> "Resuming previous recording..."
                                uiState.isRecording -> "Recording... Tap stop to finish or pause to take a break."
                                else -> "Tap to start recording"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        // Show resuming indicator
                        if (uiState.isResuming && !uiState.isRecording) {
                            Text(
                                text = "Previous recording will be resumed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Error message
                        uiState.error?.let { error ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Button(
                                        onClick = { viewModel.retryRecording() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Resume Recording")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Show paywall when limits are reached
    if (uiState.showPaywall) {
        PaywallScreen(
            onDismiss = { /* User can't dismiss when limit reached */ },
            onSubscribed = { 
                // User upgraded, they can now record
                onCancel() // Go back to previous screen
            }
        )
    }
    
    // Show duration limit warning
    if (uiState.maxDurationReached) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Recording Limit Reached") },
            text = { 
                Text("You've reached the maximum recording duration for free users (2 minutes). Upgrade to Premium for up to 10-minute recordings!")
            },
            confirmButton = {
                TextButton(onClick = onCancel) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun PulsingCircle() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
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
            .background(
                color = RecordingPulse.copy(alpha = alpha),
                shape = CircleShape
            )
    )
}

private fun formatTime(milliseconds: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return String.format("%02d:%02d", minutes, seconds)
}