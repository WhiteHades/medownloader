package com.medownloader.di

import android.content.Context
import com.medownloader.data.Aria2RpcClient
import com.medownloader.data.repository.DownloadRepository
import com.medownloader.data.repository.DownloadRepositoryImpl
import com.medownloader.data.repository.PremiumRepository
import com.medownloader.data.repository.PremiumRepositoryImpl
import com.medownloader.data.repository.SettingsRepository
import com.medownloader.data.source.Aria2ProcessManager

/**
 * Manual Dependency Injection container (Service Locator pattern).
 * 
 * Lightweight alternative to Hilt/Dagger for small apps.
 * Initialize in Application.onCreate()
 */
object ServiceLocator {
    
    @Volatile
    private var appContext: Context? = null
    
    @Volatile
    private var processManager: Aria2ProcessManager? = null
    
    @Volatile
    private var rpcClient: Aria2RpcClient? = null
    
    @Volatile
    private var downloadRepository: DownloadRepository? = null
    
    @Volatile
    private var premiumRepository: PremiumRepository? = null

    @Volatile
    private var settingsRepository: SettingsRepository? = null

    /**
     * Initialize with application context. Call from Application.onCreate()
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun provideProcessManager(): Aria2ProcessManager {
        return processManager ?: synchronized(this) {
            processManager ?: Aria2ProcessManager(
                requireNotNull(appContext) { "ServiceLocator not initialized" }
            ).also { processManager = it }
        }
    }

    fun provideRpcClient(): Aria2RpcClient {
        return rpcClient ?: synchronized(this) {
            val pm = provideProcessManager()
            rpcClient ?: Aria2RpcClient(
                rpcUrl = pm.getRpcUrl(),
                secret = pm.getRpcSecret()
            ).also { rpcClient = it }
        }
    }

    fun provideDownloadRepository(): DownloadRepository {
        return downloadRepository ?: synchronized(this) {
            downloadRepository ?: DownloadRepositoryImpl(
                rpcClient = provideRpcClient(),
                processManager = provideProcessManager(),
                context = requireNotNull(appContext)
            ).also { downloadRepository = it }
        }
    }

    fun providePremiumRepository(): PremiumRepository {
        return premiumRepository ?: synchronized(this) {
            premiumRepository ?: PremiumRepositoryImpl(
                context = requireNotNull(appContext)
            ).also { premiumRepository = it }
        }
    }

    fun provideSettingsRepository(): SettingsRepository {
        return settingsRepository ?: synchronized(this) {
            settingsRepository ?: SettingsRepository(
                requireNotNull(appContext)
            ).also { settingsRepository = it }
        }
    }

    /**
     * Reset all dependencies. Used for testing.
     */
    fun reset() {
        synchronized(this) {
            processManager = null
            rpcClient = null
            downloadRepository = null
            premiumRepository = null
            settingsRepository = null
        }
    }
}
