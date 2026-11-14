# QRISHook

Aplikasi Android untuk otomatisasi webhook: saat pemicu terjadi (notifikasi masuk / interval),
kirim HTTP request ke URL webhook.

Spesifikasi lengkap: `docs/PRD.md`, `docs/UI_SPEC.md`. Rencana task: `PROMPTS.md`.

## Build & Run

Prasyarat:
- JDK 17
- Android SDK (compileSdk 34), set `ANDROID_HOME` atau `local.properties`:
  `sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk`

```bash
./gradlew assembleDebug          # build APK debug
./gradlew installDebug           # install ke device/emulator
./gradlew testDebugUnitTest      # unit test
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Atau buka folder ini di Android Studio (Ladybug+) dan Run `app`.

## Stack

Kotlin · Jetpack Compose (M3) · Navigation Compose · Hilt (KSP) · Room (KSP) ·
OkHttp · kotlinx.serialization · WorkManager · EncryptedSharedPreferences.
