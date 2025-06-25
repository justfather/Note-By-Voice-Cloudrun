package com.ppai.voicetotask.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.domain.model.Note
import com.ppai.voicetotask.domain.repository.NoteRepository
import com.ppai.voicetotask.domain.repository.TaskRepository
import com.ppai.voicetotask.domain.usecase.ProcessVoiceNoteUseCase
import com.ppai.voicetotask.domain.usecase.RecordAudioUseCase
import com.ppai.voicetotask.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    application: Application,
    private val recordAudioUseCase: RecordAudioUseCase,
    private val processVoiceNoteUseCase: ProcessVoiceNoteUseCase,
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()
    
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    
    fun startRecording(resumeFromPrevious: Boolean = false) {
        viewModelScope.launch {
            try {
                recordAudioUseCase.startRecording(resumeFromPrevious)
                _uiState.update { it.copy(isRecording = true, isPaused = false, error = null) }
                startTimer()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to start recording: ${e.message}")
                }
            }
        }
    }
    
    fun pauseRecording() {
        viewModelScope.launch {
            try {
                recordAudioUseCase.pauseRecording()
                timerJob?.cancel()
                _uiState.update { it.copy(isPaused = true) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to pause recording: ${e.message}")
                }
            }
        }
    }
    
    fun resumeRecording() {
        viewModelScope.launch {
            try {
                recordAudioUseCase.resumeRecording()
                _uiState.update { it.copy(isPaused = false, isResuming = true) }
                startTimer()
                // Clear resuming flag after a short delay
                delay(500)
                _uiState.update { it.copy(isResuming = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to resume recording: ${e.message}")
                }
            }
        }
    }
    
    fun retryRecording() {
        // Check if we have a previous recording to resume from
        val shouldResume = recordAudioUseCase.hasPreviousRecording()
        _uiState.update { it.copy(error = null, isResuming = shouldResume) }
        startRecording(resumeFromPrevious = shouldResume)
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            try {
                timerJob?.cancel()
                _uiState.update { it.copy(isRecording = false, isProcessing = true) }
                
                val audioFile = recordAudioUseCase.stopRecording()
                
                if (audioFile != null) {
                    processRecording(audioFile.absolutePath)
                } else {
                    _uiState.update { 
                        it.copy(
                            isProcessing = false,
                            error = "No audio recorded"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        error = "Failed to stop recording: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun cancelRecording() {
        recordingJob?.cancel()
        timerJob?.cancel()
        recordAudioUseCase.cancelRecording()
    }
    
    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRecording && !_uiState.value.isPaused) {
                delay(100)
                _uiState.update { currentState ->
                    currentState.copy(
                        recordingDuration = currentState.recordingDuration + 100
                    )
                }
            }
        }
    }
    
    private suspend fun processRecording(audioFilePath: String) {
        try {
            // Network check removed temporarily - permission was missing
            
            // Log the audio file details
            val audioFile = java.io.File(audioFilePath)
            if (!audioFile.exists()) {
                throw IllegalStateException("Audio file does not exist: $audioFilePath")
            }
            
            android.util.Log.d("RecordingViewModel", "Processing audio file: $audioFilePath")
            android.util.Log.d("RecordingViewModel", "Audio file size: ${audioFile.length()} bytes")
            
            // Process the voice note (transcribe, summarize, extract tasks)
            val processedNote = processVoiceNoteUseCase(audioFilePath)
            
            // Save note to database
            noteRepository.insertNote(processedNote)
            
            // Save tasks to database (they'll be automatically linked to the note)
            if (processedNote.tasks.isNotEmpty()) {
                val tasksWithNoteId = processedNote.tasks.map { task ->
                    task.copy(noteId = processedNote.id)
                }
                println("RecordingViewModel: Saving ${tasksWithNoteId.size} tasks to database")
                tasksWithNoteId.forEach { task ->
                    println("Saving task: ${task.title} with noteId: ${task.noteId}")
                }
                taskRepository.insertTasks(tasksWithNoteId)
            } else {
                println("RecordingViewModel: No tasks to save")
            }
            
            _uiState.update { 
                it.copy(
                    isProcessing = false,
                    isComplete = true,
                    savedNoteId = processedNote.id
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("RecordingViewModel", "Failed to process recording", e)
            
            val errorMessage = when {
                e.message?.contains("model not found", ignoreCase = true) == true || 
                e.message?.contains("404") == true -> 
                    "AI model unavailable. Please check your internet connection and try again."
                e.message?.contains("503") == true || e.message?.contains("UNAVAILABLE") == true -> 
                    "Service temporarily unavailable. Please try again in a moment."
                e.message?.contains("429") == true -> 
                    "Too many requests. Please wait a moment and try again."
                e.message?.contains("401") == true || e.message?.contains("403") == true ||
                e.message?.contains("API key", ignoreCase = true) == true -> 
                    "Authentication failed. Please check your API key configuration."
                e.message?.contains("transcribe", ignoreCase = true) == true || 
                e.message?.contains("transcription", ignoreCase = true) == true -> 
                    "Failed to transcribe audio. Please check your internet connection."
                e.message?.contains("Audio file does not exist") == true -> 
                    "Recording file not found. Please try recording again."
                e.message?.contains("Audio file is empty") == true -> 
                    "Recording is empty. Please try recording again."
                else -> 
                    "Failed to process recording. Please try again."
            }
            
            _uiState.update { 
                it.copy(
                    isProcessing = false,
                    error = errorMessage
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        cancelRecording()
    }
}

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val isProcessing: Boolean = false,
    val isComplete: Boolean = false,
    val recordingDuration: Long = 0L,
    val error: String? = null,
    val savedNoteId: String? = null,
    val isResuming: Boolean = false
)