# OScan

[![Build APK](https://github.com/jmdspeedy/OScan/actions/workflows/build-apk.yml/badge.svg)](https://github.com/jmdspeedy/OScan/actions/workflows/build-apk.yml)

OScan is a privacy-first, offline document-scanning app written in Kotlin.

All document detection and image processing run locally on-device using OpenCV and ONNX Runtime. Images and telemetry are never sent to a remote service.

## Current capabilities

- **Offline Machine Learning & Classical Detection**: Hybrid four-corner detection using MakeACopy's DocQuadNet ONNX model with a robust OpenCV line-based classical fallback.
- **Interactive Jetpack Compose UI**: 4 draggable corner handles clamped to image bounds, live polygon rendering, and corner geometry validation.
- **Perspective Correction**: Precise 4-point perspective warp from confirmed corners.
- **Magic Enhancement Filter**: Grayscale + CLAHE + adaptive thresholding for clear background whitening and legible text.
- **Recoverable Multi-page Imports**: Ordered image import, per-page review and retry, reordering, and durable local document saving.
- **Local Document Library**: Recent and All documents collections, persistent grid/list and sort preferences, document metadata, rename, favorites, folder moves, and Trash moves.
- **Document Inspection**: Multi-page detail grids and an edge-to-edge page viewer with zoom, pan, double-tap zoom, and accessible page navigation.
- **Single-page PDF Export**: SAF (Storage Access Framework) saving and Android content URI sharing via system share sheet.
- **Desktop Tester**: Desktop batch harness (`:desktop-tester`) preserving desktop visual regression testing.

## Repository layout

```text
core-engine/      Platform-agnostic Kotlin detection, crop, filter, geometry & coordinate logic
android-app/      Jetpack Compose Material 3 Android app module
desktop-tester/   Phase 1 desktop batch runner for visual regression testing
docs/             Design, testing, and Phase 2 implementation documentation
test-images/      Inputs and manually approved visual references
```

## Requirements

- JDK 17 or newer
- Android SDK (API level 34 build-tools, minSdk 26)
- Internet access on the first build to resolve Gradle dependencies
- A compatible Microsoft Visual C++ runtime on Windows for desktop OpenCV native loading

The Gradle wrapper is included.

## Build and Run Instructions

### 1. Build Android Debug APK

```powershell
.\gradlew.bat :android-app:assembleDebug
```

The APK will be generated at `android-app/build/outputs/apk/debug/android-app-debug.apk`.

### 2. Run Desktop Visual Tester

```powershell
.\gradlew.bat :desktop-tester:run
```

Output artifacts (boundary overlay, crop, magic filter, PDF) will be written to `test-images/output/`.

### 3. Run Unit Tests

```powershell
.\gradlew.bat :core-engine:test :android-app:test
```

## Documentation

- [Product design specification](docs/DESIGN.md)
- [Feature backlog and status](docs/FEATURES.md)
- [Phase 1 testing guide](docs/testing_guide.md)
- [Android Studio run and testing guide](docs/android_testing_guide.md)
- [Phase 2 Android implementation instructions](docs/phase2_instructions.md)

## Technology

- Kotlin 1.9.22 & Java 17
- Jetpack Compose & Material 3
- Official OpenCV Android AAR 4.12.0 / Desktop OpenPnP OpenCV 4.9
- ONNX Runtime for Android 1.22.0 / JVM 1.24.1
- Android Platform `PdfDocument` / Desktop Apache PDFBox
- Gradle 8.7 and Android Gradle Plugin 8.5.2
