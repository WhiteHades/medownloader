package com.medownloader.data.engine

data class DownloadProgress(
    val gid: String,
    val status: DownloadStatus,
    val filename: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Long,
    val eta: Long,
    val errorMessage: String? = null
) {
    val downloadSpeed: Long get() = speed
    val completedLength: Long get() = downloadedBytes
    val totalLength: Long get() = totalBytes

    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val isComplete: Boolean
        get() = status == DownloadStatus.COMPLETE

    val isPaused: Boolean
        get() = status == DownloadStatus.PAUSED

    val isActive: Boolean
        get() = status == DownloadStatus.ACTIVE

    val remainingBytes: Long
        get() = (totalBytes - downloadedBytes).coerceAtLeast(0)

    val etaSeconds: Long
        get() = if (speed > 0) remainingBytes / speed else 0
}
