package com.medownloader.data.engine

import org.junit.Assert.*
import org.junit.Test

class DownloadProgressTest {

    @Test
    fun `progressPercent is zero when totalBytes is zero`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 50,
            totalBytes = 0,
            speed = 1000,
            eta = 60
        )
        assertEquals(0, progress.progressPercent)
    }

    @Test
    fun `progressPercent calculates correctly at half`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 500,
            totalBytes = 1000,
            speed = 1000,
            eta = 60
        )
        assertEquals(50, progress.progressPercent)
    }

    @Test
    fun `progressPercent clamps to 100`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 1500,
            totalBytes = 1000,
            speed = 1000,
            eta = 60
        )
        assertEquals(100, progress.progressPercent)
    }

    @Test
    fun `progress is zero when totalBytes is zero`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 100,
            totalBytes = 0,
            speed = 1000,
            eta = 60
        )
        assertEquals(0f, progress.progress, 0.001f)
    }

    @Test
    fun `progress calculates correctly at 25 percent`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 250,
            totalBytes = 1000,
            speed = 1000,
            eta = 60
        )
        assertEquals(0.25f, progress.progress, 0.001f)
    }

    @Test
    fun `completed status has isComplete true`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.COMPLETE,
            filename = "test.mp4",
            downloadedBytes = 1000,
            totalBytes = 1000,
            speed = 0,
            eta = 0
        )
        assertTrue(progress.isComplete)
        assertFalse(progress.isActive)
        assertFalse(progress.isPaused)
    }

    @Test
    fun `active status flags are correct`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 0,
            totalBytes = 1000,
            speed = 1000,
            eta = 60
        )
        assertTrue(progress.isActive)
        assertFalse(progress.isComplete)
        assertFalse(progress.isPaused)
    }

    @Test
    fun `paused status flags are correct`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.PAUSED,
            filename = "test.mp4",
            downloadedBytes = 500,
            totalBytes = 1000,
            speed = 0,
            eta = 0
        )
        assertTrue(progress.isPaused)
        assertFalse(progress.isActive)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `downloadSpeed aliases speed`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 0,
            totalBytes = 1000,
            speed = 2048,
            eta = 60
        )
        assertEquals(2048L, progress.downloadSpeed)
    }

    @Test
    fun `completedLength aliases downloadedBytes`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 512,
            totalBytes = 1000,
            speed = 1024,
            eta = 60
        )
        assertEquals(512L, progress.completedLength)
    }

    @Test
    fun `totalLength aliases totalBytes`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 0,
            totalBytes = 2048,
            speed = 1024,
            eta = 60
        )
        assertEquals(2048L, progress.totalLength)
    }

    @Test
    fun `remainingBytes is correct at half done`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 500,
            totalBytes = 1000,
            speed = 1000,
            eta = 60
        )
        assertEquals(500L, progress.remainingBytes)
    }

    @Test
    fun `remainingBytes is zero when exceeded`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 1200,
            totalBytes = 1000,
            speed = 1000,
            eta = 60
        )
        assertEquals(0L, progress.remainingBytes)
    }

    @Test
    fun `etaSeconds calculates correctly`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 500,
            totalBytes = 1000,
            speed = 100,
            eta = 0
        )
        assertEquals(5L, progress.etaSeconds)
    }

    @Test
    fun `etaSeconds is zero when speed is zero`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 500,
            totalBytes = 1000,
            speed = 0,
            eta = 0
        )
        assertEquals(0L, progress.etaSeconds)
    }

    @Test
    fun `errorMessage is null by default`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ACTIVE,
            filename = "test.mp4",
            downloadedBytes = 0,
            totalBytes = 1000,
            speed = 0,
            eta = 0
        )
        assertNull(progress.errorMessage)
    }

    @Test
    fun `errorMessage is preserved`() {
        val progress = DownloadProgress(
            gid = "test",
            status = DownloadStatus.ERROR,
            filename = "test.mp4",
            downloadedBytes = 0,
            totalBytes = 1000,
            speed = 0,
            eta = 0,
            errorMessage = "connection timeout"
        )
        assertEquals("connection timeout", progress.errorMessage)
    }
}
