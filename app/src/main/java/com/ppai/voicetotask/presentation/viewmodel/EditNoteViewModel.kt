package com.ppai.voicetotask.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.domain.model.Note
import com.ppai.voicetotask.domain.model.Priority
import com.ppai.voicetotask.domain.model.Task
import com.ppai.voicetotask.domain.repository.NoteRepository
import com.ppai.voicetotask.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val noteId: String = savedStateHandle.get<String>("noteId") ?: ""
    private var originalNote: Note? = null
    
    private val _uiState = MutableStateFlow(EditNoteUiState())
    val uiState: StateFlow<EditNoteUiState> = _uiState.asStateFlow()
    
    fun loadNote(noteId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val note = noteRepository.getNoteWithTasks(noteId)
                if (note != null) {
                    originalNote = note
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = note.title,
                            summary = note.summary,
                            transcript = note.transcript,
                            tasks = note.tasks,
                            hasChanges = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Note not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load note: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun updateTitle(title: String) {
        _uiState.update {
            it.copy(title = title, hasChanges = true)
        }
    }
    
    fun updateSummary(summary: String) {
        _uiState.update {
            it.copy(summary = summary, hasChanges = true)
        }
    }
    
    fun updateTranscript(transcript: String) {
        _uiState.update {
            it.copy(transcript = transcript, hasChanges = true)
        }
    }
    
    fun updateTask(index: Int, task: Task) {
        _uiState.update { state ->
            val updatedTasks = state.tasks.toMutableList()
            if (index in updatedTasks.indices) {
                updatedTasks[index] = task
            }
            state.copy(tasks = updatedTasks, hasChanges = true)
        }
    }
    
    fun addTask() {
        _uiState.update { state ->
            val newTask = Task(
                id = UUID.randomUUID().toString(),
                title = "",
                priority = Priority.MEDIUM,
                noteId = noteId
            )
            state.copy(
                tasks = state.tasks + newTask,
                hasChanges = true
            )
        }
    }
    
    fun removeTask(index: Int) {
        _uiState.update { state ->
            val updatedTasks = state.tasks.toMutableList()
            if (index in updatedTasks.indices) {
                updatedTasks.removeAt(index)
            }
            state.copy(tasks = updatedTasks, hasChanges = true)
        }
    }
    
    fun saveNote() {
        val currentState = _uiState.value
        if (!currentState.hasChanges || originalNote == null) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            try {
                // Update the note
                val updatedNote = originalNote!!.copy(
                    title = currentState.title,
                    summary = currentState.summary,
                    transcript = currentState.transcript
                )
                noteRepository.updateNote(updatedNote)
                
                // Delete all existing tasks for this note
                val existingTasks = originalNote!!.tasks
                existingTasks.forEach { task ->
                    taskRepository.deleteTask(task)
                }
                
                // Insert updated tasks (only non-empty ones)
                val validTasks = currentState.tasks.filter { it.title.isNotBlank() }
                if (validTasks.isNotEmpty()) {
                    taskRepository.insertTasks(validTasks.map { it.copy(noteId = noteId) })
                }
                
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSaved = true,
                        hasChanges = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Failed to save changes: ${e.message}"
                    )
                }
            }
        }
    }
}

data class EditNoteUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val title: String = "",
    val summary: String = "",
    val transcript: String = "",
    val tasks: List<Task> = emptyList(),
    val hasChanges: Boolean = false,
    val error: String? = null
)