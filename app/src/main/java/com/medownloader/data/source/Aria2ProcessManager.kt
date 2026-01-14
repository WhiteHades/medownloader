package com.medownloader.data.source

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class Aria2ProcessManager(private val context: Context) {

    companion object {
        private const val TAG = "Aria2ProcessManager"
        private const val RPC_PORT = 6800
        private const val RPC_SECRET = "medownloader-secret"
        private const val WATCHDOG_INTERVAL_MS = 5000L
        private const val MAX_RESTART_ATTEMPTS = 3
    }

    @Volatile
    private var aria2Process: Process? = null
    private var watchdogJob: Job? = null
    private var restartCount = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val startMutex = Mutex()

    private val _processState = MutableStateFlow<ProcessState>(ProcessState.Stopped)
    val processState: StateFlow<ProcessState> = _processState.asStateFlow()

    sealed class ProcessState {
        data object Stopped : ProcessState()
        data object Starting : ProcessState()
        data object Running : ProcessState()
        data class Error(val message: String) : ProcessState()
    }

    suspend fun start(): Result<Unit> = startMutex.withLock {
        withContext(Dispatchers.IO) {
            if (isRpcResponding()) {
                Log.d(TAG, "RPC is already responding, skipping start")
                _processState.value = ProcessState.Running
                return@withContext Result.success(Unit)
            }

            if (_processState.value == ProcessState.Running && aria2Process?.isAlive == true) {
                Log.d(TAG, "Process already running, skipping start")
                return@withContext Result.success(Unit)
            }

            _processState.value = ProcessState.Starting

        try {
            val binaryPath = extractAria2Binary()
            val publicDownloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val downloadDir = File(publicDownloadsDir, "meDownloader")
            downloadDir.mkdirs()
            val sessionFile = File(downloadDir, ".aria2session")
            val caCertPath = copyCaCertificate()

            if (!sessionFile.exists()) {
                sessionFile.createNewFile()
                Log.d(TAG, "Created new session file: ${sessionFile.absolutePath}")
            }

            val serverStatFile = File(downloadDir, ".aria2-server-stats")
            val userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

            val command = mutableListOf(
                binaryPath,
                "--enable-rpc=true",
                "--rpc-listen-port=$RPC_PORT",
                "--rpc-listen-all=false",
                "--rpc-secret=$RPC_SECRET",
                "--rpc-allow-origin-all=false",
                "--rpc-save-upload-metadata=true",
                "--rpc-max-request-size=2M",
                "--ca-certificate=$caCertPath",
                "--check-certificate=true",
                "--min-tls-version=TLSv1.2",
                "--dir=${downloadDir.absolutePath}",
                "--input-file=${sessionFile.absolutePath}",
                "--save-session=${sessionFile.absolutePath}",
                "--save-session-interval=60",
                "--force-save=true",
                "--auto-save-interval=60",
                "--max-concurrent-downloads=5",
                "--max-connection-per-server=4",
                "--split=8",
                "--min-split-size=5M",
                "--continue=true",
                "--always-resume=true",
                "--max-tries=0",
                "--retry-wait=20",
                "--connect-timeout=60",
                "--timeout=60",
                "--lowest-speed-limit=0",
                "--user-agent=$userAgent",
                "--enable-http-keep-alive=true",
                "--http-accept-gzip=false",
                "--uri-selector=feedback",
                "--server-stat-of=${serverStatFile.absolutePath}",
                "--server-stat-if=${serverStatFile.absolutePath}",
                "--server-stat-timeout=86400",
                "--file-allocation=falloc",
                "--disk-cache=32M",
                "--auto-file-renaming=true",
                "--allow-overwrite=false",
                "--remote-time=true",
                "--conditional-get=true",
                "--enable-dht=true",
                "--enable-dht6=true",
                "--dht-file-path=${File(downloadDir, ".aria2-dht.dat").absolutePath}",
                "--dht-file-path6=${File(downloadDir, ".aria2-dht6.dat").absolutePath}",
                "--enable-peer-exchange=true",
                "--bt-enable-lpd=true",
                "--bt-max-peers=55",
                "--bt-save-metadata=true",
                "--bt-load-saved-metadata=true",
                "--seed-ratio=1.0",
                "--bt-hash-check-seed=true",
                "--follow-torrent=true",
                "--follow-metalink=true",
                "--async-dns=true",
                "--async-dns-server=8.8.8.8,8.8.4.4,1.1.1.1",
                "--console-log-level=notice",
                "--log-level=notice",
                "--daemon=false",
                "--enable-color=false",
                "--show-console-readout=false",
                "--summary-interval=0"
            )

            Log.d(TAG, "Starting aria2c: ${command.joinToString(" ")}")

            val processBuilder = ProcessBuilder(command)
                .directory(context.filesDir)
                .redirectErrorStream(true)

            val process = processBuilder.start()
            aria2Process = process
            
            thread(name = "Aria2Logger") {
                try {
                    process.inputStream.bufferedReader().use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            Log.i("Aria2Native", line ?: "")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading process output", e)
                }
            }
            
            delay(500)
            
            if (process.isAlive) {
                _processState.value = ProcessState.Running
                restartCount = 0
                startWatchdog()
                Log.i(TAG, "aria2c started successfully on port $RPC_PORT")
                Result.success(Unit)
            } else {
                val exitCode = process.exitValue()
                val error = "aria2c failed to start (exit code: $exitCode)"
                _processState.value = ProcessState.Error(error)
                Log.e(TAG, error)
                Result.failure(RuntimeException(error))
            }
        } catch (e: Exception) {
            _processState.value = ProcessState.Error(e.message ?: "Unknown error")
            Log.e(TAG, "Failed to start aria2c", e)
            Result.failure(e)
        }
        }
    }

    private fun copyCaCertificate(): String {
        val caFile = File(context.filesDir, "cacert.pem")
        if (!caFile.exists()) {
            try {
                context.assets.open("cacert.pem").use { input ->
                    FileOutputStream(caFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Copied cacert.pem to internal storage")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy cacert.pem", e)
            }
        }
        return caFile.absolutePath
    }

    private fun isRpcResponding(): Boolean {
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("127.0.0.1", RPC_PORT), 1000)
            }
            Log.d(TAG, "isRpcResponding: Socket connect succeeded, port 6800 is in use")
        } catch (e: Exception) {
            Log.d(TAG, "isRpcResponding: Socket connect failed (port likely free): ${e.message}")
            return false
        }

        return try {
            val url = URL("http://127.0.0.1:$RPC_PORT/jsonrpc")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { os ->
                os.write("""{"jsonrpc":"2.0","id":"ping","method":"aria2.getVersion","params":["token:$RPC_SECRET"]}""".toByteArray())
            }
            val responseCode = connection.responseCode
            connection.disconnect()
            val isResponding = responseCode == 200
            Log.d(TAG, "isRpcResponding: RPC call returned $responseCode, isResponding=$isResponding")
            isResponding
        } catch (e: Exception) {
            Log.w(TAG, "isRpcResponding: RPC call failed: ${e.message}")
            true
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        watchdogJob?.cancel()
        watchdogJob = null

        aria2Process?.let { process ->
            try {
                saveSessionViaRpc()
                delay(1000)
                
                if (process.isAlive) {
                    process.destroy()
                    delay(500)
                    
                    if (process.isAlive) {
                        process.destroyForcibly()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error during shutdown", e)
            }
        }

        aria2Process = null
        _processState.value = ProcessState.Stopped
        Log.i(TAG, "aria2c stopped")
    }

    private fun saveSessionViaRpc() {
        try {
            val url = URL("http://127.0.0.1:$RPC_PORT/jsonrpc")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val payload = """{"jsonrpc":"2.0","id":"save","method":"aria2.saveSession","params":["token:$RPC_SECRET"]}"""
            connection.outputStream.use { it.write(payload.toByteArray()) }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "saveSession RPC response: $responseCode")
            connection.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save session via RPC: ${e.message}")
        }
    }

    private fun extractAria2Binary(): String {
        val binaryName = "libaria2c.so"
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val sourceDir = File(context.applicationInfo.sourceDir).parentFile
        
        val abiVariants = listOf(
            android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            "arm64-v8a", "arm64", "armeabi-v7a", "x86_64", "x86"
        ).distinct()
        
        val searchPaths = mutableListOf<File>()
        searchPaths.add(File(nativeLibDir, binaryName))
        
        sourceDir?.let { base ->
            for (abi in abiVariants) {
                searchPaths.add(File(base, "lib/$abi/$binaryName"))
            }
        }
        
        Log.d(TAG, "Searching for aria2c in: ${searchPaths.map { it.absolutePath }}")
        
        for (path in searchPaths) {
            Log.d(TAG, "Checking: ${path.absolutePath} exists=${path.exists()} exec=${path.canExecute()}")
            if (path.exists() && path.canExecute()) {
                Log.i(TAG, "aria2c binary found: ${path.absolutePath}")
                return path.absolutePath
            }
        }
        
        throw RuntimeException("aria2c binary not found in native library paths: ${searchPaths.map { it.absolutePath }}")
    }

    private fun startWatchdog() {
        watchdogJob = scope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)

                val process = aria2Process
                if (process == null || !process.isAlive) {
                    Log.w(TAG, "aria2c process died unexpectedly")
                    
                    if (restartCount < MAX_RESTART_ATTEMPTS) {
                        restartCount++
                        Log.i(TAG, "Attempting restart ($restartCount/$MAX_RESTART_ATTEMPTS)")
                        start()
                    } else {
                        _processState.value = ProcessState.Error(
                            "aria2c crashed $MAX_RESTART_ATTEMPTS times. Manual restart required."
                        )
                        break
                    }
                }
            }
        }
    }

    fun getRpcSecret(): String = RPC_SECRET

    fun getRpcUrl(): String = "http://localhost:$RPC_PORT/jsonrpc"

    suspend fun updateLimits(maxConcurrent: Int, maxConnections: Int, split: Int) {
    }
}
