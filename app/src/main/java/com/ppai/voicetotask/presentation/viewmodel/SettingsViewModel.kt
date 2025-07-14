package com.ppai.voicetotask.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.data.billing.BillingManager
import com.ppai.voicetotask.data.preferences.OutputLanguage
import com.ppai.voicetotask.data.preferences.ThemeMode
import com.ppai.voicetotask.data.preferences.UserPreferencesData
import com.ppai.voicetotask.domain.model.Subscription
import com.ppai.voicetotask.domain.repository.SettingsRepository
import com.ppai.voicetotask.domain.repository.SubscriptionRepository
import com.ppai.voicetotask.domain.usecase.DeleteAllNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deleteAllNotesUseCase: DeleteAllNotesUseCase,
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val billingManager: BillingManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    val subscription: StateFlow<Subscription?> = subscriptionRepository.getCurrentSubscription()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val userPreferences: StateFlow<UserPreferencesData> = settingsRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesData()
        )
    
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
    
    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoringPurchase = true)
            try {
                val restored = billingManager.restorePurchases()
                _uiState.value = _uiState.value.copy(
                    isRestoringPurchase = false,
                    restorePurchaseResult = restored
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRestoringPurchase = false,
                    restorePurchaseResult = false
                )
            }
        }
    }
    
    fun clearRestorePurchaseResult() {
        _uiState.value = _uiState.value.copy(restorePurchaseResult = null)
    }
    
    fun refreshSubscription() {
        // Subscription is automatically refreshed via Flow
    }
    
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(themeMode)
        }
    }
    
    fun updateDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDynamicColors(enabled)
        }
    }
    
    fun updateOutputLanguage(language: OutputLanguage) {
        viewModelScope.launch {
            settingsRepository.updateOutputLanguage(language)
        }
    }
}

data class SettingsUiState(
    val showDeleteConfirmDialog: Boolean = false,
    val deleteConfirmationText: String = "",
    val deleteConfirmationError: String? = null,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val isRestoringPurchase: Boolean = false,
    val restorePurchaseResult: Boolean? = null
)