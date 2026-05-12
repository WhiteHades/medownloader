package com.medownloader.presentation.screen

import com.medownloader.data.engine.DownloadStatus
import com.medownloader.data.model.DownloadHistoryEntry
import java.util.Calendar
import java.util.TimeZone

/**
 * Time window filter applied to stats calculations.
 *
 * [SEVEN_DAYS] and [THIRTY_DAYS] use a rolling window anchored to [nowMs]. [ALL_TIME]
 * includes every entry. The [days] property is only meaningful for fixed-window ranges;
 * callers should branch on [isAllTime] for all-time behavior.
 */
enum class StatsRange(val days: Int, val isAllTime: Boolean) {
    SEVEN_DAYS(7, false),
    THIRTY_DAYS(30, false),
    ALL_TIME(0, true)
}

/**
 * Pre-computed stats the Stats screen renders.
 *
 * All fields are derived from a single pass over [history] (plus a small inspection of
 * [com.medownloader.data.engine.DownloadProgress] for the active count).
 */
data class StatsSnapshot(
    val range: StatsRange,
    val rangeStartMs: Long,
    val rangeEndMs: Long,
    val totalBytesDownloaded: Long,
    val completedCount: Int,
    val failedCount: Int,
    val activeCount: Int,
    val averageSpeed: Long,
    val peakSpeed: Long,
    val dailyVolume: List<DailyVolumePoint>,
    val heatmap: List<HeatmapCell>,
    val fileTypes: List<FileTypeStat>,
    val priorPeriodBytes: Long,
    val allTimeBytes: Long,
    val allTimeCompletedCount: Int
) {
    val hasAnyHistory: Boolean get() = completedCount > 0 || failedCount > 0
    val totalForStatusBar: Int get() = completedCount + failedCount + activeCount

    val completedRatio: Float
        get() = if (totalForStatusBar == 0) 0f else completedCount.toFloat() / totalForStatusBar

    val failedRatio: Float
        get() = if (totalForStatusBar == 0) 0f else failedCount.toFloat() / totalForStatusBar

    val activeRatio: Float
        get() = if (totalForStatusBar == 0) 0f else activeCount.toFloat() / totalForStatusBar

    val peakDailyVolume: Long get() = dailyVolume.maxOfOrNull { it.bytes } ?: 0L

    val trendBytesDelta: Long get() = totalBytesDownloaded - priorPeriodBytes
}

data class DailyVolumePoint(
    val epochDayUtc: Long,
    val label: String,
    val bytes: Long
)

data class HeatmapCell(
    val epochDayUtc: Long,
    val count: Int,
    val bytes: Long
)

data class FileTypeStat(
    val type: String,
    val count: Int,
    val bytes: Long
)

/**
 * Builds a [StatsSnapshot] from persisted history plus a live active count.
 *
 * @param history      full persisted download history (already sorted newest-first is fine).
 * @param activeCount  count of currently running downloads (ACTIVE/PAUSED/QUEUED).
 * @param range        time window to compute for.
 * @param nowMs        clock override, injectable for tests.
 */
fun buildStatsSnapshot(
    history: List<DownloadHistoryEntry>,
    activeCount: Int,
    range: StatsRange,
    nowMs: Long = System.currentTimeMillis()
): StatsSnapshot {
    val rangeStart = if (range.isAllTime) Long.MIN_VALUE else nowMs - range.days * MS_PER_DAY
    val priorStart = if (range.isAllTime) Long.MIN_VALUE else rangeStart - range.days * MS_PER_DAY
    val priorEnd = rangeStart

    val inRange = history.filter { it.finishedAtEpochMs in rangeStart..nowMs }
    val priorRange = history.filter {
        !range.isAllTime && it.finishedAtEpochMs in priorStart until priorEnd
    }

    val completedInRange = inRange.filter { it.status == DownloadStatus.COMPLETE.name }
    val failedInRange = inRange.filter { it.status == DownloadStatus.ERROR.name }
    val completedPrior = priorRange.filter { it.status == DownloadStatus.COMPLETE.name }

    val totalBytes = completedInRange.sumOf { it.totalBytes }
    val priorBytes = completedPrior.sumOf { it.totalBytes }

    val speeds = completedInRange.map { it.averageSpeed }.filter { it > 0 }
    val averageSpeed = if (speeds.isEmpty()) 0L else speeds.average().toLong()
    val peakSpeed = completedInRange.maxOfOrNull { it.averageSpeed } ?: 0L

    val dailyVolume = buildDailyVolume(completedInRange, range, nowMs)
    val heatmap = buildHeatmap(completedInRange, nowMs)
    val fileTypes = buildFileTypes(completedInRange)

    val allCompleted = history.filter { it.status == DownloadStatus.COMPLETE.name }
    val allTimeBytes = allCompleted.sumOf { it.totalBytes }

    return StatsSnapshot(
        range = range,
        rangeStartMs = if (range.isAllTime) (history.minOfOrNull { it.finishedAtEpochMs } ?: nowMs) else rangeStart,
        rangeEndMs = nowMs,
        totalBytesDownloaded = totalBytes,
        completedCount = completedInRange.size,
        failedCount = failedInRange.size,
        activeCount = activeCount,
        averageSpeed = averageSpeed,
        peakSpeed = peakSpeed,
        dailyVolume = dailyVolume,
        heatmap = heatmap,
        fileTypes = fileTypes,
        priorPeriodBytes = priorBytes,
        allTimeBytes = allTimeBytes,
        allTimeCompletedCount = allCompleted.size
    )
}

private fun buildDailyVolume(
    completed: List<DownloadHistoryEntry>,
    range: StatsRange,
    nowMs: Long
): List<DailyVolumePoint> {
    val dayCount = when (range) {
        StatsRange.SEVEN_DAYS -> 7
        StatsRange.THIRTY_DAYS -> 30
        StatsRange.ALL_TIME -> 14 // compact preview
    }

    val bucket = completed.groupBy { epochDayUtc(it.finishedAtEpochMs) }
    val todayDay = epochDayUtc(nowMs)

    return (0 until dayCount).map { offset ->
        val day = todayDay - (dayCount - 1 - offset)
        val bytes = bucket[day]?.sumOf { it.totalBytes } ?: 0L
        DailyVolumePoint(
            epochDayUtc = day,
            label = shortDayLabel(day, dayCount),
            bytes = bytes
        )
    }
}

private fun buildHeatmap(
    completed: List<DownloadHistoryEntry>,
    nowMs: Long
): List<HeatmapCell> {
    val weeks = 12
    val days = weeks * 7
    val bucket = completed.groupBy { epochDayUtc(it.finishedAtEpochMs) }
    val todayDay = epochDayUtc(nowMs)

    return (0 until days).map { offset ->
        val day = todayDay - (days - 1 - offset)
        val entries = bucket[day].orEmpty()
        HeatmapCell(
            epochDayUtc = day,
            count = entries.size,
            bytes = entries.sumOf { it.totalBytes }
        )
    }
}

private fun buildFileTypes(completed: List<DownloadHistoryEntry>): List<FileTypeStat> {
    return completed
        .groupBy { it.fileType.ifBlank { "unknown" }.lowercase() }
        .map { (type, entries) ->
            FileTypeStat(
                type = type,
                count = entries.size,
                bytes = entries.sumOf { it.totalBytes }
            )
        }
        .sortedWith(compareByDescending<FileTypeStat> { it.count }.thenByDescending { it.bytes })
}

private const val MS_PER_DAY: Long = 24L * 60L * 60L * 1000L

private fun epochDayUtc(epochMs: Long): Long {
    return Math.floorDiv(epochMs, MS_PER_DAY)
}

private fun shortDayLabel(epochDay: Long, dayCount: Int): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = epochDay * MS_PER_DAY
    }
    return when {
        dayCount <= 7 -> {
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "S"
                Calendar.MONDAY -> "M"
                Calendar.TUESDAY -> "T"
                Calendar.WEDNESDAY -> "W"
                Calendar.THURSDAY -> "T"
                Calendar.FRIDAY -> "F"
                Calendar.SATURDAY -> "S"
                else -> ""
            }
        }
        else -> cal.get(Calendar.DAY_OF_MONTH).toString()
    }
}
