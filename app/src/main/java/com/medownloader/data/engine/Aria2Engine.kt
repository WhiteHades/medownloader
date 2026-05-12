package com.medownloader.data.engine

import android.util.Log
import com.medownloader.data.Aria2RpcClient
import com.medownloader.data.source.Aria2ProcessManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.retryWhen

private const val TAG = "Aria2Engine"

// Maximum consecutive RPC failures before giving up on a download
private const val MAX_FAIL = 5

// Exponential backoff: delay = BASE_DELAY_MS * 2^(attempt-1), capped at MAX_DELAY_MS
private const val BASE_DELAY_MS = 500L
private const val MAX_DELAY_MS = 16_000L

private fun backoffMs(attempt: Int): Long =
    (BASE_DELAY_MS * (1L shl (attempt - 1).coerceAtMost(5))).coerceAtMost(MAX_DELAY_MS)

class Aria2Engine(
    private val rpcClient: Aria2RpcClient,
    private val processManager: Aria2ProcessManager
) : DownloadEngine {

    override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> = flow {
        // Add the URI to aria2c; retry up to 3 times on transient failure
        var addResult = rpcClient.addUri(options.url, options.filename)
        var addAttempt = 0
        while (addResult.isFailure && addAttempt < 3) {
            addAttempt++
            delay(backoffMs(addAttempt))
            addResult = rpcClient.addUri(options.url, options.filename)
        }

        if (addResult.isFailure) {
            emit(
                DownloadProgress(
                    gid = "error",
                    status = DownloadStatus.ERROR,
                    filename = options.filename ?: options.url,
                    downloadedBytes = 0,
                    totalBytes = 0,
                    speed = 0,
                    eta = 0,
                    errorMessage = addResult.exceptionOrNull()?.message ?: "failed to add URI"
                )
            )
            return@flow
        }

        val gid = addResult.getOrThrow()
        var failCount = 0

        while (true) {
            val status = rpcClient.tellStatus(gid)

            if (status.isFailure) {
                failCount++
                if (failCount >= MAX_FAIL) {
                    emit(
                        DownloadProgress(
                            gid = gid,
                            status = DownloadStatus.ERROR,
                            filename = options.filename ?: options.url,
                            downloadedBytes = 0,
                            totalBytes = 0,
                            speed = 0,
                            eta = 0,
                            errorMessage = "RPC unreachable after $failCount retries"
                        )
                    )
                    return@flow
                }
                Log.w(TAG, "tellStatus failed (attempt $failCount/$MAX_FAIL): ${status.exceptionOrNull()?.message}")
                delay(backoffMs(failCount))
                continue
            }

            failCount = 0
            val download = status.getOrThrow()

            val engineStatus = when {
                download.isComplete -> DownloadStatus.COMPLETE
                download.isPaused -> DownloadStatus.PAUSED
                download.isActive -> DownloadStatus.ACTIVE
                download.status == com.medownloader.data.model.DownloadStatus.REMOVED -> DownloadStatus.STOPPED
                else -> DownloadStatus.QUEUED
            }

            emit(
                DownloadProgress(
                    gid = download.gid,
                    status = engineStatus,
                    filename = download.filename,
                    downloadedBytes = download.completedLength,
                    totalBytes = download.totalLength,
                    speed = download.downloadSpeed,
                    eta = download.etaSeconds
                )
            )

            if (engineStatus == DownloadStatus.COMPLETE || engineStatus == DownloadStatus.STOPPED) break

            delay(500)
        }
    }

    override suspend fun pause(gid: String): Result<Unit> = rpcClient.pause(gid).map {}

    override suspend fun resume(gid: String): Result<Unit> = rpcClient.unpause(gid).map {}

    override suspend fun stop(gid: String): Result<Unit> = rpcClient.remove(gid).map {}

    override suspend fun queryAll(): List<DownloadProgress> {
        val active = rpcClient.tellActive().getOrElse { emptyList() }
        val waiting = rpcClient.tellWaiting().getOrElse { emptyList() }
        val stopped = rpcClient.tellStopped(num = 20).getOrElse { emptyList() }

        return (active + waiting + stopped).map { download ->
            val engineStatus = when {
                download.isComplete -> DownloadStatus.COMPLETE
                download.isPaused -> DownloadStatus.PAUSED
                download.isActive -> DownloadStatus.ACTIVE
                download.status == com.medownloader.data.model.DownloadStatus.REMOVED -> DownloadStatus.STOPPED
                else -> DownloadStatus.QUEUED
            }
            DownloadProgress(
                gid = download.gid,
                status = engineStatus,
                filename = download.filename,
                downloadedBytes = download.completedLength,
                totalBytes = download.totalLength,
                speed = download.downloadSpeed,
                eta = download.etaSeconds
            )
        }
    }

    override suspend fun isHealthy(): Boolean {
        if (processManager.processState.value != Aria2ProcessManager.ProcessState.Running) return false
        return rpcClient.getVersion().isSuccess
    }

    /**
     * Observe all downloads. When [eventDriven] is true, WebSocket push events from aria2c
     * trigger an immediate status query so the UI reacts within ~100 ms of a state change.
     * A 1-second polling fallback runs in parallel so the UI stays live even if the WebSocket
     * drops. The WebSocket flow auto-reconnects on failure via [retryWhen].
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeAll(eventDriven: Boolean = true): Flow<List<DownloadProgress>> {
        val pollingFlow = flow {
            while (true) {
                try { emit(queryAll()) } catch (_: Exception) { /* swallow, polling will retry */ }
                delay(1000)
            }
        }

        if (!eventDriven) return pollingFlow

        val eventsFlow = rpcClient.observeDownloadEvents()
            .retryWhen { cause, attempt ->
                if (cause is CancellationException) return@retryWhen false
                Log.w(TAG, "WebSocket event stream failed (attempt $attempt), reconnecting: ${cause.message}")
                delay(backoffMs((attempt + 1).toInt()))
                true
            }
            .flatMapLatest {
                flow {
                    try { emit(queryAll()) } catch (_: Exception) { }
                }
            }
            .catch { /* if events flow dies entirely, polling keeps things alive */ }

        return merge(pollingFlow, eventsFlow)
    }
}
