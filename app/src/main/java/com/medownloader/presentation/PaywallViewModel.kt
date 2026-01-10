package com.medownloader.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medownloader.data.repository.PremiumRepository
import com.medownloader.data.repository.PurchaseCancelledException
import com.medownloader.data.repository.RevenueCatException
import com.medownloader.di.ServiceLocator
import com.medownloader.presentation.screen.PaywallTriggerReason
import com.revenuecat.purchases.Package
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Paywall screen.
 * 
 * Lifetime-Only Model ($3.99):
 * - Single purchase option (no subscription confusion)
 * - Simple UI: "Buy Once, Own Forever"
 * - Restore for users who reinstall
 * 
 * Why no subscriptions?
 * - Download managers are burst-usage tools
 * - Users download 10 movies, then nothing for weeks
 * - Subscriptions cause immediate churn
 * - Lifetime = one transaction, user feels ownership, we keep the money
 */
class PaywallViewModel(
    private val premiumRepository: PremiumRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<PaywallEvent>()
    val events: SharedFlow<PaywallEvent> = _events.asSharedFlow()
    
    init {
        loadLifetimePackage()
    }
    
    fun loadLifetimePackage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            premiumRepository.getLifetimePackage()
                .onSuccess { pkg ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            lifetimePackage = pkg,
                            priceFormatted = pkg.product.price.formatted
                        )
                    }
                }
                .onFailure { error ->
                    val message = when (error) {
                        is RevenueCatException -> {
                            if (error.isNetworkError) "network error. please check your connection."
                            else "failed to load pricing: ${error.message}"
                        }
                        else -> "failed to load pricing"
                    }
                    _uiState.update { 
                        it.copy(isLoading = false, error = message) 
                    }
                }
        }
    }
    
    fun purchaseLifetime(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, error = null) }
            
            premiumRepository.purchaseLifetime(activity)
                .onSuccess { result ->
                    _uiState.update { it.copy(isPurchasing = false) }
                    
                    if (result.isPremiumNow) {
                        _events.emit(PaywallEvent.PurchaseSuccess)
                    } else {
                        _uiState.update { 
                            it.copy(error = "purchase completed. please restart if pro isn't unlocked.")
                        }
                    }
                }
                .onFailure { error ->
                    val message = when (error) {
                        is PurchaseCancelledException -> null // silent on cancel
                        is RevenueCatException -> {
                            when {
                                error.isNetworkError -> "network error. please try again."
                                error.isStoreProblem -> "google play error. please try again later."
                                error.isReceiptAlreadyInUse -> "already purchased on another account. try restore."
                                else -> "purchase failed: ${error.error.message}"
                            }
                        }
                        else -> "purchase failed. please try again."
                    }
                    _uiState.update { it.copy(isPurchasing = false, error = message) }
                }
        }
    }
    
    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, error = null) }
            
            premiumRepository.restorePurchases()
                .onSuccess { result ->
                    _uiState.update { it.copy(isRestoring = false) }
                    
                    if (result.isPremiumNow) {
                        _events.emit(PaywallEvent.RestoreSuccess)
                    } else {
                        _uiState.update { 
                            it.copy(error = "no previous purchases found for this account.")
                        }
                    }
                }
                .onFailure { error ->
                    val message = when (error) {
                        is RevenueCatException -> {
                            if (error.isNetworkError) "network error. please check your connection."
                            else "restore failed: ${error.error.message}"
                        }
                        else -> "restore failed. please try again."
                    }
                    _uiState.update { it.copy(isRestoring = false, error = message) }
                }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun setTriggerReason(reason: PaywallTriggerReason) {
        _uiState.update { it.copy(triggerReason = reason) }
    }
    
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PaywallViewModel(
                    premiumRepository = ServiceLocator.providePremiumRepository()
                ) as T
            }
        }
    }
}

/**
 * Paywall UI State - Lifetime-only model
 */
data class PaywallUiState(
    val isLoading: Boolean = true,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val lifetimePackage: Package? = null,
    val priceFormatted: String = "$3.99",  // Fallback
    val error: String? = null,
    val triggerReason: PaywallTriggerReason = PaywallTriggerReason.SETTINGS_UPGRADE
)

sealed class PaywallEvent {
    data object PurchaseSuccess : PaywallEvent()
    data object RestoreSuccess : PaywallEvent()
}
