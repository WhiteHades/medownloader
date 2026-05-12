# ADR-0003: yt-dlp engine exposes progress via `channelFlow`

**Date:** 2026-05-11
**Status:** Accepted

## Context

`YtDlpEngine.download()` used to:

1. Create a `MutableSharedFlow<DownloadProgress>(extraBufferCapacity = 64)` with the
   default `replay = 0`.
2. Immediately run `ydl.callAttr("download", listOf(url))` inside
   `withContext(Dispatchers.IO)`, blocking the suspend function until yt-dlp
   finished.
3. Return the SharedFlow.

Because the SharedFlow used `replay = 0`, every event emitted during step 2 was
discarded — there were no subscribers yet. The repository attached its collector
in step 3, by which point all events had already been dropped. The UI therefore
never saw any progress updates or terminal event, and the download card never
appeared.

## Decision

`YtDlpEngine.download()` returns a `channelFlow { withContext(IO) { ... } }`. The
Flow is cold: the `withContext(IO)` block (and therefore the yt-dlp work) runs
inside the collector's coroutine scope, after a subscriber attaches. `trySend`
from the yt-dlp progress hook is safe from any thread and non-blocking.

## Rationale

- `channelFlow` is the idiomatic Kotlin coroutines primitive for bridging a
  callback-driven API into a cold Flow.
- It preserves the engine's existing seam (`DownloadEngine.download`) without
  forcing the repository to change.
- The fix is local to one file and verifiable by unit-test-style reasoning: the
  old code's failure mode ("events emitted before a subscriber") is structurally
  impossible with `channelFlow`.

## Consequences

- Python's GIL still blocks the collector's coroutine for the duration of the
  download. That is acceptable: the collector is on `Dispatchers.IO`, not the
  main thread, and each download has its own scope.
- A terminal COMPLETE event is now emitted explicitly after `ydl.download()`
  returns, because yt-dlp does not always fire a `"finished"` progress event.
  The repository relies on this terminal event to record history.
- See also ADR-0002 for why fewer URLs actually reach this engine now.
