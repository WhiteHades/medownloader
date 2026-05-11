package com.medownloader.data.engine

import com.medownloader.data.Aria2RpcClient
import com.medownloader.data.source.Aria2ProcessManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class Aria2Engine(
    private val rpcClient: Aria2RpcClient,
    private val processManager: Aria2ProcessManager
) : DownloadEngine {

    override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> = flow {
        val result = rpcClient.addUri(options.url, options.filename)
        if (result.isFailure) {
            emit(DownloadProgress(
                gid = "error",
                status = DownloadStatus.ERROR,
                filename = options.filename ?: options.url,
                downloadedBytes = 0,
                totalBytes = 0,
                speed = 0,
                eta = 0
            ))
            return@flow
        }

        val gid = result.getOrThrow()

        while (true) {
            val status = rpcClient.tellStatus(gid)
            if (status.isFailure) {
                delay(500)
                continue
            }

            val download = status.getOrThrow()
            val engineStatus = when {
                download.isComplete -> DownloadStatus.COMPLETE
                download.isPaused -> DownloadStatus.PAUSED
                download.isActive -> DownloadStatus.ACTIVE
                download.isStopped -> DownloadStatus.STOPPED
                else -> DownloadStatus.QUEUED
            }

            emit(DownloadProgress(
                gid = download.gid,
                status = engineStatus,
                filename = download.filename,
                downloadedBytes = download.completedLength,
                totalBytes = download.totalLength,
                speed = download.downloadSpeed,
                eta = download.eta
            ))

            if (engineStatus == DownloadStatus.COMPLETE || engineStatus == DownloadStatus.STOPPED) {
                break
            }

            delay(500)
        }
    }

    override suspend fun pause(gid: String): Result<Unit> {
        return rpcClient.pause(gid).map {}
    }

    override suspend fun resume(gid: String): Result<Unit> {
        return rpcClient.unpause(gid).map {}
    }

    override suspend fun stop(gid: String): Result<Unit> {
        return rpcClient.remove(gid).map {}
    }

    override suspend fun queryAll(): List<DownloadProgress> {
        val active = rpcClient.tellActive().getOrElse { emptyList() }
        val waiting = rpcClient.tellWaiting().getOrElse { emptyList() }
        val stopped = rpcClient.tellStopped(num = 20).getOrElse { emptyList() }

        return (active + waiting + stopped).map { download ->
            val engineStatus = when {
                download.isComplete -> DownloadStatus.COMPLETE
                download.isPaused -> DownloadStatus.PAUSED
                download.isActive -> DownloadStatus.ACTIVE
                download.isStopped -> DownloadStatus.STOPPED
                else -> DownloadStatus.QUEUED
            }
            DownloadProgress(
                gid = download.gid,
                status = engineStatus,
                filename = download.filename,
                downloadedBytes = download.completedLength,
                totalBytes = download.totalLength,
                speed = download.downloadSpeed,
                eta = download.eta
            )
        }
    }

    override suspend fun isHealthy(): Boolean {
        if (processManager.processState.value != Aria2ProcessManager.ProcessState.Running) {
            return false
        }
        return rpcClient.getVersion().isSuccess
    }
}
