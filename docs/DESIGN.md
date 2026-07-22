---
name: OScan
description: A local-first Android document manager with a calm, precise, image-led scanning experience
design_language: "Quiet Precision - Material Design 3 for an offline scanning tool"
reference: "https://m3.material.io/"
color_mode: "Follows the Android system setting; light palette is listed first"
colors:
  primary: "#006874"
  on-primary: "#FFFFFF"
  primary-container: "#9EEFFD"
  on-primary-container: "#001F24"
  secondary: "#4A6267"
  on-secondary: "#FFFFFF"
  secondary-container: "#CDE7EC"
  on-secondary-container: "#051F23"
  tertiary: "#545D7E"
  on-tertiary: "#FFFFFF"
  tertiary-container: "#DCE1FF"
  on-tertiary-container: "#111A37"
  error: "#BA1A1A"
  on-error: "#FFFFFF"
  error-container: "#FFDAD6"
  on-error-container: "#410002"
  success: "#326A3B"
  on-success: "#FFFFFF"
  success-container: "#B5F2B7"
  on-success-container: "#002107"
  warning: "#765A00"
  on-warning: "#FFFFFF"
  warning-container: "#FFE08A"
  on-warning-container: "#241A00"
  background: "#F7FAFA"
  on-background: "#171D1E"
  surface: "#F7FAFA"
  on-surface: "#171D1E"
  surface-variant: "#DBE4E6"
  on-surface-variant: "#3F484A"
  surface-container-lowest: "#FFFFFF"
  surface-container-low: "#F1F4F4"
  surface-container: "#EBEEEE"
  surface-container-high: "#E5E9E9"
  surface-container-highest: "#E0E3E3"
  outline: "#6F797A"
  outline-variant: "#BFC8CA"
  workspace: "#101415"
  workspace-on-surface: "#F1F4F4"
  crop-boundary: "#4FD8E8"
  crop-scrim: "#00000099"
  scrim: "#000000"
dark_colors:
  primary: "#4FD8E8"
  on-primary: "#00363D"
  primary-container: "#004F58"
  on-primary-container: "#9EEFFD"
  secondary: "#B1CBD0"
  on-secondary: "#1C3438"
  secondary-container: "#334B4F"
  on-secondary-container: "#CDE7EC"
  tertiary: "#BBC5EB"
  on-tertiary: "#252F4D"
  tertiary-container: "#3C4665"
  on-tertiary-container: "#DCE1FF"
  error: "#FFB4AB"
  on-error: "#690005"
  error-container: "#93000A"
  on-error-container: "#FFDAD6"
  success: "#99D69D"
  on-success: "#003911"
  success-container: "#185125"
  on-success-container: "#B5F2B7"
  warning: "#EBC248"
  on-warning: "#3D2F00"
  warning-container: "#584500"
  on-warning-container: "#FFE08A"
  background: "#0F1415"
  on-background: "#DEE3E3"
  surface: "#0F1415"
  on-surface: "#DEE3E3"
  surface-variant: "#3F484A"
  on-surface-variant: "#BFC8CA"
  surface-container-lowest: "#0A0F10"
  surface-container-low: "#171D1E"
  surface-container: "#1B2122"
  surface-container-high: "#252B2C"
  surface-container-highest: "#303637"
  outline: "#899294"
  outline-variant: "#3F484A"
  workspace: "#080B0C"
  workspace-on-surface: "#DEE3E3"
  crop-boundary: "#4FD8E8"
  crop-scrim: "#000000A3"
typography:
  family: "Roboto, Android system sans-serif"
  display-small: "36sp / 44sp / 400 / 0sp"
  headline-large: "32sp / 40sp / 400 / 0sp"
  headline-medium: "28sp / 36sp / 400 / 0sp"
  headline-small: "24sp / 32sp / 400 / 0sp"
  title-large: "22sp / 28sp / 500 / 0sp"
  title-medium: "16sp / 24sp / 500 / 0.15sp"
  title-small: "14sp / 20sp / 500 / 0.1sp"
  body-large: "16sp / 24sp / 400 / 0.5sp"
  body-medium: "14sp / 20sp / 400 / 0.25sp"
  body-small: "12sp / 16sp / 400 / 0.4sp"
  label-large: "14sp / 20sp / 500 / 0.1sp"
  label-medium: "12sp / 16sp / 500 / 0.5sp"
  label-small: "11sp / 16sp / 500 / 0.5sp"
shape:
  none: "0dp"
  extra-small: "4dp"
  small: "8dp"
  medium: "12dp"
  large: "16dp"
  extra-large: "28dp"
  full: "999dp"
spacing:
  unit: "4dp"
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  xxl: "32dp"
  xxxl: "48dp"
motion:
  short: "100ms"
  medium: "250ms"
  long: "400ms"
  easing-standard: "cubic-bezier(0.2, 0, 0, 1)"
  easing-emphasized: "cubic-bezier(0.2, 0, 0, 1)"
window_classes:
  compact: "width < 600dp"
  medium: "width 600-839dp"
  expanded: "width >= 840dp"
---

# OScan Visual Design System

## 1. Overview

**Creative north star: "A precise instrument that gets out of the document's way."**

OScan is a local-first document manager built around a high-quality scanning experience. Its interface should feel calm, capable, and trustworthy: neutral surfaces, legible controls, deliberate cyan/teal accents, and a dark inspection workspace that lets paper edges remain visible. Home and folder surfaces should make saved documents feel organized without competing with their thumbnails. It should look at home on Android without feeling like an unmodified Material sample.

Material Design 3 supplies component behavior, semantic roles, typography, shape, elevation, adaptive layouts, and accessibility conventions. OScan adds a focused scanning character through its crop geometry, document-frame mark, compact task language, and high-contrast image workspace.

**Key characteristics:**

- The document, not the app chrome, is visually dominant.
- The local library feels permanent, organized, and immediately useful.
- Cyan/teal signals the active scanning tool and primary action.
- Neutral surfaces communicate restraint and reduce color cast around images.
- Light and dark schemes follow the Android system setting.
- Status colors have one semantic job and are never decorative.
- Motion confirms state changes but never delays image work.
- Every design choice supports offline trust, clear editing, or readable output.

## 2. Experience principles

### 2.1 Quiet precision

Use strong alignment, exact geometry, compact copy, and predictable Material controls. Avoid visual noise, novelty decoration, and large branding regions once a document is active.

### 2.2 Image-first hierarchy

On Adjust crop and Enhance, the workspace receives the largest uninterrupted region. Controls gather at the edges in clear tool areas. Never float routine controls over document text unless the overlay can move or collapse.

On Home and in Folders, document thumbnails lead each item while names, dates, page counts, and locations remain easy to scan. Visual previews support recognition but never replace readable metadata.

### 2.3 Trust through clarity

Say what OScan is doing: finding edges, straightening, applying Magic, or creating a PDF. Do not suggest that a file is uploaded, synced, or secured by a service. Privacy statements are factual and restrained.

### 2.4 Functional color

Primary teal means action or active scanning geometry. Green means completed success. Amber means attention or manual intervention. Red means invalid input or failure. These roles do not trade places.

### 2.5 Familiar controls, distinctive workspace

Buttons, app bars, segmented controls, dialogs, and progress indicators follow Material 3. Brand character concentrates in the mark, workspace, crop boundary, and subtle document-frame motifs.

## 3. Brand expression

### 3.1 Name and voice

Use `OScan` in product identity and app-bar contexts. Use sentence case for all other interface copy. The voice is concise, helpful, and technically honest.

Prefer:

- `Finding document edges…`
- `Place each handle on a document corner.`
- `Processing stays on this device.`
- `Save PDF`

Avoid:

- `AI-powered magic scanning!`
- `Please wait…`
- `Crop & Warp`
- `Operation failed: IllegalStateException`

### 3.2 App mark

The mark is an open document frame made from four corner brackets, with a subtle page shape inside. It must work as a one-color vector and remain recognizable at 24dp. Do not use a camera lens, cloud, sparkle field, or literal scanner hardware as the primary mark.

### 3.3 Iconography

Use Material Symbols Rounded or the equivalent Material icon set already available to Compose. Use outlined icons by default and filled icons only for selected state or strong confirmation.

- Image source: `image`
- Back: `arrow_back`
- More actions: `more_vert`
- Save PDF: `picture_as_pdf`
- Share: `share`
- Privacy: `lock`
- Success: `check_circle`
- Warning: `warning`
- Reset crop: `restart_alt`

Icons support labels rather than replace unfamiliar actions. Decorative icons use no accessibility description.

## 4. Color

### 4.1 Semantic system

The YAML front matter is the canonical palette for the initial implementation. Compose components consume semantic roles through an OScan theme; they must not contain screen-local hex values.

- **Primary:** scanner teal. Primary buttons, active segmented selection, progress, focus, and active crop geometry.
- **Secondary:** cool blue-gray. Supporting tonal controls and secondary selection.
- **Tertiary:** restrained indigo. Rare informational emphasis, never crop or success state.
- **Success:** export completion only.
- **Warning:** manual crop fallback and guidance that requires attention.
- **Error:** invalid geometry and failed operations.
- **Surface:** neutral cool-gray app chrome.
- **Workspace:** stable near-black environment behind images in both schemes.

The crop boundary deliberately uses the dark-scheme primary value in both themes because it is drawn over unpredictable photography. It still requires a dark outer keyline or contrasting handle center so it remains visible over light cyan content.

### 4.2 Light scheme

Light mode uses a cool near-white background. App bars remain neutral rather than using a large primary container. Tonal containers distinguish controls and messages before borders or shadows are introduced.

Do not use pure white for every region. Reserve `surface-container-lowest` for elevated or focused content. The permanent image workspace remains near-black to avoid glare and make white paper boundaries obvious.

### 4.3 Dark scheme

Dark mode uses charcoal surfaces with a brighter cyan primary. It is a complete scheme rather than an inversion. Body text is soft off-white. Elevated regions become lighter through tonal surfaces; they do not rely on heavy shadows.

The workspace is slightly darker than the page surface, but its boundary must remain visible using either an outline or an adjacent control surface.

### 4.4 Crop overlay

The crop overlay has four layers:

1. A translucent black scrim outside the selected polygon.
2. A 1dp near-black keyline beneath the active boundary for contrast on light imagery.
3. A 2-3dp `crop-boundary` line.
4. Handles with a 10-12dp visible radius and at least a 48dp invisible gesture target.

Handles use a workspace-colored center, a 2-3dp cyan ring, and a thin contrasting outer keyline. The active handle increases by approximately 2dp and may gain a subtle tonal halo.

Invalid geometry switches the boundary and handle rings to `error`; it does not change the document image or flash.

### 4.5 Color rules

- Never encode success with the crop cyan; use `success`.
- Never use success green for valid crop geometry. Valid is the normal scanner state.
- Never use amber for decoration; it means user attention is needed.
- Never show low-opacity text over a document image.
- Never apply the application palette to the scanned output.
- Never tint previews, screenshots, or PDFs.
- Never add gradients to buttons, tool panels, or app bars.
- Use text, icon, and control state in addition to hue.

## 5. Typography

OScan uses Roboto and the Android system sans-serif to avoid font downloads, reduce APK complexity, and fit naturally with system dialogs. The Material 3 type scale in the front matter is the baseline.

### 5.1 Type mapping

- **Headline medium:** empty Home, local profile, major permission, and first-use headings such as `Your documents will appear here`.
- **Title large:** full-screen success and fatal-error headings.
- **Title medium:** app-bar titles and important dialog titles.
- **Body large:** short start-screen support copy and primary status messages.
- **Body medium:** normal instructions, banners, and dialogs.
- **Body small:** privacy support, filenames, and secondary status.
- **Label large:** buttons and segmented controls.
- **Label medium:** compact metadata and accessibility-supporting labels.

### 5.2 Typography rules

- Use sentence case everywhere except the `OScan` name.
- Keep instructions below approximately 70 characters when practical.
- Use one clear sentence instead of an eyebrow, title, and paragraph for routine states.
- Do not use all caps, condensed type, monospaced type, or wide tracking as decoration.
- Do not reduce essential text below 12sp.
- Allow Material typography to respond to system font scale; never disable scaling.
- Prefer wrapping over truncation for actions and error explanations.

## 6. Shape, spacing, and elevation

### 6.1 Shape

- Extra-small: tooltips and compact progress surfaces.
- Small: banners, segmented-control segments, and small menu items.
- Medium: buttons, dialogs, and control panels.
- Large: start-state icon container and full-screen empty/error content blocks.
- Extra-large: reserved for the large app-mark container or an expanded-width tool panel.
- Full: circular icon buttons, progress indicators, and crop handles.

Do not round the document image itself during crop or preview; users need to see its actual corners. Do not give every full-width panel floating-card treatment.

### 6.2 Spacing

Use a 4dp grid. Standard screen padding is 16dp compact, 24dp medium, and 32dp expanded. Use 8dp between icon and label, 12-16dp between related controls, 24dp between content groups, and at least 32dp between the main explanation and primary start action.

Bottom action areas include navigation-bar safe insets. Tool panels align to the app bar and workspace edges.

### 6.3 Elevation

Prefer tonal separation:

- Level 0: background and workspace.
- Level 1: app bar and persistent bottom action area.
- Level 2: menus, magnifier, snackbar, and temporarily lifted control panels.
- Level 3: dialogs.

Crop handles use outlines and contrast, not general Material shadow. Avoid colored glows, glass blur, and deep card shadows.

## 7. Layout and composition

### 7.1 Compact portrait

- Home and Me use a small top app bar, content region, and three-item primary navigation. Scan occupies the center destination and opens the camera workflow.
- Document grids use two columns when cards remain readable; list mode uses full-width rows.
- Camera is edge-to-edge and hides top-level navigation.
- Crop and enhancement workspaces expand between their app bar/banner and controls.
- Editor primary actions fill available width. Secondary actions use text buttons or a second row when font scaling needs space.
- Keep controls out of the image's active crop region.

### 7.2 Compact landscape

When vertical space is limited, use a horizontal editor: workspace left, 280-320dp control area right. Home and folder collections gain columns rather than stretching items. Empty, profile, and terminal states stay centered and vertically scroll if necessary.

### 7.3 Medium and expanded

Use a navigation rail and centered maximum-width shell. Home, Folder detail, and Document detail may become list-detail layouts with selection and scroll state preserved. Editor screens become two-pane: a dominant flexible workspace and a 320-360dp side panel containing instructions, treatment selection, and actions. Avoid scaling phone controls into an empty tablet canvas.

### 7.4 Image workspace

The workspace is edge-to-edge within its allocated pane. Letterboxing uses `workspace`, never the app background. A 1dp outline may divide it from neighboring surfaces. The image must use its native aspect ratio and explicit display bounds so overlay coordinate transforms remain exact.

### 7.5 Home collections

Document and folder collections align to the same page grid. Thumbnails use stable aspect-ratio containers and never stretch source imagery. Grid cards emphasize visual recognition; list rows emphasize name, date, page count, and folder. Selection adds a check indicator and tonal state layer without obscuring the thumbnail.

Empty Home artwork remains restrained and occupies less visual weight than the `Scan document` action. A populated Home does not retain a large welcome header. Folders and Trash use secondary Browse or overflow access rather than competing with Recents and All documents.

### 7.6 Camera composition

The live preview fills the screen behind dark translucent control regions. Shutter, captured-page count, Done, Torch, Auto, Close, and Import remain reachable without covering the detected document center. Guidance is one short high-contrast line and changes in place so the layout does not jump as detection state changes.

## 8. Components

### 8.1 App bar

Use a small neutral Material 3 top app bar. The title is destination- or task-specific. Search may expand into the bar. Navigation and overflow icons have 48dp targets. Do not use `primaryContainer` as the permanent bar color; a large colored band competes with image inspection and document collections.

### 8.2 Buttons

- **Filled:** one next-step action per screen: Choose image, Continue, Save PDF, Share PDF.
- **Filled tonal:** an important supporting action when two affirmative actions coexist.
- **Outlined:** Scan another or another medium-emphasis alternative.
- **Text:** Reset, Cancel, and other reversible low-emphasis actions.
- **Icon button:** app-bar navigation and overflow only when the icon is familiar.

Buttons use a minimum 48dp height and 48dp touch target. Loading replaces or overlays screen content; never place an indefinite spinner inside a button while leaving other conflicting actions active.

### 8.3 Segmented treatment control

Use a Material 3 single-select segmented button row with equal-width `Original` and `Magic` options. Selection uses container, label, and check/icon state rather than color alone. Do not use filter chips for this two-option mutually exclusive choice.

### 8.4 Status banners

Banners are full-width within the active pane, compact, and non-dismissible when they describe current state.

- Warning banner: warning icon, one sentence, warning container.
- Error guidance: error icon, one sentence, optional Reset text action.

Do not stack multiple banners. Invalid geometry takes priority over manual-fallback guidance until the geometry is valid again.

### 8.5 Progress

Use a 40-48dp circular progress indicator for full-screen operations and a 24dp indicator for treatment changes over an existing preview. Pair every indicator with specific text. Do not use skeleton content for local image processing.

### 8.6 Dialogs and snackbars

Dialogs preserve the visible screen beneath a scrim and use direct actions such as Try again, Choose another, or Cancel. Snackbars are reserved for low-risk transient feedback. Routine content never belongs in a dialog.

### 8.7 Success state

The success icon sits in a `success-container` circle or rounded-square container. Avoid confetti, checkmark animation loops, or green body text. One short entrance scale/fade may play when motion is enabled.

### 8.8 Primary navigation

Compact layouts use a three-destination Material navigation bar: Home, Scan, and Me. Expanded layouts use a navigation rail with the same order. Active destinations combine icon, label, and indicator state. Scan may receive stronger emphasis, but it remains a labeled destination and must not visually become an unlabeled floating action button.

### 8.9 Document items

Document cards and rows use tonal surfaces with restrained outlines and no heavy elevation. Required states are default, pressed, focused, selected, loading thumbnail, and missing thumbnail. Overflow remains a separate target. Page-count badges are compact and always include a readable label or accessible name.

Folder cards use the same family but are distinguishable through a folder icon, name, count, and optional preview stack. Do not imitate physical manila folders with ornamental tab shapes.

### 8.10 Camera controls

Camera controls use high-contrast dark scrims independent of the app theme. The shutter is a familiar circular control with a visible inner disc and 64-72dp target. Auto and Torch expose icon, label or state description, and selected state. The live-edge polygon follows the crop-overlay rules rather than introducing a second detection color system.

### 8.11 Preferences

Me uses a calm local-profile header followed by grouped utility rows. Settings uses grouped preference rows with a title, optional summary/current value, and a control only where immediate inline editing is appropriate. Use switches for independent booleans and dialogs or subpages for mutually exclusive values. Destructive storage actions remain visually separate from ordinary preferences.

## 9. Motion and haptics

Motion communicates continuity:

- Press/state feedback: 100ms.
- Control and banner changes: 150-250ms.
- Screen content transition: 250-400ms.
- Crop handles follow the finger with no decorative interpolation.

Recommended behaviors:

- Crossfade or shared-axis transition between task screens.
- Fade the invalid-crop banner in and out without shifting the workspace repeatedly during a drag; reserve stable banner space where practical.
- Crossfade between Original and Magic only after the replacement bitmap is ready.
- Give a single light haptic tick when a handle becomes active and a warning haptic when a drag first creates invalid geometry, subject to system haptic settings.

Under reduced-motion settings, use immediate state changes or short opacity fades. No animation is required to understand progress or success.

## 10. Accessibility

- Target WCAG 2.2 AA contrast for text, icons, boundaries, and controls.
- Crop geometry must remain visible over both the lightest paper and varied photography.
- All touch targets are at least 48x48dp.
- Focus indicators remain visible on keyboard, switch, and D-pad navigation.
- Crop handles expose names, current coordinates in understandable terms where useful, and directional adjustment actions.
- Font scaling to 200% may reflow action rows into vertical stacks.
- TalkBack announcements cover asynchronous state completion and actionable failure, not every progress recomposition.
- Icons paired with visible labels are decorative to accessibility.
- Error, warning, selection, and success are expressed with words and icons as well as color.
- Image previews use task-specific descriptions such as `Document crop preview`; decorative app-mark art is hidden.

## 11. Privacy and content rules

Privacy is part of the interface, not a badge applied to every screen.

- State `Processing stays on this device` in empty Home, the local Me profile, camera-permission rationale, and optionally on the first processing state.
- Do not repeat privacy copy on every editor screen.
- Never display full raw content URIs.
- Use a human-readable filename when available.
- Do not log or surface document titles inferred from pixels.
- Do not imply encryption, secure deletion, or permanent URI access unless implemented and verified.

## 12. Do's and don'ts

### Do

- Do keep the current document visually dominant.
- Do use semantic theme roles for all UI colors.
- Do make the primary next action obvious.
- Do keep editing reversible and state-preserving.
- Do distinguish normal crop geometry from success state.
- Do use the same workspace treatment in light and dark themes.
- Do support system font scale, theme, motion, and navigation modes.
- Do test overlay contrast on varied real-world document photos.

### Don't

- Don't show bottom navigation inside camera, crop, enhancement, or full-page viewing.
- Don't add decorative gradients, glass panels, or neon glow.
- Don't use bright green for normal crop edges.
- Don't color the top bar with a large primary block on every screen.
- Don't round or tint the user's document.
- Don't use filter chips for mutually exclusive Original/Magic selection.
- Don't show internal errors or raw URIs.
- Don't ship inert camera, library, folder, or settings controls; use route-level feature flags until a complete slice works.
- Don't hide essential controls behind gestures alone.

## 13. Implementation contract

Create a dedicated Compose theme layer containing:

- `OScanLightColorScheme` and `OScanDarkColorScheme`.
- Extended semantic colors for success, warning, workspace, crop boundary, and crop scrim.
- The OScan Material typography and shape scales.
- Window-size-aware dimensions and spacing.

Build reusable primitives for the features listed in `FEATURES.md`. Screens should consume `MaterialTheme` and the OScan extended theme rather than literal colors or dimensions.

The adaptive app shell must support Home, Scan, and Me navigation bar/rail variants, grid/list document collections, secondary folder and Trash routes, local profile content, camera controls, and grouped settings without creating separate visual systems for management and scanning.

Replace current hard-coded presentation values including `Color(0xFF00E676)`, `Color.Red`, and `Color.White` with semantic tokens. Replace the generic root `MaterialTheme` with `OScanTheme`, including system dark-mode handling and edge-to-edge system bar colors.

Before a visual change is accepted, verify:

1. Light and dark system themes.
2. Compact portrait and landscape layouts.
3. Medium and expanded window layouts or the documented first-release fallback.
4. 200% font scaling and longest specified copy.
5. TalkBack, keyboard/D-pad focus, and crop-handle alternatives.
6. Valid, invalid, auto-detected, and manual-fallback crop states.
7. Light documents, dark receipts, colorful backgrounds, and low-contrast edges.
8. Reduced motion and system haptic preferences.
9. System picker, save dialog, and share-sheet return paths.
10. No unintended color or processing change in the exported document.
11. Home grid/list, Recents/All documents, folder, selection, empty, and missing-thumbnail states.
12. Live camera controls and edge overlays against bright, dark, and cluttered scenes.
13. Navigation bar/rail state and settings controls at large font scales.

This document governs OScan's colors, typography, shapes, spacing, composition, motion, and component appearance. [`FEATURES.md`](FEATURES.md) governs the implementation backlog and required product behavior. If an implementation detail conflicts with either document, resolve the product behavior first, then apply the closest conforming visual treatment.

## 14. Decision log

| Decision | Alternatives considered | Reason |
| --- | --- | --- |
| Adopt "Quiet Precision" | Playful consumer scanner; enterprise utility; neon technical tool | It communicates trust and competence without competing with the user's document. |
| Use Material 3 as the behavioral base | Fully custom controls; legacy Material styling | OScan is Android-first and benefits from familiar, accessible system behavior. |
| Use teal/cyan as primary | Bright green; royal blue; orange | Teal feels technical and calm, remains distinct from success green, and is legible in the dark workspace. |
| Keep the workspace dark in both themes | Match the system background; use a checkerboard | A stable dark surround reduces glare and improves visibility around light paper. |
| Follow system light/dark mode by default | Light only; dark only | System-following behavior is predictable, while Settings may offer explicit Light and Dark overrides. |
| Use system typography | Bundle a branded font | Roboto is readable, native, offline, and adds no font-loading cost. |
| Use segmented buttons for treatments | Filter chips; tabs; dropdown | Original and Magic are a small, mutually exclusive choice and should remain simultaneously visible. |
| Keep success green separate from crop state | Green crop edges and success | Separate semantics make normal editing and completed export immediately distinguishable. |
| Use neutral app bars | Primary-container app bars | Neutral chrome gives the document and primary actions stronger hierarchy. |
| Use Home, Scan, and Me as primary destinations | Folders and Settings as tabs; Scan FAB; four-tab bar with History | Home owns documents and recents, Scan remains central, and Me groups local identity with settings and supporting options; Folders and Trash are less-frequent secondary routes. |
