package com.medownloader.di

import android.content.Context
import com.medownloader.data.Aria2RpcClient
import com.medownloader.data.engine.Aria2Engine
import com.medownloader.data.engine.YtDlpEngine
import com.medownloader.data.repository.DownloadQueueRepository
import com.medownloader.data.repository.DownloadRepository
import com.medownloader.data.repository.DownloadHistoryRepository
import com.medownloader.data.repository.DownloadRepositoryImpl
import com.medownloader.data.repository.PremiumRepository
import com.medownloader.data.repository.PremiumRepositoryImpl
import com.medownloader.data.repository.SettingsRepository
import com.medownloader.data.source.Aria2ProcessManager

object ServiceLocator {

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var processManager: Aria2ProcessManager? = null

    @Volatile
    private var rpcClient: Aria2RpcClient? = null

    @Volatile
    private var aria2Engine: Aria2Engine? = null

    @Volatile
    private var ytDlpEngine: YtDlpEngine? = null

    @Volatile
    private var downloadRepository: DownloadRepository? = null

    @Volatile
    private var downloadHistoryRepository: DownloadHistoryRepository? = null

    @Volatile
    private var downloadQueueRepository: DownloadQueueRepository? = null

    @Volatile
    private var premiumRepository: PremiumRepository? = null

    @Volatile
    private var settingsRepository: SettingsRepository? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun provideProcessManager(): Aria2ProcessManager {
        return processManager ?: synchronized(this) {
            processManager ?: Aria2ProcessManager(
                context = requireNotNull(appContext) { "ServiceLocator not initialized" },
                settingsRepository = provideSettingsRepository()
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

    fun provideAria2Engine(): Aria2Engine {
        return aria2Engine ?: synchronized(this) {
            aria2Engine ?: Aria2Engine(
                rpcClient = provideRpcClient(),
                processManager = provideProcessManager()
            ).also { aria2Engine = it }
        }
    }

    fun provideYtDlpEngine(): YtDlpEngine {
        return ytDlpEngine ?: synchronized(this) {
            ytDlpEngine ?: YtDlpEngine(
                aria2ProcessManager = provideProcessManager(),
                settingsRepository = provideSettingsRepository()
            ).also { ytDlpEngine = it }
        }
    }

    fun provideDownloadRepository(): DownloadRepository {
        return downloadRepository ?: synchronized(this) {
            downloadRepository ?: DownloadRepositoryImpl(
                primaryEngine = provideYtDlpEngine(),
                fallbackEngine = provideAria2Engine(),
                historyRepository = provideDownloadHistoryRepository(),
                queueRepository = provideDownloadQueueRepository(),
                rpcClient = provideRpcClient(),
                processManager = provideProcessManager(),
                context = requireNotNull(appContext),
                settingsRepository = provideSettingsRepository()
            ).also { downloadRepository = it }
        }
    }

    fun provideDownloadHistoryRepository(): DownloadHistoryRepository {
        return downloadHistoryRepository ?: synchronized(this) {
            downloadHistoryRepository ?: DownloadHistoryRepository(
                requireNotNull(appContext)
            ).also { downloadHistoryRepository = it }
        }
    }

    fun provideDownloadQueueRepository(): DownloadQueueRepository {
        return downloadQueueRepository ?: synchronized(this) {
            downloadQueueRepository ?: DownloadQueueRepository(
                requireNotNull(appContext)
            ).also { downloadQueueRepository = it }
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

    fun reset() {
        synchronized(this) {
            processManager = null
            rpcClient = null
            aria2Engine = null
            ytDlpEngine = null
            downloadRepository = null
            downloadHistoryRepository = null
            downloadQueueRepository = null
            premiumRepository = null
            settingsRepository = null
        }
    }
}
