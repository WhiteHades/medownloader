package com.medownloader.data.repository

import com.medownloader.data.model.Aria2GlobalStat
import com.medownloader.data.model.DownloadHistoryEntry

/**
 * Narrow seams for the dependencies [DownloadRepositoryImpl] uses, so the
 * repository can be unit-tested with fakes that have no Android or aria2
 * runtime dependency.
 *
 * The production classes ([com.medownloader.data.source.Aria2ProcessManager],
 * [com.medownloader.data.Aria2RpcClient], [DownloadHistoryRepository],
 * [DownloadQueueRepository]) already satisfy these contracts via their
 * existing API. The interfaces themselves are intentionally tiny — only what
 * the repository actually calls — so tests don't have to drag in DataStore,
 * OkHttp, or a live aria2c process to verify gid routing, queue draining,
 * and lifecycle behavior.
 */

/**
 * What [DownloadRepositoryImpl] needs from the aria2c process manager:
 * a way to check whether aria2c is up, start it on demand, and stop it on
 * shutdown.
 *
 * Deliberately free of the production class's sealed-class state model so
 * tests don't have to reproduce it.
 */
interface EngineProcessController {
    /** True when aria2c is up and serving JSON-RPC. */
    fun isRunning(): Boolean
    suspend fun start(): Result<Unit>
    suspend fun stop()
}

/** What [DownloadRepositoryImpl] needs from [com.medownloader.data.Aria2RpcClient]. */
interface DownloadRpcOps {
    suspend fun shutdown(): Result<String>
    suspend fun getGlobalStat(): Result<Aria2GlobalStat>
    suspend fun changeGlobalOption(options: Map<String, String>): Result<String>
}

/** Persisted history sink used to record terminal download events. */
interface DownloadHistorySink {
    suspend fun append(entry: DownloadHistoryEntry)
}

/** Persisted queue store used to survive process death with a pending queue. */
interface DownloadQueueStore {
    suspend fun save(items: List<QueuedDownload>)
    suspend fun snapshot(): List<QueuedDownload>
    suspend fun clear()
}
