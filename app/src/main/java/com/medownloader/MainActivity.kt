package com.medownloader

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import com.medownloader.service.DownloadService
import com.medownloader.ui.theme.MeDownloaderTheme
import kotlinx.coroutines.flow.collectLatest

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
            
            MeDownloaderTheme(theme = appTheme) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
                
                // Settings States
                val wifiOnly by viewModel.wifiOnly.collectAsStateWithLifecycle()
                val maxConcurrent by viewModel.maxConcurrent.collectAsStateWithLifecycle()
                val connectionLimit by viewModel.connectionLimit.collectAsStateWithLifecycle()
                
                val navController = rememberNavController()
                
                // Handle one-time events
                LaunchedEffect(Unit) {
                    viewModel.events.collectLatest { event ->
                        when (event) {
                            is UiEvent.ShowError -> {
                                Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                            }
                            is UiEvent.DownloadStarted -> {
                                Toast.makeText(this@MainActivity, "download started", Toast.LENGTH_SHORT).show()
                            }
                            is UiEvent.ShowPaywall -> {
                                navController.navigate("paywall/${event.reason.name}")
                            }
                            is UiEvent.ShowSpeedBoostSuggestion -> {
                                // TODO: Show visible nudge
                                Toast.makeText(this@MainActivity, "tip: get pro for 4x speed", Toast.LENGTH_LONG).show()
                            }
                            is UiEvent.PurchaseSuccess -> {
                                Toast.makeText(this@MainActivity, "welcome to pro!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }
                        }
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "dashboard") {
                        
                        composable("dashboard") {
                            DashboardScreen(
                                uiState = uiState,
                                isPremium = isPremium,
                                onAddClick = { showAddDownloadDialog() },
                                onPauseClick = viewModel::pauseDownload,
                                onResumeClick = viewModel::resumeDownload,
                                onRemoveClick = viewModel::removeDownload,
                                onSettingsClick = { navController.navigate("settings") }
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
                                wifiOnly = wifiOnly,
                                maxConcurrent = maxConcurrent,
                                connectionLimit = connectionLimit,
                                downloadPath = downloadDir,
                                onThemeSelected = viewModel::updateTheme,
                                onWifiOnlyChanged = viewModel::updateWifiOnly,
                                onMaxConcurrentChanged = viewModel::updateMaxConcurrent,
                                onConnectionLimitChanged = viewModel::updateConnectionLimit,
                                onDownloadPathClick = { folderPickerLauncher.launch(null) },
                                onBackClick = { navController.popBackStack() },
                                onGetProClick = { navController.navigate("paywall/SETTINGS_UPGRADE") }
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
                                isLoading = false,
                                errorMessage = uiState.purchaseError,
                                onPurchase = { viewModel.purchaseLifetime(this@MainActivity) },
                                onRestore = { viewModel.restorePurchases() },
                                onDismiss = { navController.popBackStack() },
                                triggerReason = reason
                            )
                        }
                    }
                    
                    if (uiState.showAddDialog) {
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
