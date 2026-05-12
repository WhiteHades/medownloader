package com.medownloader.data.repository

import android.content.Context
import android.util.Log
import com.medownloader.data.Aria2RpcClient
import com.medownloader.data.engine.*
import com.medownloader.data.model.Aria2GlobalStat
import com.medownloader.data.model.DownloadHistoryEntry
import com.medownloader.data.source.Aria2ProcessManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

interface DownloadRepository {
    suspend fun ensureEngineRunning(): Result<Unit>
    suspend fun stopEngine()
    suspend fun addDownload(url: String, filename: String? = null): Result<String>
    suspend fun pauseDownload(gid: String): Result<Unit>
    suspend fun resumeDownload(gid: String): Result<Unit>
    suspend fun removeDownload(gid: String): Result<Unit>
    fun observeAllDownloads(): Flow<List<DownloadProgress>>
    fun observeDownload(gid: String): Flow<DownloadProgress>
    suspend fun getGlobalStats(): Result<Aria2GlobalStat>
    fun isUrlAllowed(url: String): Boolean
    suspend fun fetchFileInfo(url: String): Result<FileInfo>
    suspend fun applyRuntimeLimits(maxConcurrent: Int, connectionLimit: Int): Result<Unit>
    fun checkDiskSpaceFor(expectedBytes: Long?): com.medownloader.util.DiskSpaceCheck
}

data class FileInfo(
    val filename: String,
    val size: Long?,
    val resumable: Boolean,
    val mimeType: String?
)

class DownloadRepositoryImpl(
    private val primaryEngine: YtDlpEngine,
    private val fallbackEngine: Aria2Engine,
    private val historyRepository: DownloadHistoryRepository,
    // rpcClient kept only for getGlobalStats + applyRuntimeLimits — no download ops use it directly
    private val rpcClient: Aria2RpcClient,
    private val processManager: Aria2ProcessManager,
    private val context: Context,
    private val settingsRepository: SettingsRepository? = null,
    private val diskSpaceProbe: com.medownloader.util.DiskSpaceProbe =
        com.medownloader.util.AndroidDiskSpaceProbe
) : DownloadRepository {

    companion object {
        private const val TAG = "DownloadRepository"
        private const val DEFAULT_MAX_CONCURRENT = 3
    }

    // Which engine is currently handling a given GID
    private enum class EngineOwner { YT_DLP, ARIA2C }
    private data class ActiveEntry(val job: Job, val owner: EngineOwner)
    private data class PendingDownload(val gid: String, val options: DownloadOptions, val route: EngineType)

    private val activeDownloads = ConcurrentHashMap<String, ActiveEntry>()
    private val downloadRegistry = ConcurrentHashMap<String, DownloadProgress>()
    private val startedAtByGid = ConcurrentHashMap<String, Long>()
    private val recordedTerminalStates = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Reactive state — updated on every progress event, no polling
    private val _downloadsFlow = MutableStateFlow<List<DownloadProgress>>(emptyList())

    // Concurrency pool
    private val poolMutex = Mutex()
    private val activeCount = AtomicInteger(0)
    @Volatile private var maxConcurrent = DEFAULT_MAX_CONCURRENT
    private val pendingQueue = ArrayDeque<PendingDownload>()

    private val fileInfoHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    init {
        // React to settings changes for concurrency cap
        settingsRepository?.let { settings ->
            scope.launch {
                settings.maxConcurrentDownloads.collect { max ->
                    maxConcurrent = max.coerceAtLeast(1)
                    drainQueue()
                }
            }
        }
    }

    override suspend fun ensureEngineRunning(): Result<Unit> {
        return if (processManager.processState.value == Aria2ProcessManager.ProcessState.Running) {
            Result.success(Unit)
        } else {
            processManager.start()
        }
    }

    override suspend fun stopEngine() {
        activeDownloads.values.forEach { it.job.cancel() }
        rpcClient.shutdown()
        processManager.stop()
        activeDownloads.clear()
        downloadRegistry.clear()
        pendingQueue.clear()
        activeCount.set(0)
        _downloadsFlow.value = emptyList()
    }

    override suspend fun addDownload(url: String, filename: String?): Result<String> {
        if (!isUrlAllowed(url)) {
            return Result.failure(IllegalArgumentException("this source is blocked"))
        }

        val route = ProtocolRouter.route(url)
        val options = DownloadOptions(url = url, filename = filename, protocolType = route)
        val gid = url

        ensureEngineRunning().onFailure { return Result.failure(it) }

        val shouldLaunch = poolMutex.withLock {
            if (activeCount.get() < maxConcurrent) {
                activeCount.incrementAndGet()
                true
            } else {
                pendingQueue.addLast(PendingDownload(gid, options, route))
                false
            }
        }

        if (shouldLaunch) {
            launchDownload(gid, options, route)
        } else {
            // Show a QUEUED placeholder so the UI knows about it
            downloadRegistry[gid] = DownloadProgress(
                gid = gid,
                status = DownloadStatus.QUEUED,
                filename = options.filename ?: url,
                downloadedBytes = 0,
                totalBytes = 0,
                speed = 0,
                eta = 0
            )
            publishState()
        }

        return Result.success(gid)
    }

    private fun launchDownload(gid: String, options: DownloadOptions, route: EngineType) {
        activeDownloads[gid]?.job?.cancel()

        val job = scope.launch {
            try {
                collectDownload(gid, options, route)
            } finally {
                activeCount.decrementAndGet()
                activeDownloads.remove(gid)
                drainQueue()
            }
        }

        activeDownloads[gid] = ActiveEntry(job, engineOwnerFor(route))
    }

    private suspend fun collectDownload(gid: String, options: DownloadOptions, route: EngineType) {
        val flow = when (route) {
            EngineType.ARIA2C -> {
                Log.d(TAG, "routing to aria2c: $gid")
                fallbackEngine.download(options)
            }
            EngineType.YT_DLP -> {
                Log.d(TAG, "trying yt-dlp: $gid")
                primaryEngine.download(options)
            }
        }

        var switchedToFallback = false

        try {
            flow.collect { progress ->
                startedAtByGid.putIfAbsent(progress.gid, System.currentTimeMillis())

                // yt-dlp emitted an ERROR → switch to aria2c inline
                if (!switchedToFallback && progress.status == DownloadStatus.ERROR && route == EngineType.YT_DLP) {
                    Log.w(TAG, "yt-dlp error event, falling back to aria2c: $gid")
                    switchedToFallback = true
                    activeDownloads[gid]?.let { activeDownloads[gid] = it.copy(owner = EngineOwner.ARIA2C) }
                    fallbackEngine.download(options).collect { fp ->
                        downloadRegistry[fp.gid] = fp
                        publishState()
                        recordHistoryIfTerminal(fp, options.url)
                    }
                    return@collect
                }

                downloadRegistry[progress.gid] = progress
                publishState()
                recordHistoryIfTerminal(progress, options.url)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // yt-dlp threw an exception → try aria2c fallback
            if (route == EngineType.YT_DLP && !switchedToFallback) {
                Log.w(TAG, "yt-dlp threw, falling back to aria2c: $gid — ${e.message}")
                activeDownloads[gid]?.let { activeDownloads[gid] = it.copy(owner = EngineOwner.ARIA2C) }
                try {
                    fallbackEngine.download(options).collect { fp ->
                        downloadRegistry[fp.gid] = fp
                        publishState()
                        recordHistoryIfTerminal(fp, options.url)
                    }
                } catch (fe: CancellationException) {
                    throw fe
                } catch (fe: Exception) {
                    emitError(gid, options, fe)
                }
            } else {
                emitError(gid, options, e)
            }
        }
    }

    private suspend fun emitError(gid: String, options: DownloadOptions, e: Exception) {
        val errProgress = DownloadProgress(
            gid = gid,
            status = DownloadStatus.ERROR,
            filename = options.filename ?: options.url,
            downloadedBytes = 0,
            totalBytes = 0,
            speed = 0,
            eta = 0,
            errorMessage = e.message
        )
        downloadRegistry[gid] = errProgress
        publishState()
        recordHistoryIfTerminal(errProgress, options.url)
    }

    private fun publishState() {
        _downloadsFlow.value = downloadRegistry.values.toList()
    }

    private suspend fun drainQueue() {
        while (true) {
            val next = poolMutex.withLock {
                if (activeCount.get() < maxConcurrent && pendingQueue.isNotEmpty()) {
                    activeCount.incrementAndGet()
                    pendingQueue.removeFirst()
                } else null
            } ?: break
            launchDownload(next.gid, next.options, next.route)
        }
    }

    private fun engineOwnerFor(route: EngineType) = when (route) {
        EngineType.YT_DLP -> EngineOwner.YT_DLP
        EngineType.ARIA2C -> EngineOwner.ARIA2C
    }

    override suspend fun pauseDownload(gid: String): Result<Unit> {
        return when (activeDownloads[gid]?.owner) {
            EngineOwner.YT_DLP -> Result.failure(
                UnsupportedOperationException("yt-dlp downloads cannot be paused")
            )
            EngineOwner.ARIA2C, null -> fallbackEngine.pause(gid)
        }
    }

    override suspend fun resumeDownload(gid: String): Result<Unit> {
        return when (activeDownloads[gid]?.owner) {
            EngineOwner.YT_DLP -> Result.failure(
                UnsupportedOperationException("yt-dlp downloads cannot be resumed")
            )
            EngineOwner.ARIA2C, null -> fallbackEngine.resume(gid)
        }
    }

    override suspend fun removeDownload(gid: String): Result<Unit> {
        // Remove from pending queue first
        poolMutex.withLock {
            pendingQueue.removeAll { it.gid == gid }
        }
        activeDownloads.remove(gid)?.job?.cancel()
        downloadRegistry.remove(gid)
        publishState()
        // Best-effort stop on aria2c side (may already be gone)
        return fallbackEngine.stop(gid)
    }

    // Reactive — no polling, updated on every progress event
    override fun observeAllDownloads(): Flow<List<DownloadProgress>> = _downloadsFlow.asStateFlow()

    override fun observeDownload(gid: String): Flow<DownloadProgress> =
        _downloadsFlow.mapNotNull { list -> list.firstOrNull { it.gid == gid } }

    override suspend fun getGlobalStats(): Result<Aria2GlobalStat> = rpcClient.getGlobalStat()

    override fun isUrlAllowed(url: String): Boolean {
        val scheme = url.trim().lowercase()
        return scheme.startsWith("http://") ||
            scheme.startsWith("https://") ||
            scheme.startsWith("ftp://") ||
            scheme.startsWith("magnet:")
    }

    override suspend fun fetchFileInfo(url: String): Result<FileInfo> = withContext(Dispatchers.IO) {
        try {
            if (url.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("URL is empty"))
            }

            if (!url.startsWith("http://") && !url.startsWith("https://") &&
                !url.startsWith("ftp://") && !url.startsWith("magnet:")
            ) {
                return@withContext Result.failure(IllegalArgumentException("Invalid URL scheme"))
            }

            if (ProtocolRouter.route(url) == EngineType.YT_DLP && primaryEngine.isHealthy()) {
                val extracted = primaryEngine.fetchInfo(url)
                if (extracted.isSuccess) return@withContext extracted
            }

            if (url.startsWith("magnet:")) {
                val displayName = url.substringAfter("dn=", "").substringBefore("&")
                    .ifEmpty { "magnet_download" }
                return@withContext Result.success(
                    FileInfo(
                        filename = java.net.URLDecoder.decode(displayName, "UTF-8"),
                        size = null,
                        resumable = true,
                        mimeType = "application/x-bittorrent"
                    )
                )
            }

            var request = okhttp3.Request.Builder()
                .url(url)
                .head()
                .addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                .build()

            var response = fileInfoHttpClient.newCall(request).execute()
            var responseCode = response.code

            if (!response.isSuccessful) {
                response.close()
                request = okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Range", "bytes=0-0")
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    )
                    .build()
                response = fileInfoHttpClient.newCall(request).execute()
                responseCode = response.code
            }

            if (!response.isSuccessful && responseCode != 206) {
                response.close()
                return@withContext Result.success(
                    FileInfo(
                        filename = extractFilenameFromUrl(url),
                        size = null,
                        resumable = false,
                        mimeType = null
                    )
                )
            }

            val contentDisposition = response.header("Content-Disposition")
            val contentLength = response.header("Content-Length")?.toLongOrNull()
                ?: response.header("Content-Range")?.substringAfterLast("/")?.toLongOrNull()
            val contentType = response.header("Content-Type")
            val acceptRanges = response.header("Accept-Ranges")
            response.close()

            val extractedFilename = contentDisposition
                ?.substringAfter("filename=", "")
                ?.trim('"', '\'', ' ')
                ?.ifEmpty { null }
                ?: extractFilenameFromUrl(url)

            Result.success(
                FileInfo(
                    filename = extractedFilename,
                    size = contentLength,
                    resumable = acceptRanges == "bytes" || responseCode == 206,
                    mimeType = contentType
                )
            )
        } catch (e: java.net.MalformedURLException) {
            Result.failure(Exception("Invalid URL format"))
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Could not resolve host: ${e.message}"))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Connection timed out"))
        } catch (e: javax.net.ssl.SSLException) {
            Result.failure(Exception("SSL/TLS error: ${e.message}"))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Failed: ${e::class.simpleName} - ${e.message ?: e.toString()}"))
        }
    }

    override suspend fun applyRuntimeLimits(maxConcurrent: Int, connectionLimit: Int): Result<Unit> {
        val safeMax = maxConcurrent.coerceIn(1, 16)
        val safeConn = connectionLimit.coerceIn(1, 16)
        this.maxConcurrent = safeMax

        if (processManager.processState.value != Aria2ProcessManager.ProcessState.Running) {
            return Result.success(Unit)
        }

        return rpcClient.changeGlobalOption(
            mapOf(
                "max-concurrent-downloads" to safeMax.toString(),
                "max-connection-per-server" to safeConn.toString(),
                "split" to safeConn.toString()
            )
        ).map { }
    }

    override fun checkDiskSpaceFor(expectedBytes: Long?): com.medownloader.util.DiskSpaceCheck {
        val dir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val target = java.io.File(dir, "meDownloader").takeIf { it.exists() } ?: dir
        val free = diskSpaceProbe.availableBytes(target.absolutePath)
        return com.medownloader.util.evaluateDiskSpace(freeBytes = free, expectedBytes = expectedBytes)
    }

    private fun extractFilenameFromUrl(url: String): String {
        val filenameParam = url.substringAfter("filename=", "")
            .substringBefore("&")
            .takeIf { it.isNotEmpty() }

        if (filenameParam != null) {
            return try { java.net.URLDecoder.decode(filenameParam, "UTF-8") } catch (e: Exception) { filenameParam }
        }

        val pathPart = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
        return if (pathPart.isNotEmpty() && pathPart.length < 200 && pathPart.contains('.')) {
            try { java.net.URLDecoder.decode(pathPart, "UTF-8") } catch (e: Exception) { pathPart }
        } else {
            "download_${System.currentTimeMillis()}"
        }
    }

    private suspend fun recordHistoryIfTerminal(progress: DownloadProgress, url: String) {
        if (progress.status != DownloadStatus.COMPLETE && progress.status != DownloadStatus.ERROR) return

        val finishedAt = System.currentTimeMillis()
        val startedAt = startedAtByGid.remove(progress.gid) ?: finishedAt
        val terminalKey = "${progress.gid}:$startedAt:${progress.status.name}"
        if (!recordedTerminalStates.add(terminalKey)) return

        historyRepository.append(
            DownloadHistoryEntry(
                id = "${progress.gid}-${progress.status.name}-$finishedAt",
                gid = progress.gid,
                url = url,
                filename = progress.filename,
                status = progress.status.name,
                totalBytes = progress.totalLength,
                averageSpeed = progress.downloadSpeed,
                startedAtEpochMs = startedAt,
                finishedAtEpochMs = finishedAt,
                durationMs = (finishedAt - startedAt).coerceAtLeast(0L),
                fileType = progress.filename.substringAfterLast('.', "unknown").lowercase()
            )
        )
    }
}
