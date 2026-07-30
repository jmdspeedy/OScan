# OScan

[![Build APK](https://github.com/jmdspeedy/OScan/actions/workflows/build-apk.yml/badge.svg?event=push)](https://github.com/jmdspeedy/OScan/actions/workflows/build-apk.yml)
[![Release](https://img.shields.io/badge/release-v0.8.1-blue)](https://github.com/jmdspeedy/OScan/releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://github.com/jmdspeedy/OScan/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)

OScan is a privacy-first Android document scanner and local document library. It turns photos into clean, perspective-corrected documents entirely on the device—no accounts, cloud processing, telemetry, or app-initiated document uploads.

## What OScan can do

### Scan and enhance

- Capture documents with a live CameraX preview or import several images at once.
- Detect four document corners with the bundled DocQuadNet ONNX model and an OpenCV classical fallback.
- Refine the crop with draggable corners, full-edge controls, geometry validation, and accessible non-drag adjustments.
- Correct perspective at high resolution.
- Apply Original, colour-preserving Magic, Gray, or B&W treatments.
- Capture and align the front and back of an ID card, then create combined images or a PDF.

### Organize a private local library

- Save recoverable multi-page scan sessions as durable local documents.
- Browse Recent and All documents in grid or list layouts.
- Rename, favorite, search, sort, select, move into folders, and move documents to Trash.
- Reorder, rotate, re-crop, reprocess, add, or remove individual pages.
- Inspect pages in an edge-to-edge viewer with zoom, pan, and accessible navigation.

### Export and protect

- Export single-page or multi-page PDFs through Android's Storage Access Framework.
- Export images and multi-page image ZIPs, or share through the Android system share sheet.
- Protect sensitive documents in an encrypted local Vault with passcode and biometric unlock support.
- Keep Vault metadata, thumbnails, pages, favorites, search, and Trash behind a separate encrypted storage boundary.

### Fit the device and the reader

- Use compact, landscape, tablet, split-screen, and resizable layouts.
- Follow the system theme or select Light or Dark mode.
- Use OScan in English, Simplified Chinese, or Japanese.
- Navigate with TalkBack-friendly semantics, large touch targets, and alternatives to drag-only interactions.

## Privacy by design

Document detection, image enhancement, library management, encryption, and export all run locally. OScan does not require an account, request internet access, or send images or telemetry to a remote service.

### Android system backup

Android may include standard-library documents, scan sessions, database metadata, and preferences in system backup or device-to-device transfer, depending on the device and the user's Android backup settings. This behavior is provided by Android rather than by an OScan server. Encrypted Vault files and biometric unlock data are explicitly excluded from both cloud backup and device transfer.

Users who do not want standard OScan data included in Android backup can disable app-data backup in their device settings. Exporting or sharing a document sends it only to the location or app the user selects.

## Download

GitHub Releases provide signed, ABI-specific APKs and SHA-256 checksums. Most current Android phones use `arm64-v8a`; older 32-bit phones generally use `armeabi-v7a`. The `x86` and `x86_64` builds are intended primarily for compatible emulators and devices. Android may ask users to allow installation from their browser or file manager because OScan is distributed outside an app store.

The signing certificate is kept stable between releases so Android can verify upgrades. Download APKs only from the [official OScan releases](https://github.com/jmdspeedy/OScan/releases).

Release certificate SHA-256 fingerprint:

```text
EC:B8:BB:C2:AE:84:55:57:FE:79:51:0C:D5:CE:5E:07:C9:B2:4F:DF:97:7E:DF:55:EC:20:D4:CA:AA:3D:20:DE
```

## Project structure

```text
android-app/      Jetpack Compose Android application
core-engine/      Shared detection, crop, enhancement, geometry, ID-card, and PDF logic
desktop-tester/   Desktop visual-regression and ID-card verification harness
```

## Technology

- Kotlin 1.9.22 and Java 17
- Jetpack Compose and Material 3
- CameraX
- OpenCV 4.12.0 on Android and OpenPnP OpenCV 4.9 on desktop
- ONNX Runtime 1.22.0 on Android and 1.24.1 on desktop
- Room, DataStore, Android Keystore, and biometric authentication
- Android `PdfDocument` and desktop Apache PDFBox
- Gradle 8.7 and Android Gradle Plugin 8.5.2
- Android minSdk 26, compileSdk/targetSdk 34

## Build

Requirements: JDK 17 or newer, Android SDK 34, and internet access for the first dependency resolution.

```powershell
.\gradlew.bat :android-app:assembleDebug
```

The ABI-specific debug APKs are written to `android-app/build/outputs/apk/debug/`.

Run the unit tests with:

```powershell
.\gradlew.bat :core-engine:test :android-app:testDebugUnitTest
```

Run the desktop visual harness with:

```powershell
.\gradlew.bat :desktop-tester:run
```

The desktop harness expects local test fixtures in the ignored `test-images/` directory.

See the [complete testing guide](docs/testing_guide.md) for automated, desktop, Android, Vault, localization, accessibility, privacy, and release validation.

Maintainers should also follow the [release and signing guide](docs/releasing.md) before creating a version tag.

## Ownership

OScan is a personal project maintained by its owner. The repository is published as a showcase and is not accepting external contributions.

## License

OScan is released under the [MIT License](LICENSE). Third-party models and libraries retain their respective licenses and notices; see the [complete third-party notices](core-engine/src/main/resources/THIRD_PARTY_NOTICES.txt), which are also bundled and viewable inside the app.
