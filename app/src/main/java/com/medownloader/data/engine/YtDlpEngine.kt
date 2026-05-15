package com.medownloader.data.engine

import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.medownloader.data.repository.FileInfo
import com.medownloader.data.repository.SettingsRepository
import com.medownloader.data.source.Aria2ProcessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.function.Consumer

class YtDlpEngine(
    private val aria2ProcessManager: Aria2ProcessManager,
    private val settingsRepository: SettingsRepository? = null
) : DownloadEngine {

    companion object {
        private const val TAG = "YtDlpEngine"
        private const val DEFAULT_DNS_SERVERS = "8.8.8.8,8.8.4.4,1.1.1.1"
    }

    /**
     * Runs yt-dlp and streams [DownloadProgress] events to the collector.
     *
     * Implementation note:
     * - We use [channelFlow] so the Flow is returned immediately and the yt-dlp blocking
     *   work starts inside the collector's coroutine. The previous implementation used
     *   `MutableSharedFlow(replay = 0)` and emitted events before a collector was attached,
     *   which silently dropped every progress update. That is why paste-triggered downloads
     *   "ran in the background" but never showed up in the UI.
     * - Progress hook events arrive on whichever thread yt-dlp dispatches on; [trySend] is
     *   thread-safe and non-blocking, which is what we want inside a Python callback.
     * - We emit a terminal COMPLETE (or ERROR) event and then let the channelFlow block
     *   return, which closes the channel and ends the Flow.
     */
    override suspend fun download(options: DownloadOptions): Flow<DownloadProgress> =
        channelFlow {
            withContext(Dispatchers.IO) {
                try {
                    val py = Python.getInstance()
                    val ytdlp = py.getModule("yt_dlp")
                    var lastProgress: DownloadProgress? = null
                    var emittedTerminal = false

                    val progressHook = Consumer<Map<String, Any?>> { raw ->
                        val status = raw["status"] as? String ?: return@Consumer
                        val downloaded = (raw["downloaded_bytes"] as? Number)?.toLong() ?: 0L
                        val total = (raw["total_bytes"] as? Number)?.toLong()
                            ?: (raw["total_bytes_estimate"] as? Number)?.toLong()
                            ?: 0L
                        val speed = (raw["speed"] as? Number)?.toLong() ?: 0L
                        val eta = (raw["eta"] as? Number)?.toLong() ?: 0L
                        val filename = raw["filename"] as? String
                            ?: options.filename
                            ?: options.url

                        val mappedStatus = when (status) {
                            "downloading" -> DownloadStatus.ACTIVE
                            "finished" -> DownloadStatus.COMPLETE
                            "error" -> DownloadStatus.ERROR
                            else -> DownloadStatus.QUEUED
                        }

                        val progress = DownloadProgress(
                            gid = options.url,
                            status = mappedStatus,
                            filename = filename,
                            downloadedBytes = downloaded,
                            totalBytes = total,
                            speed = speed,
                            eta = eta
                        )
                        lastProgress = progress
                        if (mappedStatus == DownloadStatus.COMPLETE || mappedStatus == DownloadStatus.ERROR) {
                            emittedTerminal = true
                        }
                        trySend(progress)
                    }

                    val params = buildYtdlpParams(options).toMutableMap()
                    params["progress_hooks"] = listOf(progressHook)

                    val ydl = ytdlp.callAttr("YoutubeDL", params.toPython(py))
                    ydl.callAttr("download", listOf(options.url).toPython(py))

                    // yt-dlp does not always fire a "finished" progress event; emit our own
                    // terminal COMPLETE so the repository records the download as finished.
                    if (!emittedTerminal) {
                        send(buildTerminalProgress(options, lastProgress))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "yt-dlp download failed", e)
                    send(
                        DownloadProgress(
                            gid = options.url,
                            status = DownloadStatus.ERROR,
                            filename = options.filename ?: options.url,
                            downloadedBytes = 0,
                            totalBytes = 0,
                            speed = 0,
                            eta = 0,
                            errorMessage = e.message
                        )
                    )
                }
            }
        }

    override suspend fun pause(gid: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("yt-dlp pause/resume not yet implemented"))

    override suspend fun resume(gid: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("yt-dlp pause/resume not yet implemented"))

    override suspend fun stop(gid: String): Result<Unit> = Result.success(Unit)

    override suspend fun queryAll(): List<DownloadProgress> = emptyList()

    override suspend fun isHealthy(): Boolean = try {
        Python.getInstance().getModule("yt_dlp")
        true
    } catch (_: Exception) {
        false
    }

    suspend fun canExtract(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val py = Python.getInstance()
            val ytdlp = py.getModule("yt_dlp")
            val ydl = ytdlp.callAttr(
                "YoutubeDL",
                mapOf("quiet" to true, "no_warnings" to true).toPython(py)
            )
            ydl.callAttr("extract_info", url, false)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchInfo(url: String): Result<FileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val py = Python.getInstance()
            val ytdlp = py.getModule("yt_dlp")
            val ydl = ytdlp.callAttr(
                "YoutubeDL",
                mapOf("quiet" to true, "no_warnings" to true, "skip_download" to true).toPython(py)
            )
            val info = ydl.callAttr("extract_info", url, false)

            val title = info.get("title")?.toString()?.ifBlank { null }
            val ext = info.get("ext")?.toString()?.ifBlank { null }
            val filesize = info.get("filesize")?.toLong()
                ?: info.get("filesize_approx")?.toLong()

            FileInfo(
                filename = buildString {
                    append(title ?: "download")
                    if (ext != null && !endsWith(".$ext")) append('.').append(ext)
                },
                size = filesize,
                resumable = true,
                mimeType = null
            )
        }
    }

    private suspend fun buildYtdlpParams(options: DownloadOptions): Map<String, Any> {
        val runtime = loadRuntimeSettings()
        val outTemplate = options.filename ?: "%(title).200s.%(ext)s"
        val aria2Args = listOf(
            "--rpc-secret=${aria2ProcessManager.getRpcSecret()}",
            "--summary-interval=0",
            "--enable-color=false",
            "--max-connection-per-server=${runtime.connectionLimit}",
            "--split=${runtime.splitCount}",
            "--min-split-size=5M",
            "--disk-cache=${runtime.diskCacheMb}M",
            "--async-dns=true",
            "--async-dns-server=${runtime.dnsServers}",
            "--file-allocation=none",
            "--http-accept-gzip=true"
        )

        return mapOf(
            "quiet" to true,
            "no_warnings" to true,
            "outtmpl" to outTemplate,
            "external_downloader" to "aria2c",
            "external_downloader_args" to mapOf("aria2c" to aria2Args),
            "concurrent_fragment_downloads" to runtime.splitCount,
            "fragment_retries" to 10,
            "retries" to 10,
            "continuedl" to true,
            "overwrites" to true
        )
    }

    private suspend fun loadRuntimeSettings(): YtDlpRuntimeSettings {
        val connectionLimit = settingsRepository
            ?.connectionLimit
            ?.first()
            ?.coerceIn(1, 16)
            ?: 8
        val splitCount = settingsRepository
            ?.splitCount
            ?.first()
            ?.coerceIn(1, 16)
            ?: connectionLimit
        val diskCacheMb = settingsRepository
            ?.diskCacheMb
            ?.first()
            ?.coerceIn(4, 128)
            ?: 32
        val dnsServers = settingsRepository
            ?.dnsServers
            ?.first()
            ?.ifBlank { DEFAULT_DNS_SERVERS }
            ?: DEFAULT_DNS_SERVERS

        return YtDlpRuntimeSettings(
            connectionLimit = connectionLimit,
            splitCount = splitCount,
            diskCacheMb = diskCacheMb,
            dnsServers = dnsServers
        )
    }

    private fun buildTerminalProgress(
        options: DownloadOptions,
        lastProgress: DownloadProgress?
    ): DownloadProgress {
        val baseline = lastProgress ?: DownloadProgress(
            gid = options.url,
            status = DownloadStatus.ACTIVE,
            filename = options.filename ?: options.url,
            downloadedBytes = 0,
            totalBytes = 0,
            speed = 0,
            eta = 0
        )
        val completedBytes = when {
            baseline.totalBytes > 0L -> baseline.totalBytes
            baseline.downloadedBytes > 0L -> baseline.downloadedBytes
            else -> 0L
        }

        return baseline.copy(
            status = DownloadStatus.COMPLETE,
            downloadedBytes = completedBytes,
            totalBytes = maxOf(baseline.totalBytes, completedBytes),
            speed = 0,
            eta = 0,
            errorMessage = null
        )
    }

    private data class YtDlpRuntimeSettings(
        val connectionLimit: Int,
        val splitCount: Int,
        val diskCacheMb: Int,
        val dnsServers: String
    )

}

private fun Any.toPython(py: Python): Any {
    return when (this) {
        is Map<*, *> -> {
            val dict = py.getBuiltins().callAttr("dict")
            forEach { (key, value) ->
                if (value != null) {
                    dict.callAttr("__setitem__", key.toString(), value.toPython(py))
                }
            }
            dict
        }
        is List<*> -> {
            val list = py.getBuiltins().callAttr("list")
            forEach { item ->
                if (item != null) {
                    list.callAttr("append", item.toPython(py))
                }
            }
            list
        }
        is Consumer<*> -> this
        is Boolean, is Int, is Long, is Float, is Double, is String -> this
        is PyObject -> this
        else -> this.toString()
    }
}
