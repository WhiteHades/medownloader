package com.medownloader.data.engine

import org.junit.Assert.*
import org.junit.Test

class ProtocolRouterTest {

    // --- media hosts -> yt-dlp ---

    @Test
    fun `youtube watch url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://www.youtube.com/watch?v=abc123"))
    }

    @Test
    fun `youtu_be short url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://youtu.be/abc123"))
    }

    @Test
    fun `vimeo url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://vimeo.com/12345"))
    }

    @Test
    fun `twitter url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://twitter.com/user/status/123"))
    }

    @Test
    fun `x_com url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://x.com/user/status/123"))
    }

    @Test
    fun `tiktok url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://www.tiktok.com/@user/video/123"))
    }

    @Test
    fun `twitch clip url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://www.twitch.tv/abc/clip/xyz"))
    }

    @Test
    fun `soundcloud url routes to yt-dlp`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://soundcloud.com/artist/track"))
    }

    // --- direct file URLs -> aria2c ---

    @Test
    fun `generic https cdn with mp4 routes to aria2c`() {
        // previously this went to yt-dlp, which blocked direct-file downloads from showing up.
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://cdn.example.com/video.mp4"))
    }

    @Test
    fun `generic http file url routes to aria2c`() {
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("http://example.com/file.zip"))
    }

    @Test
    fun `github release asset routes to aria2c`() {
        assertEquals(
            EngineType.ARIA2C,
            ProtocolRouter.route("https://github.com/someone/repo/releases/download/v1/app.apk")
        )
    }

    // --- torrent-family -> aria2c ---

    @Test
    fun `magnet uri routes to aria2c`() {
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("magnet:?xt=urn:btih:abc&dn=test"))
    }

    @Test
    fun `torrent file routes to aria2c`() {
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://example.com/file.torrent"))
    }

    @Test
    fun `btih query routes to aria2c`() {
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

    // --- malformed / edge cases -> aria2c ---

    @Test
    fun `garbage input routes to aria2c and does not throw`() {
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("not-a-url"))
    }

    @Test
    fun `whitespace is trimmed`() {
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("  https://youtu.be/abc  "))
    }
}
