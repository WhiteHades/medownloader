# meDownloader — Domain Glossary

## Core Concepts

- **Download Engine** — A seam that abstracts the download mechanism. Callers interact with one uniform interface; behind it, engine selection, protocol routing, and fallback logic are internal implementation details. One engine instance handles one download at a time.

- **Primary Engine** — The yt-dlp backend. Handles URL extraction (1000+ site support), format selection, and byte transfer. Configured to use aria2c as its external downloader for multi-connection speed.

- **Fallback Engine** — The bare aria2c backend. Used when yt-dlp fails (extraction error, Python crash) — it downloads the raw URL directly. Also used directly for non-HTTP protocols (torrent, magnet, metalink).

- **Engine Pool** — Manages N concurrent engine instances (yt-dlp × N + aria2c × N). One instance per download. Isolation means a crash in one download doesn't affect others.

- **Protocol Router** — Engine selection logic. Torrent-family URLs (magnet, `.torrent`, `btih`, `.metalink`, `ftp://`) go to Fallback Engine. `http(s)` URLs whose host matches a small whitelist of media platforms (youtube, vimeo, twitter, tiktok, twitch, soundcloud, reddit, instagram, facebook, dailymotion, bilibili, streamable, rumble, odysee) go to Primary Engine. Every other `http(s)` URL goes to Fallback Engine. See ADR-0002.

- **Chaquopy Bridge** — The Python↔Kotlin integration layer. Allows calling yt-dlp's Python API from Kotlin, and registering Kotlin coroutine-based progress callbacks as Python callables.

- **Download Session** — A single URL being actively downloaded. Owned by one engine instance in the pool. Has a unique identifier, progress state, and lifecycle (queued → active → paused → completed).

- **Tier Limits** — RevenueCat-backed restrictions on concurrency, speed, and feature access. Enforced at the repository level, before engine allocation.

- **Disk-Space Probe** — A seam (`DiskSpaceProbe`) that reports bytes available on the filesystem backing the downloads directory. The repository composes it with a pure `evaluateDiskSpace(...)` function to answer `Sufficient | Insufficient | Unknown`. The ViewModel uses this to fail fast on obviously-doomed downloads. See ADR-0004.

- **Download History Entry** — A persisted record of a terminated download (COMPLETE or ERROR). Stored via `DownloadHistoryRepository` in DataStore. The Stats screen reads this as its source of truth; it is never re-derived from the aria2 session file.

## Architectural Principles

- **One adapter = hypothetical seam. Two adapters = real seam.** yt-dlp is the second adapter, making DownloadEngine a real seam.
- **Caller owns lifecycle.** The repository explicitly starts, pauses, resumes, and stops each download. The engine doesn't auto-manage.
- **Fail open per download.** A crash in one engine instance doesn't affect others in the pool.
- **Degrade gracefully.** yt-dlp failure → aria2c fallback. Never silently drop a download.
