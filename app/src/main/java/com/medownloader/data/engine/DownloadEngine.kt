package com.medownloader.data.engine

import kotlinx.coroutines.flow.Flow

interface DownloadEngine {
    suspend fun download(options: DownloadOptions): Flow<DownloadProgress>
    suspend fun pause(gid: String): Result<Unit>
    suspend fun resume(gid: String): Result<Unit>
    suspend fun stop(gid: String): Result<Unit>
    suspend fun queryAll(): List<DownloadProgress>
    suspend fun isHealthy(): Boolean
}
