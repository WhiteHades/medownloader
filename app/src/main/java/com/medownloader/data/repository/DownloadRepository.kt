package com.medownloader.data.repository

import android.content.Context
import com.medownloader.data.Aria2RpcClient
import com.medownloader.data.model.Download
import com.medownloader.data.model.Aria2GlobalStat
import com.medownloader.data.source.Aria2ProcessManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for download operations.
 * Abstracts the data layer from presentation.
 */
interface DownloadRepository {
    
    /**
     * Start the download engine if not running.
     */
    suspend fun ensureEngineRunning(): Result<Unit>
    
    /**
     * Stop the download engine.
     */
    suspend fun stopEngine()
    
    /**
     * Add a new download.
     * 
     * @param url Download URL
     * @param filename Optional custom filename
     * @return GID of the created download
     */
    suspend fun addDownload(url: String, filename: String? = null): Result<String>
    
    /**
     * Pause a download.
     */
    suspend fun pauseDownload(gid: String): Result<Unit>
    
    /**
     * Resume a paused download.
     */
    suspend fun resumeDownload(gid: String): Result<Unit>
    
    /**
     * Remove/cancel a download.
     */
    suspend fun removeDownload(gid: String): Result<Unit>
    
    /**
     * Get all downloads as a Flow.
     */
    fun observeAllDownloads(): Flow<List<Download>>
    
    /**
     * Get a specific download's status as a Flow.
     */
    fun observeDownload(gid: String): Flow<Download>
    
    /**
     * Get global statistics (speed, counts).
     */
    suspend fun getGlobalStats(): Result<Aria2GlobalStat>
    
    /**
     * Check if a URL is allowed (e.g., not YouTube).
     */
    fun isUrlAllowed(url: String): Boolean
    
    /**
     * Fetch file info from URL (HEAD request).
     * Returns filename and size if available.
     */
    suspend fun fetchFileInfo(url: String): Result<FileInfo>
}

data class FileInfo(
    val filename: String,
    val size: Long?,       // null if unknown
    val resumable: Boolean,
    val mimeType: String?
)

/**
 * Implementation of DownloadRepository.
 */
class DownloadRepositoryImpl(
    private val rpcClient: Aria2RpcClient,
    private val processManager: Aria2ProcessManager,
    private val context: Context
) : DownloadRepository {
    
    companion object {
        // Domains that are forbidden by Google Play Policy
        private val BLOCKED_DOMAINS = listOf(
            "youtube.com",
            "youtu.be",
            "googlevideo.com",
            "ytimg.com"
        )
    }

    override suspend fun ensureEngineRunning(): Result<Unit> {
        return if (processManager.processState.value == Aria2ProcessManager.ProcessState.Running) {
            Result.success(Unit)
        } else {
            processManager.start()
        }
    }

    override suspend fun stopEngine() {
        rpcClient.shutdown()
        processManager.stop()
    }

    override suspend fun addDownload(url: String, filename: String?): Result<String> {
        if (!isUrlAllowed(url)) {
            return Result.failure(
                IllegalArgumentException("Downloads from this source are not allowed by Google Play Policy")
            )
        }
        
        ensureEngineRunning().onFailure { return Result.failure(it) }
        return rpcClient.addUri(url, filename)
    }

    override suspend fun pauseDownload(gid: String): Result<Unit> {
        return rpcClient.pause(gid).map { }
    }

    override suspend fun resumeDownload(gid: String): Result<Unit> {
        return rpcClient.unpause(gid).map { }
    }

    override suspend fun removeDownload(gid: String): Result<Unit> {
        return rpcClient.remove(gid).map { }
    }

    override fun observeAllDownloads(): Flow<List<Download>> {
        return rpcClient.observeDownloads()
    }

    override fun observeDownload(gid: String): Flow<Download> {
        return rpcClient.observeDownload(gid)
    }

    override suspend fun getGlobalStats(): Result<Aria2GlobalStat> {
        return rpcClient.getGlobalStat()
    }

    override fun isUrlAllowed(url: String): Boolean {
        val lowercaseUrl = url.lowercase()
        return BLOCKED_DOMAINS.none { domain -> lowercaseUrl.contains(domain) }
    }

    override suspend fun fetchFileInfo(url: String): Result<FileInfo> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // Validate URL first
            if (url.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("URL is empty"))
            }
            
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("magnet:")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid URL scheme"))
            }
            
            // Magnet links don't support HEAD requests
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
            
            // Use OkHttp HEAD request with timeout
            val client = okhttp3.OkHttpClient.Builder()
                .followRedirects(true)
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            // Try HEAD first
            var request = okhttp3.Request.Builder()
                .url(url)
                .head()
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()
            
            var response = client.newCall(request).execute()
            var responseCode = response.code
            
            // Many servers reject HEAD (405, 403, 410, etc.), fallback to GET with Range header
            if (!response.isSuccessful) {
                response.close()
                request = okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Range", "bytes=0-0")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build()
                response = client.newCall(request).execute()
                responseCode = response.code
            }
            
            // If still failing, return a basic FileInfo extracted from URL (download might still work)
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
            
            // Extract filename from Content-Disposition, URL query param, or URL path
            val filename = contentDisposition
                ?.substringAfter("filename=", "")
                ?.trim('"', '\'', ' ')
                ?.ifEmpty { null }
                ?: extractFilenameFromUrl(url)
            
            Result.success(
                FileInfo(
                    filename = filename,
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
    
    /**
     * Extract filename from URL - checks query param "filename" first, then path
     */
    private fun extractFilenameFromUrl(url: String): String {
        // First check for filename= query parameter (common in redirect URLs)
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
        
        // Fallback to path extraction
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
