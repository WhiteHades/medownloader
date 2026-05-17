# ADR-0005: Stable local id for downloads, decoupled from aria2's hex GID

**Date:** 2026-05-17
**Status:** Accepted

## Context

`DownloadRepositoryImpl` previously used the URL itself as the gid in
`activeDownloads` (the Job/owner registry) and wrote a `QUEUED` placeholder
into `downloadRegistry` under the same URL key when the pool was full. But the
`Aria2Engine.download` flow emits `DownloadProgress` with **aria2's hex gid**
(returned from `aria2.addUri`) — not the URL. The repository was forwarding
those events into `downloadRegistry[progress.gid]`, i.e. under the hex key.

When the pool was full, the registry ended up with **two rows for one logical
download**: a URL-keyed `QUEUED` ghost plus a hex-keyed live row.
Pause/resume/remove on the URL ghost called `fallbackEngine.{pause,resume}(url)`,
which aria2 rejected as an unknown gid; the result was a silently-dropped UI
action. `removeDownload(hexGid)` looked up `activeDownloads[hexGid]`, found
nothing, and never cancelled the in-flight collection job — so any further
events from the engine could leak back into the registry after remove returned.

This bug class is hard to spot from reading code; we only locked it down once
we built a regression test at the repository seam that fills the pool, queues a
download, drains it, and asserts there is exactly one row per logical download.

## Decision

The repository now uses a **stable local id** (currently the URL itself) as the
single key for everything user-facing:

- `activeDownloads` is keyed by `localId`.
- `downloadRegistry` is keyed by `localId`. `handleProgress(localId, progress, …)`
  normalises every engine emission onto that key, regardless of what the engine
  put in `progress.gid`.
- A separate `realGidToLocalId: ConcurrentHashMap<String, String>` records the
  aria2 hex gid that the engine reports for a given localId.
- `pause`/`resume`/`remove` accept either the localId or the hex gid; they
  resolve to the localId, look up the `ActiveEntry` (which carries
  `realGid: String?`), and route the engine call using the real aria2 gid.
- `removeDownload` cancels the active job, drops the registry entry, and
  removes the reverse mapping. Engine events that arrive after cancel cannot
  re-add the row.

The QUEUED placeholder is now also keyed by `localId`, so when the engine
starts and emits with the hex gid, the placeholder is **overwritten** rather
than stacked alongside a new row.

## Architectural side-effect

To make this testable without Android, OkHttp, or a live aria2c, we extracted
narrow seams in `data/repository/RepositorySeams.kt`:

- `EngineProcessController` — what the repository needs from
  `Aria2ProcessManager` (`isRunning()`, `start()`, `stop()`).
- `DownloadRpcOps` — the three `Aria2RpcClient` methods the repo actually
  calls (`shutdown`, `getGlobalStat`, `changeGlobalOption`).
- `DownloadHistorySink`, `DownloadQueueStore` — what the repo writes to.

Production classes implement these via their existing API. Tests construct
`DownloadRepositoryImpl` with in-memory fakes and exercise the gid-mapping,
queue-drain, and pause/resume/remove paths directly.

## Consequences

- The UI sees one stable row per download. Pause/resume/remove always reach
  aria2 with the correct hex gid.
- Notification actions (which had only the engine-level gid) keep working
  because `pauseDownload`/`removeDownload` accept either id.
- Tests at the repository seam are now possible. Five regression tests
  (`DownloadRepositoryImplTest`) lock down the bug class.
- The `context: Context` parameter on the repository is gone; it was unused.

## Out of scope (not verified by these tests)

The same incident report also called out three speculative aria2c-process-side
suspicions: a startup race on `--rpc-listen-all` readiness, `--file-allocation=falloc`
failing on emulator userdata, and `--rpc-allow-origin-all=false` rejecting OkHttp
WebSocket upgrades. Patches for all three are included in the same change but
**have not been verified with a feedback loop**: they require a live aria2c
process and at minimum a Robolectric or instrumented test. They are flagged
here so the next debugger does not assume they are confirmed root causes.
