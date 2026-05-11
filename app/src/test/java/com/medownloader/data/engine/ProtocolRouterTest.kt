package com.medownloader.data.engine

import org.junit.Assert.*
import org.junit.Test

class ProtocolRouterTest {

    @Test
    fun `https url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://www.youtube.com/watch?v=abc123"))
    }

    @Test
    fun `http url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("http://example.com/file.mp4"))
    }

    @Test
    fun `magnet uri routes to aria2c`() {
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("magnet:?xt=urn:btih:abc&dn=test"))
    }

    @Test
    fun `torrent file routes to aria2c`() {
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://example.com/file.torrent"))
    }

    @Test
    fun `btih url routes to aria2c`() {
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://example.com?btih=abc123"))
    }

    @Test
    fun `ftp url routes to aria2c`() {
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("ftp://files.example.com/archive.zip"))
    }

    @Test
    fun `metalink file routes to aria2c`() {
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://example.com/file.metalink"))
    }

    @Test
    fun `generic https url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://cdn.example.com/video.mp4"))
    }

    @Test
    fun `twitter url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://twitter.com/user/status/123"))
    }
}
