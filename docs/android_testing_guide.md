# OScan Android Studio Run and Testing Guide

This guide explains how to import, run, and manually test the Phase 2 Android app. The Android application module is `:android-app`, its application ID is `com.oscan.android`, and it requires Android API 26 or newer.

Use Android Studio's bundled JDK (JDK 21 is verified) when running Robolectric tests. Java 24 is not compatible with the current Robolectric 4.11.1 dependency.

## Milestone 3 library checks

After saving at least two multi-page documents:

1. Restart the app and verify both documents appear under **Recent** and **All documents**.
2. Switch between grid and list, change the sort order, restart, and verify both choices persist.
3. Open a document and verify its name, date, page count, optional folder, and every page thumbnail.
4. Rename it, toggle Favorite, and move it to an existing folder; return Home and verify the updated metadata and ordering.
5. Open a page, pinch to zoom, pan, double-tap to toggle zoom, and use Previous/Next to inspect every page.
6. Return to Home and verify the prior scroll position is retained.
7. Move the document to Trash and verify it disappears from Home.
8. For resilience testing, remove a managed thumbnail or processed page asset from app-private storage in a debug environment and verify OScan shows an unavailable-image state without crashing.

## Fixing the “Module not specified” configuration

If the Run/Debug Configurations window shows `<no module>` and `Error: Module not specified`, do not save that configuration yet. Android Studio has not imported the new `:android-app` module into its Gradle project model.

1. Click **Cancel** in the Run/Debug Configurations window.
2. Confirm that Android Studio opened the repository root—the folder containing `settings.gradle.kts`—rather than opening `android-app/` by itself.
3. Select **File → Sync Project with Gradle Files**. You can also use the Gradle/elephant sync button in the toolbar.
4. Wait for the sync indicator to finish and inspect the **Build** window for errors.
5. In the Project panel, confirm that `android-app` appears as an Android application module.

The module is already declared by this line in the root `settings.gradle.kts`:

```kotlin
include(":android-app")
```

If sync still does not import it:

1. Open **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**.
2. Set **Gradle JDK** to Android Studio's embedded JDK 17, or another installed JDK 17.
3. Open **Tools → SDK Manager** and install **Android SDK Platform 34** plus current SDK Platform Tools.
4. Sync again. If needed, close Android Studio and reopen the root `OScan` folder.

Do not manually edit `.idea/gradle.xml`; a successful Gradle sync regenerates the module list.

## Create the Android run configuration

Android Studio often creates a working configuration automatically after sync. If it does not:

1. Open **Run → Edit Configurations**.
2. Delete the red, unnamed configuration or select it for editing.
3. Click **+** and choose **Android App**.
4. Set the fields as follows:

| Field | Value |
|:---|:---|
| Name | `OScan Android` |
| Module | `OScan.android-app` or `android-app`—select whichever Android Studio displays |
| Deploy | `Default APK` |
| Launch | `Default Activity` |
| Install flags | Leave empty |
| Launch flags | Leave empty |
| Before launch | Keep the default Gradle-aware make/build task |

Leave **Clear app storage before deployment** off for ordinary testing. Turn it on only when deliberately testing a fresh-install state. **Allow multiple instances** is not required and may be turned off.

Click **Apply**, then **OK**. The red “Module not specified” message should be gone.

If the Module dropdown remains empty, return to the previous section: the issue is Gradle sync, not the values in this dialog.

## Choose a test device

### Emulator

1. Open **Tools → Device Manager**.
2. Select **+ → Create Virtual Device**.
3. Choose a recent Pixel phone profile.
4. Select an API 34 system image. Download it if Android Studio prompts you.
5. Finish setup and start the emulator with its ▶ button.
6. Wait until the Android home screen is responsive.

### Physical Android phone

1. Enable Developer options by tapping **Build number** seven times in the phone's About screen.
2. Enable **USB debugging** under Developer options.
3. Connect the phone by USB and accept its RSA/debugging prompt.
4. Select the phone in Android Studio's device dropdown.

The app supports Android 8.0/API 26 and later. A physical device is recommended for the final memory and photo-picker checks.

## Build and launch

Select **OScan Android**, choose the running emulator or phone, and press the green **Run ▶** button. Android Studio should build, install, and launch `com.oscan.android.MainActivity`.

The initial screen should show **OScan Document Scanner** and a **Choose Image** button.

You can also verify the build from PowerShell in the repository root:

```powershell
.\gradlew.bat :android-app:assembleDebug
```

The resulting APK is:

```text
android-app/build/outputs/apk/debug/android-app-debug.apk
```

## Put the regression images on the device

The app uses Android's system photo picker; it does not browse the PC's `test-images/` directory directly.

For an emulator, drag `test-images/test1.jpg` from Windows Explorer onto the running emulator. Android normally copies it to Downloads. Repeat for the images you want to test. If the photo picker does not show a newly copied file immediately, open the Files app once or restart the picker.

For a physical phone, transfer the images by USB, Nearby Share/Quick Share, or another local method. Do not use sensitive real documents for test runs.

Start with `test1.jpg`, then run all `test1.jpg` through `test5.jpg`. The `_expected.jpg` files are visual references, not app inputs.

## Test the complete user flow

For each fixture:

1. Tap **Choose Image** and select the image in the system picker.
2. Confirm that **Detecting document corners…** appears and the UI remains responsive.
3. On the crop screen, check that the green polygon follows the document.
4. Drag every corner handle. Verify that handles track the finger and remain inside the image.
5. Deliberately make an invalid or crossed shape. The polygon should turn red, an error message should appear, and **Crop & Warp** should be disabled.
6. Tap **Reset Corners**, then align the corners with the page.
7. Tap **Crop & Warp**. The resulting preview should be rectangular and contain the intended document.
8. Switch between **Original** and **Magic**. Magic should produce readable black-and-white output with a generally white background.
9. Tap **Back to Crop** once and confirm that editing can continue. Crop again.
10. Tap **Export PDF**, choose a location, keep or change `Scanned_Document.pdf`, and save it.
11. Confirm that **PDF Created Successfully!** appears.
12. Tap **Share PDF** and confirm that Android's share sheet opens. You do not need to send the file anywhere.
13. Open the saved PDF in a PDF viewer and verify that it contains one readable page without clipping or stretching.
14. Tap **Scan Another Document** and confirm that the app returns to its start state.

If automatic detection fails, the app should show an inset manual crop and the message **Could not auto-detect document edges. Please adjust corners manually.** This is a valid recovery path, not necessarily a test failure.

## Additional manual checks

Run these at least once:

- Rotate the device on the crop and preview screens; the app should not crash or corrupt corner positions.
- Send the app to the background during detection, return to it, and check that it remains usable.
- Select another image while revising a crop and confirm the old processing result does not replace the new one.
- Cancel the photo picker and PDF save dialogs; the app should return without crashing.
- Enable airplane mode and repeat import, crop, filter, export, and share-sheet opening. Processing must work offline.
- Try one high-resolution phone photo and watch for a long UI freeze or out-of-memory crash.
- Check that no document content or local URI is printed unnecessarily in Logcat.

## Automated checks

Run local unit tests from PowerShell:

```powershell
.\gradlew.bat :core-engine:test :android-app:testDebugUnitTest
```

With an emulator or phone connected, run the Android instrumentation test:

```powershell
.\gradlew.bat :android-app:connectedDebugAndroidTest
```

The instrumentation test creates a synthetic document, validates its corners, exports a PDF in app cache, and verifies that the PDF is non-empty.

To run it in Android Studio, open `PipelineIntegrationTest.kt` and click the green run icon beside the class or test method.

## Troubleshooting

### Module dropdown is empty

The Gradle Android module was not imported. Cancel the configuration, sync Gradle, use JDK 17, ensure SDK Platform 34 is installed, and inspect the first sync error in the Build window.

### No target devices are shown

Start an emulator in Device Manager or reconnect the USB-debugging phone. For a phone, accept its authorization prompt and try a data-capable USB cable.

### SDK location error

Use **Tools → SDK Manager** to install/configure the SDK. Android Studio normally writes the machine-specific `local.properties` file automatically. Do not commit that file.

### Gradle daemon exits or Android Studio creates `.hprof` files

This indicates a JVM/IDE memory failure rather than an app run configuration problem. Stop Gradle from a terminal:

```powershell
.\gradlew.bat --stop
```

Then restart Android Studio, close memory-heavy applications, confirm Gradle uses JDK 17, and sync again. Heap dumps are ignored by Git and can be removed after debugging if no longer needed.

### App installs but does not launch

Confirm the configuration uses **Default Activity** and the `android-app` module. In Logcat, filter by package `com.oscan.android` and inspect the first `FATAL EXCEPTION` entry.

### Native OpenCV or ONNX error

Capture the complete Logcat stack trace and note the device model, Android version, and CPU architecture. Also confirm that the error occurs in the debug APK produced from the current checkout.

If the app reports **Failed to initialize OpenCV native libraries**, first sync Gradle and rebuild the app. The Android module must resolve `org.opencv:opencv:4.12.0`; `org.openpnp:opencv` is the desktop-only dependency used by `core-engine` and must remain excluded from the Android runtime. In **Build → Analyze APK**, confirm that `lib/<device-abi>/libopencv_java4.so` is present.

### Android reports that the app is not 16 KB compatible

Do not dismiss this warning for a release build. OScan uses native OpenCV and ONNX Runtime libraries, so every packaged `.so` must have 16 KB-aligned ELF load segments and the APK must package uncompressed native libraries on 16 KB boundaries. The project uses Android Gradle Plugin 8.5.2, Gradle 8.7, ONNX Runtime Android 1.22.0, and the official OpenCV Android AAR 4.12.0 to meet those requirements. Rebuild the APK instead of testing an older installed build, then use **Build → Analyze APK** and inspect the Alignment column under `lib/`.

### PDF saves but cannot be shared

First confirm that the saved PDF opens from the Files app. Then inspect Logcat while tapping **Share PDF** and record whether the failure comes from URI access, the share activity, or PDF generation.
