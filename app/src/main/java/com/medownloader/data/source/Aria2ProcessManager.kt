package com.medownloader.data.source

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Manages the aria2c native process lifecycle.
 * 
 * Responsibilities:
 * - Start/stop the aria2c daemon process
 * - Monitor process health (watchdog)
 * - Auto-restart on crash
 * - Graceful shutdown
 */
class Aria2ProcessManager(private val context: Context) {

    companion object {
        private const val TAG = "Aria2ProcessManager"
        private const val RPC_PORT = 6800
        private const val RPC_SECRET = "medownloader-secret"
        private const val WATCHDOG_INTERVAL_MS = 5000L
        private const val MAX_RESTART_ATTEMPTS = 3
    }

    private var aria2Process: Process? = null
    private var watchdogJob: Job? = null
    private var restartCount = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _processState = MutableStateFlow<ProcessState>(ProcessState.Stopped)
    val processState: StateFlow<ProcessState> = _processState.asStateFlow()

    sealed class ProcessState {
        data object Stopped : ProcessState()
        data object Starting : ProcessState()
        data object Running : ProcessState()
        data class Error(val message: String) : ProcessState()
    }

    /**
     * Starts the aria2c daemon process with RPC enabled.
     */
    suspend fun start(): Result<Unit> = withContext(Dispatchers.IO) {
        if (_processState.value == ProcessState.Running) {
            return@withContext Result.success(Unit)
        }

        _processState.value = ProcessState.Starting

        try {
            val binaryPath = extractAria2Binary()
            val sessionFile = File(context.filesDir, "aria2.session")
            val downloadDir = File(context.filesDir, "downloads")
            downloadDir.mkdirs()
            
            // Create session file if it doesn't exist (required for --input-file)
            if (!sessionFile.exists()) {
                sessionFile.createNewFile()
                Log.d(TAG, "Created new session file: ${sessionFile.absolutePath}")
            }

            val command = mutableListOf(
                binaryPath,
                // RPC Options (from docs: RPC Options section)
                "--enable-rpc=true",
                "--rpc-listen-port=$RPC_PORT",
                "--rpc-listen-all=false",           // Security: only localhost
                "--rpc-secret=$RPC_SECRET",
                "--rpc-allow-origin-all=false",     // Security: no CORS
                "--rpc-save-upload-metadata=true",  // Save .torrent/.metalink files
                "--rpc-max-request-size=2M",        // Default from docs
                
                // Basic Options (from docs: Basic Options section)
                "--dir=${downloadDir.absolutePath}",
                "--input-file=${sessionFile.absolutePath}",
                "--save-session=${sessionFile.absolutePath}",
                "--save-session-interval=30",
                "--max-concurrent-downloads=3",     // Free tier (docs: -j option)
                
                // HTTP/FTP/SFTP Options (from docs)
                "--max-connection-per-server=8",    // Free tier (docs: -x option, default 1)
                "--split=8",                        // Free tier (docs: -s option, default 5)
                "--min-split-size=1M",              // Docs: default 20M, we use 1M for mobile
                "--max-tries=5",                    // Docs: -m option, default 5
                "--retry-wait=3",                   // Wait 3 seconds between retries
                "--connect-timeout=60",             // Docs: default 60
                "--timeout=60",                     // Docs: default 60
                "--lowest-speed-limit=0",           // Docs: 0 means disabled
                "--max-file-not-found=5",           // Fail after 5 "file not found"
                "--continue=true",                  // Docs: -c option for resume
                "--auto-file-renaming=true",        // Docs: rename if file exists
                "--allow-overwrite=false",          // Docs: don't overwrite by default
                "--remote-time=true",               // Docs: -R option, apply server timestamp
                "--uri-selector=feedback",          // Docs: use download speed feedback
                "--stream-piece-selector=default",  // Docs: reduce connections
                
                // HTTP Specific (from docs: HTTP Specific Options section)
                "--http-accept-gzip=true",          // Accept gzip compression
                "--enable-http-keep-alive=true",    // Docs: default true
                "--enable-http-pipelining=false",   // Docs: usually no performance gain
                "--user-agent=meDownloader/1.0",    // Custom UA
                
                // Advanced Options (from docs: Advanced Options section)
                "--file-allocation=prealloc",       // Docs: pre-allocate for space check
                "--disk-cache=16M",                 // Docs: default 16M, reduces disk I/O
                "--async-dns=true",                 // Docs: default true
                "--auto-save-interval=60",          // Docs: save control file every 60s
                "--console-log-level=notice",       // Docs: default notice
                "--log=${context.filesDir}/aria2.log",
                "--log-level=notice",               // Docs: default debug, we use notice
                "--download-result=full",           // Docs: show full result info
                "--max-download-result=100",        // Keep 100 results in memory
                "--keep-unfinished-download-result=true",
                "--human-readable=true",            // Docs: format sizes nicely
                
                // TLS Options (from docs: Advanced Options section)
                "--min-tls-version=TLSv1.2",        // Docs: default TLSv1.2
                "--check-certificate=true",         // Docs: verify SSL certs
                
                // Daemon mode (we manage the process ourselves)
                "--daemon=false",                   // Docs: don't daemonize
                "--enable-color=false",             // No color codes in log
                "--show-console-readout=false",     // No console output
                
                // Piece/Chunk options
                "--piece-length=1M",                // Docs: HTTP/FTP chunk size
                "--realtime-chunk-checksum=true"    // Docs: validate during download
            )

            val processBuilder = ProcessBuilder(command)
                .directory(context.filesDir)
                .redirectErrorStream(true)

            aria2Process = processBuilder.start()
            
            // Wait a bit and verify it started
            delay(500)
            
            if (aria2Process?.isAlive == true) {
                _processState.value = ProcessState.Running
                restartCount = 0
                startWatchdog()
                Log.i(TAG, "aria2c started successfully on port $RPC_PORT")
                Result.success(Unit)
            } else {
                val exitCode = aria2Process?.exitValue() ?: -1
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

    /**
     * Gracefully stops the aria2c process.
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        watchdogJob?.cancel()
        watchdogJob = null

        aria2Process?.let { process ->
            try {
                // Send shutdown via RPC first (graceful)
                // aria2.shutdown via JSON-RPC would go here
                
                // Give it time to save session
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

    private fun extractAria2Binary(): String {
        val binaryName = "libaria2c.so"
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val sourceDir = File(context.applicationInfo.sourceDir).parentFile
        
        // android can use different ABI directory names, try all possibilities
        val abiVariants = listOf(
            android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            "arm64-v8a", "arm64", "armeabi-v7a", "x86_64", "x86"
        ).distinct()
        
        val searchPaths = mutableListOf<File>()
        
        // primary: nativeLibraryDir (most common)
        searchPaths.add(File(nativeLibDir, binaryName))
        
        // try ABI variants in lib subdirectory
        sourceDir?.let { base ->
            for (abi in abiVariants) {
                searchPaths.add(File(base, "lib/$abi/$binaryName"))
            }
        }
        
        // log all paths for debugging
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

    /**
     * Watchdog coroutine that monitors process health.
     */
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

    /**
     * Get the RPC secret for client authentication.
     */
    fun getRpcSecret(): String = RPC_SECRET

    /**
     * Get the RPC URL.
     */
    fun getRpcUrl(): String = "http://localhost:$RPC_PORT/jsonrpc"

    /**
     * Update connection limits (for premium unlock).
     */
    suspend fun updateLimits(maxConcurrent: Int, maxConnections: Int, split: Int) {
        // This would call aria2.changeGlobalOption via RPC
        // Implemented in Aria2RpcClient
    }
}
