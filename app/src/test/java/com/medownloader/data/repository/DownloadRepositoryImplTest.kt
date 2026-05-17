package com.medownloader.data.repository

import com.medownloader.data.engine.*
import com.medownloader.data.model.Aria2GlobalStat
import com.medownloader.data.model.DownloadHistoryEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger as JuAtomicInteger

/**
 * Regression tests for the bug class "downloads / pause / resume / remove
 * silently misbehave because the repository's internal id mapping is wrong".
 *
 * These exercise [DownloadRepositoryImpl] directly, with fakes that have no
 * Android, OkHttp, or aria2 runtime dependency. The fake engines mimic the
 * real ones in the dimension the bugs live in: the engine emits
 * [DownloadProgress] with its own gid (aria2's hex gid in the real client),
 * which is **not** the URL the repository called add() with.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DownloadRepositoryImplTest {

    // ------------------------------------------------------------------------
    // Fakes
    // ------------------------------------------------------------------------

    /**
     * Engine fake that, like the real Aria2Engine, emits progress with a
     * **different gid** from the URL the repository handed it. This is the
     * exact shape the production gid-mismatch bug relies on.
     */
    private class HexGidEngine(
        private val hexGid: String = "2089b05c1a5fb7e0",
        // When non-null, holds emissions until the test closes the channel.
        private val gate: Channel<DownloadProgress>? = null,
        // Records pause/resume/stop calls so tests can assert routing.
        val pauseCalls: MutableList<String> = mutableListOf(),
        val resumeCalls: MutableList<String> = mutableListOf(),
        val stopCalls: MutableList<String> = mutableListOf()
    ) : DownloadEngine {

        // Records the URL we last received so the repo's queue / pool can be inspected.
        val downloadInvocations = JuAtomicInteger(0)

        override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> {
            downloadInvocations.incrementAndGet()
            return if (gate != null) {
                gate.receiveAsFlow()
            } else {
                flow {
                    emit(progress(options, DownloadStatus.ACTIVE, 50L, 100L))
                    emit(progress(options, DownloadStatus.COMPLETE, 100L, 100L))
                }
            }
        }

        private fun progress(opts: DownloadOptions, s: DownloadStatus, dl: Long, total: Long) =
            DownloadProgress(
                gid = hexGid,
                status = s,
                filename = opts.filename ?: opts.url,
                downloadedBytes = dl,
                totalBytes = total,
                speed = 0,
                eta = 0
            )

        override suspend fun pause(gid: String): Result<Unit> {
            pauseCalls.add(gid); return Result.success(Unit)
        }
        override suspend fun resume(gid: String): Result<Unit> {
            resumeCalls.add(gid); return Result.success(Unit)
        }
        override suspend fun stop(gid: String): Result<Unit> {
            stopCalls.add(gid); return Result.success(Unit)
        }
        override suspend fun queryAll(): List<DownloadProgress> = emptyList()
        override suspend fun isHealthy(): Boolean = true
    }

    private class NoopProcessController : EngineProcessController {
        override fun isRunning(): Boolean = true
        override suspend fun start(): Result<Unit> = Result.success(Unit)
        override suspend fun stop() {}
    }

    private class NoopRpcOps : DownloadRpcOps {
        override suspend fun shutdown(): Result<String> = Result.success("ok")
        override suspend fun getGlobalStat(): Result<Aria2GlobalStat> =
            Result.success(Aria2GlobalStat())
        override suspend fun changeGlobalOption(options: Map<String, String>): Result<String> =
            Result.success("ok")
    }

    private class NoopHistorySink(
        val appended: MutableList<DownloadHistoryEntry> = mutableListOf()
    ) : DownloadHistorySink {
        override suspend fun append(entry: DownloadHistoryEntry) { appended.add(entry) }
    }

    private class InMemoryQueueStore(
        private val state: AtomicReference<List<QueuedDownload>> = AtomicReference(emptyList())
    ) : DownloadQueueStore {
        override suspend fun save(items: List<QueuedDownload>) { state.set(items) }
        override suspend fun snapshot(): List<QueuedDownload> = state.get()
        override suspend fun clear() { state.set(emptyList()) }
    }

    /** Disk space probe that always reports plenty of space. */
    private val infiniteDisk = object : com.medownloader.util.DiskSpaceProbe {
        override fun availableBytes(path: String): Long = Long.MAX_VALUE
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private fun newRepo(
        primary: DownloadEngine = HexGidEngine(hexGid = "primary-hex"),
        fallback: DownloadEngine = HexGidEngine(hexGid = "fallback-hex"),
        process: EngineProcessController = NoopProcessController(),
        rpc: DownloadRpcOps = NoopRpcOps(),
        history: DownloadHistorySink = NoopHistorySink(),
        queue: DownloadQueueStore = InMemoryQueueStore()
    ): DownloadRepositoryImpl = DownloadRepositoryImpl(
        primaryEngine = primary,
        fallbackEngine = fallback,
        historyRepository = history,
        queueRepository = queue,
        rpcClient = rpc,
        processManager = process,
        diskSpaceProbe = infiniteDisk
    )

    /** Wait until [predicate] is true, polling the repo's flow. Times out so a hung test fails fast. */
    private suspend fun DownloadRepositoryImpl.waitFor(
        timeoutMs: Long = 2000L,
        predicate: (List<DownloadProgress>) -> Boolean
    ): List<DownloadProgress> = withTimeout(timeoutMs) {
        observeAllDownloads().first { predicate(it) }
    }

    // ------------------------------------------------------------------------
    // Bug 1: registry key consistency
    // ------------------------------------------------------------------------

    /**
     * The original bug: addDownload registered a QUEUED placeholder under the URL key,
     * but Aria2Engine emitted progress with aria2's hex gid, so the registry ended up
     * with **two entries** for one logical download — the URL-keyed ghost plus the
     * hex-keyed live entry.
     *
     * After the fix, [observeAllDownloads] must show exactly one entry per logical
     * download keyed by a stable id (the URL, not aria2's hex gid).
     *
     * To reliably trigger the QUEUED-placeholder path we need to fill the pool
     * first; the cap defaults to 3.
     */
    @Test
    fun `engine emitting a different gid does not duplicate the registry entry`(): Unit = runBlocking {
        // Fill the pool with 3 long-running downloads so the 4th gets queued.
        val gates = (0..3).map { Channel<DownloadProgress>(capacity = Channel.UNLIMITED) }
        val byUrl = HashMap<String, Channel<DownloadProgress>>()
        val hexGidByUrl = HashMap<String, String>()
        val engine = object : DownloadEngine {
            override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> {
                val gate = byUrl[options.url] ?: error("no gate for ${options.url}")
                return gate.receiveAsFlow()
            }
            override suspend fun pause(gid: String) = Result.success(Unit)
            override suspend fun resume(gid: String) = Result.success(Unit)
            override suspend fun stop(gid: String) = Result.success(Unit)
            override suspend fun queryAll(): List<DownloadProgress> = emptyList()
            override suspend fun isHealthy(): Boolean = true
        }
        val repo = newRepo(fallback = engine)

        val urls = (1..4).map { "https://example.com/file-$it.zip" }
        urls.forEachIndexed { i, u ->
            byUrl[u] = gates[i]
            hexGidByUrl[u] = "hex-$i"
        }

        urls.forEach { repo.addDownload(it) }

        // Push ACTIVE on the first 3 with their distinctive hex gids, and free
        // the 4th slot by completing #1.
        for (i in 0..2) {
            gates[i].send(DownloadProgress("hex-$i", DownloadStatus.ACTIVE, "f", 10, 100, 0, 0))
        }
        repo.waitFor { it.count { p -> p.status == DownloadStatus.ACTIVE } == 3 }

        gates[0].send(DownloadProgress("hex-0", DownloadStatus.COMPLETE, "f", 100, 100, 0, 0))
        gates[0].close()
        // url[3] should drain in. Push ACTIVE on it.
        gates[3].send(DownloadProgress("hex-3", DownloadStatus.ACTIVE, "f", 5, 100, 0, 0))

        val list = repo.waitFor { dls ->
            dls.any { it.status == DownloadStatus.ACTIVE && (it.gid == urls[3] || it.gid == "hex-3") }
        }

        // The bug: with broken code, urls[3] sits in the registry as a URL-keyed
        // QUEUED placeholder AND hex-3 lands as a separate ACTIVE row. After the
        // fix, only one row exists.
        val rowsForLastDownload = list.filter {
            it.gid == urls[3] || it.gid == "hex-3"
        }
        assertEquals(
            "expected exactly one registry row per logical download, got: $rowsForLastDownload",
            1,
            rowsForLastDownload.size
        )

        gates.forEach { runCatching { it.close() } }
    }

    /**
     * The UI uses [DownloadProgress.gid] as the row key. If that key is unstable across
     * the lifetime of one download (URL while QUEUED → hex once active), the UI reorders
     * or duplicates rows. The repo must normalise emissions onto a single id.
     *
     * To trigger the QUEUED→ACTIVE transition, we again need to fill the pool first.
     */
    @Test
    fun `gid in UI-visible flow is stable across queued-to-active transition`(): Unit = runBlocking {
        val gates = (0..3).map { Channel<DownloadProgress>(capacity = Channel.UNLIMITED) }
        val byUrl = HashMap<String, Channel<DownloadProgress>>()
        val engine = object : DownloadEngine {
            override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> =
                byUrl.getValue(options.url).receiveAsFlow()
            override suspend fun pause(gid: String) = Result.success(Unit)
            override suspend fun resume(gid: String) = Result.success(Unit)
            override suspend fun stop(gid: String) = Result.success(Unit)
            override suspend fun queryAll() = emptyList<DownloadProgress>()
            override suspend fun isHealthy() = true
        }
        val repo = newRepo(fallback = engine)

        val urls = (1..4).map { "https://example.com/file-$it.zip" }
        urls.forEachIndexed { i, u -> byUrl[u] = gates[i] }
        urls.forEach { repo.addDownload(it) }

        // Capture the gid for url[3] while it is QUEUED.
        for (i in 0..2) {
            gates[i].send(DownloadProgress("hex-$i", DownloadStatus.ACTIVE, "f", 10, 100, 0, 0))
        }
        val queuedSnapshot = repo.waitFor { dls ->
            dls.any { it.status == DownloadStatus.QUEUED }
        }
        val queuedGid = queuedSnapshot.single { it.status == DownloadStatus.QUEUED }.gid

        // Free a slot, then ACTIVE comes in for url[3].
        gates[0].send(DownloadProgress("hex-0", DownloadStatus.COMPLETE, "f", 100, 100, 0, 0))
        gates[0].close()
        gates[3].send(DownloadProgress("hex-3", DownloadStatus.ACTIVE, "f", 5, 100, 0, 0))

        // After the transition, the row for url[3] (whatever its gid is now) must
        // still be findable by the same gid that the UI has been holding.
        val active = repo.waitFor { dls ->
            dls.any { it.gid == queuedGid && it.status == DownloadStatus.ACTIVE }
        }
        assertNotNull(
            "queued gid $queuedGid disappeared after transitioning to ACTIVE (UI rows would orphan)",
            active.firstOrNull { it.gid == queuedGid && it.status == DownloadStatus.ACTIVE }
        )

        gates.forEach { runCatching { it.close() } }
    }

    // ------------------------------------------------------------------------
    // Bug 2: pause/resume must work for downloads that were ever queued
    // ------------------------------------------------------------------------

    /**
     * The original bug: when the pool was full, addDownload wrote a QUEUED
     * placeholder under the URL key. When the engine later started and emitted
     * progress with aria2's hex gid, the registry kept BOTH the URL placeholder
     * AND a new hex-keyed row.
     *
     * The user clicks on the (still-visible) URL-keyed row. The repo looks up
     * activeDownloads[url] — which IS keyed by URL in the broken version — and
     * routes through fallbackEngine.pause(url). aria2 has no such gid, so the
     * call returns failure silently and the user sees nothing happen.
     *
     * After the fix, the placeholder is replaced (not duplicated) when the
     * engine starts, so the only row the user can click has the right gid for
     * pausing.
     */
    @Test
    fun `pause works on a download that was queued before becoming active`(): Unit = runBlocking {
        val gates = (0..3).map { Channel<DownloadProgress>(capacity = Channel.UNLIMITED) }
        val byUrl = HashMap<String, Channel<DownloadProgress>>()
        val pauseCalls = mutableListOf<String>()
        val engine = object : DownloadEngine {
            override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> =
                byUrl.getValue(options.url).receiveAsFlow()
            override suspend fun pause(gid: String): Result<Unit> {
                pauseCalls.add(gid)
                // Mimic real aria2: only succeeds for known hex gids.
                return if (gid.startsWith("hex-")) Result.success(Unit)
                else Result.failure(IllegalArgumentException("aria2: no such gid: $gid"))
            }
            override suspend fun resume(gid: String) = Result.success(Unit)
            override suspend fun stop(gid: String) = Result.success(Unit)
            override suspend fun queryAll() = emptyList<DownloadProgress>()
            override suspend fun isHealthy() = true
        }
        val repo = newRepo(fallback = engine)

        val urls = (1..4).map { "https://example.com/file-$it.zip" }
        urls.forEachIndexed { i, u -> byUrl[u] = gates[i] }
        urls.forEach { repo.addDownload(it) }

        // Fill the pool with the first three.
        for (i in 0..2) {
            gates[i].send(DownloadProgress("hex-$i", DownloadStatus.ACTIVE, "f", 10, 100, 0, 0))
        }
        repo.waitFor { it.any { p -> p.status == DownloadStatus.QUEUED } }

        // Free a slot, drain url[3] into the pool.
        gates[0].send(DownloadProgress("hex-0", DownloadStatus.COMPLETE, "f", 100, 100, 0, 0))
        gates[0].close()
        gates[3].send(DownloadProgress("hex-3", DownloadStatus.ACTIVE, "f", 5, 100, 0, 0))
        val list = repo.waitFor { dls ->
            dls.any { it.gid == urls[3] && it.status == DownloadStatus.ACTIVE } ||
                dls.any { it.gid == "hex-3" && it.status == DownloadStatus.ACTIVE }
        }

        // The UI calls pause on whichever gid the row exposes. After the fix
        // there is only ONE row for url[3] and pausing it must succeed.
        val rowsForUrl3 = list.filter { it.gid == urls[3] || it.gid == "hex-3" }
        assertEquals(
            "expected exactly one row per logical download after queue drain, got: $rowsForUrl3",
            1,
            rowsForUrl3.size
        )
        val target = rowsForUrl3.single()
        val result = repo.pauseDownload(target.gid)
        assertTrue(
            "pauseDownload on the (only) row for url[3] failed: $result",
            result.isSuccess
        )
        // pause must have routed via the real aria2 gid, not the URL.
        assertTrue(
            "engine.pause was called with non-aria2 gid: $pauseCalls",
            pauseCalls.all { it.startsWith("hex-") }
        )

        gates.forEach { runCatching { it.close() } }
    }

    /**
     * The original bug: removeDownload(hexGid) tried activeDownloads.remove(hexGid),
     * which returned null because entries were keyed by URL. The job was never
     * cancelled — it kept running, polling, and emitting events even after remove()
     * returned. After the fix the in-flight job is cancelled, the entry leaves the
     * registry, and engine.stop is called with the real aria2 gid.
     *
     * Strong signal: after remove(), no new emissions from the engine reach the
     * registry. We push a fresh ACTIVE event AFTER remove() and assert it does
     * NOT appear in the flow. That can only be true if the job was cancelled.
     */
    @Test
    fun `remove cancels the active job - new engine events after remove are ignored`(): Unit = runBlocking {
        val url = "https://example.com/file.zip"
        val gate = Channel<DownloadProgress>(capacity = Channel.UNLIMITED)
        val engine = HexGidEngine(hexGid = "hex-4", gate = gate)
        val repo = newRepo(fallback = engine)

        repo.addDownload(url)
        gate.send(DownloadProgress("hex-4", DownloadStatus.ACTIVE, "f", 10, 100, 0, 0))
        val list = repo.waitFor { it.any { p -> p.status == DownloadStatus.ACTIVE } }
        val uiGid = list.single().gid

        val result = repo.removeDownload(uiGid)
        assertTrue("removeDownload returned $result", result.isSuccess)
        assertEquals(
            "engine.stop was not called with the real aria2 gid",
            listOf("hex-4"),
            engine.stopCalls
        )

        // Push another event AFTER remove. With a working remove the job is
        // cancelled and this event never lands. With the broken version the
        // collect coroutine is still alive and the event lands as a new ACTIVE
        // row in the registry.
        gate.send(DownloadProgress("hex-4", DownloadStatus.ACTIVE, "f", 90, 100, 0, 0))

        // Give the (potentially still-alive) collector a chance to write through.
        kotlinx.coroutines.delay(100)

        val finalList = repo.observeAllDownloads().first()
        assertTrue(
            "removed download came back via post-remove engine event: $finalList",
            finalList.none { it.gid == uiGid || it.gid == "hex-4" }
        )

        gate.close()
    }

    // ------------------------------------------------------------------------
    // Pool / queue interaction with id mapping
    // ------------------------------------------------------------------------

    /**
     * If the cap is 1, the second add must show up as QUEUED with the right id, and
     * that QUEUED placeholder must be **replaced** (not duplicated) once the real
     * download starts.
     */
    @Test
    fun `queued placeholder is overwritten exactly once when the slot opens`(): Unit = runBlocking {
        // We can't easily change the repo's max-concurrent without a settings flow,
        // but we can use long-running gated downloads to keep the pool busy. The
        // cap defaults to 3, so we need 4 downloads to exercise the queue path.
        val gates = (1..4).map { Channel<DownloadProgress>(capacity = Channel.UNLIMITED) }
        val engines = gates.mapIndexed { i, g ->
            HexGidEngine(hexGid = "hex-pool-$i", gate = g)
        }
        // We can't inject 4 different engines, so use a single engine that picks
        // the gate by URL.
        val byUrl = HashMap<String, Channel<DownloadProgress>>()
        val engine = object : DownloadEngine {
            val invocations = JuAtomicInteger(0)
            override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> {
                invocations.incrementAndGet()
                val gate = byUrl[options.url] ?: error("no gate for ${options.url}")
                return gate.receiveAsFlow()
            }
            override suspend fun pause(gid: String) = Result.success(Unit)
            override suspend fun resume(gid: String) = Result.success(Unit)
            override suspend fun stop(gid: String) = Result.success(Unit)
            override suspend fun queryAll(): List<DownloadProgress> = emptyList()
            override suspend fun isHealthy(): Boolean = true
        }
        val repo = newRepo(fallback = engine)

        // Add 4 downloads (cap is 3 by default → the 4th queues).
        val urls = (1..4).map { "https://example.com/file-$it.zip" }
        urls.forEachIndexed { i, u -> byUrl[u] = gates[i] }
        urls.forEach { repo.addDownload(it) }

        // The 4th URL should show up as QUEUED.
        val withQueued = repo.waitFor { dls ->
            dls.any { it.gid == urls[3] && it.status == DownloadStatus.QUEUED }
        }
        assertEquals(
            "expected one QUEUED row for the over-cap download",
            1,
            withQueued.count { it.status == DownloadStatus.QUEUED && it.gid == urls[3] }
        )

        // Free a slot by completing the first download.
        gates[0].send(DownloadProgress("hex-pool-0", DownloadStatus.COMPLETE, "f", 100, 100, 0, 0))
        gates[0].close()

        // Now urls[3] should drain into the pool and become ACTIVE. The QUEUED
        // placeholder must be **overwritten**, not stacked alongside the active row.
        gates[3].send(DownloadProgress("hex-pool-3", DownloadStatus.ACTIVE, "f", 10, 100, 0, 0))
        val drained = repo.waitFor { dls ->
            dls.any { it.gid == urls[3] && it.status == DownloadStatus.ACTIVE }
        }
        val rowsForLastUrl = drained.filter { it.gid == urls[3] }
        assertEquals(
            "QUEUED placeholder duplicated instead of being replaced: $rowsForLastUrl",
            1,
            rowsForLastUrl.size
        )

        // Cleanup gates so the test scope finishes.
        gates.drop(1).forEach { it.close() }
    }

    // ------------------------------------------------------------------------
    // History recording must not double-record when gid normalises
    // ------------------------------------------------------------------------

    @Test
    fun `terminal events are recorded to history exactly once even when queued first`(): Unit = runBlocking {
        val gates = (0..3).map { Channel<DownloadProgress>(capacity = Channel.UNLIMITED) }
        val byUrl = HashMap<String, Channel<DownloadProgress>>()
        val engine = object : DownloadEngine {
            override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> =
                byUrl.getValue(options.url).receiveAsFlow()
            override suspend fun pause(gid: String) = Result.success(Unit)
            override suspend fun resume(gid: String) = Result.success(Unit)
            override suspend fun stop(gid: String) = Result.success(Unit)
            override suspend fun queryAll() = emptyList<DownloadProgress>()
            override suspend fun isHealthy() = true
        }
        val history = NoopHistorySink()
        val repo = newRepo(fallback = engine, history = history)

        val urls = (1..4).map { "https://example.com/file-$it.zip" }
        urls.forEachIndexed { i, u -> byUrl[u] = gates[i] }
        urls.forEach { repo.addDownload(it) }

        for (i in 0..2) {
            gates[i].send(DownloadProgress("hex-$i", DownloadStatus.ACTIVE, "f", 10, 100, 0, 0))
        }
        repo.waitFor { it.any { p -> p.status == DownloadStatus.QUEUED } }

        gates[0].send(DownloadProgress("hex-0", DownloadStatus.COMPLETE, "f", 100, 100, 0, 0))
        gates[0].close()
        gates[3].send(DownloadProgress("hex-3", DownloadStatus.COMPLETE, "f", 100, 100, 0, 0))
        gates[3].close()

        // Wait for both completions to settle.
        repo.waitFor { dls -> dls.count { it.status == DownloadStatus.COMPLETE } >= 2 }
        kotlinx.coroutines.delay(100) // allow history append to flush

        // Two distinct downloads completed; history must have exactly two entries.
        val urlsRecorded = history.appended.map { it.url }.toSet()
        assertEquals(
            "expected one history entry per logical download, got: ${history.appended}",
            2,
            history.appended.size
        )
        assertTrue(
            "history should record both completed urls: $urlsRecorded",
            urls[0] in urlsRecorded && urls[3] in urlsRecorded
        )

        gates.forEach { runCatching { it.close() } }
    }
}
