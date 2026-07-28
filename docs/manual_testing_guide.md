# OScan Owner Manual Testing Guide

This is a private, hands-on checklist for testing normal OScan workflows on an Android phone or emulator. It intentionally excludes Gradle commands, automated tests, and internal fault injection. Use only synthetic or disposable images—never real identity, financial, medical, or confidential documents.

## How to use this guide

Run the sections in order on a clean install for a full pass. For a smaller regression pass, run the cases marked **Smoke**. Mark each case `PASS`, `FAIL`, `BLOCKED`, or `SKIPPED` and add a short note beside any result that is not `PASS`.

Before starting, record:

- App version/build:
- Device or emulator:
- Android version:
- Screen size/orientation:
- Start state: clean install / upgrade / existing data
- Tester/date:

Prepare these disposable fixtures:

- A clear one-page document with visible margins
- Three distinct pages labeled 1, 2, and 3
- A skewed or low-contrast document
- Front and back images of a fake ID-style card
- Optional: one non-image or damaged image for an import failure check

When something fails, record the case ID, exact step, expected result, actual result, whether it reproduces, and a screenshot or short recording.

## 1. Launch and navigation

### NAV-01 — First launch **Smoke**

1. Install or clear OScan's app data, then launch it.
2. Observe the initial screen and dismiss no system prompts unless a test asks you to.
3. Move between `Library`, `Scan`, and `Me` using the bottom navigation (or navigation rail on a wide screen).

Expected: OScan launches without an account, network request, or crash. The empty Library is understandable, each destination opens, and the selected destination is visibly highlighted.

### NAV-02 — Swipe and Back behavior

1. Swipe horizontally between the three main destinations.
2. Open a secondary screen from `Me`, then press Android Back.
3. From an empty Scan screen, press Back.

Expected: swiping and navigation controls stay synchronized. Back closes the current secondary screen before leaving the app. Back from Scan returns to Library when no scan is in progress.

### NAV-03 — Relaunch state

1. Leave the app from each main destination, remove it from Recents, and relaunch.
2. Rotate the physical device while on each destination.

Expected: the app returns to a safe, usable state with no blank screen, duplicated screen, or crash. OScan remains in portrait even when device auto-rotate is enabled and the device is held horizontally.

## 2. Camera permission and capture

### CAM-01 — Denied camera permission **Smoke**

1. Open `Scan` on a clean install.
2. Deny camera permission.
3. Try the permission action again; if available, deny permanently.
4. Use `Import from gallery`.

Expected: the same `Allow camera access` explanation and action remain visible after each denial. The action reopens the Android permission popup while the system permits it; after Android permanently suppresses that popup, it opens OScan's app-permission settings instead. Gallery import remains usable in Document mode.

### CAM-02 — Grant permission and camera controls **Smoke**

1. Grant camera permission and wait for preview.
2. Aim at a document, then at an empty background.
3. Toggle the torch/flash where supported and the camera grid.
4. Switch between `Document` and `ID card`, then return to `Document`.

Expected: preview starts promptly; guidance responds plausibly to the scene; supported controls update visibly; unsupported flash is clearly unavailable; mode changes do not capture or lose data.

### CAM-03 — Capture quality and feedback **Smoke**

1. Frame the clear document with all edges visible.
2. Tap the shutter once.
3. Repeat with the document skewed and under poorer light.

Expected: each tap creates only one page, capture feedback occurs according to settings, the shutter cannot be spammed while capturing, and processing advances to edge adjustment without a crash.

### CAM-04 — Capture session navigation

1. Capture one page, return to the camera, then capture a second page.
2. Press Done/Finish.
3. Start another scan, accept one page, press Back, and choose `Keep editing`.
4. Press Back again and choose `Discard`.

Expected: accepted-page count is correct; Finish opens review; keeping preserves the session; discarding removes the in-progress session only after confirmation.

## 3. Import images

### IMP-01 — Single-image import **Smoke**

1. From Scan, choose `Import from gallery` and select the clear document.
2. Complete crop and treatment.

Expected: the picker returns to OScan, the image is oriented correctly, and it follows the same crop/review flow as camera capture.

### IMP-02 — Multi-image import and ordering **Smoke**

1. Import pages 1, 2, and 3 in that order.
2. Watch the import progress and complete each page.
3. Inspect the session review.

Expected: progress reaches the selected count; all valid images appear once and in selection order; a failure on one item is explained without silently duplicating other pages.

### IMP-03 — Picker cancellation and bad input

1. Open the picker and cancel it.
2. If the picker permits it, try the damaged or unsupported fixture alongside valid images.

Expected: cancellation returns to the unchanged app state. Bad input produces a useful error while recoverable valid work remains available.

## 4. Crop and enhancement

### CRP-01 — Automatic and manual crop **Smoke**

1. Process the clear document and inspect the proposed boundary.
2. Drag each corner, then drag each full edge.
3. Move a handle slowly near every image boundary.
4. Use `Reset edges`.

Expected: the initial crop follows the paper or uses a safe inset; handles track the finger; the polygon stays on-image; the magnified/precision feedback is useful; Reset restores a sensible boundary.

### CRP-02 — Invalid crop prevention

1. Attempt to cross corners or make an extremely thin/invalid crop.
2. Try to complete the crop.

Expected: invalid geometry is visibly identified and cannot be accepted. Restoring a valid shape re-enables the action.

### CRP-03 — Treatments **Smoke**

1. Accept a crop and compare `Original`, `Magic`, `Grayscale`, and `B&W`.
2. Choose each treatment once, waiting for the preview to settle.
3. Accept the desired result.

Expected: every option produces a distinct, stable preview; text remains readable; no option unexpectedly changes crop or orientation; the accepted page uses the selected treatment.

### CRP-04 — Portrait lock and lifecycle

1. Rotate the physical phone during crop and again during treatment processing.
2. Confirm the app remains portrait, then background OScan and return.

Expected: the current page/session is restored or safely resumed, controls remain usable, and no accepted page is duplicated.

## 5. Multi-page review and save

### SCN-01 — Review page set **Smoke**

1. Reach review with pages 1, 2, and 3 accepted.
2. Confirm thumbnails, count, and order.
3. Reorder pages; remove one; retry or replace one; add another page if those actions are shown.

Expected: every action updates the count and order immediately, applies to the intended page, and preserves the other pages.

### SCN-02 — Save document **Smoke**

1. Continue to Save Document.
2. Enter a unique name and select no folder.
3. Save and open the saved document.

Expected: the saved confirmation shows the right name and page count; exactly one document appears in Library; its pages and treatment match review.

### SCN-03 — Save directly into a folder

1. Create a folder in Library.
2. Scan or import another document.
3. Select that folder on Save Document and save.

Expected: the document appears in the chosen folder and in the appropriate all-documents view, with no duplicate unfiled copy.

### SCN-04 — Interrupted session recovery

1. Start and accept a multi-page scan but do not save it.
2. Background the app, rotate it, then force-stop and relaunch.

Expected: OScan restores the recoverable scan with correct pages/order or gives a clear safe recovery choice; it does not create a partial Library document without confirmation.

## 6. ID-card workflow

### IDC-01 — Capture/import both sides **Smoke**

1. Switch camera mode to `ID card`.
2. Capture the fake front and then the fake back. If import is offered for this mode, repeat using fixtures.
3. Adjust both boundaries and continue.

Expected: the UI clearly requests front then back, each side is assigned correctly, boundaries can be adjusted, and the result is upright, consistently scaled, and unclipped.

### IDC-02 — Incomplete pair

1. Start ID-card mode and capture only the front.
2. press Back or leave Scan.

Expected: OScan clearly warns about or abandons the incomplete pair; it never saves a misleading two-sided result.

### IDC-03 — ID-card output

1. Complete both sides and inspect available combined-image/PDF results and rounded-corner choices.
2. Save and open each available output type.

Expected: front precedes back, both sides have consistent scale and margins, rounded corners do not remove content, and every output opens successfully.

## 7. Library browsing and organization

### LIB-01 — Library display **Smoke**

1. Ensure at least three differently named documents and two folders exist.
2. Switch between available filters/tabs and grid/list presentation.
3. Close and relaunch OScan.

Expected: thumbnails, names, dates, page counts, folder membership, and display preference are correct and persist.

### LIB-02 — Search and sort **Smoke**

1. Search using a full document name, partial name, different letter case, a folder name, and a nonexistent term.
2. Clear search.
3. Try every sort option: modified newest/oldest, created newest/oldest, and name A–Z/Z–A.

Expected: only name/folder matches appear; empty results are clear; clearing restores all items; every sort order is correct and remains selected after relaunch.

### LIB-03 — Rename and favorite

1. Open a document, rename it, and toggle Favorite.
2. Return to Library and filter for favorites if available.
3. Relaunch.

Expected: the new name and favorite state update everywhere and persist. Empty or invalid names are rejected or safely normalized.

### LIB-04 — Selection and bulk actions **Smoke**

1. Enter selection mode and select multiple documents.
2. Favorite them, then move them into a folder.
3. Select multiple documents again and move them to Trash, first cancelling and then confirming.

Expected: selected count is accurate; cancel changes nothing; each confirmed action affects only selected items; selection mode exits cleanly.

### LIB-05 — Folder lifecycle

1. Create and rename a folder.
2. Open it, move a document into it, and move the document back to no folder/Unfiled.
3. Put documents in the folder and delete the folder.

Expected: folder names and counts update; moves do not duplicate documents; deleting a folder moves its documents to Unfiled and does not delete them.

## 8. Document viewing and editing

### DOC-01 — Document detail and viewer **Smoke**

1. Open a multi-page document.
2. Open each page full-screen and use previous/next controls.
3. Pinch to zoom, pan, and double-tap to zoom/reset.

Expected: detail metadata is correct; the selected page opens; navigation stops at first/last page; zoom is smooth and resets when changing pages.

### DOC-02 — Page operations **Smoke**

1. Add a page by camera and by import where offered.
2. Reorder pages and rotate one page.
3. Re-crop and change one page's treatment.
4. Remove one page, cancelling first and then confirming.

Expected: every operation affects only the intended page; page order/count and thumbnail update; edits persist after leaving and relaunching; the final page cannot be removed if that would create an invalid document, or the outcome is clearly explained.

### DOC-03 — Back/cancel during editing

1. Begin a page edit, change crop or treatment, then press Back/Cancel.
2. Reopen the document.
3. From review, choose `Add pages` → `Take picture`, capture one image, then press Back through treatment and crop without accepting it.
4. Repeat using `Add pages` → image import.

Expected: cancellation does not silently commit incomplete edits and the previous saved page remains readable. Backing out of a newly captured or imported page discards that image completely; it does not leave a `Needs review` entry.

## 9. Export and sharing

### EXP-01 — Save PDF **Smoke**

1. Open a one-page document and choose `Export / Save`.
2. Select PDF and each available quality; note estimated size.
3. Save through the Android file picker and open the result.
4. Repeat with a multi-page document.

Expected: the picker uses a sensible filename; the saved file opens; pages are ordered, oriented, sized, and cropped correctly; quality/size estimates are plausible.

### EXP-02 — Save images **Smoke**

1. Export a single page as JPG and PNG.
2. Export a multi-page document as JPG and PNG.
3. Open the resulting files or archive.

Expected: single-page output is a normal image; multi-page output preserves every page in order (using the app's multi-file/ZIP behavior); PNG correctly shows no lossy quality choice.

### EXP-03 — Share and cancel

1. Choose Share for PDF, JPG, and PNG; inspect the system share sheet, then cancel.
2. Open Export again, start Save, then cancel the system picker.
3. Export successfully afterward.

Expected: the correct type and filename reach the share sheet; cancellation returns to the unchanged document; no duplicate Library item is created; later export still succeeds.

### EXP-04 — Export after edits/restart

1. Rotate, reorder, and reprocess pages, then force-stop and relaunch.
2. Export the edited document.

Expected: the export reflects all saved edits exactly and contains no stale page version.

## 10. Trash

### TRS-01 — Move, restore, and delete **Smoke**

1. Move a document to Trash and confirm it disappears from normal Library/search.
2. Open Trash and restore it.
3. Trash it again and permanently delete it, cancelling once before confirmation.

Expected: restore returns the intact document; cancellation is harmless; permanent deletion removes it only after confirmation.

### TRS-02 — Empty Trash

1. Put several disposable documents in Trash.
2. Choose Empty Trash, cancel, and verify all remain.
3. Repeat and confirm.

Expected: the dialog reports the correct scope/count; confirmation permanently clears Trash; no unrelated Library or Vault item is removed.

## 11. Vault

Use disposable content only. Destructive Vault cases are intentionally last in this section.

### VLT-01 — Set up Vault **Smoke**

1. Open Vault from Library or `Me` and start setup.
2. Try a passcode shorter than 6 digits, a mismatch, and then a valid 6–12 digit passcode.
3. Acknowledge the recovery warning and create the Vault.

Expected: invalid/mismatched input is rejected clearly; the warning explains that recovery is impossible; valid setup succeeds without exposing content.

### VLT-02 — Lock and unlock **Smoke**

1. Lock or background OScan, then reopen Vault.
2. Try a wrong passcode, then the correct one.
3. Make repeated wrong attempts until lockout, if practical.

Expected: Vault locks after leaving/backgrounding as designed; incorrect input reveals no document data; correct input unlocks; repeated failures show remaining lockout time.

### VLT-03 — Biometric unlock

1. On a device with a strong enrolled biometric, enable biometric unlock after passcode confirmation.
2. Lock and test biometric success, cancellation, and failure.
3. Use passcode fallback, then disable biometric unlock.

Expected: biometric never replaces the passcode recovery boundary; cancellation/failure reveals nothing; fallback works; disabling removes the biometric option.

### VLT-04 — Move into and out of Vault **Smoke**

1. From Library selection mode, move one then several documents to Vault.
2. Lock Vault and search/browse normal Library and Trash.
3. Unlock Vault, open a moved document, then move it back to Library after confirming the warning.

Expected: moved content disappears completely from normal thumbnails, search, folders, and Trash while locked; it is readable inside Vault; moving out restores a normal intact Library document without duplication.

### VLT-05 — Vault library and Trash

1. In Vault, search, favorite, open, and inspect documents/pages.
2. Move one to Vault Trash, restore it, then move it again and permanently delete it.
3. Test Empty Vault Trash by cancelling and then confirming.

Expected: Vault operations remain inside the encrypted boundary; restore retains content; destructive actions require confirmation; normal Trash is unaffected.

### VLT-06 — Change passcode

1. In Vault settings, try an incorrect current passcode and mismatched new passcodes.
2. Change to a valid new passcode.
3. Lock and verify the old passcode fails and the new one succeeds.

Expected: only the correct current passcode authorizes the change; documents remain intact; credentials switch atomically.

### VLT-07 — Disable or reset Vault

1. With disposable documents present, test `Move documents to library & disable`.
2. Recreate Vault with disposable content and test the delete option, including the exact confirmation phrase.
3. Separately test `Forgot passcode?` reset if shown.

Expected: migration preserves documents in normal Library before disabling; delete/reset requires strong confirmation and permanently removes only Vault content; setup can be started again afterward.

## 12. Me and settings

### SET-01 — Local profile

1. In `Me`, edit the local display name and choose each avatar color.
2. Relaunch OScan.

Expected: name/color update immediately and persist locally; long or empty names do not break layout.

### SET-02 — Capture settings **Smoke**

1. Toggle shutter feedback and choose rear/front camera preference.
2. Return to Scan and capture with each available combination.

Expected: selected camera and feedback behavior match settings; unavailable hardware is handled safely; settings persist.

### SET-03 — Enhancement settings

1. Change the default treatment between Magic and Original.
2. Start a new scan after each choice.

Expected: new pages default to the selected treatment while still allowing manual changes; existing documents are unchanged.

### SET-04 — Appearance **Smoke**

1. Test Auto/System, Light, and Dark themes.
2. Test every accent palette.
3. Change Android system theme while Auto is selected.

Expected: theme and accent apply immediately across Library, Scan, dialogs, editor, Me, and Vault; text remains readable; choices persist.

### SET-05 — Language **Smoke**

1. Test System default, English, Simplified Chinese, and Japanese.
2. After each switch, visit Library, Scan, document detail, export, Me, and Vault.
3. On Android 13+, also change OScan's language in system per-app settings.

Expected: the UI updates without restart where supported, no screen mixes old/new language, user-created names remain unchanged, and long labels do not clip important actions.

### SET-06 — Storage

1. Note saved-document and temporary-cache usage.
2. Choose Clean temporary cache, cancel, then confirm.
3. Reopen several saved documents and export one.

Expected: cancellation changes nothing; cleanup reports success and removes only temporary data; saved pages/thumbnails remain usable or regenerate safely.

### SET-07 — Information screens

1. Open Privacy, About, Developer, licenses, and any external repository/profile links.
2. Return using both in-app Back and Android Back.

Expected: content is readable, app version is plausible, external links ask for/use an appropriate app, and returning restores the prior OScan screen.

## 13. Persistence, offline use, and resilience

### RES-01 — Full persistence **Smoke**

1. Set a theme/language/profile, create folders, save/favorite/edit documents, choose sort/display settings, and configure Vault.
2. Force-stop and relaunch; then reboot the device and relaunch.

Expected: all committed data and preferences persist; Vault returns locked; no in-progress or temporary UI leaks Vault content.

### RES-02 — Offline operation **Smoke**

1. Enable airplane mode.
2. Capture/import, crop, enhance, save, search, edit, unlock Vault, and export.
3. Open the share sheet (actual delivery to an online app is out of scope).

Expected: core workflows work without sign-in, upload, or connectivity error. Only the chosen external sharing target may require network.

### RES-03 — Low storage / interrupted operation

1. If safe on the test device, repeat a large import/export with little free storage.
2. Background OScan during import, enhancement, save, and export.

Expected: errors are actionable; the app does not crash or leave a corrupt/duplicate document; retry works after space is restored.

## 14. Layout and accessibility pass

### A11Y-01 — Layout matrix **Smoke**

Repeat the main smoke path at portrait phone width, split-screen/narrow width, and tablet/expanded width. Physically rotate each device once during the pass.

Expected: OScan remains portrait when the device is rotated. Navigation adapts appropriately to the available portrait width; no important control is clipped, overlapped, unreachable, or hidden under system bars/keyboard.

### A11Y-02 — Large text and display size

1. Set Android font size to 200% and increase display size.
2. Repeat navigation, capture, crop, save, document detail, export, settings, and Vault unlock.

Expected: essential text and actions remain readable/reachable; screens scroll where necessary; dialogs and keyboards do not trap controls.

### A11Y-03 — TalkBack

1. Enable TalkBack and traverse main navigation, camera controls, crop handles/alternatives, review/reorder, selection mode, dialogs, viewer, and Vault.
2. Perform a scan without relying on drag-only gestures where alternatives exist.

Expected: focus order is logical; controls have meaningful names and states; changing counts/statuses are announced; decorative elements are skipped; sensitive Vault content is not announced while locked.

## 15. Final smoke path

Run this short path before every feedback round:

1. **NAV-01** — launch and navigate.
2. **CAM-02/03** — grant permission and capture.
3. **IMP-02** — import three pages.
4. **CRP-01/03** — crop and treatments.
5. **SCN-01/02** — review and save.
6. **LIB-02/04** — search, sort, and bulk action.
7. **DOC-01/02** — view and edit pages.
8. **EXP-01/02/03** — save PDF/image and open share sheet.
9. **TRS-01** — trash and restore.
10. **VLT-01/02/04** — create, lock/unlock, move in/out.
11. **SET-04/05** — theme and language.
12. **RES-01/02** — relaunch and offline check.

## Feedback template

Copy this block for each issue:

```text
Case ID:
Result: FAIL / BLOCKED
Device + Android version:
App version/build:
Starting state:
Exact steps:
Expected:
Actual:
Reproducibility: always / sometimes / once
Screenshot or recording:
Extra notes:
```

At the end of a test pass, report:

- Passed:
- Failed:
- Blocked/skipped:
- Three most important issues:
- Any confusing behavior that technically worked:
- Overall confidence: low / medium / high
