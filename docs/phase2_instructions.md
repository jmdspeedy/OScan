# Phase 2 Implementation Instructions — Android MVP

## Objective

Port the completed Phase 1 single-image scanning pipeline to an installable Android app with a basic Jetpack Compose UI. Preserve current detection, crop, filter, and PDF behavior before improving algorithms.

The required user flow is:

`Choose image → detect → adjust four corners → crop → preview Original/Magic → export PDF → share`

All image processing and inference must remain on-device and work without network access.

## Start by protecting Phase 1

1. Run the desktop acceptance procedure in `docs/testing_guide.md` and retain the generated artifacts as the baseline.
2. Do not remove `:desktop-tester` or make it depend on Android.
3. Keep platform-independent scanner decisions and preprocessing rules aligned between desktop and Android. If code cannot be shared because native types or dependencies differ, isolate platform adapters and document any intentional behavioral difference.
4. Do not tune the magic filter during this phase. Android output should visually match the Phase 1 grayscale/CLAHE/adaptive-threshold result closely enough for the existing fixtures.

## Project and dependency work

1. Add an Android application module, preferably `:android-app`, using Kotlin, Jetpack Compose, Material 3, and a supported Android Gradle Plugin/JDK combination.
2. Select and document a minimum SDK supported by every native dependency. Do not guess that the desktop artifacts will run on Android:
   - replace the OpenPnP desktop OpenCV binding with an Android-compatible OpenCV package;
   - replace ONNX Runtime JVM with ONNX Runtime for Android;
   - use an Android-compatible PDF implementation or Android's platform PDF APIs;
   - package the DocQuadNet model and its license/notice in the Android app.
3. Keep dependency versions centralized and avoid bundling desktop native binaries in the APK.
4. Add only the permissions actually required. Prefer the system photo picker and Storage Access Framework so broad media/storage permissions are unnecessary.

## Processing architecture

Create a small app-facing boundary so Compose code never manipulates OpenCV `Mat` objects directly. A suitable contract should expose:

- decoded image metadata and a display preview;
- detected corners in source-image coordinates;
- perspective crop using user-confirmed corners;
- Original and Magic preview results;
- PDF export to a caller-provided destination; and
- structured success/failure results instead of console output.

Requirements:

- Decode the selected URI through `ContentResolver`; do not assume a filesystem path.
- Correct EXIF orientation before detection and keep one documented coordinate system for source, preview, and crop-overlay transforms.
- Run decode, ONNX inference, OpenCV processing, and PDF writing off the main thread using coroutines.
- Initialize native runtimes once and expose initialization failure as a recoverable UI state.
- Close/release tensors, sessions, matrices, streams, and temporary bitmaps deterministically.
- Downsample only for display/detection as appropriate; perform the accepted perspective crop at sufficient resolution for a readable PDF.
- Avoid holding the original, multiple full-resolution intermediates, and PDF bytes in memory simultaneously.
- Keep the ML-first/classical-fallback behavior and the same plausibility checks unless an Android API difference makes that impossible.

## Basic UI

Use a single activity and a small number of screens/states. Visual polish is secondary to a reliable workflow.

### 1. Start state

- App title and a primary **Choose image** action.
- Use the Android photo picker where available, with a compatible system-document fallback.

### 2. Detecting state

- Show a progress indicator and prevent duplicate processing actions.
- Preserve enough state to retry after a recoverable error.

### 3. Crop state

- Display the selected image with the detected quadrilateral.
- Provide four draggable corner handles.
- Clamp handles to image bounds and prevent an invalid/self-intersecting quadrilateral.
- Correctly map gestures through content scaling/letterboxing back to source-image coordinates.
- Provide **Reset**, **Retake/Choose another**, and **Crop** actions.
- If automatic detection fails, initialize the handles to a reasonable inset rectangle and explain that manual adjustment is required.

### 4. Preview state

- Show the perspective-corrected image.
- Provide at least **Original** and **Magic** choices; Magic uses the existing Phase 1 filter.
- Provide **Back to crop** and **Export PDF** actions.

### 5. Export result

- Let the user create a `.pdf` using the Storage Access Framework.
- Show success or a useful error without losing the scan state.
- Provide a **Share** action using a content URI and Android's share sheet; never expose a raw file URI.

## State and lifecycle

- Model the workflow with explicit states such as Empty, Loading, CropReady, Processing, PreviewReady, Exporting, and Error.
- Keep heavy image objects out of saveable UI state. Preserve the selected URI and lightweight edit state across ordinary configuration changes, then reload/recompute safely when necessary.
- Cancellation must stop obsolete work when the user selects another image or leaves the flow.
- Disable actions whose prerequisites are unavailable and prevent two exports or processing jobs from running concurrently.

## Error handling

Provide actionable UI for:

- unsupported or unreadable images;
- permission/URI access loss;
- native library or model initialization failure;
- automatic detection failure, with manual crop fallback;
- invalid corner geometry;
- processing or out-of-memory failure; and
- cancelled or failed PDF creation/share.

Do not crash or silently fall back to an unrelated output.

## Verification requirements

### Automated checks

- Unit-test coordinate transforms between source images, letterboxed previews, and overlay gestures.
- Unit-test corner validation/order rules and workflow state transitions.
- Add at least one Android integration/instrumentation check that loads a fixture, performs crop/filter, and produces a non-empty, readable PDF.
- Keep `:desktop-tester:run` working as the Phase 1 regression check.

### Manual checks on an emulator and one physical device

For all five Phase 1 fixture images:

- import succeeds without broad storage permission;
- automatic corners are plausible or manual fallback is usable;
- every handle can be moved accurately, including near image edges;
- the crop matches the confirmed overlay;
- Original and Magic previews render correctly;
- the exported PDF opens and shares successfully; and
- enabling airplane mode does not break scanning or export.

Also verify:

- portrait and landscape images with EXIF rotation;
- configuration change/background-and-return behavior;
- detection failure and retry;
- cancellation and re-selection while processing;
- a large phone photo without a crash or prolonged UI freeze;
- app behavior on the selected minimum SDK and current target SDK; and
- no image or document data appears in logs.

Record approximate detection, crop/filter, and export duration plus peak-memory observations for at least one typical modern phone. These are baseline measurements, not optimization targets.

## Definition of done

Phase 2 is complete when:

- a debug APK builds from a clean checkout using documented commands;
- the app completes the full required flow on an emulator and a physical Android device;
- all processing works offline;
- automatic detection retains a usable manual-crop fallback;
- the MVP magic output is behaviorally consistent with Phase 1;
- a valid one-page PDF can be saved and shared through Android content URIs;
- the five existing fixtures pass the Android manual regression run;
- required automated checks pass;
- the desktop Phase 1 harness still passes; and
- setup, build, run, architecture differences, dependencies/licenses, and known limitations are documented.

## Non-goals

Do not add multi-page documents, OCR, database-backed document management, cloud features, accounts, camera auto-capture, live edge detection, new filter algorithms, or elaborate visual design. Capture these as future work instead of expanding Phase 2.
