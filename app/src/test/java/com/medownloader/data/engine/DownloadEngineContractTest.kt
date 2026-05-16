package com.medownloader.data.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * DownloadEngine contract tests using pure fake implementations.
 * Also covers the fallback-chain logic that DownloadRepositoryImpl applies.
 */
class DownloadEngineContractTest {

    // -------------------------------------------------------------------------
    // Fakes
    // -------------------------------------------------------------------------

    private class SuccessEngine(
        private val gid: String = "gid-ok",
        private val filename: String = "file.mp4",
        private val totalBytes: Long = 1_000L
    ) : DownloadEngine {
        override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> = flow {
            emit(DownloadProgress(gid, DownloadStatus.ACTIVE, filename, 500L, totalBytes, 100L, 5L))
            emit(DownloadProgress(gid, DownloadStatus.COMPLETE, filename, totalBytes, totalBytes, 0L, 0L))
        }
        override suspend fun pause(gid: String) = Result.success(Unit)
        override suspend fun resume(gid: String) = Result.success(Unit)
        override suspend fun stop(gid: String) = Result.success(Unit)
        override suspend fun queryAll() = emptyList<DownloadProgress>()
        override suspend fun isHealthy() = true
    }

    private class ErrorEngine(private val message: String = "extraction failed") : DownloadEngine {
        override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> = flow {
            emit(
                DownloadProgress(
                    gid = options.url,
                    status = DownloadStatus.ERROR,
                    filename = options.filename ?: options.url,
                    downloadedBytes = 0L,
                    totalBytes = 0L,
                    speed = 0L,
                    eta = 0L,
                    errorMessage = message
                )
            )
        }
        override suspend fun pause(gid: String) = Result.failure<Unit>(UnsupportedOperationException())
        override suspend fun resume(gid: String) = Result.failure<Unit>(UnsupportedOperationException())
        override suspend fun stop(gid: String) = Result.success(Unit)
        override suspend fun queryAll() = emptyList<DownloadProgress>()
        override suspend fun isHealthy() = false
    }

    // -------------------------------------------------------------------------
    // Contract: download() flow
    // -------------------------------------------------------------------------

    @Test
    fun `successful engine emits ACTIVE then COMPLETE`() = runBlocking {
        val events = SuccessEngine().download(DownloadOptions("https://example.com/file.mp4", protocolType = EngineType.ARIA2C)).toList()
        assertEquals(2, events.size)
        assertEquals(DownloadStatus.ACTIVE, events[0].status)
        assertEquals(DownloadStatus.COMPLETE, events[1].status)
    }

    @Test
    fun `successful engine final event has isComplete true`() = runBlocking {
        val last = SuccessEngine().download(DownloadOptions("https://example.com/file.mp4", protocolType = EngineType.ARIA2C)).toList().last()
        assertTrue(last.isComplete)
    }

    @Test
    fun `error engine emits single ERROR event with message`() = runBlocking {
        val events = ErrorEngine("network error").download(DownloadOptions("https://example.com/file.mp4", protocolType = EngineType.ARIA2C)).toList()
        assertEquals(1, events.size)
        assertEquals(DownloadStatus.ERROR, events[0].status)
        assertEquals("network error", events[0].errorMessage)
    }

    @Test
    fun `progress percent stays within 0-100`() = runBlocking {
        SuccessEngine(totalBytes = 1000L)
            .download(DownloadOptions("https://example.com/file.mp4", protocolType = EngineType.ARIA2C))
            .toList()
            .forEach { p -> assertTrue(p.progressPercent in 0..100) }
    }

    @Test
    fun `gid is stable across all events from same download`() = runBlocking {
        val gids = SuccessEngine(gid = "stable-gid")
            .download(DownloadOptions("https://example.com/file.mp4", protocolType = EngineType.ARIA2C))
            .toList()
            .map { it.gid }
            .toSet()
        assertEquals(1, gids.size)
    }

    // -------------------------------------------------------------------------
    // Contract: isHealthy()
    // -------------------------------------------------------------------------

    @Test
    fun `healthy engine returns true`() = runBlocking { assertTrue(SuccessEngine().isHealthy()) }

    @Test
    fun `unhealthy engine returns false`() = runBlocking { assertFalse(ErrorEngine().isHealthy()) }

    // -------------------------------------------------------------------------
    // Contract: stop() always succeeds
    // -------------------------------------------------------------------------

    @Test
    fun `stop returns success on healthy engine`() = runBlocking {
        assertTrue(SuccessEngine().stop("any-gid").isSuccess)
    }

    @Test
    fun `stop returns success on error engine`() = runBlocking {
        assertTrue(ErrorEngine().stop("any-gid").isSuccess)
    }

    // -------------------------------------------------------------------------
    // Fallback chain simulation (mirrors DownloadRepositoryImpl logic)
    // -------------------------------------------------------------------------

    private suspend fun downloadWithFallback(
        primary: DownloadEngine,
        fallback: DownloadEngine,
        options: DownloadOptions
    ): List<DownloadProgress> {
        val results = mutableListOf<DownloadProgress>()
        var usedFallback = false
        primary.download(options).collect { progress ->
            if (!usedFallback && progress.status == DownloadStatus.ERROR) {
                usedFallback = true
                fallback.download(options).collect { fp -> results.add(fp) }
                return@collect
            }
            results.add(progress)
        }
        return results
    }

    @Test
    fun `fallback chain - primary error triggers fallback`() = runBlocking {
        val results = downloadWithFallback(
            primary = ErrorEngine("yt-dlp failed"),
            fallback = SuccessEngine(gid = "fallback-gid"),
            options = DownloadOptions("https://youtube.com/watch?v=abc", protocolType = EngineType.ARIA2C)
        )
        assertTrue(results.isNotEmpty())
        assertTrue(results.last().isComplete)
        assertFalse(results.any { it.status == DownloadStatus.ERROR })
    }

    @Test
    fun `fallback chain - primary success does not invoke fallback`() = runBlocking {
        val results = downloadWithFallback(
            primary = SuccessEngine(gid = "primary-gid"),
            fallback = ErrorEngine("should not be called"),
            options = DownloadOptions("https://youtube.com/watch?v=abc", protocolType = EngineType.ARIA2C)
        )
        assertTrue(results.last().isComplete)
        assertTrue(results.all { it.gid == "primary-gid" })
    }

    @Test
    fun `fallback chain - both engines fail produces error`() = runBlocking {
        val results = downloadWithFallback(
            primary = ErrorEngine("primary failed"),
            fallback = ErrorEngine("fallback also failed"),
            options = DownloadOptions("https://youtube.com/watch?v=abc", protocolType = EngineType.ARIA2C)
        )
        assertTrue(results.isNotEmpty())
        assertEquals(DownloadStatus.ERROR, results.last().status)
    }
}
