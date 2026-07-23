# OScan Design Specification

## Product direction

OScan is a privacy-first, open-source document scanner. Image analysis is performed locally; Phase 1 does not upload images or telemetry.

## Current repository structure

- `:core-engine` contains the Phase 1 Kotlin/JVM scanning pipeline.
- `:desktop-tester` is a batch CLI used to exercise that pipeline on Windows or another desktop JVM.
- `test-images/` contains the five Phase 1 input/reference pairs.
- `test-images/output/` contains the generated diagnostic images and PDFs.

The root build uses Kotlin 1.9.22, Gradle 8.7, Android Gradle Plugin 8.5.2, and a Java 17 toolchain.

## Phase 1 — completed MVP

Phase 1 established an end-to-end, PC-testable pipeline:

1. Load a document photograph.
2. Detect four document corners.
3. draw the detected boundary for inspection.
4. Perspective-warp the detected document to a rectangle.
5. Apply the MVP “magic” filter.
6. Export the enhanced result as a one-page A4 PDF.

### Document detection as implemented

`DocumentScanner.detectCorners()` rejects empty or very small inputs and then uses two detection paths in order:

1. **Offline ML detector.** The bundled DocQuadNet-256 ONNX model receives a 256 × 256, mid-gray-letterboxed RGB tensor. It produces TL/TR/BR/BL corner heatmaps. Peaks are refined to sub-pixel coordinates, mapped back to the source image, and accepted only when the quadrilateral is convex, sufficiently large, and within reasonable image bounds.
2. **Classical fallback.** If the model cannot load or its result is implausible, the image is reduced to at most 900 pixels on its longest side. The engine builds grayscale/blurred and CLAHE-enhanced representations, combines two Canny edge maps, and collects line candidates using both LSD and probabilistic Hough transforms. Near-duplicate lines are removed. Pairs of roughly parallel, separated lines are intersected into candidate quadrilaterals, which are filtered and ranked by geometry, detected edge support, color change across the sides, observed line coverage, area, and center alignment.

The fallback intentionally does not require a single connected four-point contour. This improves recovery when glare, shadows, or low paper/background contrast interrupt a border. A weak result is rejected instead of returning a misleading crop.

`DocumentScanner.cropWarped()` expects corners in TL/TR/BR/BL order. It estimates the physical rectangle ratio from projective vanishing geometry, snaps common sheets to A-series or Letter proportions, keeps unusual receipt ratios, and preserves the best-observed edge at native sampling density.

### Magic filter as implemented

`ImageEnhancer.applyMagicFilter()` converts the page to Lab, removes large-scale illumination gradients from luminance, and applies restrained local contrast. A chroma-aware mask median-smooths bright neutral paper and maps it through a nonlinear white shoulder, suppressing paper/sensor texture without whitening coloured stamps, logos, or table fills. Chroma is modestly strengthened before a final unsharp mask. It returns a full-resolution colour image. `applyFilter()` also exposes Original, Gray, and adaptive B&W treatments.

### PDF export as implemented

`PdfExporter` uses Apache PDFBox to create one A4 page, load the filtered image, scale it proportionally to fit the page, center it, and save the PDF. It does not currently support multiple pages, searchable text, explicit JPEG quality controls, page rotation, or document metadata.

### Desktop test harness as implemented

The CLI scans `test-images/` for `.jpg` files, ignores files whose names contain `_expected`, and processes inputs in filename order. For each successfully detected input it creates:

- `<name>_step1_box.jpg` — original image with the detected quadrilateral, corners, and side markers;
- `<name>_step2_cropped.jpg` — perspective-corrected document;
- `<name>_step3_magic.jpg` — colour-preserving Magic enhancement;
- `<name>_step3_grayscale.jpg` and `<name>_step3_black_white.jpg` — alternate treatment previews; and
- `<name>_step4_output.pdf` — one-page A4 PDF.

Detection failure is reported per image and does not stop the remaining batch. The checked-in `test1` through `test5` images and their `_expected` references provide a visual regression set; there is no automated pixel or geometry comparison yet.

### Phase 1 dependencies and assets

- OpenCV 4.9.0 desktop bindings via `org.openpnp:opencv:4.9.0-0`
- ONNX Runtime JVM 1.24.1
- Apache PDFBox 3.0.1
- Bundled `docquadnet256_trained_opset17.ort` model and its license/notice files

These exact JVM dependencies are desktop choices. Android needs platform-compatible variants while preserving the public behavior and model preprocessing.

### Known Phase 1 limitations

- Detection is evaluated against a small five-image visual fixture set.
- Reference images are not compared automatically.
- Corner correction is not interactive.
- The magic filter is binary and has no user-adjustable modes.
- PDF output is single-page only.
- Native resources are not explicitly released throughout the pipeline; Android lifecycle and memory pressure require extra care.
- There are no automated unit, integration, Android instrumentation, or performance tests yet.

## Phase 2 — Android port with basic UI

Phase 2 ports the completed behavior to an installable Android application. Its definition of done is a single-image, on-device workflow:

`Choose image → detect corners → review/adjust crop → crop → apply MVP filter → export/share PDF`

The detailed implementation brief and acceptance criteria are in [`phase2_instructions.md`](phase2_instructions.md).

### Phase 2 scope

- Create an Android app module using Kotlin and Jetpack Compose.
- Import one image from the system photo/document picker. Camera capture is optional only if it does not delay the core flow.
- Run all detection and processing offline and away from the main thread.
- Display the proposed four-corner crop and allow the user to drag each corner.
- Preview the cropped result and toggle between Original and Magic.
- Export a one-page PDF through Android-compatible storage APIs and open the system share sheet.
- Handle loading, no-document-detected, processing failure, and retry states.
- Preserve the desktop tester as the Phase 1 regression harness.

### Explicitly out of scope for Phase 2

- Multi-page scanning and document libraries
- Room/database persistence, folders, accounts, or cloud sync
- OCR and searchable PDFs
- A redesign of document detection or the magic filter
- Automatic capture, live camera edge overlays, advanced editing, or production-grade visual polish

## Later phases

- **Phase 3:** multi-page document management, reordering, folders, and local persistence.
- **Phase 4:** on-device OCR, text search, and searchable PDF output.
