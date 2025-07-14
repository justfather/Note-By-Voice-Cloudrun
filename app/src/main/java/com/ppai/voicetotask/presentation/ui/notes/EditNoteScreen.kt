package com.ppai.voicetotask.presentation.ui.notes

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ppai.voicetotask.presentation.viewmodel.EditNoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
    noteId: String,
    onBackClick: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: EditNoteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaveComplete()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Note") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveNote() },
                        enabled = !uiState.isSaving && uiState.hasChanges
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error ?: "An error occurred",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadNote(noteId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = { viewModel.updateTitle(it) },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )
                    
                    // Summary
                    OutlinedTextField(
                        value = uiState.summary,
                        onValueChange = { viewModel.updateSummary(it) },
                        label = { Text("AI Summary") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        supportingText = {
                            Text("Edit the AI-generated summary or add your own notes")
                        }
                    )
                    
                    // Transcript
                    OutlinedTextField(
                        value = uiState.transcript,
                        onValueChange = { viewModel.updateTranscript(it) },
                        label = { Text("Transcript") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        supportingText = {
                            Text("Original voice transcript")
                        }
                    )
                    
                    // Tasks Section
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Generated Tasks",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                IconButton(
                                    onClick = { viewModel.addTask() }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                                }
                            }
                            
                            if (uiState.tasks.isEmpty()) {
                                Text(
                                    text = "No tasks. Tap + to add one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                uiState.tasks.forEachIndexed { index, task ->
                                    OutlinedTextField(
                                        value = task.title,
                                        onValueChange = { newTitle ->
                                            viewModel.updateTask(index, task.copy(title = newTitle))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { viewModel.removeTask(index) }
                                            ) {
                                                Icon(
                                                    Icons.Default.Clear,
                                                    contentDescription = "Remove",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        },
                                        placeholder = { Text("Task ${index + 1}") }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Add some bottom padding for better scrolling
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}