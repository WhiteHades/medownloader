package com.medownloader.data.repository

import android.content.Context
import android.util.Log
import com.medownloader.data.engine.*
import com.medownloader.data.model.Download
import com.medownloader.data.model.Aria2GlobalStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

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
    private val context: Context
) : DownloadRepository {

    companion object {
        private const val TAG = "DownloadRepository"
        private val BLOCKED_DOMAINS = listOf("youtube.com", "youtu.be", "googlevideo.com", "ytimg.com")
    }

    private val activeDownloads = ConcurrentHashMap<String, Job>()
    private val downloadRegistry = ConcurrentHashMap<String, DownloadProgress>()

    private val fileInfoHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    override suspend fun ensureEngineRunning(): Result<Unit> {
        return if (fallbackEngine.isHealthy()) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("download engine not available"))
        }
    }

    override suspend fun stopEngine() {
        activeDownloads.clear()
        downloadRegistry.clear()
    }

    override suspend fun addDownload(url: String, filename: String?): Result<String> {
        if (!isUrlAllowed(url)) {
            return Result.failure(IllegalArgumentException("this source is blocked"))
        }

        val route = ProtocolRouter.route(url)
        val options = DownloadOptions(url = url, filename = filename, protocolType = route)
        val gid = url

        val progressFlow = when (route) {
            EngineType.ARIA2C -> {
                Log.d(TAG, "routing to aria2c directly: $url")
                fallbackEngine.download(options)
            }
            EngineType.YT_DLP -> {
                Log.d(TAG, "trying yt-dlp: $url")
                try {
                    primaryEngine.download(options)
                } catch (e: Exception) {
                    Log.w(TAG, "yt-dlp failed, falling back to aria2c: ${e.message}")
                    fallbackEngine.download(options)
                }
            }
        }

        kotlinx.coroutines.GlobalScope.launch {
            progressFlow.collect { progress ->
                if (progress.status == DownloadStatus.ERROR && route == EngineType.YT_DLP) {
                    Log.w(TAG, "yt-dlp emitted error, falling back to aria2c")
                    fallbackEngine.download(options).collect { fallbackProgress ->
                        downloadRegistry[fallbackProgress.gid] = fallbackProgress
                    }
                    return@collect
                }
                downloadRegistry[progress.gid] = progress
            }
            downloadRegistry.remove(gid)
        }

        return Result.success(gid)
    }

    override suspend fun pauseDownload(gid: String): Result<Unit> {
        return fallbackEngine.pause(gid)
    }

    override suspend fun resumeDownload(gid: String): Result<Unit> {
        return fallbackEngine.resume(gid)
    }

    override suspend fun removeDownload(gid: String): Result<Unit> {
        return fallbackEngine.stop(gid)
    }

    override fun observeAllDownloads(): Flow<List<DownloadProgress>> = flow {
        while (true) {
            val snapshots = downloadRegistry.values.toList()
            emit(snapshots)
            kotlinx.coroutines.delay(1000)
        }
    }

    override fun observeDownload(gid: String): Flow<DownloadProgress> = flow {
        while (true) {
            downloadRegistry[gid]?.let { emit(it) }
            kotlinx.coroutines.delay(500)
        }
    }

    override suspend fun getGlobalStats(): Result<Aria2GlobalStat> {
        return Result.failure(UnsupportedOperationException("global stats via new engine not yet wired"))
    }

    override fun isUrlAllowed(url: String): Boolean {
        val lowercaseUrl = url.lowercase()
        return BLOCKED_DOMAINS.none { domain -> lowercaseUrl.contains(domain) }
    }

    override suspend fun fetchFileInfo(url: String): Result<FileInfo> = withContext(Dispatchers.IO) {
        try {
            if (url.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("URL is empty"))
            }

            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("magnet:")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid URL scheme"))
            }

            if (url.startsWith("magnet:")) {
                val displayName = url.substringAfter("dn=", "").substringBefore("&").ifEmpty { "magnet_download" }
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
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()

            var response = fileInfoHttpClient.newCall(request).execute()
            var responseCode = response.code

            if (!response.isSuccessful) {
                response.close()
                request = okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Range", "bytes=0-0")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
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
        return Result.success(Unit)
    }

    private fun extractFilenameFromUrl(url: String): String {
        val filenameParam = url.substringAfter("filename=", "")
            .substringBefore("&")
            .takeIf { it.isNotEmpty() }

        if (filenameParam != null) {
            return try {
                java.net.URLDecoder.decode(filenameParam, "UTF-8")
            } catch (e: Exception) {
                filenameParam
            }
        }

        val pathPart = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
        return if (pathPart.isNotEmpty() && pathPart.length < 200 && pathPart.contains('.')) {
            try {
                java.net.URLDecoder.decode(pathPart, "UTF-8")
            } catch (e: Exception) {
                pathPart
            }
        } else {
            "download_${System.currentTimeMillis()}"
        }
    }
}
