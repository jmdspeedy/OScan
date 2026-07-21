# OScan

OScan is a privacy-first, offline document-scanning project written in Kotlin. The current Phase 1 MVP provides a desktop-testable engine that detects a document, corrects its perspective, applies a high-contrast scan filter, and exports the result as a PDF.

All document detection and image processing run locally. Images and telemetry are not sent to a remote service.

## Current capabilities

- Hybrid four-corner detection using an offline DocQuadNet ONNX model with a classical OpenCV line-based fallback
- Perspective correction from detected or supplied corners
- MVP grayscale “Magic” filter using CLAHE and adaptive thresholding
- Single-page A4 PDF export
- Desktop batch runner with five checked-in visual regression fixtures

Phase 2 will port this workflow to Android with a basic Jetpack Compose UI, interactive crop handles, system image selection, PDF saving, and sharing.

## Repository layout

```text
core-engine/      Kotlin/JVM detection, crop, enhancement, and PDF pipeline
desktop-tester/   Batch CLI for exercising the Phase 1 pipeline
docs/             Design, testing, and Phase 2 implementation documentation
test-images/      Phase 1 inputs and manually approved visual references
```

## Requirements

- JDK 17 or newer
- Internet access on the first build to resolve Gradle dependencies
- A compatible Microsoft Visual C++ runtime on Windows if OpenCV native loading fails

The Gradle wrapper is included, so a separate Gradle installation is unnecessary.

## Run the desktop pipeline

From the repository root on Windows:

```powershell
.\gradlew.bat :desktop-tester:run
```

The runner processes each `.jpg` in `test-images/` except names containing `_expected`. Generated boundary overlays, corrected crops, filtered images, and PDFs are written to `test-images/output/`.

See [the Phase 1 testing guide](docs/testing_guide.md) for acceptance checks and troubleshooting.

## Documentation

- [Design specification](docs/design_doc.md)
- [Phase 1 testing guide](docs/testing_guide.md)
- [Phase 2 Android implementation instructions](docs/phase2_instructions.md)

## Technology

- Kotlin/JVM 1.9.22 and Java 17
- OpenCV 4.9 desktop bindings
- ONNX Runtime 1.24.1
- Apache PDFBox 3.0.1
- Gradle 8.5

The DocQuadNet model is distributed with its upstream license and notice in `core-engine/src/main/resources/docquad/`.

## Project status

Phase 1 is an MVP. The checked-in fixtures provide manual visual regression coverage; automated image-quality comparison and Android support are planned work. The current Magic filter favors readable black-and-white output and is expected to evolve after the initial Android port.

