package com.ppai.voicetotask.presentation.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ppai.voicetotask.presentation.ui.components.TaskItem
import com.ppai.voicetotask.presentation.ui.components.SwipeableTaskItem
import com.ppai.voicetotask.presentation.viewmodel.NoteDetailViewModel
import com.ppai.voicetotask.presentation.ui.components.swipeGesture
import com.ppai.voicetotask.presentation.ui.components.FocusModeContainer
import com.ppai.voicetotask.presentation.ui.components.ProgressiveBlurOverlay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit = {},
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isTranscriptExpanded by remember { mutableStateOf(false) }
    var focusMode by remember { mutableStateOf(false) }
    
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }
    
    Scaffold(
        modifier = Modifier.swipeGesture(
            onSwipeRight = onBackClick
        ),
        topBar = {
            TopAppBar(
                title = { Text("Note Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEditClick(noteId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.note != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.note?.let { note ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = note.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = formatDateTime(note.createdAt),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (note.duration > 0) {
                                    Text(
                                        text = "Duration: ${formatDuration(note.duration)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    if (note.summary.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "AI Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    // Display summary as formatted bullet points
                                    val summaryLines = note.summary.lines()
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        summaryLines.forEach { line ->
                                            val trimmedLine = line.trim()
                                            if (trimmedLine.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Start
                                                ) {
                                                    // Check if line starts with bullet point
                                                    if (trimmedLine.startsWith("-") || trimmedLine.startsWith("•") || trimmedLine.startsWith("*")) {
                                                        Text(
                                                            text = "•",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.padding(end = 8.dp)
                                                        )
                                                        Text(
                                                            text = trimmedLine.removePrefix("-").removePrefix("•").removePrefix("*").trim(),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = trimmedLine,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { isTranscriptExpanded = !isTranscriptExpanded }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Transcript",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Icon(
                                        imageVector = if (isTranscriptExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isTranscriptExpanded) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                if (isTranscriptExpanded) {
                                    Text(
                                        text = note.transcript,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    
                        if (note.tasks.isNotEmpty()) {
                        item {
                            Text(
                                text = "Generated Tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        items(note.tasks) { task ->
                            SwipeableTaskItem(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskComplete(task.id) },
                                onDelete = { viewModel.deleteTask(task.id) },
                                onAddToCalendar = { /* Calendar integration will be handled by TaskItem */ }
                            )
                        }
                    }
                    }
                }
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote()
                        onBackClick()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatDateTime(date: Date): String {
    return SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(date)
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
}