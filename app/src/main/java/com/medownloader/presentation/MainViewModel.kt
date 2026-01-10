package com.medownloader.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medownloader.data.model.Download
import com.medownloader.data.model.Aria2GlobalStat
import com.medownloader.data.repository.DownloadRepository
import com.medownloader.data.repository.FileInfo
import com.medownloader.data.repository.PremiumRepository
import com.medownloader.data.repository.SettingsRepository
import com.medownloader.di.ServiceLocator
import com.medownloader.presentation.screen.PaywallTriggerReason
import com.medownloader.ui.theme.AppTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val downloadRepository: DownloadRepository,
    private val premiumRepository: PremiumRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ========================================================================
    // UI State
    // ========================================================================
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()
    
    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium
    
    // settings flows
    val appTheme = settingsRepository.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.DEFAULT)
        
    val wifiOnly = settingsRepository.wifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val maxConcurrent = settingsRepository.maxConcurrentDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)
        
    val connectionLimit = settingsRepository.connectionLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)
        
    val downloadDirUri = settingsRepository.downloadDirUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        observeDownloads()
        checkPremiumOnStart()
    }

    // ========================================================================
    // Public Actions
    // ========================================================================
    
    fun addDownload(url: String, filename: String? = null) {
        // youtube blocked by google play rules
        if (!downloadRepository.isUrlAllowed(url)) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowError("downloads from youtube are not allowed by google play policy"))
            }
            return
        }
        
        // torrent/magnet requires premium
        if (isTorrentOrMagnet(url) && !premiumRepository.isTorrentEnabled()) {
            requestPaywall(PaywallTriggerReason.TORRENT_BLOCKED)
            return
        }
        
        // check concurrent downloads
        val activeCount = _uiState.value.activeDownloads.size
        val configuredMax = maxConcurrent.value
        
        if (activeCount >= configuredMax) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowError("maximum $configuredMax concurrent downloads reached"))
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingDownload = true) }
            
            downloadRepository.addDownload(url, filename)
                .onSuccess { gid ->
                    _events.emit(UiEvent.DownloadStarted(gid))
                }
                .onFailure { error ->
                    _events.emit(UiEvent.ShowError(error.message ?: "failed to add download"))
                }
            
            _uiState.update { it.copy(isAddingDownload = false) }
        }
    }
    
    fun fetchFileInfo(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFileInfo = true, pendingUrl = url) }
            
            downloadRepository.fetchFileInfo(url)
                .onSuccess { info ->
                    _uiState.update { 
                        it.copy(
                            isLoadingFileInfo = false, 
                            fileInfo = info,
                            showAddDialog = true
                        ) 
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingFileInfo = false) }
                    _events.emit(UiEvent.ShowError("could not fetch file info: ${error.message}"))
                }
        }
    }
    
    fun openAddDialog(pendingUrl: String? = null) {
        _uiState.update { it.copy(showAddDialog = true, pendingUrl = pendingUrl) }
    }
    
    fun dismissAddDialog() {
        _uiState.update { 
            it.copy(
                showAddDialog = false, 
                fileInfo = null, 
                pendingUrl = null
            ) 
        }
    }
    
    fun pauseDownload(gid: String) {
        viewModelScope.launch {
            downloadRepository.pauseDownload(gid)
        }
    }
    
    fun resumeDownload(gid: String) {
        viewModelScope.launch {
            downloadRepository.resumeDownload(gid)
        }
    }
    
    fun removeDownload(gid: String) {
        viewModelScope.launch {
            downloadRepository.removeDownload(gid)
        }
    }
    
    fun refreshStats() {
        viewModelScope.launch {
            downloadRepository.getGlobalStats()
                .onSuccess { stats ->
                    _uiState.update { it.copy(globalStats = stats) }
                }
        }
    }
    
    fun requestPaywall(reason: PaywallTriggerReason) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowPaywall(reason))
        }
    }
    
    // settings actions
    
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            if (!isPremium.value && theme != AppTheme.DEFAULT) {
                requestPaywall(PaywallTriggerReason.CUSTOM_THEMES_BLOCKED)
                return@launch
            }
            settingsRepository.setAppTheme(theme)
        }
    }
    
    fun updateWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWifiOnly(enabled)
        }
    }
    
    fun updateMaxConcurrent(count: Int) {
        viewModelScope.launch {
            if (!isPremium.value && count > 4) {
                 requestPaywall(PaywallTriggerReason.CONCURRENT_LIMIT)
                 return@launch
            }
            settingsRepository.setMaxConcurrentDownloads(count)
        }
    }
    
    fun updateConnectionLimit(count: Int) {
         viewModelScope.launch {
            if (!isPremium.value && count > 7) {
                 requestPaywall(PaywallTriggerReason.SPEED_BOOST_BLOCKED)
                 return@launch
            }
            settingsRepository.setConnectionLimit(count)
         }
    }
    
    fun updateDownloadDir(uriString: String) {
        viewModelScope.launch {
            settingsRepository.setDownloadDirUri(uriString)
        }
    }
    
    // monetization actions
    
    fun purchaseLifetime(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, purchaseError = null) }
            
            premiumRepository.purchaseLifetime(activity)
                .onSuccess {
                    _uiState.update { it.copy(isPurchasing = false) }
                    _events.emit(UiEvent.PurchaseSuccess)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isPurchasing = false, purchaseError = error.message) }
                }
        }
    }
    
    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, purchaseError = null) }
            
            premiumRepository.restorePurchases()
                .onSuccess { result ->
                     _uiState.update { it.copy(isPurchasing = false) }
                     if (result.wasRestored) {
                         _events.emit(UiEvent.PurchaseSuccess)
                     } else {
                         _events.emit(UiEvent.ShowError("no previous purchases found to restore"))
                     }
                }
                .onFailure { error ->
                     _uiState.update { it.copy(isPurchasing = false, purchaseError = error.message) }
                }
        }
    }
    
    fun clearPurchaseError() {
        _uiState.update { it.copy(purchaseError = null) }
    }

    // ========================================================================
    // Private Methods
    // ========================================================================

    private fun checkPremiumOnStart() {
        viewModelScope.launch {
            premiumRepository.checkPremiumStatus()
            fetchOfferings()
        }
    }
    
    private fun fetchOfferings() {
         viewModelScope.launch {
             premiumRepository.getLifetimePackage()
                 .onSuccess { pkg ->
                     _uiState.update { it.copy(formattedPrice = pkg.product.price.formatted) }
                 }
         }
    }
    
    private fun isTorrentOrMagnet(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("magnet:") || 
               lower.endsWith(".torrent") ||
               lower.contains("btih:")
    }
    
    private fun observeDownloads() {
        viewModelScope.launch {
            downloadRepository.observeAllDownloads()
                .catch { error ->
                    _events.emit(UiEvent.ShowError("connection lost: ${error.message}"))
                }
                .collect { downloads ->
                    _uiState.update { state ->
                        state.copy(
                            downloads = downloads,
                            activeDownloads = downloads.filter { it.isActive },
                            completedDownloads = downloads.filter { it.isComplete }
                        )
                    }
                }
        }
    }

    // ========================================================================
    // Factory
    // ========================================================================
    
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    downloadRepository = ServiceLocator.provideDownloadRepository(),
                    premiumRepository = ServiceLocator.providePremiumRepository(),
                    settingsRepository = ServiceLocator.provideSettingsRepository()
                ) as T
            }
        }
    }
}

// ============================================================================
// UI State
// ============================================================================

data class MainUiState(
    val downloads: List<Download> = emptyList(),
    val activeDownloads: List<Download> = emptyList(),
    val completedDownloads: List<Download> = emptyList(),
    val globalStats: Aria2GlobalStat? = null,
    val isAddingDownload: Boolean = false,
    val isLoadingFileInfo: Boolean = false,
    val showAddDialog: Boolean = false,
    val pendingUrl: String? = null,
    val fileInfo: FileInfo? = null,
    // monetization state
    val formattedPrice: String = "$3.99",
    val isPurchasing: Boolean = false,
    val purchaseError: String? = null
)

sealed class UiEvent {
    data class ShowError(val message: String) : UiEvent()
    data class DownloadStarted(val gid: String) : UiEvent()
    data class ShowPaywall(val reason: PaywallTriggerReason) : UiEvent()
    data class ShowSpeedBoostSuggestion(val currentSpeedFormatted: String) : UiEvent()
    object PurchaseSuccess : UiEvent()
}
