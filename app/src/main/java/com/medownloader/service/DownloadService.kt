package com.medownloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.medownloader.MainActivity
import com.medownloader.R
import com.medownloader.data.repository.DownloadRepository
import com.medownloader.data.engine.DownloadProgress
import com.medownloader.data.engine.DownloadStatus
import com.medownloader.di.ServiceLocator
import com.medownloader.util.formatSpeed
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"

        const val CHANNEL_ID = "medownloader_channel"
        const val PROGRESS_CHANNEL_ID = "medownloader_progress"
        const val NOTIFICATION_ID = 1

        const val ACTION_START_ENGINE = "com.medownloader.START_ENGINE"
        const val ACTION_STOP_ENGINE = "com.medownloader.STOP_ENGINE"
        const val ACTION_ADD_DOWNLOAD = "com.medownloader.ADD_DOWNLOAD"
        const val ACTION_PAUSE = "com.medownloader.PAUSE"
        const val ACTION_RESUME = "com.medownloader.RESUME"
        const val ACTION_REMOVE = "com.medownloader.REMOVE"

        const val EXTRA_URL = "url"
        const val EXTRA_FILENAME = "filename"
        const val EXTRA_GID = "gid"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var downloadRepository: DownloadRepository

    private var progressJob: Job? = null
    private val activeNotificationIds = mutableSetOf<Int>()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")

        createNotificationChannels()
        downloadRepository = ServiceLocator.provideDownloadRepository()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        startForeground(NOTIFICATION_ID, createServiceNotification("initializing..."))

        when (intent?.action) {
            ACTION_START_ENGINE -> handleStartEngine()
            ACTION_STOP_ENGINE -> handleStopEngine()
            ACTION_ADD_DOWNLOAD -> handleAddDownload(intent)
            ACTION_PAUSE -> handlePause(intent)
            ACTION_RESUME -> handleResume(intent)
            ACTION_REMOVE -> handleRemove(intent)
            else -> handleStartEngine()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "Service destroyed")
        scope.launch {
            downloadRepository.stopEngine()
            clearProgressNotifications()
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun handleStartEngine() {
        scope.launch {
            downloadRepository.ensureEngineRunning().onSuccess {
                startProgressUpdates()
            }.onFailure { error ->
                Log.e(TAG, "Failed to start engine", error)
            }
        }
    }

    private fun handleStopEngine() {
        scope.launch {
            progressJob?.cancel()
            clearProgressNotifications()
            downloadRepository.stopEngine()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleAddDownload(intent: Intent) {
        val url = intent.getStringExtra(EXTRA_URL) ?: return
        val filename = intent.getStringExtra(EXTRA_FILENAME)

        scope.launch {
            downloadRepository.ensureEngineRunning()
            downloadRepository.addDownload(url, filename).onSuccess { gid ->
                Log.i(TAG, "Download added: $gid")
            }.onFailure { error ->
                Log.e(TAG, "Failed to add download", error)
            }
        }
    }

    private fun handlePause(intent: Intent) {
        val gid = intent.getStringExtra(EXTRA_GID) ?: return
        scope.launch {
            downloadRepository.pauseDownload(gid)
        }
    }

    private fun handleResume(intent: Intent) {
        val gid = intent.getStringExtra(EXTRA_GID) ?: return
        scope.launch {
            downloadRepository.resumeDownload(gid)
        }
    }

    private fun handleRemove(intent: Intent) {
        val gid = intent.getStringExtra(EXTRA_GID) ?: return
        scope.launch {
            downloadRepository.removeDownload(gid)
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            downloadRepository.observeAllDownloads().collect { downloads ->
                updateProgressNotifications(downloads)
            }
        }
    }

    private fun updateProgressNotifications(downloads: List<DownloadProgress>) {
        val manager = getSystemService(NotificationManager::class.java)

        val activeDownloads = downloads.filter { it.status == DownloadStatus.ACTIVE || it.status == DownloadStatus.PAUSED }
        val currentIds = activeDownloads
            .map { notificationIdForGid(it.gid) }
            .toSet()

        (activeNotificationIds - currentIds).forEach { staleId ->
            manager.cancel(staleId)
        }

        activeNotificationIds.clear()
        activeNotificationIds.addAll(currentIds)

        activeDownloads.forEach { download ->
            manager.notify(
                notificationIdForGid(download.gid),
                createProgressNotification(download)
            )
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "download engine",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "shows when the download engine is running"
                setShowBadge(false)
            }

            val progressChannel = NotificationChannel(
                PROGRESS_CHANNEL_ID,
                "download progress",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "shows progress for active downloads"
                setShowBadge(true)
            }

            manager.createNotificationChannels(listOf(serviceChannel, progressChannel))
        }
    }

    private fun createServiceNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("meDownloader")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .build()
    }

    private fun createProgressNotification(download: DownloadProgress): Notification {
        val speedText = formatSpeed(download.speed)
        val progressText = "${download.progressPercent}% • $speedText"

        return NotificationCompat.Builder(this, PROGRESS_CHANNEL_ID)
            .setContentTitle(download.filename)
            .setContentText(progressText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, download.progressPercent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_media_play,
                "pause",
                createActionIntent(ACTION_PAUSE, download.gid)
            )
            .addAction(
                android.R.drawable.ic_delete,
                "cancel",
                createActionIntent(ACTION_REMOVE, download.gid)
            )
            .build()
    }

    private fun createActionIntent(action: String, gid: String): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).apply {
            this.action = action
            putExtra(EXTRA_GID, gid)
        }
        return PendingIntent.getService(
            this,
            gid.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun clearProgressNotifications() {
        val manager = getSystemService(NotificationManager::class.java)
        activeNotificationIds.forEach { manager.cancel(it) }
        activeNotificationIds.clear()
    }

    private fun notificationIdForGid(gid: String): Int {
        return NOTIFICATION_ID + 1000 + (gid.hashCode() and 0x7fffffff)
    }
}
