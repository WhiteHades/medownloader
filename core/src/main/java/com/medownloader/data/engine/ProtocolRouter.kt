package com.medownloader.data.engine

import java.net.URI

/**
 * Chooses the download engine for a given URL.
 *
 * Rules, in order:
 *   1. Torrent-like schemes (magnet, .torrent, btih, metalink) -> aria2c.
 *   2. FTP -> aria2c.
 *   3. http(s) hosts on the media whitelist (youtube, vimeo, tiktok, ...) -> yt-dlp.
 *   4. Everything else -> aria2c. This is the common case for direct file downloads;
 *      yt-dlp is only useful for sites that need URL extraction.
 *
 * The whitelist approach matters because routing every plain URL through yt-dlp makes
 * direct-file downloads slow to start and can silently drop downloads if yt-dlp cannot
 * extract the URL.
 */
object ProtocolRouter {

    private val MEDIA_HOST_SUFFIXES: Set<String> = setOf(
        // youtube
        "youtube.com",
        "youtu.be",
        "youtube-nocookie.com",
        // vimeo
        "vimeo.com",
        // twitter / x
        "twitter.com",
        "x.com",
        "t.co",
        // tiktok
        "tiktok.com",
        // twitch
        "twitch.tv",
        // soundcloud
        "soundcloud.com",
        // reddit
        "reddit.com",
        "redd.it",
        // instagram
        "instagram.com",
        "cdninstagram.com",
        // facebook
        "facebook.com",
        "fb.watch",
        // dailymotion
        "dailymotion.com",
        "dai.ly",
        // bilibili
        "bilibili.com",
        // streamable / misc
        "streamable.com",
        "rumble.com",
        "odysee.com"
    )

    fun route(url: String): EngineType {
        val trimmed = url.trim()
        val lower = trimmed.lowercase()

        if (lower.startsWith("magnet:")) return EngineType.ARIA2C
        if (lower.endsWith(".torrent")) return EngineType.ARIA2C
        if (lower.contains("btih:") || lower.contains("btih=")) return EngineType.ARIA2C
        if (lower.endsWith(".metalink")) return EngineType.ARIA2C
        if (lower.startsWith("ftp://")) return EngineType.ARIA2C

        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return EngineType.ARIA2C
        }

        val host = hostOf(trimmed) ?: return EngineType.ARIA2C
        return if (isMediaHost(host)) EngineType.YT_DLP else EngineType.ARIA2C
    }

    private fun hostOf(url: String): String? {
        return try {
            URI(url).host?.lowercase()
        } catch (_: Exception) {
            null
        }
    }

    private fun isMediaHost(host: String): Boolean {
        return MEDIA_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }
}
