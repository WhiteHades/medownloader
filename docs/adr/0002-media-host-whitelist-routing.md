# ADR-0002: Route only whitelisted media hosts through yt-dlp

**Date:** 2026-05-11
**Status:** Accepted

## Context

The original `ProtocolRouter` sent every `http(s)` URL through yt-dlp first and only
fell back to aria2c on failure. This produced two failure modes:

1. **Silent failures on direct file URLs.** `YtDlpEngine.download()` returned a Flow
   that emitted into a `MutableSharedFlow(replay = 0)` _before_ a collector was
   attached (see ADR-0003). Any progress event — success or failure — was dropped.
   On a direct file URL (e.g. an S3 CDN link to a PDF or zip), yt-dlp would succeed,
   but the UI would never see the download.
2. **Slow first byte.** Even for genuinely direct URLs, yt-dlp does a full extractor
   pass before delegating to aria2c. That extractor pass has no value for URLs that
   are already the real file.

## Decision

`ProtocolRouter` now routes to yt-dlp only when the URL's host matches a small,
explicit whitelist of media platforms (youtube, youtu.be, vimeo, twitter/x, tiktok,
twitch, soundcloud, reddit, instagram, facebook, dailymotion, bilibili, streamable,
rumble, odysee). Every other `http(s)` URL goes directly to aria2c. Torrent-like
schemes (magnet, `.torrent`, `btih:`, `.metalink`, `ftp://`) still go to aria2c.

## Rationale

- The set of sites that actually need yt-dlp's extractor is small and stable.
- Direct-file downloads form the 80% case; sending them to aria2c is both faster
  and avoids the lifecycle bug class fixed in ADR-0003.
- A whitelist is easy to reason about, easy to extend, and easy to test.

## Consequences

- Any new media host we care about must be added to `MEDIA_HOST_SUFFIXES`.
- URLs from unknown media sites fall back to aria2c, which will download the page
  HTML instead of the media. This is a conscious trade-off against the previous
  behaviour's silent failure mode.
- Routing is deterministic and unit-tested (`ProtocolRouterTest`).
