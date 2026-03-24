# meDownloader

meDownloader is an Android download manager built with Jetpack Compose and powered by aria2c.

## Features

- Multi-connection downloads through aria2 JSON-RPC.
- Queue, pause, resume, and remove download tasks.
- Share URL or magnet links from other apps into meDownloader.
- Foreground service with per-download notifications.
- Dashboard, stats, settings, and paywall flows.
- Optional Pro unlock via RevenueCat for higher limits and premium features.

## Tech Stack

- Android SDK: compile 35, min 29.
- Kotlin + Jetpack Compose (Material 3).
- OkHttp + kotlinx-serialization for RPC networking.
- DataStore for settings persistence.
- Prebuilt aria2c binaries in `app/src/main/jniLibs`.

## Build and Run

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

Note: `:app:lintDebug` currently reports pre-existing lint issues (for example in `app/src/main/res/xml/backup_rules.xml`).

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
