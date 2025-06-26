package com.ppai.voicetotask.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.data.billing.BillingError
import com.ppai.voicetotask.data.billing.BillingManager
import com.ppai.voicetotask.domain.model.SubscriptionType
import com.ppai.voicetotask.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()
    
    init {
        observeBillingState()
        observeSubscription()
    }
    
    private fun observeBillingState() {
        viewModelScope.launch {
            billingManager.billingState.collect { billingState ->
                _uiState.update { currentState ->
                    currentState.copy(
                        error = when (billingState.error) {
                            is BillingError.NetworkError -> "No internet connection"
                            is BillingError.PurchaseCancelled -> null // Don't show error for cancellation
                            is BillingError.PurchasePending -> "Purchase is pending"
                            is BillingError.ProductNotAvailable -> "Product not available"
                            is BillingError.ProductsNotAvailable -> "Products not available"
                            is BillingError.ConnectionError -> "Failed to connect to Play Store"
                            is BillingError.Unknown -> billingState.error.message
                            null -> null
                        }
                    )
                }
            }
        }
    }
    
    private fun observeSubscription() {
        viewModelScope.launch {
            subscriptionRepository.getCurrentSubscription().collect { subscription ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isSubscribed = subscription.isPremium()
                    )
                }
            }
        }
    }
    
    fun subscribe(activity: Activity, subscriptionType: SubscriptionType) {
        _uiState.update { it.copy(isProcessing = true, error = null) }
        
        try {
            billingManager.launchBillingFlow(activity, subscriptionType)
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(
                    isProcessing = false,
                    error = "Failed to start subscription: ${e.message}"
                )
            }
        } finally {
            // Processing will be set to false when billing flow completes
            viewModelScope.launch {
                billingManager.billingState.collect { state ->
                    if (state.error != null || !state.isConnected) {
                        _uiState.update { it.copy(isProcessing = false) }
                    }
                }
            }
        }
    }
    
    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            
            try {
                val restored = billingManager.restorePurchases()
                if (restored) {
                    _uiState.update { 
                        it.copy(
                            isProcessing = false,
                            showRestoreSuccess = true
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isProcessing = false,
                            error = "No purchases found to restore"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isProcessing = false,
                        error = "Failed to restore purchases: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class PaywallUiState(
    val isProcessing: Boolean = false,
    val isSubscribed: Boolean = false,
    val error: String? = null,
    val showRestoreSuccess: Boolean = false
)