package com.ppai.voicetotask.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.domain.model.Note
import com.ppai.voicetotask.domain.repository.NoteRepository
import com.ppai.voicetotask.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()
    
    fun loadNote(noteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val note = noteRepository.getNoteWithTasks(noteId)
                _uiState.update { currentState ->
                    currentState.copy(
                        note = note,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun toggleTaskComplete(taskId: String) {
        viewModelScope.launch {
            _uiState.value.note?.let { note ->
                val task = note.tasks.find { it.id == taskId } ?: return@launch
                taskRepository.updateTaskCompletion(
                    taskId = taskId,
                    completed = !task.completed,
                    updatedAt = Date()
                )
                // Reload note to get updated tasks
                loadNote(note.id)
            }
        }
    }
    
    fun deleteNote() {
        viewModelScope.launch {
            _uiState.value.note?.let { note ->
                noteRepository.deleteNote(note.id)
            }
        }
    }
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _uiState.value.note?.let { note ->
                val task = note.tasks.find { it.id == taskId } ?: return@launch
                taskRepository.deleteTask(task)
                // Reload note to get updated tasks
                loadNote(note.id)
            }
        }
    }
}

data class NoteDetailUiState(
    val note: Note? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)