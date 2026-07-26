# OScan

[![Build APK](https://github.com/jmdspeedy/OScan/actions/workflows/build-apk.yml/badge.svg)](https://github.com/jmdspeedy/OScan/actions/workflows/build-apk.yml)

OScan is a privacy-first Android document scanner and local document library. It turns photos into clean, perspective-corrected documents entirely on the device—no accounts, cloud processing, telemetry, or document uploads.

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

Document detection, image enhancement, library management, encryption, and export all run locally. OScan does not require an account and does not send images or telemetry to a remote service.

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

The debug APK is written to `android-app/build/outputs/apk/debug/android-app-debug.apk`.

Run the unit tests with:

```powershell
.\gradlew.bat :core-engine:test :android-app:test
```

Run the desktop visual harness with:

```powershell
.\gradlew.bat :desktop-tester:run
```

The desktop harness expects local test fixtures in the ignored `test-images/` directory.

## Ownership

OScan is a personal project maintained by its owner. The repository is published as a showcase and is not accepting external contributions.

## License

OScan is released under the [MIT License](LICENSE). Third-party models and libraries retain their respective licenses and notices.
