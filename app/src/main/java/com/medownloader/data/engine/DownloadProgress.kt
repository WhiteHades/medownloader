package com.medownloader.data.engine

data class DownloadProgress(
    val gid: String,
    val status: DownloadStatus,
    val filename: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speed: Long,
    val eta: Long
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
}
