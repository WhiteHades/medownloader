package com.medownloader.data.engine

import com.medownloader.data.repository.FileInfo
import kotlinx.coroutines.flow.Flow

interface DownloadEngine {
    suspend fun download(options: DownloadOptions): Flow<DownloadProgress>
    suspend fun pause(gid: String): Result<Unit>
    suspend fun resume(gid: String): Result<Unit>
    suspend fun stop(gid: String): Result<Unit>
    suspend fun queryAll(): List<DownloadProgress>
    suspend fun isHealthy(): Boolean

    /**
     * Pre-fetch metadata about [url] (filename, size, resumability). Engines
     * that cannot extract this information should return a failure; the
     * repository falls back to HTTP HEAD probing in that case.
     */
    suspend fun fetchInfo(url: String): Result<FileInfo> =
        Result.failure(UnsupportedOperationException("fetchInfo not supported by this engine"))
}
