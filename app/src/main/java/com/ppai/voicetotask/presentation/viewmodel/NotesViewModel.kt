package com.ppai.voicetotask.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.domain.model.Note
import com.ppai.voicetotask.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()
    
    init {
        observeNotes()
    }
    
    @OptIn(FlowPreview::class)
    private fun observeNotes() {
        viewModelScope.launch {
            try {
                _searchQuery
                    .debounce(300) // Debounce to avoid excessive queries
                    .distinctUntilChanged()
                    .flatMapLatest { query ->
                        if (query.isEmpty()) {
                            noteRepository.getAllNotesWithTasks()
                        } else {
                            noteRepository.searchNotes(query)
                        }
                    }
                    .collect { notes ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                notes = notes,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Failed to load notes: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNote(noteId)
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
    
    fun archiveNote(noteId: String) {
        viewModelScope.launch {
            try {
                noteRepository.getNoteById(noteId)?.let { note ->
                    val updatedNote = note.copy(archived = true)
                    noteRepository.updateNote(updatedNote)
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }
}

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)