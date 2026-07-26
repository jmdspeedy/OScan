# OScan Testing Guide

This is the single testing reference for OScan. It covers automated checks, the desktop image-processing harness, Android device testing, and release-focused manual validation.

## Test layers

OScan uses complementary test layers:

1. **JVM unit tests** validate shared image geometry, enhancement, Android state management, persistence, export, Vault cryptography, localization resources, and accessibility helpers.
2. **Desktop visual regression** exercises real document and ID-card fixtures through the shared OpenCV/ONNX processing code.
3. **Android instrumentation** validates behavior that requires an emulator or physical device.
4. **Manual device testing** covers camera behavior, visual quality, system pickers, sharing, biometrics, adaptive layouts, and lifecycle transitions.

## Prerequisites

- JDK 17 or newer
- Android SDK Platform 34 for Android builds
- An Android emulator or API 26+ physical device for instrumentation and manual testing
- Internet access for the first Gradle dependency download
- On Windows, a compatible Microsoft Visual C++ runtime if desktop OpenCV native loading fails

Use the checked-in Gradle wrapper. Open the repository root—the directory containing `settings.gradle.kts`—in Android Studio.

## Automated checks

Run the shared and Android JVM test suites from the repository root:

```powershell
.\gradlew.bat :core-engine:test :android-app:testDebugUnitTest
```

These suites cover:

- Corner validation, coordinate mapping, perspective warp, and enhancement behavior
- Recoverable scan sessions and multi-page scanner state
- Local document repository and preference persistence
- PDF and document export
- ID-card detection policy
- Vault encryption and repository operations
- Locale selection and translation-resource parity
- Crop interaction and adaptive-accessibility helpers

Build the debug APK:

```powershell
.\gradlew.bat :android-app:assembleDebug
```

The APK is written to:

```text
android-app/build/outputs/apk/debug/android-app-debug.apk
```

With an emulator or device connected, run instrumentation tests:

```powershell
.\gradlew.bat :android-app:connectedDebugAndroidTest
```

Instrumentation includes the end-to-end processing pipeline, per-app language behavior, and accessibility checks. A passing JVM suite does not replace device testing for camera, picker, biometric, layout, or native-library behavior.

## Desktop document regression

Place local `.jpg` fixtures directly in `test-images/`. This directory is intentionally ignored because fixtures may contain private documents. Filenames containing `_expected` are treated as manual references rather than inputs.

Run the document harness:

```powershell
.\gradlew.bat :desktop-tester:run
```

For a clean rebuild:

```powershell
.\gradlew.bat clean :desktop-tester:run
```

Outputs are written to `test-images/output/`. For each successfully detected input, inspect:

1. `<name>_step1_box.jpg` — the quadrilateral follows the document boundary and the corners are sensible.
2. `<name>_step2_cropped.jpg` — the document is rectangular and meaningful content is not clipped.
3. `<name>_step3_magic.jpg` — illumination is even, text remains sharp, and document colours are preserved.
4. `<name>_step3_grayscale.jpg` and `<name>_step3_black_white.jpg` — alternate treatments remain readable.
5. `<name>_step4_output.pdf` — the PDF opens, preserves aspect ratio, and does not clip the page.

Review individual errors even if the batch reaches its final completion message. Weak or implausible detections are intentionally rejected instead of producing a misleading crop.

Useful regression scenes include:

- Bright paper and dark receipts
- Low-contrast or patterned backgrounds
- Shadows, glare, blur, and perspective distortion
- Documents close to the image boundary
- Unusual receipt and card aspect ratios

## Desktop ID-card regression

Name fixture pairs `card_test*_front.jpg` and `card_test*_back.jpg`, then run:

```powershell
.\gradlew.bat :desktop-tester:runIdCard
```

Inspect `test-images/output/id-cards/` for front/back boundary overlays, rectangular and rounded crops, combined sheets, and PDFs. Verify that both sides are upright, consistently scaled, ordered front then back, and not clipped.

## Android Studio setup

If Android Studio reports **Module not specified**:

1. Confirm that the repository root, not `android-app/`, is open.
2. Select **File → Sync Project with Gradle Files**.
3. Confirm that `android-app` appears as an Android application module.
4. Configure Gradle to use JDK 17.
5. Install Android SDK Platform 34 and current Platform Tools.

Android Studio normally creates a run configuration after synchronization. Otherwise create an **Android App** configuration using the `android-app` module, Default APK, and Default Activity. Do not manually edit `.idea/gradle.xml`.

For an emulator, use a recent Pixel profile with an API 34 system image. A physical device is preferred for final camera, memory, biometric, picker, and sharing checks.

## Core Android workflow

Test both live capture and multi-image import.

### Capture and import

- Deny camera permission and verify image import remains available.
- Grant permission and verify preview, lens selection, torch where supported, shutter feedback, and captured-page count.
- Capture several pages, finish capture, and confirm they enter the same review flow used by imported images.
- Import several images in a known order; include one invalid or unreadable input and verify valid pages continue processing.
- Background, rotate, or recreate the activity during processing and verify the session recovers safely.

### Crop and enhancement

- Verify detected corners match the document or fall back to a safe inset crop.
- Move every corner and full-edge target; confirm geometry stays within the image.
- Create an invalid or crossed polygon and verify confirmation is blocked with a clear message.
- Test accessible non-drag corner adjustment.
- Confirm Original, Magic, Gray, and B&W previews and saved results match the selected treatment.
- Reorder, remove, retry, replace, and add pages before saving.

### ID cards

- Select ID-card mode and capture or import front and back.
- Verify incomplete pairs are clearly handled when navigating away.
- Adjust both boundaries and confirm front/back orientation and ordering.
- Export combined images and PDF output, checking scale, rounded-corner options, and clipping.

## Library and editing

After saving multiple documents:

- Restart OScan and verify documents, thumbnails, page order, names, favorites, folders, and preferences persist.
- Switch grid/list presentation and sort order, then restart and verify persistence.
- Search names and verify the UI does not imply full-text OCR search.
- Rename, favorite, move to a folder, select several items, and move items to Trash.
- Restore from Trash, permanently delete an item, and empty Trash with confirmation.
- Open every page in the document viewer and test zoom, pan, double-tap, and previous/next navigation.
- Add, reorder, rotate, re-crop, change treatment, and remove saved pages; export afterward to confirm edits are reflected.
- In a debug environment, remove a managed thumbnail or page asset and verify a safe unavailable-image state instead of a crash.

## Export and sharing

- Export one-page and multi-page PDFs and verify page order, rotation, sizing, image quality, and filenames.
- Export a single image and a multi-page image ZIP.
- Cancel the system save dialog and confirm document/editor state is retained.
- Open the Android share sheet, cancel it, and verify no duplicate or corrupt temporary export remains.
- Repeat export after process restart.
- Confirm exported pages have no unintended crop, colour, or resolution changes.

## Vault

Use non-sensitive fixtures for all Vault testing.

- Create a Vault, reject invalid passcodes, lock it, and test successful and unsuccessful unlock attempts.
- Verify lockout behavior and remaining-time feedback after repeated failures.
- Enable biometric unlock where supported and test success, cancellation, failure, and fallback to passcode.
- Move single and selected documents into Vault; verify they disappear from the normal library while locked.
- Verify locked Vault names, thumbnails, pages, favorites, search results, and Trash do not appear outside the boundary.
- Test search, favorites, viewing, Vault Trash, restore, permanent deletion, and moving documents back out.
- Background the app and confirm Vault locks and plaintext UI/content is cleared.
- Change the passcode, disable Vault using each offered migration choice, and test reset with disposable content.
- Interrupt move or migration operations in a debug environment and verify originals are preserved or recovery completes safely.

## Localization, accessibility, and layouts

- Test System default, English, Simplified Chinese, and Japanese.
- Change language from inside OScan and through Android 13+ per-app language settings.
- Traverse every major route after a language switch; verify no stale mixed-language screen remains.
- Check long translations, formatted counts, placeholders, and user-created names.
- Test at 320dp width, compact landscape, tablet/expanded width, split screen, and 200% font scale.
- Use TalkBack to traverse primary navigation, crop adjustment, page reordering, selection mode, dialogs, camera feedback, and Vault lock state.
- Verify touch targets, focus order, contrast, system insets, reduced motion, and disabled-state explanations.

## Offline and privacy checks

- Enable airplane mode and repeat capture/import, detection, enhancement, save, search, Vault unlock, export, and share-sheet opening.
- Inspect Logcat for document pixels, content URIs, filenames, passcodes, encryption keys, raw exception messages, or database identifiers that should not be exposed.
- Confirm no account, analytics, upload, or remote-processing request is required.
- Verify Android backup rules exclude sensitive app and Vault material as designed.

## Release-focused device matrix

Before a release, include at least:

- API 26 and a current Android version
- A compact phone and an expanded/tablet layout
- One low-memory device or emulator profile
- Front and rear cameras where available
- Camera permission denied, denied permanently, and granted
- Light, dark, high-contrast, large-font, and TalkBack configurations
- Airplane mode
- Large multi-page documents and high-resolution camera images
- A device or emulator capable of biometric testing

Record the device model, Android version, CPU architecture, test input class, and exact failing step for every issue.

## Troubleshooting

### Gradle cannot download dependencies

The first build requires network access. Confirm proxy/firewall settings and retry. A global Gradle installation is not required.

### Gradle daemon exits or creates `.hprof` files

Stop Gradle:

```powershell
.\gradlew.bat --stop
```

Restart Android Studio, close memory-heavy applications, confirm Gradle uses JDK 17, and retry. Heap dumps are local diagnostics, ignored by Git, and may be deleted when no longer needed.

### Desktop OpenCV fails to load

Install or repair the Microsoft Visual C++ runtime and confirm the JDK architecture matches the native library. The desktop harness uses OpenPnP OpenCV 4.9.

### Android OpenCV or ONNX initialization fails

Rebuild the current debug APK and capture the first relevant Logcat stack trace with device model, Android version, and CPU architecture. The Android package uses the official OpenCV 4.12.0 AAR and ONNX Runtime Android 1.22.0; desktop OpenPnP artifacts must remain excluded from the Android runtime.

### Android reports 16 KB incompatibility

Test a newly built APK. Inspect packaged native libraries in Android Studio's APK Analyzer and verify their alignment rather than relying on an older installed build.

### A PDF saves but cannot be shared

First open the saved PDF from the Files app. Then reproduce while inspecting Logcat to distinguish PDF generation, content-URI permission, temporary-file, and share-activity failures.

### Processing is slow or runs out of memory

Record source dimensions and page count. Detection uses downscaled working images, but full-resolution crop, treatment, diagnostics, and export still require substantial memory. Reproduce on a low-memory device before changing processing limits.

## Test-data policy

Keep `test-images/` local and ignored. Use synthetic, public-domain, or deliberately created fixtures without personal, financial, medical, identity, or confidential information. Generated output is disposable and should not be committed.
