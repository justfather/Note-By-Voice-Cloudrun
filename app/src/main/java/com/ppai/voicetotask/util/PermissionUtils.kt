package com.ppai.voicetotask.util

import android.Manifest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.*
import com.ppai.voicetotask.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    permission: String,
    onPermissionResult: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val permissionState = rememberPermissionState(permission) { isGranted ->
        onPermissionResult(isGranted)
    }
    
    var showRationaleDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(permissionState.status) {
        when {
            permissionState.status.isGranted -> {
                onPermissionResult(true)
            }
            permissionState.status.shouldShowRationale -> {
                showRationaleDialog = true
            }
        }
    }
    
    if (showRationaleDialog) {
        PermissionRationaleDialog(
            permission = permission,
            onConfirm = {
                showRationaleDialog = false
                permissionState.launchPermissionRequest()
            },
            onDismiss = {
                showRationaleDialog = false
                onPermissionResult(false)
            }
        )
    }
    
    content()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MultiplePermissionHandler(
    permissions: List<String>,
    onPermissionResult: (Map<String, Boolean>) -> Unit,
    content: @Composable () -> Unit
) {
    val permissionStates = rememberMultiplePermissionsState(permissions) { permissions ->
        onPermissionResult(permissions)
    }
    
    var showRationaleDialog by remember { mutableStateOf(false) }
    var rationalePermission by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(permissionStates.permissions) {
        val allGranted = permissionStates.permissions.all { it.status.isGranted }
        if (allGranted) {
            onPermissionResult(permissions.associateWith { true })
        } else {
            val shouldShowRationale = permissionStates.permissions.firstOrNull { 
                it.status.shouldShowRationale 
            }
            if (shouldShowRationale != null) {
                rationalePermission = shouldShowRationale.permission
                showRationaleDialog = true
            } else {
                // Launch permission request immediately if no rationale needed
                permissionStates.launchMultiplePermissionRequest()
            }
        }
    }
    
    if (showRationaleDialog && rationalePermission != null) {
        PermissionRationaleDialog(
            permission = rationalePermission!!,
            onConfirm = {
                showRationaleDialog = false
                permissionStates.launchMultiplePermissionRequest()
            },
            onDismiss = {
                showRationaleDialog = false
                onPermissionResult(permissionStates.permissions.associate { 
                    it.permission to it.status.isGranted 
                })
            }
        )
    }
    
    content()
}

@Composable
private fun PermissionRationaleDialog(
    permission: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message) = when (permission) {
        Manifest.permission.RECORD_AUDIO -> {
            "Microphone Permission Required" to stringResource(R.string.microphone_permission_rationale)
        }
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR -> {
            "Calendar Permission Required" to stringResource(R.string.calendar_permission_rationale)
        }
        Manifest.permission.POST_NOTIFICATIONS -> {
            "Notification Permission Required" to stringResource(R.string.notification_permission_rationale)
        }
        else -> {
            "Permission Required" to "This app needs this permission to function properly."
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
fun PermissionStatus.textToShow(): String {
    return when (this) {
        is PermissionStatus.Granted -> "Permission Granted"
        is PermissionStatus.Denied -> {
            if (shouldShowRationale) {
                "Permission denied. Please grant permission to use this feature."
            } else {
                "Permission denied. Please enable it in Settings to use this feature."
            }
        }
    }
}