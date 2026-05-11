package com.medownloader.data.engine

import android.util.Log
import com.chaquo.python.Python
import com.medownloader.data.source.Aria2ProcessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.function.Consumer

class YtDlpEngine(
    private val aria2ProcessManager: Aria2ProcessManager
) : DownloadEngine {

    companion object {
        private const val TAG = "YtDlpEngine"
    }

    override suspend fun download(options: DownloadOptions): MutableSharedFlow<DownloadProgress> {
        val progressChannel = MutableSharedFlow<DownloadProgress>(extraBufferCapacity = 64)

        withContext(Dispatchers.IO) {
            try {
                val py = Python.getInstance()

                val progressHook = Consumer<Map<String, Any?>> { raw ->
                    val status = raw["status"] as? String ?: return@Consumer
                    val downloaded = (raw["downloaded_bytes"] as? Number)?.toLong() ?: 0L
                    val total = (raw["total_bytes"] as? Number)?.toLong() ?: 0L
                    val speed = (raw["speed"] as? Number)?.toLong() ?: 0L
                    val eta = (raw["eta"] as? Number)?.toLong() ?: 0L
                    val filename = raw["filename"] as? String ?: options.filename ?: options.url

                    val engineStatus = when (status) {
                        "downloading" -> DownloadStatus.ACTIVE
                        "finished" -> DownloadStatus.COMPLETE
                        "error" -> DownloadStatus.ERROR
                        else -> DownloadStatus.ACTIVE
                    }

                    progressChannel.tryEmit(DownloadProgress(
                        gid = options.url,
                        status = engineStatus,
                        filename = filename,
                        downloadedBytes = downloaded,
                        totalBytes = total,
                        speed = speed,
                        eta = eta
                    ))
                }

                val params = buildYtdlpParams(options).toMutableMap()
                params["progress_hooks"] = listOf(progressHook)

                val ytdlp = py.getModule("yt_dlp")
                val pyParams = params.toPython(py)
                val ydl = ytdlp.callAttr("YoutubeDL", pyParams)

                try {
                    ydl.callAttr("download", listOf(options.url))
                    progressChannel.tryEmit(DownloadProgress(
                        gid = options.url,
                        status = DownloadStatus.COMPLETE,
                        filename = options.filename ?: options.url,
                        downloadedBytes = 0,
                        totalBytes = 0,
                        speed = 0,
                        eta = 0
                    ))
                } catch (e: Exception) {
                    Log.e(TAG, "yt-dlp download failed: ${e.message}", e)
                    progressChannel.tryEmit(DownloadProgress(
                        gid = options.url,
                        status = DownloadStatus.ERROR,
                        filename = options.filename ?: options.url,
                        downloadedBytes = 0,
                        totalBytes = 0,
                        speed = 0,
                        eta = 0
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "python runtime error: ${e.message}", e)
                progressChannel.tryEmit(DownloadProgress(
                    gid = options.url,
                    status = DownloadStatus.ERROR,
                    filename = options.filename ?: options.url,
                    downloadedBytes = 0,
                    totalBytes = 0,
                    speed = 0,
                    eta = 0
                ))
            }
        }

        return progressChannel
    }

    override suspend fun pause(gid: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("yt-dlp pause/resume not supported via chaquopy"))
    }

    override suspend fun resume(gid: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException("yt-dlp pause/resume not supported via chaquopy"))
    }

    override suspend fun stop(gid: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun queryAll(): List<DownloadProgress> {
        return emptyList()
    }

    override suspend fun isHealthy(): Boolean {
        return try {
            Python.getInstance().getModule("yt_dlp")
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun canExtract(url: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val py = Python.getInstance()
                val ytdlp = py.getModule("yt_dlp")
                val ydl = ytdlp.callAttr(
                    "YoutubeDL",
                    mapOf("quiet" to true, "no_warnings" to true).toPython(py)
                )
                ydl.callAttr(
                    "extract_info", url,
                    mapOf("download" to false, "process" to false).toPython(py)
                )
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "extraction check failed: ${e.message}")
            false
        }
    }

    private fun buildYtdlpParams(options: DownloadOptions): Map<String, Any> {
        val outTmpl = options.filename ?: "%(title).200s.%(ext)s"
        val aria2Args = listOf(
            "--rpc-secret=${aria2ProcessManager.getRpcSecret()}",
            "--summary-interval=0",
            "--enable-color=false",
            "-x", "8", "-s", "8", "-j", "8",
            "--file-allocation=none",
            "--http-accept-gzip=true"
        )

        return mapOf(
            "quiet" to true,
            "no_warnings" to true,
            "outtmpl" to mapOf("default" to outTmpl),
            "external_downloader" to mapOf("default" to "aria2c"),
            "external_downloader_args" to mapOf("aria2c" to aria2Args),
            "concurrent_fragment_downloads" to 8,
            "fragment_retries" to 10,
            "retries" to 10,
            "continuedl" to true,
            "overwrites" to true
        )
    }

    private fun Map<String, Any>.toPython(py: Python): com.chaquo.python.PyObject {
        val dict = py.getModule("builtins").callAttr("dict")
        forEach { (key, value) ->
            dict.callAttr("__setitem__", key, value.toPyValue(py))
        }
        return dict
    }
}

private fun Any.toPyValue(py: Python): Any {
    return when (this) {
        is Map<*, *> -> {
            val dict = py.getModule("builtins").callAttr("dict")
            this.forEach { (k, v) ->
                dict.callAttr("__setitem__", k.toString(), (v as Any).toPyValue(py))
            }
            dict
        }
        is List<*> -> {
            val list = py.getModule("builtins").callAttr("list")
            this.forEach { item ->
                list.callAttr("append", (item as Any).toPyValue(py))
            }
            list
        }
        is Consumer<*> -> this
        is Boolean -> this
        is Int -> this
        is Long -> this
        is String -> this
        else -> this.toString()
    }
}
