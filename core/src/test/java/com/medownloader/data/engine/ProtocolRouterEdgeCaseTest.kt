package com.medownloader.data.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Additional ProtocolRouter edge-case coverage beyond the existing ProtocolRouterTest.
 * Focuses on: subdomain variants, case-insensitivity, query-string edge cases, and
 * all whitelisted media hosts.
 */
class ProtocolRouterEdgeCaseTest {

    // --- all whitelisted media hosts ---

    @Test fun `instagram url routes to yt-dlp`() =
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://www.instagram.com/p/abc123/"))

    @Test fun `facebook video routes to yt-dlp`() =
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://www.facebook.com/watch?v=123"))

    @Test fun `dailymotion routes to yt-dlp`() =
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://www.dailymotion.com/video/x7abc"))

    @Test fun `bilibili routes to yt-dlp`() =
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://www.bilibili.com/video/BV1xx"))

    @Test fun `streamable routes to yt-dlp`() =
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://streamable.com/abc123"))

    @Test fun `rumble routes to yt-dlp`() =
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://rumble.com/v-abc.html"))

    @Test fun `odysee routes to yt-dlp`() =
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://odysee.com/@channel/video"))

    @Test fun `reddit video routes to yt-dlp`() =
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://www.reddit.com/r/videos/comments/abc/"))

    // --- case insensitivity ---

    @Test fun `uppercase scheme is handled`() =
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("HTTP://example.com/file.zip"))

    @Test fun `mixed-case youtube host routes to yt-dlp`() =
        assertEquals(EngineType.YT_DLP, ProtocolRouter.route("https://WWW.YOUTUBE.COM/watch?v=abc"))

    // --- torrent-family edge cases ---

    @Test fun `meta4 extension routes to aria2c`() =
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://example.com/file.meta4"))

    @Test fun `magnet with no dn param routes to aria2c`() =
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("magnet:?xt=urn:btih:abc123"))

    // --- direct-file URLs that must NOT go to yt-dlp ---

    @Test fun `pdf download routes to aria2c`() =
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://example.com/document.pdf"))

    @Test fun `apk download routes to aria2c`() =
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://example.com/app.apk"))

    @Test fun `zip download routes to aria2c`() =
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://example.com/archive.zip"))

    @Test fun `unknown host with mp4 extension routes to aria2c`() =
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("https://storage.example.com/video.mp4"))

    // --- empty / blank ---

    @Test fun `empty string routes to aria2c without throwing`() =
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route(""))

    @Test fun `blank string routes to aria2c without throwing`() =
        assertEquals(EngineType.ARIA2C, ProtocolRouter.route("   "))
}
