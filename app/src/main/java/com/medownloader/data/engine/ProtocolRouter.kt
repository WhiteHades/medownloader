package com.medownloader.data.engine

object ProtocolRouter {
    fun route(url: String): EngineType {
        val lower = url.lowercase()
        return when {
            lower.startsWith("magnet:") -> EngineType.ARIA2C
            lower.endsWith(".torrent") -> EngineType.ARIA2C
            lower.contains("btih:") -> EngineType.ARIA2C
            lower.startsWith("ftp://") -> EngineType.ARIA2C
            lower.endsWith(".metalink") -> EngineType.ARIA2C
            else -> EngineType.YT_DLP
        }
    }
}
