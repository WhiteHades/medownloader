package com.medownloader.presentation

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medownloader.data.model.Aria2GlobalStat
import com.medownloader.data.engine.DownloadProgress
import com.medownloader.data.engine.DownloadStatus
import com.medownloader.data.model.DownloadHistoryEntry
import com.medownloader.data.repository.DownloadHistoryRepository
import com.medownloader.data.repository.DownloadRepository
import com.medownloader.data.repository.FileInfo
import com.medownloader.data.repository.FreeTierLimits
import com.medownloader.data.repository.PremiumRepository
import com.medownloader.data.repository.PremiumTierLimits
import com.medownloader.data.repository.SettingsRepository
import com.medownloader.di.ServiceLocator
import com.medownloader.presentation.screen.PaywallTriggerReason
import com.medownloader.ui.theme.AppTheme
import com.medownloader.util.formatSize
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val downloadRepository: DownloadRepository,
    private val downloadHistoryRepository: DownloadHistoryRepository,
    private val premiumRepository: PremiumRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    downloadRepository = ServiceLocator.provideDownloadRepository(),
                    downloadHistoryRepository = ServiceLocator.provideDownloadHistoryRepository(),
                    premiumRepository = ServiceLocator.providePremiumRepository(),
                    settingsRepository = ServiceLocator.provideSettingsRepository()
                ) as T
            }
        }
    }

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

    val splitCount = settingsRepository.splitCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8)

    val enableDht = settingsRepository.enableDht
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dnsServers = settingsRepository.dnsServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "8.8.8.8,8.8.4.4,1.1.1.1")

    val diskCacheMb = settingsRepository.diskCacheMb
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 32)

    val history = downloadHistoryRepository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadDirUri = settingsRepository.downloadDirUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        observeDownloads()
        observeHistory()
        observeRuntimeLimits()
        checkPremiumOnStart()
    }

    // ========================================================================
    // Public Actions
    // ========================================================================
    
    fun addDownload(url: String, filename: String? = null) {
        val normalizedUrl = normalizeIncomingUrl(url)
        if (normalizedUrl == null) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowError("enter a valid download url"))
            }
            return
        }

        if (!downloadRepository.isUrlAllowed(normalizedUrl)) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowError("enter a supported url (http, https, ftp, or magnet)"))
            }
            return
        }
        
        // torrent/magnet requires premium
        if (isTorrentOrMagnet(normalizedUrl) && !premiumRepository.isTorrentEnabled()) {
            requestPaywall(PaywallTriggerReason.TORRENT_BLOCKED)
            return
        }

        // preflight disk space check. if FileInfo.size is known we can block before
        // aria2 tries to fallocate and errors the download out on the emulator.
        val expectedBytes = _uiState.value.fileInfo?.size
        when (val check = downloadRepository.checkDiskSpaceFor(expectedBytes)) {
            is com.medownloader.util.DiskSpaceCheck.Insufficient -> {
                viewModelScope.launch {
                    _events.emit(
                        UiEvent.ShowError(
                            "not enough free space: need ${formatSize(check.requiredBytes)}, " +
                                "have ${formatSize(check.freeBytes)}"
                        )
                    )
                }
                return
            }
            else -> { /* Sufficient or Unknown -> proceed */ }
        }
        
        // check concurrent downloads
        val activeCount = _uiState.value.activeDownloads.size
        val configuredMax = maxConcurrent.value
        val tierMax = if (isPremium.value) {
            PremiumTierLimits.MAX_CONCURRENT_DOWNLOADS
        } else {
            FreeTierLimits.MAX_CONCURRENT_DOWNLOADS
        }
        val effectiveMax = configuredMax.coerceAtMost(tierMax)
        
        if (activeCount >= effectiveMax) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowError("maximum $effectiveMax concurrent downloads reached"))
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingDownload = true) }
            
            downloadRepository.addDownload(normalizedUrl, filename)
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
        val normalizedUrl = normalizeIncomingUrl(url)
        if (normalizedUrl == null) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowError("enter a valid download url"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFileInfo = true, pendingUrl = normalizedUrl) }
            
            downloadRepository.fetchFileInfo(normalizedUrl)
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

    /**
     * User action for the error card on the dashboard.
     * Clears the failed entry from the registry so the card disappears.
     * The caller can re-add the same URL if they want to retry.
     */
    fun dismissError(gid: String) {
        viewModelScope.launch {
            downloadRepository.removeDownload(gid)
        }
    }

    /**
     * Retry a failed download: drop the old ERROR entry and re-submit the URL.
     * The gid used internally is the URL itself, so we reuse it as the retry target.
     */
    fun retryDownload(gid: String) {
        viewModelScope.launch {
            downloadRepository.removeDownload(gid)
            // gid == url (see DownloadRepositoryImpl.addDownload)
            downloadRepository.addDownload(gid, filename = null)
                .onFailure { error ->
                    _events.emit(UiEvent.ShowError(error.message ?: "retry failed"))
                }
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

    fun clearHistory() {
        viewModelScope.launch {
            downloadHistoryRepository.clear()
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
            val tierMax = if (isPremium.value) {
                PremiumTierLimits.MAX_CONCURRENT_DOWNLOADS
            } else {
                FreeTierLimits.MAX_CONCURRENT_DOWNLOADS
            }

            if (!isPremium.value && count > tierMax) {
                 requestPaywall(PaywallTriggerReason.CONCURRENT_LIMIT)
                 return@launch
             }

            settingsRepository.setMaxConcurrentDownloads(count.coerceIn(1, tierMax))
        }
    }
    
    fun updateConnectionLimit(count: Int) {
         viewModelScope.launch {
            val tierMax = if (isPremium.value) {
                PremiumTierLimits.CONNECTIONS_PER_FILE
            } else {
                FreeTierLimits.CONNECTIONS_PER_FILE
            }

            if (!isPremium.value && count > tierMax) {
                  requestPaywall(PaywallTriggerReason.SPEED_BOOST_BLOCKED)
                  return@launch
             }

            settingsRepository.setConnectionLimit(count.coerceIn(1, tierMax))
         }
    }
    
    fun updateDownloadDir(uriString: String) {
        viewModelScope.launch {
            settingsRepository.setDownloadDirUri(uriString)
        }
    }

    fun updateSplitCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setSplitCount(count.coerceIn(1, 32))
        }
    }

    fun updateEnableDht(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEnableDht(enabled)
        }
    }

    fun updateDnsServers(servers: String) {
        viewModelScope.launch {
            settingsRepository.setDnsServers(servers.ifEmpty { "8.8.8.8,8.8.4.4,1.1.1.1" })
        }
    }

    fun updateDiskCacheMb(mb: Int) {
        viewModelScope.launch {
            settingsRepository.setDiskCacheMb(mb.coerceIn(4, 128))
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
            _uiState.update { it.copy(isPricingLoading = true) }

            premiumRepository.getLifetimePackage()
                .onSuccess { pkg ->
                    _uiState.update {
                        it.copy(
                            formattedPrice = pkg.product.price.formatted,
                            isPricingLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    Log.w(TAG, "Failed to load lifetime package", error)
                    _uiState.update { it.copy(isPricingLoading = false) }
                }
        }
    }
    
    private fun isTorrentOrMagnet(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("magnet:") || 
               lower.endsWith(".torrent") ||
               lower.contains("btih:")
    }

    private fun normalizeIncomingUrl(url: String): String? {
        val normalized = url.trim()
        return if (normalized.isEmpty()) null else normalized
    }
    
    private fun observeDownloads() {
        viewModelScope.launch {
            downloadRepository.observeAllDownloads()
                .catch { error ->
                    _events.emit(UiEvent.ShowError("connection lost: ${error.message}"))
                }
                .distinctUntilChanged()
                .collect { downloads ->
                    _uiState.update { state ->
                        state.copy(
                            downloads = downloads,
                            activeDownloads = downloads.filter { it.status == DownloadStatus.ACTIVE },
                            completedDownloads = downloads.filter { it.status == DownloadStatus.COMPLETE },
                            erroredDownloads = downloads.filter { it.status == DownloadStatus.ERROR }
                        )
                    }
                }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            history.collect { entries ->
                _uiState.update { it.copy(history = entries) }
            }
        }
    }

    private fun observeRuntimeLimits() {
        viewModelScope.launch {
            combine(maxConcurrent, connectionLimit) { concurrent, connections ->
                concurrent to connections
            }
                .distinctUntilChanged()
                .collectLatest { (concurrent, connections) ->
                    downloadRepository.applyRuntimeLimits(
                        maxConcurrent = concurrent,
                        connectionLimit = connections
                    ).onFailure { error ->
                        Log.w(TAG, "Failed to apply runtime limits", error)
                    }
                }
        }
    }

}

// ============================================================================
// UI State
// ============================================================================

data class MainUiState(
    val downloads: List<DownloadProgress> = emptyList(),
    val activeDownloads: List<DownloadProgress> = emptyList(),
    val completedDownloads: List<DownloadProgress> = emptyList(),
    val erroredDownloads: List<DownloadProgress> = emptyList(),
    val history: List<DownloadHistoryEntry> = emptyList(),
    val globalStats: Aria2GlobalStat? = null,
    val isAddingDownload: Boolean = false,
    val isLoadingFileInfo: Boolean = false,
    val showAddDialog: Boolean = false,
    val pendingUrl: String? = null,
    val fileInfo: FileInfo? = null,
    // monetization state
    val formattedPrice: String = "$3.99",
    val isPricingLoading: Boolean = true,
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
