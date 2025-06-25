package com.ppai.voicetotask.presentation.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ppai.voicetotask.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var darkThemeEnabled by remember { mutableStateOf(false) }
    var dynamicColorEnabled by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Theme Settings
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Theme",
                    subtitle = "Use dark theme",
                    trailing = {
                        Switch(
                            checked = darkThemeEnabled,
                            onCheckedChange = { darkThemeEnabled = it }
                        )
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Dynamic Colors",
                    subtitle = "Use Material You colors",
                    trailing = {
                        Switch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = { dynamicColorEnabled = it }
                        )
                    }
                )
            }
            
            Divider()
            
            // AI Settings
            SettingsSection(title = "AI Configuration") {
                SettingsItem(
                    icon = Icons.Default.Psychology,
                    title = "AI Provider",
                    subtitle = "OpenAI GPT-4",
                    onClick = { /* TODO: Show AI provider dialog */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.Key,
                    title = "API Key",
                    subtitle = "Configure API key",
                    onClick = { /* TODO: Show API key dialog */ }
                )
            }
            
            Divider()
            
            // Data Management
            SettingsSection(title = "Data Management") {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Delete All Notes",
                    subtitle = "Permanently delete all notes and tasks",
                    onClick = { viewModel.onDeleteAllNotesClick() }
                )
            }
            
            Divider()
            
            // About
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0"
                )
                
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Open Source Licenses",
                    subtitle = "View licenses",
                    onClick = { /* TODO: Show licenses */ }
                )
                
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "View privacy policy",
                    onClick = { 
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(context.getString(com.ppai.voicetotask.R.string.privacy_policy_url))
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (uiState.showDeleteConfirmDialog) {
        DeleteAllNotesConfirmationDialog(
            confirmationText = uiState.deleteConfirmationText,
            onConfirmationTextChange = viewModel::onDeleteConfirmationTextChange,
            onConfirm = viewModel::onConfirmDeleteAllNotes,
            onDismiss = viewModel::onDismissDeleteConfirmDialog,
            isDeleting = uiState.isDeleting,
            error = uiState.deleteConfirmationError
        )
    }
    
    // Success snackbar
    if (uiState.deleteSuccess) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearDeleteSuccess()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearDeleteSuccess() }) {
                    Text("OK")
                }
            }
        ) {
            Text("All notes and tasks have been deleted")
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = trailing,
        modifier = if (onClick != null) {
            Modifier.clickable { onClick() }
        } else {
            Modifier
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAllNotesConfirmationDialog(
    confirmationText: String,
    onConfirmationTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDeleting: Boolean,
    error: String?
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = {
            Text(
                text = "Delete All Notes",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "This action cannot be undone. All your notes and associated tasks will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "To confirm, type DELETE ALL below:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = onConfirmationTextChange,
                    label = { Text("Confirmation") },
                    placeholder = { Text("DELETE ALL") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeleting
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete All")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("Cancel")
            }
        }
    )
}