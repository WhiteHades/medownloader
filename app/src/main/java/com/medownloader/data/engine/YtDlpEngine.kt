package com.medownloader.data.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class YtDlpEngine : DownloadEngine {

    override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> {
        val channel = MutableSharedFlow<DownloadProgress>(extraBufferCapacity = 1)
        channel.tryEmit(DownloadProgress(
            gid = options.url,
            status = DownloadStatus.ERROR,
            filename = options.filename ?: options.url,
            downloadedBytes = 0,
            totalBytes = 0,
            speed = 0,
            eta = 0,
            errorMessage = "yt-dlp not available — rebuild with chaquopy plugin to enable"
        ))
        return channel
    }

    override suspend fun pause(gid: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("yt-dlp not available"))

    override suspend fun resume(gid: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("yt-dlp not available"))

    override suspend fun stop(gid: String): Result<Unit> = Result.success(Unit)

    override suspend fun queryAll(): List<DownloadProgress> = emptyList()

    override suspend fun isHealthy(): Boolean = false

    suspend fun canExtract(url: String): Boolean = false
}
