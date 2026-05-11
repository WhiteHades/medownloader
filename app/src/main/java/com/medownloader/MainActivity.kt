package com.medownloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medownloader.presentation.MainViewModel
import com.medownloader.presentation.UiEvent
import com.medownloader.presentation.screen.DashboardScreen
import com.medownloader.presentation.screen.AddDownloadSheet
import com.medownloader.presentation.screen.PaywallScreen
import com.medownloader.presentation.screen.PaywallTriggerReason
import com.medownloader.presentation.screen.SettingsScreen
import com.medownloader.presentation.screen.StatsScreen
import com.medownloader.service.DownloadService
import com.medownloader.ui.theme.MeDownloaderTheme
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start download engine service
        startDownloadService()
        
        // Handle share intent
        handleIntent(intent)
        
        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            
            MeDownloaderTheme(appTheme = appTheme) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
                
                // Settings States
                val wifiOnly by viewModel.wifiOnly.collectAsStateWithLifecycle()
                val maxConcurrent by viewModel.maxConcurrent.collectAsStateWithLifecycle()
                val connectionLimit by viewModel.connectionLimit.collectAsStateWithLifecycle()
                val splitCount by viewModel.splitCount.collectAsStateWithLifecycle()
                val enableDht by viewModel.enableDht.collectAsStateWithLifecycle()
                val diskCacheMb by viewModel.diskCacheMb.collectAsStateWithLifecycle()
                
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val downloadStartedMessage = stringResource(R.string.message_download_started)
                val speedBoostTipMessage = stringResource(R.string.message_speed_boost_tip)
                val purchaseSuccessMessage = stringResource(R.string.message_purchase_success)
                
                // Handle one-time events
                LaunchedEffect(Unit) {
                    viewModel.events.collectLatest { event ->
                        when (event) {
                            is UiEvent.ShowError -> {
                                snackbarHostState.showSnackbar(
                                    message = event.message,
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Long
                                )
                            }
                            is UiEvent.DownloadStarted -> {
                                snackbarHostState.showSnackbar(
                                    message = downloadStartedMessage,
                                    duration = SnackbarDuration.Short
                                )
                            }
                            is UiEvent.ShowPaywall -> {
                                navController.navigate("paywall/${event.reason.name}")
                            }
                            is UiEvent.ShowSpeedBoostSuggestion -> {
                                snackbarHostState.showSnackbar(
                                    message = speedBoostTipMessage,
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Long
                                )
                            }
                            is UiEvent.PurchaseSuccess -> {
                                snackbarHostState.showSnackbar(
                                    message = purchaseSuccessMessage,
                                    duration = SnackbarDuration.Short
                                )
                                navController.popBackStack()
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        
                        composable("dashboard") {
                            DashboardScreen(
                                uiState = uiState,
                                isPremium = isPremium,
                                onAddClick = { showAddDownloadDialog() },
                                onPauseClick = viewModel::pauseDownload,
                                onResumeClick = viewModel::resumeDownload,
                                onRemoveClick = viewModel::removeDownload,
                                onSettingsClick = { navController.navigate("settings") },
                                onStatsClick = { navController.navigate("stats") }
                            )
                        }
                        
                        composable("settings") {
                            val folderPickerLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.OpenDocumentTree()
                            ) { uri ->
                                uri?.let {
                                    contentResolver.takePersistableUriPermission(
                                        it,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )
                                    viewModel.updateDownloadDir(it.toString())
                                }
                            }
                            
                            val downloadDir by viewModel.downloadDirUri.collectAsStateWithLifecycle()

                            SettingsScreen(
                                currentTheme = appTheme,
                                isPremium = isPremium,
                                proPrice = uiState.formattedPrice,
                                wifiOnly = wifiOnly,
                                maxConcurrent = maxConcurrent,
                                connectionLimit = connectionLimit,
                                splitCount = splitCount,
                                enableDht = enableDht,
                                diskCacheMb = diskCacheMb,
                                downloadPath = downloadDir ?: stringResource(R.string.settings_download_path_default),
                                onThemeSelected = viewModel::updateTheme,
                                onWifiOnlyChanged = viewModel::updateWifiOnly,
                                onMaxConcurrentChanged = viewModel::updateMaxConcurrent,
                                onConnectionLimitChanged = viewModel::updateConnectionLimit,
                                onSplitCountChanged = viewModel::updateSplitCount,
                                onEnableDhtChanged = viewModel::updateEnableDht,
                                onDiskCacheMbChanged = viewModel::updateDiskCacheMb,
                                onDownloadPathClick = { folderPickerLauncher.launch(null) },
                                onBackClick = { navController.popBackStack() },
                                onGetProClick = { navController.navigate("paywall/SETTINGS_UPGRADE") }
                            )
                        }
                        
                        composable("stats") {
                            StatsScreen(
                                downloads = uiState.downloads,
                                history = uiState.history,
                                onBackClick = { navController.popBackStack() }
                                , onClearHistory = viewModel::clearHistory
                            )
                        }
                        
                        composable(
                            route = "paywall/{reason}",
                            arguments = listOf(navArgument("reason") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val reasonName = backStackEntry.arguments?.getString("reason") 
                                ?: PaywallTriggerReason.SETTINGS_UPGRADE.name
                            val reason = try {
                                PaywallTriggerReason.valueOf(reasonName)
                            } catch (e: Exception) {
                                PaywallTriggerReason.SETTINGS_UPGRADE
                            }
                            
                            PaywallScreen(
                                priceFormatted = uiState.formattedPrice,
                                isPurchasing = uiState.isPurchasing,
                                isLoading = uiState.isPricingLoading,
                                errorMessage = uiState.purchaseError,
                                onPurchase = { viewModel.purchaseLifetime(this@MainActivity) },
                                onRestore = { viewModel.restorePurchases() },
                                onDismiss = { navController.popBackStack() },
                                triggerReason = reason
                            )
                        }
                    }
                    
                    if (uiState.showAddDialog) {
                        ModalBottomSheet(
                            onDismissRequest = viewModel::dismissAddDialog,
                            sheetState = addSheetState,
                            dragHandle = null,
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                            AddDownloadSheet(
                                fileInfo = uiState.fileInfo,
                                isLoading = uiState.isLoadingFileInfo,
                                pendingUrl = uiState.pendingUrl,
                                onFetchInfo = viewModel::fetchFileInfo,
                                onConfirmAdd = { url, filename ->
                                    viewModel.addDownload(url, filename)
                                    viewModel.dismissAddDialog()
                                },
                                onDismiss = viewModel::dismissAddDialog
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                // Shared text/URL from another app
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                    val url = extractUrl(text)
                    if (url != null) {
                        viewModel.openAddDialog(url)
                        viewModel.fetchFileInfo(url)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                // Direct link click (http/https/magnet)
                intent.data?.toString()?.let { url ->
                    viewModel.openAddDialog(url)
                    viewModel.fetchFileInfo(url)
                }
            }
        }
    }

    private fun startDownloadService() {
        val serviceIntent = Intent(this, DownloadService::class.java).apply {
            action = DownloadService.ACTION_START_ENGINE
        }
        startForegroundService(serviceIntent)
    }

    private fun showAddDownloadDialog() {
        viewModel.openAddDialog()
    }

    private fun extractUrl(text: String): String? {
        val urlRegex = """(https?://[^\s]+)""".toRegex()
        val magnetRegex = """(magnet:\?[^\s]+)""".toRegex()
        
        return urlRegex.find(text)?.value ?: magnetRegex.find(text)?.value
    }
}
