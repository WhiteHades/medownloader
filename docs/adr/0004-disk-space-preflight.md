# ADR-0004: Preflight disk-space check before starting a download

**Date:** 2026-05-12
**Status:** Accepted

## Context

aria2 runs with `--file-allocation=falloc`, which pre-allocates the full file at
download start. On filesystems that cannot satisfy the allocation (typically an
emulator with a small userdata partition, or a device low on storage), aria2
errors out within ~1 second. The download flashes into the dashboard, transitions
to `ERROR`, and then — because the dashboard only rendered `ACTIVE` / `COMPLETE`
cards — disappears silently. Users have no feedback.

We want to fix the silent-disappear bug _and_ prevent obviously-doomed downloads
from ever starting.

## Decision

Introduce a `DiskSpaceProbe` seam with an Android-backed default
(`AndroidDiskSpaceProbe` using `StatFs`) and a pure comparison function
`evaluateDiskSpace(freeBytes, expectedBytes, headroomBytes)` that returns
`Sufficient | Insufficient | Unknown`.

- The repository exposes `checkDiskSpaceFor(expectedBytes: Long?)`.
- `MainViewModel.addDownload` calls it immediately before
  `repository.addDownload(...)`. When the result is `Insufficient`, the
  ViewModel emits `UiEvent.ShowError("not enough free space: need X, have Y")`
  and never submits the download.
- When the result is `Unknown` (usually: `FileInfo.size` is null because the
  server did not return a Content-Length), we do **not** block — aria2 will
  learn the real size during download.
- Default headroom is 256 MiB, covering aria2's `.aria2` control file and
  general filesystem safety margin.

## Rationale

- A `DiskSpaceProbe` seam keeps the comparison logic unit-testable without the
  Android framework (`DiskSpaceTest`).
- A tri-state (`Sufficient | Insufficient | Unknown`) is more honest than a
  boolean. We only fail closed when we are sure.
- Doing the check in the ViewModel — not inside the engine — keeps the engine
  free of error-surfacing concerns and matches the existing pattern (tier
  limits, URL validation).

## Consequences

- Downloads with a known, too-large size now fail fast with a clear message
  instead of appearing briefly and vanishing.
- Downloads where the server omits `Content-Length` behave as before (try and
  see what happens). That is acceptable.
- The ERROR path on the dashboard is still user-visible — see the Failed
  section added in the same change set — so mid-download failures (quota hit,
  network drop, falloc failing on a device we couldn't inspect beforehand)
  remain actionable via retry / dismiss.
