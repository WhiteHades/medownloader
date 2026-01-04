package com.medownloader.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Domain model representing a download task.
 */
data class Download(
    val gid: String,
    val url: String,
    val filename: String,
    val totalLength: Long,
    val completedLength: Long,
    val downloadSpeed: Long,
    val status: DownloadStatus,
    val connections: Int,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (totalLength > 0) completedLength.toFloat() / totalLength else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isComplete: Boolean
        get() = status == DownloadStatus.COMPLETE

    val isPaused: Boolean
        get() = status == DownloadStatus.PAUSED

    val isActive: Boolean
        get() = status == DownloadStatus.ACTIVE

    val remainingBytes: Long
        get() = totalLength - completedLength

    val etaSeconds: Long
        get() = if (downloadSpeed > 0) remainingBytes / downloadSpeed else 0
}

enum class DownloadStatus {
    ACTIVE,
    WAITING,
    PAUSED,
    COMPLETE,
    ERROR,
    REMOVED;

    companion object {
        fun fromAria2(status: String): DownloadStatus = when (status) {
            "active" -> ACTIVE
            "waiting" -> WAITING
            "paused" -> PAUSED
            "complete" -> COMPLETE
            "error" -> ERROR
            "removed" -> REMOVED
            else -> ERROR
        }
    }
}

// ============================================================================
// JSON-RPC Response Models (for parsing aria2 responses)
// ============================================================================

@Serializable
data class Aria2RpcResponse<T>(
    val id: String,
    val jsonrpc: String = "2.0",
    val result: T? = null,
    val error: Aria2Error? = null
)

@Serializable
data class Aria2Error(
    val code: Int,
    val message: String
)

@Serializable
data class Aria2Status(
    val gid: String,
    val status: String,
    val totalLength: String = "0",
    val completedLength: String = "0",
    val downloadSpeed: String = "0",
    val connections: String = "0",
    val errorMessage: String? = null,
    val files: List<Aria2File> = emptyList()
) {
    fun toDomain(): Download {
        val filename = files.firstOrNull()?.path?.substringAfterLast('/') ?: "Unknown"
        return Download(
            gid = gid,
            url = files.firstOrNull()?.uris?.firstOrNull()?.uri ?: "",
            filename = filename,
            totalLength = totalLength.toLongOrNull() ?: 0,
            completedLength = completedLength.toLongOrNull() ?: 0,
            downloadSpeed = downloadSpeed.toLongOrNull() ?: 0,
            status = DownloadStatus.fromAria2(status),
            connections = connections.toIntOrNull() ?: 0,
            errorMessage = errorMessage
        )
    }
}

@Serializable
data class Aria2File(
    val index: String,
    val path: String,
    val length: String,
    val completedLength: String,
    val selected: String,
    val uris: List<Aria2Uri> = emptyList()
)

@Serializable
data class Aria2Uri(
    val uri: String,
    val status: String
)

@Serializable
data class Aria2GlobalStat(
    val downloadSpeed: String = "0",
    val uploadSpeed: String = "0",
    val numActive: String = "0",
    val numWaiting: String = "0",
    val numStopped: String = "0",
    val numStoppedTotal: String = "0"
) {
    val totalDownloadSpeed: Long get() = downloadSpeed.toLongOrNull() ?: 0
    val totalUploadSpeed: Long get() = uploadSpeed.toLongOrNull() ?: 0
    val activeCount: Int get() = numActive.toIntOrNull() ?: 0
    val waitingCount: Int get() = numWaiting.toIntOrNull() ?: 0
    val stoppedCount: Int get() = numStopped.toIntOrNull() ?: 0
    val stoppedTotalCount: Int get() = numStoppedTotal.toIntOrNull() ?: 0
}

/**
 * aria2.getVersion response.
 * 
 * From docs: "Returns version of aria2 and enabled features."
 */
@Serializable
data class Aria2Version(
    val version: String,
    val enabledFeatures: List<String> = emptyList()
) {
    fun hasFeature(feature: String): Boolean = 
        enabledFeatures.any { it.equals(feature, ignoreCase = true) }
    
    val hasBitTorrent: Boolean get() = hasFeature("BitTorrent")
    val hasGZip: Boolean get() = hasFeature("GZip")
    val hasHTTPS: Boolean get() = hasFeature("HTTPS")
    val hasMetalink: Boolean get() = hasFeature("Metalink")
    val hasXMLRPC: Boolean get() = hasFeature("XML-RPC")
    val hasAsync: Boolean get() = hasFeature("Async DNS")
}

/**
 * aria2.getSessionInfo response.
 * 
 * From docs: "Returns session information."
 */
@Serializable
data class Aria2SessionInfo(
    val sessionId: String
)
