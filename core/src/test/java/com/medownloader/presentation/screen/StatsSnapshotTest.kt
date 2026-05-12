package com.medownloader.presentation.screen

import com.medownloader.data.engine.DownloadStatus
import com.medownloader.data.model.DownloadHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsSnapshotTest {

    private val now: Long = 1_700_000_000_000L // fixed clock
    private val msPerDay: Long = 24L * 60L * 60L * 1000L

    private fun entry(
        id: String,
        finishedDaysAgo: Long,
        bytes: Long,
        speed: Long = 0L,
        status: DownloadStatus = DownloadStatus.COMPLETE,
        fileType: String = "mp4"
    ): DownloadHistoryEntry {
        val finishedAt = now - finishedDaysAgo * msPerDay
        return DownloadHistoryEntry(
            id = id,
            gid = id,
            url = "https://example.com/$id",
            filename = "$id.$fileType",
            status = status.name,
            totalBytes = bytes,
            averageSpeed = speed,
            startedAtEpochMs = finishedAt - 60_000L,
            finishedAtEpochMs = finishedAt,
            durationMs = 60_000L,
            fileType = fileType
        )
    }

    @Test
    fun `empty history produces empty snapshot`() {
        val snap = buildStatsSnapshot(
            history = emptyList(),
            activeCount = 0,
            range = StatsRange.SEVEN_DAYS,
            nowMs = now
        )
        assertEquals(0L, snap.totalBytesDownloaded)
        assertEquals(0, snap.completedCount)
        assertEquals(0, snap.failedCount)
        assertFalse(snap.hasAnyHistory)
        assertEquals(7, snap.dailyVolume.size)
        assertEquals(12 * 7, snap.heatmap.size)
        assertTrue(snap.fileTypes.isEmpty())
    }

    @Test
    fun `seven day range excludes older entries but counts them as all-time`() {
        val history = listOf(
            entry("a", finishedDaysAgo = 1, bytes = 100),
            entry("b", finishedDaysAgo = 3, bytes = 200),
            entry("c", finishedDaysAgo = 10, bytes = 500)
        )
        val snap = buildStatsSnapshot(history, activeCount = 0, range = StatsRange.SEVEN_DAYS, nowMs = now)
        assertEquals(300L, snap.totalBytesDownloaded)
        assertEquals(2, snap.completedCount)
        assertEquals(800L, snap.allTimeBytes)
        assertEquals(3, snap.allTimeCompletedCount)
    }

    @Test
    fun `prior period bytes are based on the prior window of equal length`() {
        val history = listOf(
            // current 7d: days 1 + 3 = 100 + 200 = 300
            entry("a", finishedDaysAgo = 1, bytes = 100),
            entry("b", finishedDaysAgo = 3, bytes = 200),
            // prior 7d window (days 7..13): days 8 + 12 = 400 + 300 = 700
            entry("c", finishedDaysAgo = 8, bytes = 400),
            entry("d", finishedDaysAgo = 12, bytes = 300),
            // older than prior window -> ignored in trend
            entry("e", finishedDaysAgo = 20, bytes = 999)
        )
        val snap = buildStatsSnapshot(history, activeCount = 0, range = StatsRange.SEVEN_DAYS, nowMs = now)
        assertEquals(300L, snap.totalBytesDownloaded)
        assertEquals(700L, snap.priorPeriodBytes)
        assertEquals(-400L, snap.trendBytesDelta)
    }

    @Test
    fun `failed entries count but do not add to total bytes`() {
        val history = listOf(
            entry("ok", finishedDaysAgo = 1, bytes = 500, status = DownloadStatus.COMPLETE),
            entry("err", finishedDaysAgo = 2, bytes = 900, status = DownloadStatus.ERROR)
        )
        val snap = buildStatsSnapshot(history, activeCount = 0, range = StatsRange.SEVEN_DAYS, nowMs = now)
        assertEquals(500L, snap.totalBytesDownloaded)
        assertEquals(1, snap.completedCount)
        assertEquals(1, snap.failedCount)
    }

    @Test
    fun `speeds compute average and peak across completed entries only`() {
        val history = listOf(
            entry("a", finishedDaysAgo = 1, bytes = 100, speed = 1000),
            entry("b", finishedDaysAgo = 2, bytes = 200, speed = 3000),
            entry("c", finishedDaysAgo = 3, bytes = 300, speed = 0), // excluded
            entry("d", finishedDaysAgo = 4, bytes = 400, speed = 5000, status = DownloadStatus.ERROR) // excluded
        )
        val snap = buildStatsSnapshot(history, activeCount = 0, range = StatsRange.SEVEN_DAYS, nowMs = now)
        assertEquals(2000L, snap.averageSpeed) // (1000 + 3000) / 2
        assertEquals(3000L, snap.peakSpeed)
    }

    @Test
    fun `daily volume buckets match finished day`() {
        val history = listOf(
            entry("a", finishedDaysAgo = 0, bytes = 100),
            entry("b", finishedDaysAgo = 0, bytes = 50),
            entry("c", finishedDaysAgo = 2, bytes = 300)
        )
        val snap = buildStatsSnapshot(history, activeCount = 0, range = StatsRange.SEVEN_DAYS, nowMs = now)
        val today = snap.dailyVolume.last()
        val twoAgo = snap.dailyVolume[snap.dailyVolume.size - 3]
        assertEquals(150L, today.bytes)
        assertEquals(300L, twoAgo.bytes)
    }

    @Test
    fun `all-time range includes every completed entry`() {
        val history = listOf(
            entry("a", finishedDaysAgo = 1, bytes = 100),
            entry("b", finishedDaysAgo = 40, bytes = 200),
            entry("c", finishedDaysAgo = 400, bytes = 500)
        )
        val snap = buildStatsSnapshot(history, activeCount = 0, range = StatsRange.ALL_TIME, nowMs = now)
        assertEquals(800L, snap.totalBytesDownloaded)
        assertEquals(3, snap.completedCount)
        assertTrue(snap.range.isAllTime)
    }

    @Test
    fun `active count is surfaced into the snapshot`() {
        val snap = buildStatsSnapshot(
            history = emptyList(),
            activeCount = 4,
            range = StatsRange.SEVEN_DAYS,
            nowMs = now
        )
        assertEquals(4, snap.activeCount)
        assertEquals(4, snap.totalForStatusBar)
        assertEquals(0f, snap.completedRatio, 0.0001f)
        assertEquals(1f, snap.activeRatio, 0.0001f)
    }

    @Test
    fun `file types are sorted by count desc`() {
        val history = listOf(
            entry("a", 1, 100, fileType = "mp4"),
            entry("b", 1, 200, fileType = "mp4"),
            entry("c", 1, 300, fileType = "zip")
        )
        val snap = buildStatsSnapshot(history, activeCount = 0, range = StatsRange.SEVEN_DAYS, nowMs = now)
        assertEquals("mp4", snap.fileTypes.first().type)
        assertEquals(2, snap.fileTypes.first().count)
        assertEquals("zip", snap.fileTypes[1].type)
    }
}
