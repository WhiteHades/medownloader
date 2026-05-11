# ADR-0001: Chaquopy for yt-dlp Android Integration

**Date:** 2026-05-11
**Status:** Accepted

## Context

meDownloader needs to integrate yt-dlp (Python) as a download backend inside the Android app. Two approaches were evaluated:

1. **PIE binary (subprocess):** Compile CPython + yt-dlp as an ARM binary, launch via ProcessBuilder, communicate over stdio. Mirrors the existing `Aria2ProcessManager` pattern.
2. **Chaquopy:** Gradle plugin that embeds Python runtime into the APK. Call yt-dlp's Python API directly from Kotlin.

## Decision

Use Chaquopy (option 2).

## Rationale

- **Cleaner API:** Direct function calls (`YoutubeDL(params).download([url])`) vs. string-parsing stdout. Progress hooks become native Kotlin callbacks.
- **Single process model:** No subprocess management. yt-dlp runs in-process via the Python GIL, which simplifies lifecycle (no watchdog, no process health checks).
- **Acceptable size tradeoff:** ~50-80MB APK increase is acceptable for the speed and stability benefits.
- **Aria2c stays a subprocess:** aria2c binaries remain as PIE subprocesses (same pattern as today). Each yt-dlp engine instance gets its own aria2c subprocess on a unique RPC port.

## Consequences

- Build dependency on Chaquopy plugin (commercial or community license)
- Python GIL means yt-dlp calls are blocking — must run on `Dispatchers.IO`
- Each concurrent download runs in its own `YoutubeDL` instance (pool model)
- yt-dlp is configured with `'external_downloader': 'aria2c'` so bytes transfer uses aria2c's multi-connection power
