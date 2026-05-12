package com.medownloader.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DownloadHistoryEntry(
    val id: String,
    val gid: String,
    val url: String,
    val filename: String,
    val status: String,
    val totalBytes: Long,
    val averageSpeed: Long,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val durationMs: Long,
    val fileType: String
)
