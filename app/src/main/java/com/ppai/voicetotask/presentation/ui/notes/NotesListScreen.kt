package com.ppai.voicetotask.presentation.ui.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ppai.voicetotask.presentation.ui.components.EmptyState
import com.ppai.voicetotask.presentation.ui.components.NoteCard
import com.ppai.voicetotask.presentation.ui.components.SearchBar
import com.ppai.voicetotask.presentation.ui.components.SwipeableNoteItem
import com.ppai.voicetotask.presentation.ui.components.DismissDirection
import com.ppai.voicetotask.presentation.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onNoteClick: (String) -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var selectionMode by remember { mutableStateOf(false) }
    val selectedNotes = remember { mutableStateListOf<String>() }
    val haptics = LocalHapticFeedback.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (selectionMode) {
                        Text("${selectedNotes.size} selected")
                    } else {
                        Text("Voice Notes")
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedNotes.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection mode")
                        }
                    }
                },
                actions = {
                    AnimatedVisibility(visible = selectionMode && selectedNotes.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedNotes.forEach { id ->
                                    viewModel.deleteNote(id)
                                }
                                selectedNotes.clear()
                                selectionMode = false
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search notes..."
            )
            
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.notes.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Mic,
                        title = "No notes yet",
                        description = "Tap the record button to create your first voice note"
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = uiState.notes.filter { !it.archived },
                            key = { it.id }
                        ) { note ->
                            SwipeableNoteItem(
                                onDismissed = { direction ->
                                    when (direction) {
                                        DismissDirection.StartToEnd -> viewModel.archiveNote(note.id)
                                        DismissDirection.EndToStart -> viewModel.deleteNote(note.id)
                                    }
                                }
                            ) {
                                NoteCard(
                                    note = note,
                                    onClick = {
                                        if (selectionMode) {
                                            if (selectedNotes.contains(note.id)) {
                                                selectedNotes.remove(note.id)
                                            } else {
                                                selectedNotes.add(note.id)
                                            }
                                        } else {
                                            onNoteClick(note.id)
                                        }
                                    },
                                    selected = selectedNotes.contains(note.id),
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    if (!selectionMode) {
                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        selectionMode = true
                                                        selectedNotes.add(note.id)
                                                    }
                                                }
                                            )
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}