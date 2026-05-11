# meDownloader — Domain Glossary

## Core Concepts

- **Download Engine** — A seam that abstracts the download mechanism. Callers interact with one uniform interface; behind it, engine selection, protocol routing, and fallback logic are internal implementation details. One engine instance handles one download at a time.

- **Primary Engine** — The yt-dlp backend. Handles URL extraction (1000+ site support), format selection, and byte transfer. Configured to use aria2c as its external downloader for multi-connection speed.

- **Fallback Engine** — The bare aria2c backend. Used when yt-dlp fails (extraction error, Python crash) — it downloads the raw URL directly. Also used directly for non-HTTP protocols (torrent, magnet, metalink).

- **Engine Pool** — Manages N concurrent engine instances (yt-dlp × N + aria2c × N). One instance per download. Isolation means a crash in one download doesn't affect others.

- **Protocol Router** — Engine selection logic: HTTP/HTTPS URLs → Primary Engine (yt-dlp), with Fallback Engine on failure. Magnet/torrent/metalink → Fallback Engine directly. Non-HTTP raw URLs (FTP, etc.) → Fallback Engine directly.

- **Chaquopy Bridge** — The Python↔Kotlin integration layer. Allows calling yt-dlp's Python API from Kotlin, and registering Kotlin coroutine-based progress callbacks as Python callables.

- **Download Session** — A single URL being actively downloaded. Owned by one engine instance in the pool. Has a unique identifier, progress state, and lifecycle (queued → active → paused → completed).

- **Tier Limits** — RevenueCat-backed restrictions on concurrency, speed, and feature access. Enforced at the repository level, before engine allocation.

## Architectural Principles

- **One adapter = hypothetical seam. Two adapters = real seam.** yt-dlp is the second adapter, making DownloadEngine a real seam.
- **Caller owns lifecycle.** The repository explicitly starts, pauses, resumes, and stops each download. The engine doesn't auto-manage.
- **Fail open per download.** A crash in one engine instance doesn't affect others in the pool.
- **Degrade gracefully.** yt-dlp failure → aria2c fallback. Never silently drop a download.
