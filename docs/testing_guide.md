# OScan Phase 1 Testing Guide

This guide describes the Phase 1 desktop batch harness as it exists in the repository. It exercises corner detection, perspective correction, the MVP magic filter, and single-page PDF export without an Android emulator.

## Prerequisites

- JDK 17 or newer (`java -version`)
- An internet connection for the first dependency download
- On Windows, a compatible Microsoft Visual C++ runtime if OpenCV native loading fails

Use the checked-in Gradle wrapper; a global Gradle installation is not required.

## Test inputs

Inputs belong in `test-images/`, not in the repository root. The runner processes every `.jpg` file in that directory except filenames containing `_expected` (case-insensitive).

The repository currently contains:

- `test1.jpg` through `test5.jpg` — pipeline inputs
- `test1_expected.jpg` through `test5_expected.jpg` — visual references

The `_expected` files are not read or compared by the program. They are for manual visual comparison. Keep additional expected files named with `_expected` so the runner does not treat them as inputs.

## Run the full Phase 1 check

From the repository root in PowerShell or Command Prompt:

```powershell
.\gradlew.bat clean :desktop-tester:run
```

For a faster rerun that retains prior build outputs:

```powershell
.\gradlew.bat :desktop-tester:run
```

The runner creates `test-images/output/` when needed. For each successfully processed input, inspect:

1. `<name>_step1_box.jpg` — the overlay should follow the actual page boundary and label four sensible corners.
2. `<name>_step2_cropped.jpg` — the page should be rectangular, upright relative to the supplied corner order, and should not omit meaningful page content.
3. `<name>_step3_magic.jpg` — text should remain legible and the background should generally be white. The current filter is intentionally a grayscale adaptive binary filter; color retention and perfect shadow removal are not Phase 1 pass criteria.
4. `<name>_step4_output.pdf` — the PDF should open, contain one centered page image, preserve the image aspect ratio, and fit on A4 without clipping.

The console should finish with `Pipeline completed successfully for all images!`. Note that this message means the batch loop finished; individual detection failures are printed as errors and must still be reviewed.

## Phase 1 acceptance checklist

- The Gradle build and desktop runner complete without an unhandled exception.
- All five checked-in input images produce all four expected output artifact types.
- Each boundary overlay selects the intended document rather than a background object.
- Each warped crop contains the document with plausible perspective correction.
- Each magic-filter result remains readable.
- Each generated PDF opens successfully and shows the corresponding filtered result.
- No network service is contacted while processing images; dependency download during the build is separate from runtime processing.

Because the current project has no automated assertions, this is a build plus manual visual acceptance test, not a unit-test suite.

## Adding a regression image

1. Add a clearly named `.jpg` input to `test-images/`.
2. Optionally add a manually approved reference named `<name>_expected.jpg`.
3. Run the desktop tester.
4. Inspect all four generated artifacts.
5. Keep the input small enough for source control and avoid documents containing private information.

Useful cases include low-contrast paper/background, patterned surfaces, partial shadows, glare, perspective distortion, and documents close to the image boundary.

## Troubleshooting

### `Could not detect a document`

Both the offline model result and the classical fallback were rejected. Confirm that most of the document is visible, its corners form a plausible convex quadrilateral, and it occupies a meaningful part of the image. This is a valid failure mode; the engine avoids returning a weak crop.

### `Failed to load ...`

The image is missing, unreadable, corrupt, or unsupported by OpenCV. Confirm that it is a valid `.jpg` directly inside `test-images/`.

### OpenCV native library error

The desktop runner calls `OpenCV.loadLocally()`. On Windows, install/repair the Microsoft Visual C++ runtime and confirm the process architecture matches the JDK. Then retry from a clean terminal.

### ONNX model does not load

The scanner silently uses its OpenCV fallback if the bundled model or ONNX Runtime cannot initialize. Confirm that `core-engine/src/main/resources/docquad/docquadnet256_trained_opset17.ort` is packaged if ML behavior is specifically under test.

### Out-of-memory or very slow processing

Try a smaller source image. Classical detection works on a downscaled copy, but perspective crop, enhancement, diagnostic JPEGs, and PDF export still operate on the full detected crop.

## Phase 2 testing handoff

Phase 2 must keep this desktop regression check working. Follow the hands-on setup and validation steps in the [Android Studio run and testing guide](android_testing_guide.md). The original implementation requirements remain in [`phase2_instructions.md`](phase2_instructions.md).
