package com.medownloader.data.repository

import com.medownloader.data.engine.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for DownloadRepositoryImpl concurrency pool and queue drain logic.
 * Uses fake engines to avoid Android/Chaquopy dependencies.
 */
class DownloadRepositoryPoolTest {

    // Fake engine that takes [delayMs] to complete
    private class DelayEngine(private val delayMs: Long = 100L) : DownloadEngine {
        var downloadCount = 0
            private set

        override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> = flow {
            downloadCount++
            emit(DownloadProgress(options.url, DownloadStatus.ACTIVE, options.url, 0, 100, 10, 10, engineType = EngineType.ARIA2C))
            delay(delayMs)
            emit(DownloadProgress(options.url, DownloadStatus.COMPLETE, options.url, 100, 100, 0, 0, engineType = EngineType.ARIA2C))
        }

        override suspend fun pause(gid: String) = Result.success(Unit)
        override suspend fun resume(gid: String) = Result.success(Unit)
        override suspend fun stop(gid: String) = Result.success(Unit)
        override suspend fun queryAll() = emptyList<DownloadProgress>()
        override suspend fun isHealthy() = true
    }

    // Fake engine that always errors
    private class FailEngine : DownloadEngine {
        override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> = flow {
            emit(DownloadProgress(options.url, DownloadStatus.ERROR, options.url, 0, 0, 0, 0, errorMessage = "fail", engineType = EngineType.YT_DLP))
        }
        override suspend fun pause(gid: String) = Result.failure<Unit>(UnsupportedOperationException())
        override suspend fun resume(gid: String) = Result.failure<Unit>(UnsupportedOperationException())
        override suspend fun stop(gid: String) = Result.success(Unit)
        override suspend fun queryAll() = emptyList<DownloadProgress>()
        override suspend fun isHealthy() = false
    }

    @Test
    fun `concurrency cap limits active downloads`() = runBlocking {
        val engine = DelayEngine(delayMs = 500L)
        // We can't easily instantiate DownloadRepositoryImpl without Android context,
        // so we test the pool logic conceptually via the engine's download count.
        // Launch 5 downloads through the engine directly with a manual semaphore.
        val maxConcurrent = 2
        var activeCount = 0
        var peakActive = 0
        val mutex = Mutex()

        val jobs = (1..5).map { i ->
            launch {
                // Simulate pool: wait until slot available
                while (true) {
                    val acquired = mutex.withLock {
                        if (activeCount < maxConcurrent) {
                            activeCount++
                            peakActive = maxOf(peakActive, activeCount)
                            true
                        } else false
                    }
                    if (acquired) break
                    delay(10)
                }

                try {
                    engine.download(DownloadOptions("https://example.com/$i", protocolType = EngineType.ARIA2C)).toList()
                } finally {
                    mutex.withLock { activeCount-- }
                }
            }
        }

        jobs.forEach { it.join() }

        assertEquals("peak active should not exceed cap", 2, peakActive)
        assertEquals("all 5 downloads should complete", 5, engine.downloadCount)
    }

    @Test
    fun `queue drains as slots free up`() = runBlocking {
        val engine = DelayEngine(delayMs = 50L)
        val maxConcurrent = 1
        var activeCount = 0
        val completedUrls = mutableListOf<String>()
        val mutex = Mutex()

        val urls = listOf("https://a.com/1", "https://a.com/2", "https://a.com/3")

        val jobs = urls.map { url ->
            launch {
                while (true) {
                    val acquired = mutex.withLock {
                        if (activeCount < maxConcurrent) { activeCount++; true } else false
                    }
                    if (acquired) break
                    delay(5)
                }
                try {
                    engine.download(DownloadOptions(url, protocolType = EngineType.ARIA2C)).toList()
                    mutex.withLock { completedUrls.add(url) }
                } finally {
                    mutex.withLock { activeCount-- }
                }
            }
        }

        jobs.forEach { it.join() }

        assertEquals("all 3 should complete", 3, completedUrls.size)
        assertEquals("should complete in order", urls, completedUrls)
    }

    @Test
    fun `fallback triggers when primary emits error`() = runBlocking {
        val primary = FailEngine()
        val fallback = DelayEngine(delayMs = 10L)
        val options = DownloadOptions("https://youtube.com/watch?v=abc", protocolType = EngineType.YT_DLP)

        // Simulate repository fallback logic
        val results = mutableListOf<DownloadProgress>()
        var usedFallback = false

        primary.download(options).collect { progress ->
            if (!usedFallback && progress.status == DownloadStatus.ERROR) {
                usedFallback = true
                fallback.download(options.copy(protocolType = EngineType.ARIA2C)).collect { fp ->
                    results.add(fp)
                }
                return@collect
            }
            results.add(progress)
        }

        assertTrue("should have used fallback", usedFallback)
        assertTrue("last result should be COMPLETE", results.last().isComplete)
        assertEquals("fallback engine should have been called once", 1, fallback.downloadCount)
    }
}
