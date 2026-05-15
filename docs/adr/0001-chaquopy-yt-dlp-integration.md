# ADR-0001: Chaquopy 17.0.0 for yt-dlp Android Integration

**Date:** 2026-05-11
**Status:** Accepted

## Context

meDownloader needs to integrate yt-dlp (Python) as a download backend inside the Android app. Two approaches were evaluated:

1. **PIE binary (subprocess):** Compile CPython + yt-dlp as an ARM binary, launch via ProcessBuilder, communicate over stdio.
2. **Chaquopy:** Gradle plugin that embeds Python runtime into the APK. Call yt-dlp's Python API directly from Kotlin.

## Decision

Use Chaquopy 17.0.0 (option 2). Published on the Chaquopy Maven repository
(`com.chaquo.python:com.chaquo.python.gradle.plugin:17.0.0`).

## Rationale

- **Cleaner API:** Direct function calls (`YoutubeDL(params).download([url])`) vs. string-parsing stdout. Progress hooks become native Kotlin callbacks.
- **Single process model:** No subprocess management. yt-dlp runs in-process via the Python GIL, which simplifies lifecycle.
- **Python 3.10** selected for widest package compatibility across Chaquopy's supported versions (3.10–3.14).
- **Aria2c stays a subprocess:** aria2c binaries remain as PIE subprocesses (same pattern as today). Each yt-dlp engine instance gets its own aria2c subprocess on a unique RPC port.

## Consequences

- **minSdk bumped to 24** (Chaquopy minimum)
- Build dependency on the public Chaquopy Maven repository
- Python GIL means yt-dlp calls are blocking — must run on `Dispatchers.IO`
- yt-dlp progress hooks bridge via `Consumer<Map<String, Any?>>` → Chaquopy auto-converts to Python callable
- `MeDownloaderApp` starts Python via `Python.start(AndroidPlatform(context))` in `onCreate()`
- yt-dlp is configured with `'external_downloader': 'aria2c'` so bytes transfer uses aria2c's multi-connection power
