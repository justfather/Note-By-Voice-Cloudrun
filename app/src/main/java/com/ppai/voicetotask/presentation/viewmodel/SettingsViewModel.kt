package com.ppai.voicetotask.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.domain.usecase.DeleteAllNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deleteAllNotesUseCase: DeleteAllNotesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    fun onDeleteAllNotesClick() {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true)
    }
    
    fun onDismissDeleteConfirmDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmDialog = false,
            deleteConfirmationText = "",
            deleteConfirmationError = null
        )
    }
    
    fun onDeleteConfirmationTextChange(text: String) {
        _uiState.value = _uiState.value.copy(
            deleteConfirmationText = text,
            deleteConfirmationError = null
        )
    }
    
    fun onConfirmDeleteAllNotes() {
        if (_uiState.value.deleteConfirmationText == "DELETE ALL") {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isDeleting = true)
                try {
                    deleteAllNotesUseCase()
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        showDeleteConfirmDialog = false,
                        deleteConfirmationText = "",
                        deleteSuccess = true
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        deleteConfirmationError = "Failed to delete notes: ${e.message}"
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                deleteConfirmationError = "Please type DELETE ALL to confirm"
            )
        }
    }
    
    fun clearDeleteSuccess() {
        _uiState.value = _uiState.value.copy(deleteSuccess = false)
    }
}

data class SettingsUiState(
    val showDeleteConfirmDialog: Boolean = false,
    val deleteConfirmationText: String = "",
    val deleteConfirmationError: String? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false
)