# OScan Feature Backlog and Implementation Status

**Status:** Active implementation  
**Updated:** 2026-07-22  
**Platform:** Android, Jetpack Compose  
**Visual source of truth:** [`DESIGN.md`](DESIGN.md)

## Purpose

This document tracks the work required to turn OScan's durable multi-page scanning foundation into a complete local-first document manager. Implement the milestones in dependency order and expose a route only when its minimum useful flow works.

All document data and image processing must remain on-device. Do not add accounts, cloud sync, remote OCR, analytics containing document data, subscriptions, advertising, or inert `Coming soon` controls.

## Status and difficulty legend

Feature status:

- `[x]` Implemented in the current codebase.
- `[~]` Partially implemented; the remaining work is stated in the item.
- `[ ]` Not implemented.

Relative implementation difficulty:

- **Low:** Isolated UI, validation, or repository work with little architectural risk.
- **Medium:** A multi-state feature spanning a screen and an existing data/API boundary.
- **High:** Cross-layer work involving persistence, navigation, recovery, or several complex states.
- **Very high:** Hardware, image-coordinate, performance, broad accessibility, or system-integration work requiring extensive device testing.

Difficulty describes engineering complexity and risk, not product priority. It assumes the preceding milestones are complete.

## Current baseline

The repository already provides:

- An adaptive Home / Scan / Me Compose shell with light and dark OScan themes.
- A populated Home library with Recent and All documents sections, persisted grid/list and sort choices, and resilient thumbnail placeholders.
- Ordered multi-image selection through the Android system photo picker.
- Offline ONNX/OpenCV document-edge detection with a manual fallback rectangle.
- Four-corner crop adjustment, geometry validation, reset, and perspective correction.
- Original and Magic enhancement treatments.
- A Room-backed local document repository with managed original, processed, thumbnail, and temporary session assets.
- Recoverable multi-page scan sessions with per-page retry/removal, sequential review, reordering, and named local document saving.
- Stable-ID document detail and full-page viewer routes with metadata, page thumbnails, rename, favorite, folder move, Trash, zoom, pan, double-tap zoom, and page navigation.
- A legacy single-page PDF exporter remains in the codebase; the current durable-document flow still needs multi-page Save PDF and Share PDF actions.
- Unit coverage for geometry, repository behavior, session recovery, partial import failure, and the scanner ViewModel, plus an Android pipeline integration test.

The current `ScannerViewModel` uses app-private session files and durable metadata rather than transient picker URIs. Documents can be saved, found, opened, and inspected locally. There is no live camera, folder-management overview, Trash overview, or durable multi-page export flow yet.

## Implementation rules

- Use immutable `StateFlow` UI models and route-level ViewModels. Keep camera, repository, processing, and export work outside composables.
- Navigate with stable local IDs; never pass full bitmaps or raw filesystem paths between routes.
- Store original page assets separately from generated thumbnails and exports.
- Make database and file mutations transactional wherever partial completion could corrupt a document.
- Preserve usable state through configuration changes, process recreation, picker cancellation, save-dialog cancellation, and recoverable errors.
- Show user-safe messages; never expose raw URIs, exception text, database IDs, or engine terminology.
- Add normal, empty, loading, error, disabled, accessibility, and dark-mode states with each shipped feature.
- Follow the tokens and component behavior in `DESIGN.md`; do not add screen-local colors or a second visual system.

## Milestone 1 — Local document foundation (P0)

**Status:** Substantially implemented. Migration coverage and explicit repository missing-file tests remain.

Build the persistence layer before adding populated management screens.

- [x] **Medium** — Define local models for `Document`, `Page`, `Folder`, document-folder membership, favorite state, and Trash metadata.
- [~] **High** — Add a Room database with DAOs, migrations, and observable repository APIs. The version-one schema and DAOs exist; add explicit migrations when the schema changes.
- [x] **High** — Add managed local file storage for original pages, processed pages, thumbnails, and temporary scan-session assets.
- [x] **Medium** — Generate stable, correctly oriented thumbnails without loading full-resolution pages into library views.
- [x] **Low** — Add DataStore-backed user preferences for sort order, grid/list presentation, theme choice, and future capture defaults.
- [x] **Medium** — Define typed repository errors and recovery/cleanup behavior for interrupted writes.
- [~] **High** — Add repository tests covering create, update, delete, rollback, missing files, and migration behavior. Core CRUD and rollback are covered; explicit missing-file and migration tests remain.

**Complete when:** a document with one or more pages can be saved, observed, reopened after process restart, renamed, favorited, and removed without leaking temporary files.

## Milestone 2 — Multi-page import and scan sessions (P0)

**Status:** Implemented.

Replace the single-bitmap ViewModel flow with a recoverable session model.

- [x] **Medium** — Use the system photo picker with multiple selection and preserve picker order.
- [x] **High** — Track per-page import, detection, crop, treatment, progress, failure, and review status.
- [x] **High** — Continue processing valid images when one import fails; allow retry, replacement, or removal of the failed page.
- [x] **Medium** — Review imported pages sequentially with page position and a session thumbnail/count.
- [x] **High** — Allow add page, remove page, reorder pages, return to an accepted page, and finish the session.
- [x] **Low** — Prompt before discarding a session that contains accepted pages.
- [x] **High** — Retain lightweight session metadata through configuration/process recreation and reload temporary assets safely.
- [x] **High** — Save a named local document, optionally into an existing folder, rather than exporting directly from transient memory.

**Complete when:** multiple imported images can be reviewed, reordered, saved as one durable document, and recovered after recreation without reselecting the images.

## Milestone 3 — Populated Home and document detail (P0)

**Status:** Implemented. Export/share remains intentionally hidden until Milestone 4 supplies durable multi-page operations.

- [x] **High** — Populate Home from the document repository with `Recent` and `All documents` sections.
- [x] **Medium** — Implement stable-aspect-ratio document cards and rows showing thumbnail, name, date, page count, favorite state, and optional folder.
- [x] **Medium** — Support grid and list presentation, persisted locally.
- [x] **Medium** — Open a document detail route using its stable ID.
- [x] **High** — Add rename, favorite, move to folder, and move-to-Trash actions backed by repository operations. Export/share stays hidden until Milestone 4.
- [x] **Medium** — Build a multi-page detail view with a page grid and document metadata.
- [x] **High** — Build an edge-to-edge page viewer with zoom, pan, double-tap zoom, page position, and page navigation.
- [x] **Medium** — Preserve Home scroll, sort, and selection context when returning from detail.
- [x] **Medium** — Handle missing thumbnails and missing page assets without crashing.

**Complete when:** a saved document appears after restart, can be opened and inspected page by page, and all visible actions work.

## Milestone 4 — Multi-page PDF export and sharing (P0)

**Status:** Implemented.

- [x] **High** — Generate PDFs containing every document page in the selected order.
- [x] **Low** — Provide sensible defaults for page sizing, image quality, and filename.
- [x] **Medium** — Separate the editable local document from generated PDF exports.
- [x] **Medium** — Support Save PDF through SAF and Share PDF through the system share sheet.
- [x] **Low** — Prevent duplicate exports while generation is active.
- [x] **Medium** — Preserve the current document and export options when system surfaces are cancelled.
- [x] **Medium** — Report storage, permission, and generation failures with actionable retry paths.
- [x] **High** — Verify that export does not unintentionally alter the document's colors, crop, or resolution.

**Complete when:** any saved multi-page document can be exported and shared in page order, including after app restart.

## Milestone 5 — Live camera capture (P0)

- [x] **Very high** — Add CameraX preview, lifecycle binding, rotation handling, and camera permission rationale/denial states.
- [x] **Low** — Keep `Import images` usable when permission is denied or camera hardware is unavailable.
- [x] **Very high** — Run throttled, downsampled edge detection on analysis frames without blocking preview or shutter input.
- [x] **Very high** — Map detected corners accurately through sensor rotation, preview scaling/cropping, and device orientation.
- [x] **Very high** — Draw a stable proposed quadrilateral and show concise guidance for no document, edge proximity, motion/blur, and low light when measurable.
- [x] **High** — Add manual shutter, capture feedback, Torch where supported, captured-page count, Done, and session review.
- [x] **Low** — Initialize a safe inset crop when manual capture has no reliable detected boundary.
- [ ] **Very high** — Add optional auto-capture only after stable-boundary dwell, blur checks, visible countdown/progression, and capture cooldown are field-tested.
- [x] **High** — Clean transient CameraX outputs after they are copied into the recoverable session; session assets remain until explicit discard or successful save.

**Complete when:** a user can deny camera permission and still import, or capture several pages manually and complete the same review/save pipeline used by imports.

## Milestone 6 — Organization and retrieval (P1)

**Status:** Implemented.

### Search, sort, and selection

- [x] **Medium** — Search document and folder names locally with immediate results and a clear no-results state.
- [x] **Low** — Ensure search UI does not imply full-text search until on-device OCR exists.
- [x] **Medium** — Add sort by modified date, created date, and name with applicable directions.
- [x] **Low** — Add filters for all documents, favorites, and unfiled documents.
- [x] **High** — Add long-press selection mode with selected count, select all, move, favorite/unfavorite, export where supported, and Trash.
- [x] **Low** — Require count-specific confirmation for destructive bulk actions.

### Folders

- [x] **High** — Add folder overview and folder detail routes from a secondary Home Browse entry.
- [x] **High** — Support create, rename, delete, move documents, and scan-to-folder.
- [x] **Low** — Validate blank and duplicate names.
- [x] **Medium** — Deleting a folder must move its documents to Unfiled rather than deleting them.

### Trash

- [x] **High** — Move deleted documents to local Trash with deletion timestamps and previous-folder metadata.
- [x] **Medium** — Support restore, permanent delete, and Empty Trash with explicit confirmation.
- [x] **Medium** — Restore to the previous folder when it still exists, otherwise to Unfiled.
- [x] **Low** — Start with manual emptying; do not implement automatic retention until cleanup behavior is proven.

**Complete when:** library content can be found, organized, bulk-managed, deleted safely, and restored after restart.

## Milestone 7 — Page editing (P1)

**Status:** Implemented.

- [x] **High** — Add pages from camera or picker to an existing document.
- [x] **Medium** — Reorder pages with an accessible alternative to drag-and-drop.
- [x] **Medium** — Rotate left/right and persist the result non-destructively where practical.
- [x] **High** — Re-crop using the stored source image and change Original/Magic treatment.
- [x] **Medium** — Remove pages with confirmation when removing the final page or losing edits.
- [ ] **Low** — Consider duplicate-page support only after reorder, rotate, re-crop, and remove are complete.
- [x] **Medium** — Cache treatment previews and avoid recomputing unchanged results.

**Complete when:** the core page operations survive process restart and export reflects the latest saved page order and treatments.

## Milestone 8 — Me, settings, and product information (P1)

**Status:** Implemented.

- [x] **Medium** — Add editable local display name and optional local avatar; no account or remote identity.
- [x] **High** — Add Settings routes backed by DataStore:
  - [x] **Medium** — Capture: auto-capture default, shutter feedback, and supported camera preferences.
  - [x] **Low** — Enhancement: default treatment.
  - [x] **Medium** — Export: filename, page-size, and quality defaults.
  - [x] **Low** — Appearance: System, Light, and Dark.
  - [x] **Medium** — Storage: local usage summary and safe cache cleanup.
- [x] **Medium** — Add accurate Privacy, Help, and About screens with app version and open-source notices.
- [x] **Low** — Keep unsupported settings hidden rather than disabled placeholders.

**Complete when:** every visible preference persists, immediately affects the relevant flow, and remains usable at 200% font scale.

## Milestone 9 — Accessibility, adaptive layouts, and hardening (P0 before release)

**Status:** Core implementation complete. Physical-device layout/accessibility audits, the full failure-path UI matrix, varied-scene camera validation, and performance profiling remain release validation work.

This work is continuous, but the full pass is required before release.

- [x] **Very high** — Support compact portrait, compact landscape, medium, expanded, split-screen, and resizable layouts.
- [x] **High** — Use two-pane editor and library/detail layouts where width permits.
- [~] **Very high** — Maintain function at 320dp width and 200% font scale without clipped or unreachable actions. Adaptive action stacking and an automated 320dp/200% text check are present; complete route-level device validation remains.
- [~] **High** — Keep all touch targets at least 48x48dp and ensure WCAG 2.2 AA contrast. The design system targets this, but the complete audit remains.
- [x] **Very high** — Add logical TalkBack traversal and throttled announcements for camera readiness, guidance changes, capture, processing completion, errors, selection count, and crop validity.
- [x] **High** — Add non-drag crop adjustment actions and accessible page-reorder controls.
- [x] **High** — Respect reduced motion, haptics settings, high-contrast text, system theme, system bars, cutouts, and navigation insets.
- [ ] **High** — Test bright paper, dark receipts, colorful backgrounds, low-contrast edges, rotation, and preview crop modes.
- [~] **Very high** — Add automated UI tests for import, live capture, permission denial, detection fallback, invalid crop, save failure, folder operations, Trash restore, and export failure. Adaptive layout and non-drag crop semantics are covered; the complete end-to-end failure matrix remains.
- [ ] **Very high** — Profile camera analysis, thumbnail loading, repository queries, and memory use with large multi-page documents.

**Complete when:** the exposed product passes the above checks without network access, raw internal errors, data loss, or camera-preview stalls.

## Recommended delivery order

1. `[~]` Local document foundation.
2. `[x]` Multi-page import and recoverable scan sessions.
3. `[x]` Populated Home and document detail.
4. `[x]` Multi-page export and sharing.
5. `[x]` Live camera capture using the same session pipeline.
6. `[x]` Search, folders, selection, and Trash.
7. `[x]` Page editing.
8. `[x]` Me and Settings.
9. `[~]` Final accessibility, adaptive-layout, performance, and release hardening.

The first meaningful product milestone is steps 1–4: import several images, review them, save a durable document, find it in Home, and export/share a multi-page PDF. Steps 1–3 are usable now; durable multi-page export is the remaining part of that product milestone. Live camera capture follows and must reuse the same persistence and session architecture.

## Default product decisions

- Home defaults to grid presentation.
- Multi-page crop review is sequential in the first complete flow.
- Search queries are not retained initially.
- Auto-capture defaults off until field testing supports enabling it.
- Trash uses manual emptying initially.
- The first page-editing set is reorder, rotate, re-crop, treatment change, and remove.

## Explicitly deferred

- OCR and full-document-text search.
- Annotation, signatures, fax, and collaborative review.
- Accounts, cloud sync, remote backup, web access, and collaboration.
- Server-side processing or remote OCR.
- Subscriptions, paid plans, and advertising.
