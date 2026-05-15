# meDownloader

meDownloader is an Android download manager built with Jetpack Compose and powered by yt-dlp plus aria2c.

## Features

- Multi-connection downloads through aria2 JSON-RPC.
- Queue, pause, resume, and remove download tasks.
- Share URL or magnet links from other apps into meDownloader.
- Foreground service with per-download notifications.
- Dashboard, stats, settings, and paywall flows.
- Optional Pro unlock via RevenueCat for higher limits and premium features.

## Tech Stack

- Android SDK: compile 35, min 24.
- Kotlin + Jetpack Compose (Material 3).
- OkHttp + kotlinx-serialization for RPC networking.
- DataStore for settings persistence.
- Prebuilt aria2c binaries in `app/src/main/jniLibs`.

## Build and Run

Use JDK 21 for all Gradle tasks. If you use `mise`, the included `mise.toml` pins the right version automatically after `mise install`.

```bash
./gradlew :app:assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Validation Commands

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Note: `:app:lintDebug` currently reports pre-existing lint issues (for example in `app/src/main/res/xml/backup_rules.xml`). CI treats lint as non-blocking for this reason; the lint HTML report is uploaded as a workflow artifact on every run.

## Continuous Integration

Every push and PR to `main` runs `.github/workflows/ci.yml`:

- `compileDebugKotlin`
- `testDebugUnitTest` (uploads the HTML report if tests fail)
- `assembleDebug` (uploads `app-debug.apk` as an artifact)
- `lintDebug` (non-blocking, report uploaded)

## Local Git Hooks

Install the pre-push hook once per clone:

```bash
./scripts/install-hooks
```

The hook symlinks `scripts/hooks/pre-push` into `.git/hooks/pre-push`. It runs `compileDebugKotlin` and `testDebugUnitTest` before every push, mirroring CI. Skip with `git push --no-verify` if you know what you are doing.

## Emulator Helper

`scripts/test-android` wraps the Android emulator and the debug install cycle:

```bash
./scripts/test-android boot      # boot only (resizes userdata first)
./scripts/test-android install   # gradle installDebug
./scripts/test-android launch    # launch the app
./scripts/test-android logs      # stream filtered logcat
./scripts/test-android resize    # ensure userdata >= $MD_USERDATA_SIZE
./scripts/test-android all       # boot + install + launch
```

Env overrides:

- `MD_AVD_NAME` (default `Medium_Phone`)
- `MD_USERDATA_SIZE` (default `128G`) — the target `disk.dataPartition.size` for the AVD. Large enough to test multi-GB downloads without hitting `fallocate` failures.
- `ANDROID_HOME`, `ANDROID_AVD_HOME`, `ANDROID_SDK_HOME`, `JAVA_HOME`

## Project Layout

```text
app/src/main/
  java/com/medownloader/
    data/          data models, repositories, RPC client
    di/            service locator
    presentation/  viewmodels and compose screens
    service/       foreground download service
    ui/theme/      compose theme tokens and theme setup
  jniLibs/         prebuilt aria2c binaries per ABI
  res/             android resources
```
