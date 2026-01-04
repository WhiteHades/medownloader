package com.medownloader.data

import com.medownloader.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import android.util.Base64

/**
 * JSON-RPC 2.0 client for communicating with aria2c daemon.
 * 
 * Based on aria2c 1.37.0 RPC Interface documentation.
 * All methods follow the exact signatures from the official docs.
 * 
 * RPC Authorization: Uses --rpc-secret token with "token:" prefix.
 * See: https://aria2.github.io/manual/en/html/aria2c.html#rpc-interface
 * 
 * All methods are suspend functions for proper coroutine integration.
 * Uses Result type for error handling without exceptions.
 */
class Aria2RpcClient(
    private val rpcUrl: String = "http://localhost:6800/jsonrpc",
    private val secret: String = "medownloader-secret"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    private val mediaType = "application/json".toMediaType()
    private var requestId = 0

    // ========================================================================
    // aria2.addUri - Add HTTP/FTP/SFTP/BitTorrent Magnet URI
    // Docs: "uris is an array of HTTP/FTP/SFTP/BitTorrent URIs (strings)"
    // ========================================================================

    /**
     * Add download URIs to the queue.
     * 
     * From docs: "aria2.addUri([secret, ]uris[, options[, position]])"
     * 
     * @param uris Array of URIs pointing to the same resource
     * @param options Download options (see Input File subsection in docs)
     * @param position Queue position (0-based), null = append to end
     * @return GID (hex string of 16 characters) of the download
     */
    suspend fun addUri(
        uris: List<String>,
        options: Map<String, String> = emptyMap(),
        position: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(buildJsonArray { uris.forEach { add(it) } })
            add(buildJsonObject { options.forEach { (k, v) -> put(k, v) } })
            position?.let { add(it) }
        }
        executeRpc<JsonPrimitive>("aria2.addUri", params).map { it.content }
    }

    /**
     * Convenience method for single URL.
     */
    suspend fun addUri(
        url: String, 
        filename: String? = null,
        options: Map<String, String> = emptyMap()
    ): Result<String> {
        val opts = filename?.let { options + ("out" to it) } ?: options
        return addUri(listOf(url), opts)
    }

    // ========================================================================
    // aria2.addTorrent - Add BitTorrent download
    // Docs: "torrent must be a base64-encoded string"
    // ========================================================================

    /**
     * Add a BitTorrent download by uploading a ".torrent" file.
     * 
     * From docs: "aria2.addTorrent([secret, ]torrent[, uris[, options[, position]]])"
     * 
     * @param torrentBytes Raw bytes of .torrent file
     * @param webSeeds Optional URIs for web-seeding
     * @param options Download options
     * @param position Queue position
     * @return GID of the download
     */
    suspend fun addTorrent(
        torrentBytes: ByteArray,
        webSeeds: List<String> = emptyList(),
        options: Map<String, String> = emptyMap(),
        position: Int? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val base64Torrent = Base64.encodeToString(torrentBytes, Base64.NO_WRAP)
        val params = buildJsonArray {
            add("token:$secret")
            add(base64Torrent)
            add(buildJsonArray { webSeeds.forEach { add(it) } })
            add(buildJsonObject { options.forEach { (k, v) -> put(k, v) } })
            position?.let { add(it) }
        }
        executeRpc<JsonPrimitive>("aria2.addTorrent", params).map { it.content }
    }

    /**
     * Add a magnet URI download.
     * 
     * From docs: "When adding BitTorrent Magnet URIs, uris must have only one element"
     */
    suspend fun addMagnet(
        magnetUri: String,
        options: Map<String, String> = emptyMap()
    ): Result<String> = addUri(listOf(magnetUri), options)

    // ========================================================================
    // aria2.addMetalink - Add Metalink download
    // Docs: "metalink is a base64-encoded string"
    // ========================================================================

    /**
     * Add a Metalink download.
     * 
     * From docs: "aria2.addMetalink([secret, ]metalink[, options[, position]])"
     * 
     * @param metalinkBytes Raw bytes of .metalink/.meta4 file
     * @return Array of GIDs (metalink can contain multiple downloads)
     */
    suspend fun addMetalink(
        metalinkBytes: ByteArray,
        options: Map<String, String> = emptyMap(),
        position: Int? = null
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val base64Metalink = Base64.encodeToString(metalinkBytes, Base64.NO_WRAP)
        val params = buildJsonArray {
            add("token:$secret")
            add(base64Metalink)
            add(buildJsonObject { options.forEach { (k, v) -> put(k, v) } })
            position?.let { add(it) }
        }
        executeRpc<JsonArray>("aria2.addMetalink", params)
            .map { arr -> arr.map { it.jsonPrimitive.content } }
    }

    // ========================================================================
    // aria2.remove / aria2.forceRemove
    // Docs: "removes the download denoted by gid"
    // ========================================================================

    /**
     * Remove a download. If in progress, it is first stopped.
     * 
     * From docs: "aria2.remove([secret, ]gid)"
     */
    suspend fun remove(gid: String): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(gid)
        }
        executeRpc<JsonPrimitive>("aria2.remove", params).map { it.content }
    }

    /**
     * Force remove without cleanup actions (e.g., tracker unregister).
     * 
     * From docs: "aria2.forceRemove([secret, ]gid)"
     */
    suspend fun forceRemove(gid: String): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(gid)
        }
        executeRpc<JsonPrimitive>("aria2.forceRemove", params).map { it.content }
    }

    // ========================================================================
    // aria2.pause / aria2.forcePause / aria2.pauseAll
    // Docs: "pauses the download denoted by gid"
    // ========================================================================

    /**
     * Pause a download.
     * 
     * From docs: "The status of paused download becomes paused"
     */
    suspend fun pause(gid: String): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(gid)
        }
        executeRpc<JsonPrimitive>("aria2.pause", params).map { it.content }
    }

    /**
     * Force pause without cleanup actions.
     */
    suspend fun forcePause(gid: String): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(gid)
        }
        executeRpc<JsonPrimitive>("aria2.forcePause", params).map { it.content }
    }

    /**
     * Pause all active/waiting downloads.
     */
    suspend fun pauseAll(): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray { add("token:$secret") }
        executeRpc<JsonPrimitive>("aria2.pauseAll", params).map { it.content }
    }

    // ========================================================================
    // aria2.unpause / aria2.unpauseAll
    // Docs: "changes status from paused to waiting"
    // ========================================================================

    /**
     * Resume a paused download.
     * 
     * From docs: "aria2.unpause([secret, ]gid)"
     */
    suspend fun unpause(gid: String): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(gid)
        }
        executeRpc<JsonPrimitive>("aria2.unpause", params).map { it.content }
    }

    /**
     * Resume all paused downloads.
     */
    suspend fun unpauseAll(): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray { add("token:$secret") }
        executeRpc<JsonPrimitive>("aria2.unpauseAll", params).map { it.content }
    }

    // ========================================================================
    // aria2.tellStatus - Get download status
    // Docs: Returns gid, status, totalLength, completedLength, etc.
    // ========================================================================

    /**
     * Get the progress of a download.
     * 
     * From docs: "aria2.tellStatus([secret, ]gid[, keys])"
     * 
     * @param gid Download GID
     * @param keys Optional list of keys to return (for efficiency)
     */
    suspend fun tellStatus(
        gid: String,
        keys: List<String>? = null
    ): Result<Download> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(gid)
            keys?.let { add(buildJsonArray { it.forEach { k -> add(k) } }) }
        }
        executeRpc<Aria2Status>("aria2.tellStatus", params).map { it.toDomain() }
    }

    // ========================================================================
    // aria2.tellActive / tellWaiting / tellStopped - List downloads
    // ========================================================================

    /**
     * Get list of active downloads.
     * 
     * From docs: "aria2.tellActive([secret][, keys])"
     */
    suspend fun tellActive(keys: List<String>? = null): Result<List<Download>> = 
        withContext(Dispatchers.IO) {
            val params = buildJsonArray {
                add("token:$secret")
                keys?.let { add(buildJsonArray { it.forEach { k -> add(k) } }) }
            }
            executeRpc<List<Aria2Status>>("aria2.tellActive", params)
                .map { statuses -> statuses.map { it.toDomain() } }
        }

    /**
     * Get list of waiting downloads (including paused).
     * 
     * From docs: "aria2.tellWaiting([secret, ]offset, num[, keys])"
     * 
     * @param offset Start position (supports negative for reverse)
     * @param num Max number of downloads to return
     */
    suspend fun tellWaiting(
        offset: Int = 0, 
        num: Int = 100,
        keys: List<String>? = null
    ): Result<List<Download>> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(offset)
            add(num)
            keys?.let { add(buildJsonArray { it.forEach { k -> add(k) } }) }
        }
        executeRpc<List<Aria2Status>>("aria2.tellWaiting", params)
            .map { statuses -> statuses.map { it.toDomain() } }
    }

    /**
     * Get list of stopped downloads (completed/error/removed).
     * 
     * From docs: "aria2.tellStopped([secret, ]offset, num[, keys])"
     */
    suspend fun tellStopped(
        offset: Int = 0, 
        num: Int = 100,
        keys: List<String>? = null
    ): Result<List<Download>> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(offset)
            add(num)
            keys?.let { add(buildJsonArray { it.forEach { k -> add(k) } }) }
        }
        executeRpc<List<Aria2Status>>("aria2.tellStopped", params)
            .map { statuses -> statuses.map { it.toDomain() } }
    }

    // ========================================================================
    // aria2.changePosition - Change queue position
    // Docs: "how is POS_SET, POS_CUR, or POS_END"
    // ========================================================================

    enum class PositionHow(val value: String) {
        SET("POS_SET"),   // Relative to beginning
        CUR("POS_CUR"),   // Relative to current
        END("POS_END")    // Relative to end
    }

    /**
     * Change position of download in queue.
     * 
     * From docs: "aria2.changePosition([secret, ]gid, pos, how)"
     */
    suspend fun changePosition(
        gid: String, 
        pos: Int, 
        how: PositionHow
    ): Result<Int> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(gid)
            add(pos)
            add(how.value)
        }
        executeRpc<JsonPrimitive>("aria2.changePosition", params)
            .map { it.int }
    }

    // ========================================================================
    // aria2.getOption / aria2.changeOption - Per-download options
    // ========================================================================

    /**
     * Get options for a download.
     * 
     * From docs: "aria2.getOption([secret, ]gid)"
     */
    suspend fun getOption(gid: String): Result<Map<String, String>> = 
        withContext(Dispatchers.IO) {
            val params = buildJsonArray {
                add("token:$secret")
                add(gid)
            }
            executeRpc<JsonObject>("aria2.getOption", params).map { obj ->
                obj.entries.associate { it.key to it.value.jsonPrimitive.content }
            }
        }

    /**
     * Change options for a download dynamically.
     * 
     * From docs: "aria2.changeOption([secret, ]gid, options)"
     * Note: Some option changes cause restart of the download.
     */
    suspend fun changeOption(
        gid: String, 
        options: Map<String, String>
    ): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(gid)
            add(buildJsonObject { options.forEach { (k, v) -> put(k, v) } })
        }
        executeRpc<JsonPrimitive>("aria2.changeOption", params).map { it.content }
    }

    // ========================================================================
    // aria2.getGlobalOption / aria2.changeGlobalOption - Global options
    // ========================================================================

    /**
     * Get global options.
     * 
     * From docs: "aria2.getGlobalOption([secret])"
     */
    suspend fun getGlobalOption(): Result<Map<String, String>> = 
        withContext(Dispatchers.IO) {
            val params = buildJsonArray { add("token:$secret") }
            executeRpc<JsonObject>("aria2.getGlobalOption", params).map { obj ->
                obj.entries.associate { it.key to it.value.jsonPrimitive.content }
            }
        }

    /**
     * Change global options dynamically.
     * 
     * From docs: "aria2.changeGlobalOption([secret, ]options)"
     * 
     * Available options (from docs):
     * - max-concurrent-downloads, max-overall-download-limit
     * - max-overall-upload-limit, save-session, etc.
     */
    suspend fun changeGlobalOption(options: Map<String, String>): Result<String> = 
        withContext(Dispatchers.IO) {
            val params = buildJsonArray {
                add("token:$secret")
                add(buildJsonObject { options.forEach { (k, v) -> put(k, v) } })
            }
            executeRpc<JsonPrimitive>("aria2.changeGlobalOption", params).map { it.content }
        }

    // ========================================================================
    // aria2.getGlobalStat - Overall statistics
    // Docs: Returns downloadSpeed, uploadSpeed, numActive, numWaiting, numStopped
    // ========================================================================

    /**
     * Get global download/upload statistics.
     * 
     * From docs: "aria2.getGlobalStat([secret])"
     */
    suspend fun getGlobalStat(): Result<Aria2GlobalStat> = withContext(Dispatchers.IO) {
        val params = buildJsonArray { add("token:$secret") }
        executeRpc("aria2.getGlobalStat", params)
    }

    // ========================================================================
    // aria2.getVersion - Get aria2 version and features
    // ========================================================================

    /**
     * Get aria2 version and enabled features.
     * 
     * From docs: "aria2.getVersion([secret])"
     */
    suspend fun getVersion(): Result<Aria2Version> = withContext(Dispatchers.IO) {
        val params = buildJsonArray { add("token:$secret") }
        executeRpc("aria2.getVersion", params)
    }

    // ========================================================================
    // aria2.getSessionInfo - Get session information
    // ========================================================================

    /**
     * Get session ID (generated each time aria2 starts).
     * 
     * From docs: "aria2.getSessionInfo([secret])"
     */
    suspend fun getSessionInfo(): Result<Aria2SessionInfo> = withContext(Dispatchers.IO) {
        val params = buildJsonArray { add("token:$secret") }
        executeRpc("aria2.getSessionInfo", params)
    }

    // ========================================================================
    // aria2.shutdown / aria2.forceShutdown / aria2.saveSession
    // ========================================================================

    /**
     * Gracefully shutdown aria2.
     */
    suspend fun shutdown(): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray { add("token:$secret") }
        executeRpc<JsonPrimitive>("aria2.shutdown", params).map { it.content }
    }

    /**
     * Force shutdown without cleanup.
     */
    suspend fun forceShutdown(): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray { add("token:$secret") }
        executeRpc<JsonPrimitive>("aria2.forceShutdown", params).map { it.content }
    }

    /**
     * Save current session to file (for --save-session).
     */
    suspend fun saveSession(): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray { add("token:$secret") }
        executeRpc<JsonPrimitive>("aria2.saveSession", params).map { it.content }
    }

    // ========================================================================
    // aria2.purgeDownloadResult / removeDownloadResult
    // ========================================================================

    /**
     * Purge completed/error/removed downloads from memory.
     */
    suspend fun purgeDownloadResult(): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray { add("token:$secret") }
        executeRpc<JsonPrimitive>("aria2.purgeDownloadResult", params).map { it.content }
    }

    /**
     * Remove a specific download result from memory.
     */
    suspend fun removeDownloadResult(gid: String): Result<String> = withContext(Dispatchers.IO) {
        val params = buildJsonArray {
            add("token:$secret")
            add(gid)
        }
        executeRpc<JsonPrimitive>("aria2.removeDownloadResult", params).map { it.content }
    }

    // ========================================================================
    // Flow-based observers for real-time updates
    // ========================================================================

    /**
     * Flow that emits all downloads periodically.
     * 
     * @param intervalMs Polling interval in milliseconds
     */
    fun observeDownloads(intervalMs: Long = 1000): Flow<List<Download>> = flow {
        while (true) {
            val active = tellActive().getOrElse { emptyList() }
            val waiting = tellWaiting().getOrElse { emptyList() }
            val stopped = tellStopped(num = 20).getOrElse { emptyList() }
            
            emit(active + waiting + stopped)
            delay(intervalMs)
        }
    }

    /**
     * Flow that emits a specific download's status.
     */
    fun observeDownload(gid: String, intervalMs: Long = 500): Flow<Download> = flow {
        while (true) {
            tellStatus(gid).onSuccess { emit(it) }
            delay(intervalMs)
        }
    }

    /**
     * Flow that emits global statistics.
     */
    fun observeGlobalStat(intervalMs: Long = 1000): Flow<Aria2GlobalStat> = flow {
        while (true) {
            getGlobalStat().onSuccess { emit(it) }
            delay(intervalMs)
        }
    }

    // ========================================================================
    // Internal Implementation
    // ========================================================================

    private suspend inline fun <reified T> executeRpc(
        method: String, 
        params: JsonArray
    ): Result<T> {
        val requestJson = buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", "medownloader-${++requestId}")
            put("method", method)
            put("params", params)
        }

        val request = Request.Builder()
            .url(rpcUrl)
            .post(requestJson.toString().toRequestBody(mediaType))
            .header("Content-Type", "application/json")
            .build()

        return try {
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(Aria2RpcException("HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body?.string() 
                ?: return Result.failure(Aria2RpcException("Empty response body"))

            val rpcResponse = json.decodeFromString<JsonObject>(body)
            
            // Check for RPC error (from docs: error contains code and message)
            rpcResponse["error"]?.let { error ->
                val errorObj = json.decodeFromJsonElement<Aria2Error>(error)
                return Result.failure(Aria2RpcException("RPC Error ${errorObj.code}: ${errorObj.message}"))
            }

            // Parse result
            val result = rpcResponse["result"] 
                ?: return Result.failure(Aria2RpcException("No result in response"))

            Result.success(json.decodeFromJsonElement<T>(result))
        } catch (e: Exception) {
            Result.failure(Aria2RpcException("RPC call failed: ${e.message}", e))
        }
    }
}

class Aria2RpcException(message: String, cause: Throwable? = null) : Exception(message, cause)
