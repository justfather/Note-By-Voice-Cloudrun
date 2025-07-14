package com.ppai.voicetotask.presentation.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.ppai.voicetotask.presentation.ui.paywall.PaywallScreen
import com.ppai.voicetotask.data.preferences.ThemeMode
import com.ppai.voicetotask.data.preferences.OutputLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
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
                    title = "Theme",
                    subtitle = when(userPreferences.themeMode) {
                        ThemeMode.SYSTEM -> "System default"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    onClick = { showThemeModeDialog = true }
                )
            }
            
            HorizontalDivider()
            
            // Language Settings
            SettingsSection(title = "Language") {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Output Language",
                    subtitle = userPreferences.outputLanguage.displayName,
                    onClick = { showLanguageDialog = true }
                )
            }
            
            HorizontalDivider()
            
            // Subscription Section
            SettingsSection(title = "Subscription") {
                SettingsItem(
                    icon = Icons.Default.WorkspacePremium,
                    title = if (subscription?.isPremium() == true) "Premium" else "Free",
                    subtitle = when {
                        subscription?.isPremium() == true -> "Unlimited recordings • No ads"
                        else -> "${subscription?.getRemainingRecordings() ?: 30} recordings left this month"
                    },
                    onClick = { 
                        if (subscription?.isPremium() != true) {
                            showPaywall = true
                        }
                    },
                    trailing = {
                        if (subscription?.isPremium() != true) {
                            TextButton(onClick = { showPaywall = true }) {
                                Text("Upgrade")
                            }
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Premium active",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Restore Purchase",
                    subtitle = "Restore previous purchases",
                    onClick = { viewModel.restorePurchases() },
                    trailing = {
                        if (uiState.isRestoringPurchase) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }
            
            HorizontalDivider()
            
            // Data Management
            SettingsSection(title = "Data Management") {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Delete All Notes",
                    subtitle = "Permanently delete all notes and tasks",
                    onClick = { viewModel.onDeleteAllNotesClick() }
                )
            }
            
            HorizontalDivider()
            
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
    
    // Show paywall
    if (showPaywall) {
        PaywallScreen(
            onDismiss = { showPaywall = false },
            onSubscribed = { 
                showPaywall = false
                viewModel.refreshSubscription()
            }
        )
    }
    
    // Restore purchase result
    uiState.restorePurchaseResult?.let { success ->
        LaunchedEffect(success) {
            viewModel.clearRestorePurchaseResult()
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearRestorePurchaseResult() }) {
                    Text("OK")
                }
            }
        ) {
            Text(if (success) "Purchase restored successfully!" else "No purchases found to restore")
        }
    }
    
    // Theme mode selection dialog
    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentThemeMode = userPreferences.themeMode,
            onThemeModeSelected = { themeMode ->
                viewModel.updateThemeMode(themeMode)
                showThemeModeDialog = false
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }
    
    // Language selection dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = userPreferences.outputLanguage,
            onLanguageSelected = { language ->
                viewModel.updateOutputLanguage(language)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeModeDialog(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose theme",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                ThemeMode.values().forEach { themeMode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeModeSelected(themeMode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentThemeMode == themeMode,
                            onClick = { onThemeModeSelected(themeMode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when(themeMode) {
                                ThemeMode.SYSTEM -> "System default"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionDialog(
    currentLanguage: OutputLanguage,
    onLanguageSelected: (OutputLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose output language",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                OutputLanguage.values().forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onLanguageSelected(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}