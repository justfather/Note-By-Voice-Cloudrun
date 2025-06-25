package com.ppai.voicetotask.presentation.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordingFAB(
    onNavigateToRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var shouldNavigateAfterPermission by remember { mutableStateOf(false) }
    
    FloatingActionButton(
        onClick = {
            when {
                audioPermissionState.status.isGranted -> {
                    // Permission already granted, navigate to recording
                    android.util.Log.d("RecordingFAB", "Permission granted, navigating to recording")
                    onNavigateToRecording()
                }
                audioPermissionState.status.shouldShowRationale -> {
                    // Show rationale
                    Toast.makeText(
                        context,
                        "Microphone permission is required to record voice notes",
                        Toast.LENGTH_LONG
                    ).show()
                    shouldNavigateAfterPermission = true
                    audioPermissionState.launchPermissionRequest()
                }
                else -> {
                    // Request permission
                    android.util.Log.d("RecordingFAB", "Requesting audio permission")
                    shouldNavigateAfterPermission = true
                    audioPermissionState.launchPermissionRequest()
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 4.dp
        ),
        modifier = modifier
    ) {
        Icon(Icons.Default.Mic, contentDescription = "Record")
    }
    
    // Handle permission result only when explicitly requested
    LaunchedEffect(audioPermissionState.status.isGranted, shouldNavigateAfterPermission) {
        if (audioPermissionState.status.isGranted && shouldNavigateAfterPermission) {
            android.util.Log.d("RecordingFAB", "Permission granted after request, navigating to recording")
            shouldNavigateAfterPermission = false
            onNavigateToRecording()
        }
    }
}