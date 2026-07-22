---
name: James Wu Portfolio
description: A modern personal portfolio combining Kyoto atmosphere with Material Design 3 structure and behavior
design_language: "Kyoto Material - Google Material Design 3 adapted for the web"
reference: "https://m3.material.io/"
color_mode: "Kamogawa Spring by default with four seasonal themes (Spring, Summer, Autumn, Winter)"
colors:
  primary: "#8F1D14"
  on-primary: "#FFFFFF"
  primary-container: "#FFDAD4"
  on-primary-container: "#3B0804"
  secondary: "#4F6541"
  on-secondary: "#FFFFFF"
  secondary-container: "#D2EABC"
  on-secondary-container: "#0E2007"
  tertiary: "#735C16"
  on-tertiary: "#FFFFFF"
  tertiary-container: "#FFE08A"
  on-tertiary-container: "#241A00"
  error: "#BA1A1A"
  on-error: "#FFFFFF"
  error-container: "#FFDAD6"
  on-error-container: "#410002"
  background: "#FFF8F4"
  on-background: "#251916"
  surface: "#FFF8F4"
  on-surface: "#251916"
  surface-variant: "#F2DED9"
  on-surface-variant: "#53433F"
  outline: "#88716C"
  outline-variant: "#D8C2BD"
  surface-container-lowest: "#FFFFFF"
  surface-container-low: "#FFF1ED"
  surface-container: "#FBECE8"
  surface-container-high: "#F5E6E2"
  surface-container-highest: "#EFE0DC"
  inverse-surface: "#3B2D2A"
  inverse-on-surface: "#FFEDEA"
  inverse-primary: "#FFB4A8"
  scrim: "#000000"
dark_colors:
  primary: "#FFB4A8"
  on-primary: "#56100A"
  primary-container: "#732018"
  on-primary-container: "#FFDAD4"
  secondary: "#B6CEA2"
  on-secondary: "#223516"
  secondary-container: "#384C2B"
  on-secondary-container: "#D2EABC"
  tertiary: "#E5C36B"
  on-tertiary: "#3D2F00"
  tertiary-container: "#574500"
  on-tertiary-container: "#FFE08A"
  background: "#1C1110"
  on-background: "#F2DFDB"
  surface: "#1C1110"
  on-surface: "#F2DFDB"
  surface-variant: "#53433F"
  on-surface-variant: "#D8C2BD"
  outline: "#A08C87"
  outline-variant: "#53433F"
  surface-container-lowest: "#160C0B"
  surface-container-low: "#251918"
  surface-container: "#291D1C"
  surface-container-high: "#342725"
  surface-container-highest: "#3F3230"
typography:
  interface-family: "Inter, Noto Sans JP, system-ui, sans-serif"
  editorial-family: "Noto Serif JP, Shippori Mincho, serif"
  display-large: "clamp(3.5rem, 8vw, 7rem) / 0.98 / 500 / -0.04em"
  display-medium: "clamp(2.75rem, 6vw, 5rem) / 1.02 / 500 / -0.03em"
  display-small: "clamp(2.25rem, 4vw, 3.5rem) / 1.08 / 500 / -0.02em"
  headline-large: "32px / 40px / 500 / -0.01em"
  headline-medium: "28px / 36px / 500 / 0"
  headline-small: "24px / 32px / 500 / 0"
  title-large: "22px / 28px / 500 / 0"
  title-medium: "16px / 24px / 600 / 0.1px"
  title-small: "14px / 20px / 600 / 0.1px"
  body-large: "18px / 30px / 400 / 0"
  body-medium: "16px / 26px / 400 / 0"
  body-small: "14px / 20px / 400 / 0.1px"
  label-large: "14px / 20px / 600 / 0.1px"
  label-medium: "12px / 16px / 600 / 0.4px"
  label-small: "11px / 16px / 600 / 0.4px"
shape:
  none: "0"
  extra-small: "4px"
  small: "8px"
  medium: "12px"
  large: "16px"
  extra-large: "28px"
  extra-extra-large: "40px"
  full: "999px"
spacing:
  unit: "4px"
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  xxl: "32px"
  section: "clamp(72px, 10vw, 144px)"
motion:
  short: "100ms"
  medium: "250ms"
  long: "400ms"
  scene: "700ms"
  easing-standard: "cubic-bezier(0.2, 0, 0, 1)"
  easing-emphasized: "cubic-bezier(0.2, 0, 0, 1)"
breakpoints:
  compact: "0-599px"
  medium: "600-839px"
  expanded: "840-1199px"
  large: "1200-1599px"
  extra-large: "1600px+"
---

# Design System: Kyoto Material Portfolio

## 1. Overview

**Creative north star: "A modern portfolio encountered like a quiet walk through Kyoto."**

This portfolio presents James Wu as a multilingual technology professional who connects software, systems, design, and people across cultures. It should help recruiters, clients, and collaborators understand who James is, what he has built, what he can contribute, and how to contact him without working through visual spectacle first.

The redesign starts from scratch. Existing content and the broad Kyoto identity remain useful, but the current Ema cards, handscroll cards, fixed `400vh` horizontal-scroll simulation, permanent falling-leaf layer, and modal-led information architecture are not part of the new system.

[Google Material Design 3](https://m3.material.io/) supplies semantic color roles, typography hierarchy, shape scale, adaptive layouts, state layers, component behavior, and accessible interaction patterns. Kyoto supplies the atmosphere: washi surfaces, vermilion gates, moss gardens, cedar, ink, indigo evenings, shoji proportions, seasonal light, and careful negative space. The site must not look like an Android application decorated with Japanese motifs. It must feel like a contemporary web portfolio whose interface happens to be governed by Material principles.

**Key characteristics:**

- Kamogawa Spring dark theme by default with four complete seasonal schemes.
- Normal vertical document flow with adaptive navigation and layouts.
- Project work receives the strongest visual hierarchy.
- Material components behave familiarly and use semantic tokens.
- Kyoto references are abstract, restrained, and integrated into composition.
- Motion communicates continuity and changing atmosphere; it is never constant noise.
- Mobile, tablet, and desktop are one adaptive system rather than separate experiences.
- Accessibility and performance are design requirements, not cleanup tasks.

## 2. Experience Principles

### 2.1 Work before ornament

Project outcomes, screenshots, responsibilities, and technologies are primary content. Kyoto-inspired framing must improve attention and pacing rather than compete with the work. A visitor should identify James's role and reach a relevant project within the first viewport or one deliberate scroll.

### 2.2 Kyoto as atmosphere, not costume

Avoid literal temples, repeated torii silhouettes, pseudo-Japanese labels, and novelty card shapes. Prefer proportion, material, light, color, rhythm, and negative space. A quiet shoji grid or moss-colored tonal surface is stronger than turning every component into a souvenir.

### 2.3 One clear action per region

Each major region has one dominant action:

- Hero: view selected work.
- Featured project: open the case study or live project.
- Experience: read the relevant role details.
- About: download the resume.
- Contact: send an email.

Use a filled button for the primary action. Supporting actions use filled tonal, outlined, text, or icon-button treatments.

### 2.4 Calm expressiveness

M3's expressive shapes and motion may appear in hero composition, selected project containers, navigation selection, and major transitions. Routine text, lists, and metadata remain calm. Expressiveness must be concentrated so it retains meaning.

### 2.5 Progressive disclosure without modal dependence

Important information belongs in the page flow or on dedicated project detail routes. Dialogs are reserved for confirmation or small transient tasks. Do not hide normal project or experience reading behind generic modals.

### 2.6 Inclusive by default

The complete experience must work with keyboard navigation, visible focus, touch, browser zoom, reduced motion, coarse pointers, screen readers, and high-contrast user preferences. No core content depends on hover, animation, color, or decorative imagery.

## 3. Color

### 3.1 Semantic system

All application colors use M3-style semantic roles. Components must reference roles such as `primary`, `surface-container`, and `on-surface-variant`; they must not own page-specific hex values.

- **Primary:** Kyoto vermilion. Primary actions, active navigation, focus emphasis, and important links.
- **Secondary:** moss green. Supporting selection, capability groups, and quiet environmental accents.
- **Tertiary:** antique gold. Limited editorial emphasis, selected facts, and lantern-like highlights.
- **Surface roles:** seasonal layers ranging from spring cherry blossoms and autumn reds to summer matcha and winter snow.
- **Outline roles:** shoji-like dividers, focused outlined controls, and quiet boundaries.
- **Error roles:** form and resource errors only; vermilion branding must not replace error semantics.

#### Seasonal Schemes

- **Spring:** Cherry blossom palette (`--color-primary: #f3a6b5`, `--color-secondary: #e2b3c2`).
- **Summer (Kitauji Summer):** Matcha green & sailor blue palette (`--color-primary: #557b54` [matcha green], `--color-secondary: #99b5dd` [sailor blue]).
- **Autumn:** Autumn foliage palette (`--color-primary: #e86b52`, `--color-secondary: #d99d73`).
- **Winter:** Winter snow palette (`--color-primary: #5c7c99`, `--color-secondary: #727a85`).

The front matter defines the initial light and dark palettes. Final production values must be contrast-tested before implementation is accepted.

### 3.2 Washi Day

Washi Day is the optional light theme. It uses a warm near-white background rather than pure white. Elevated regions separate through tonal surfaces before shadows. Vermilion should occupy less than roughly ten percent of a typical viewport so it remains meaningful.

Use moss and gold as supporting colors, never as competing brands. Long-form text uses `on-surface`; secondary metadata uses `on-surface-variant`. Large texture overlays remain below 5% effective opacity.

### 3.3 Gion Night

Gion Night is a complete dark scheme, not an inverted light theme. Surfaces shift toward warm charcoal and deep brown-indigo. Lantern gold may become more noticeable, while vermilion remains the primary interactive color. Text uses warm off-white rather than pure white.

The theme selector offers `Gion Night` and `Washi Day`, with Gion Night as the default. Theme choice persists locally. The browser theme color should update with the active scheme.

### 3.4 Imagery and scrims

Text over photography or illustration requires an opaque-enough semantic scrim. Do not depend on blur alone. Decorative art uses empty alt text or CSS backgrounds; portfolio screenshots receive meaningful alt text.

Project imagery must retain accurate colors. Do not apply a global sepia filter. Any atmospheric color treatment fades away on hover or focus only if the untreated state remains accessible and the effect does not imply disabled content.

### 3.5 Color rules

- Never use literal white or black for general page surfaces and text.
- Never create a new red for each component.
- Never encode current, selected, available, or failed state by hue alone.
- Never place low-opacity text on textured backgrounds.
- Never use gradients on routine buttons, chips, or body text.
- Use tonal surface changes before adding shadows or borders.

## 4. Typography

The interface uses **Inter** with `Noto Sans JP` and system fallbacks. Editorial headings use **Noto Serif JP** or **Shippori Mincho**. The serif face communicates Kyoto character at meaningful moments; it is not the default for every control or paragraph.

### 4.1 Type mapping

- **Display large:** James Wu hero title on expanded layouts only.
- **Display medium/small:** major atmospheric chapter headings.
- **Headline large:** primary section titles such as Selected Work.
- **Headline medium/small:** project and experience group headings.
- **Title large:** project titles and prominent role names.
- **Title medium/small:** card titles, timeline entries, and grouped content.
- **Body large:** hero supporting statement and important project summaries.
- **Body medium:** normal descriptions and case-study prose.
- **Body small:** dates, locations, and secondary metadata.
- **Label large:** buttons, navigation, tabs, and major chips.
- **Label medium/small:** compact tags and status labels.

### 4.2 Editorial voice

Copy is direct, warm, and specific. Avoid inflated claims such as "visionary," "world-class," or "digital craftsman." Prefer concrete responsibility, outcome, tool, and context. Japanese text may be used for an authentic place or concept where it adds meaning, but English remains the primary interface language.

### 4.3 Typography rules

- Keep prose between approximately 45 and 75 characters per line.
- Use sentence case for controls and navigation.
- Do not use decorative all-caps eyebrows throughout the site.
- Do not use letter spacing as the main form of hierarchy.
- Body text does not fall below 14px; primary prose should normally be 16-18px.
- Avoid more than two font families and three active weights in one viewport.

## 5. Shape, Spacing, and Material

### 5.1 Shape scale

Use the shape scale consistently:

- Extra-small: compact badges and tooltips.
- Small: menu items and dense supporting controls.
- Medium: chips, small cards, and form fields.
- Large: standard project and experience containers.
- Extra-large: featured work and major page surfaces.
- Extra-extra-large: hero composition and contact pavilion.
- Full: buttons where appropriate, navigation indicators, and avatar treatment.

Shape variation should express hierarchy. Do not give every container the same large radius. A limited asymmetrical corner treatment may appear on featured surfaces to echo shoji or folded paper, but standard controls keep familiar M3 silhouettes.

### 5.2 Spacing

Spacing uses a 4px base. Common page padding is 16px compact, 24px medium, 32px expanded, and 48px large. Major sections use the `section` spacing token. Content alignment across the hero, project grid, timeline, and footer is more important than filling unused space.

### 5.3 Material texture

Washi texture is a nearly imperceptible environmental layer, not a filter over all content. It must not reduce contrast or create visible repetition. Wood and stone are represented through color and photography rather than CSS imitation. Glassmorphism is not a default surface treatment.

### 5.4 Elevation

Prefer tonal elevation:

- Level 0: page background and open content regions.
- Level 1: app bar, navigation rail, standard cards.
- Level 2: menus, tooltips, and temporarily lifted cards.
- Level 3: sheets and dialogs.
- Level 4-5: exceptional transient drag states only.

Shadows are soft, neutral, and subtle. Avoid colored glows in light themes. Dark themes may use a restrained lantern-like halo only in atmospheric artwork, never around ordinary controls.

## 6. Adaptive Layout and Navigation

### 6.1 Window classes

- **Compact (0-599px):** single-column content, compact top app bar, optional bottom navigation, edge-to-edge project imagery.
- **Medium (600-839px):** two-column project layouts where content permits; navigation rail or expanded top app bar.
- **Expanded (840-1199px):** persistent navigation rail, split hero composition, two-column project storytelling.
- **Large (1200-1599px):** generous editorial grid, wide featured work, compact rail or top app bar according to composition.
- **Extra-large (1600px+):** centered maximum-width canvas; content does not stretch indefinitely.

Layouts flex within each class. Breakpoints describe meaningful composition changes, not specific device brands. Components should use container queries when their available width matters more than the viewport.

### 6.2 Navigation destinations

Primary destinations are:

1. Home
2. Work
3. Experience
4. About
5. Contact

On compact layouts, use a small top app bar with a menu or a four-item navigation bar plus a clear overflow path for About. Choose one after prototype testing; do not display both simultaneously. On expanded layouts, a narrow navigation rail may use section icons with labels revealed on focus or hover. Large layouts may use a restrained top app bar if the rail competes with project imagery.

Active navigation uses a pill-shaped `secondary-container` or `primary-container` indicator. Navigation clicks use native anchors and update the URL hash. Browser back/forward behavior must remain reliable.

### 6.3 Scrolling

Use normal vertical scrolling. Sections remain semantic document regions. Scroll-linked atmosphere may change color, light, or a background illustration, but it must not control access to content. Do not hide the browser scrollbar.

## 7. Page Architecture

### 7.1 Arrival / Hero

The opening viewport introduces James, his role, relocation context where current, and two actions: `View selected work` and `Contact me`. A third lower-emphasis action may download the resume.

The composition uses one large expressive surface with an abstract Kyoto horizon or shoji-light study. The portrait may appear inside a stable circular or extra-large shape. Avoid a centered badge-card composition surrounded by empty decoration.

On compact screens, text comes before supporting imagery. On expanded screens, use a balanced split layout. The hero should establish competence within a few seconds, not require animation to reveal essential copy.

### 7.2 Selected Work

Projects are the strongest portfolio section. Lead with one featured project, followed by a responsive collection of supporting projects. Cards use accurate product imagery, title, short outcome-oriented summary, role, and selected technology chips.

Project interaction should lead to one of two destinations:

- A dedicated case-study route when sufficient content exists.
- The live project or repository when only summary content exists.

Do not open ordinary project details in a generic modal. Each card has one clear primary destination and an optional secondary external-link icon.

### 7.3 Experience Path

Experience becomes a vertical path inspired by Kyoto stepping stones. The path is expressed through alignment, spacing, and a restrained moss/stone motif rather than illustrated plaques.

Each role shows role, company, period, location, one-sentence responsibility, and expandable or linked detail. The current role receives a clear text label and icon; color alone does not communicate status. Education follows as a supporting group rather than being visually identical to employment.

### 7.4 Capabilities Garden

Skills are grouped by capability rather than displayed as an undifferentiated tag cloud:

- Application development
- Web and interface development
- Systems and infrastructure
- Security and analysis
- Design and collaboration
- Languages and cross-cultural communication

Use M3 chips inside open or softly tonal regions. Chips describe evidence already supported by projects and experience; they do not behave like filters unless filtering is actually implemented.

### 7.5 About / Personal Story

Use a readable editorial layout connecting James's technical background, multilingual communication, Japan experience, and Australia context. Include the portrait or one authentic environmental image. Keep the existing long biography as source material, then rewrite it into shorter paragraphs with concrete emphasis.

### 7.6 Contact Pavilion

The final section transitions toward Gion Night even when the light theme remains active, providing a sense of arrival at dusk. It includes email, GitHub, LinkedIn, resume, and support links.

Email is the primary filled action. External destinations use tonal or outlined treatments. A short availability statement may appear above the actions. Do not use a contact form unless a real delivery endpoint and privacy treatment are added.

## 8. Components

### 8.1 Buttons

- **Filled:** one highest-emphasis action per region.
- **Filled tonal:** important supporting action.
- **Outlined:** reversible or medium-emphasis action.
- **Text:** low-emphasis navigation or disclosure.
- **Icon button:** familiar icon action with accessible name and tooltip where needed.

Buttons are at least 40px visually and maintain a 48x48px touch target on compact layouts. All variants include hover, focus, pressed, disabled, and loading behavior.

### 8.2 Project cards

Project cards are content-led M3 cards, not themed objects. Required content:

- Product screenshot or identity image.
- Project title.
- One concise outcome or purpose statement.
- James's role where useful.
- Up to four technology chips.
- Clear destination affordance.

Featured cards may use extra-large shapes and asymmetric grid spans. Supporting cards use large shapes. Hover may add a Level 1 lift and slight image scale; focus receives an explicit ring. Essential content never appears only on hover.

### 8.3 Experience items

Use structured list items or timeline nodes rather than cards for every entry. Items include accessible expand/collapse behavior only when details genuinely need disclosure. Entire items may be clickable only if they have a clear destination.

### 8.4 Chips

Use assist chips for technologies and capability labels. Use filter chips only if the project list becomes filterable. Selected chips include an icon or other non-color indicator. Chips do not replace navigation tabs.

### 8.5 Theme control

The theme control is a compact palette dropdown with an accessible label. It supports the four seasonal themes. Selection is visible beyond icon color and persists locally.

### 8.6 Forms and feedback

If a contact form is added later, use M3 outlined or filled text fields, inline validation, a clear submission state, and a privacy note. Snackbars communicate low-risk confirmations. Dialogs are reserved for consequential confirmation, not routine content.

### 8.7 Loading and failure

Portfolio content is primarily static. Image loading uses stable aspect-ratio placeholders to prevent layout shift. Broken project images show a quiet semantic fallback with the project title; they do not remove the project. External-link failures remain browser-level behavior unless link monitoring is introduced.

## 9. Motion

Motion reinforces continuity between sections and states:

- State-layer feedback: 100ms.
- Small component changes: 150-250ms.
- Navigation and container transitions: 250-400ms.
- Major atmospheric scene change: up to 700ms, never blocking interaction.

Recommended motion:

- Hero content enters with a short emphasized fade and translate.
- Project cards reveal once as they enter the viewport, with minimal stagger.
- Project-to-case-study navigation may use a container transform where reliable.
- Section atmosphere shifts from warm morning to indigo dusk through restrained color interpolation.
- Shoji-inspired masks may appear at one or two major transitions, not between every section.

Avoid permanent leaf showers, looping floating cards, cursor trails, scroll-jacking, and decorative motion without a state change. Under `prefers-reduced-motion`, remove parallax and nonessential transforms, disable smooth scrolling, and shorten fades.

## 10. Accessibility

- Target WCAG 2.2 AA contrast for text and interactive controls.
- Every interactive element is keyboard reachable in a logical order.
- Focus is always visible and distinct from hover.
- Touch targets are at least 48x48px on compact layouts.
- Navigation uses semantic links; actions use buttons.
- Headings follow a meaningful hierarchy without skipping levels for styling.
- Decorative Kyoto imagery is ignored by assistive technology.
- Project screenshots have meaningful alternative text.
- Selection, current state, and errors never rely on color alone.
- Browser zoom to 200% preserves content and actions without horizontal page scrolling.
- The site respects reduced motion, forced colors, increased contrast, and reduced transparency where supported.
- External links clearly expose their destination behavior to assistive technology when context is ambiguous.

## 11. Performance and Reliability

Performance is part of the visual design. The initial experience should remain useful before decorative art or project imagery finishes loading.

Targets for representative production pages:

- Largest Contentful Paint below 2.5 seconds on a typical mobile connection.
- Interaction to Next Paint below 200ms.
- Cumulative Layout Shift below 0.1.
- Initial critical route JavaScript kept deliberately small; decorative scenes are lazy-loaded.

Asset rules:

- Convert multi-megabyte PNG photographs and screenshots to appropriately sized AVIF or WebP with PNG fallback only where necessary.
- Provide responsive `srcset` and explicit dimensions.
- Prefer SVG for abstract Kyoto scenery and icons.
- Do not preload every project asset after the avatar.
- Load only the hero image at high priority.
- Use local or carefully subset font files where licensing permits; avoid loading unused weights.
- Pause off-screen atmospheric animation and avoid DOM particle fields.

The site must remain deployable as a static Vite application on Vercel. No backend is required for the redesign baseline.

## 12. Content Model

Portfolio content should remain data-driven but evolve beyond the current flat constants:

- `personal`: name, role, short introduction, long biography, portrait, availability.
- `projects`: slug, title, summary, outcome, role, technologies, imagery, live URL, repository URL, case-study sections.
- `experience`: role, company, period, location, summary, responsibilities, status.
- `education`: degree, institution, period, details.
- `capabilities`: named groups containing skills and supporting evidence.
- `contact`: email, social links, resume, support link.

All visible strings should be easy to update without editing component structure. Missing optional data must produce a valid layout rather than an empty label or broken control.

## 13. Content Voice

The portfolio speaks in clear international English. It may use small Japanese terms where culturally and contextually appropriate, but they require understandable surrounding context.

Prefer:

- "Built the mobile frontend and visual system for a Japanese-learning application."
- "Selected work"
- "View case study"
- "Currently based in Japan; relocating to Australia in September 2026."

Avoid:

- Generic claims without evidence.
- Decorative Japanese copy that a visitor must translate.
- Long technology lists before explaining the work.
- Mixing first-person and third-person voice in the same section.
- Outdated availability or relocation statements without an explicit update path.

## 14. Do's and Don'ts

### Do

- Do use M3 semantic roles throughout the interface.
- Do let projects and professional evidence lead the visual hierarchy.
- Do use Kyoto through material, proportion, light, and atmosphere.
- Do adapt navigation and composition to available width.
- Do use normal vertical scrolling and native browser behavior.
- Do concentrate expressive shapes and motion in meaningful regions.
- Do optimize images before using them as full-width visual elements.
- Do verify all four seasonal schemes.
- Do make every interactive state visible and accessible.
- Do preserve comfortable negative space.

### Don't

- Don't imitate an Android app screen literally.
- Don't turn every card into a Japanese cultural object.
- Don't use Ema, scroll, temple, or torii shapes as repeated containers.
- Don't hide essential content behind hover or generic modals.
- Don't use glassmorphism as the default surface.
- Don't add gradients to routine controls.
- Don't use permanent particle animation.
- Don't hijack vertical scrolling to move content horizontally.
- Don't hide the browser scrollbar.
- Don't hardcode page-specific colors inside React components.
- Don't use low-contrast text over texture or imagery.
- Don't add a contact form without a real delivery and privacy plan.

## 15. Non-Goals

The initial redesign does not require:

- A CMS, database, authentication, or account system.
- Blog publishing infrastructure.
- Real-time analytics dashboards.
- A custom WebGL world or game-like navigation.
- Localization into multiple interface languages.
- Automatic dynamic color extraction from project imagery.
- A complete case study for every project before launch.
- Recreating M3's Android implementation packages in React.

These may be reconsidered only when a real product need appears.

## 16. Implementation Contract

The implementation should centralize semantic tokens as CSS custom properties and build reusable React primitives for:

- Adaptive app shell and navigation.
- Theme selection and persistence.
- Button and icon-button variants.
- Assist and filter chips.
- Featured and standard project cards.
- Experience timeline items.
- Section heading and editorial prose layouts.
- Loading, missing-image, empty, and error states.
- Snackbar, dialog, and sheet primitives only where needed.

Component code must not depend on the legacy hard-coded values `#f4f1e8`, `#2d2d2d`, `#c93a2b`, `#c5a059`, or `#2c3e50`. Map their intent into semantic roles instead of performing a one-to-one color replacement.

Before a visual change is accepted, verify:

1. Compact, medium, expanded, and large compositions.
2. Keyboard navigation, focus visibility, and heading order.
3. Contrast across all four seasonal themes.
4. Reduced-motion behavior.
5. Missing, slow, and unusually wide or tall project imagery.
6. Long project titles and biography text.
7. Browser back/forward and hash navigation.
8. Image weight, font loading, and layout stability.
9. Production build and lint.

This document is the visual and interaction source of truth for the redesign. `summary.md` remains useful historical context, but when the two documents differ, `DESIGN.md` governs the new presentation, interaction model, and implementation constraints.

## 17. Decision Log

| Decision | Alternatives considered | Reason |
| --- | --- | --- |
| Rebuild the presentation from scratch | Reskin current components; incrementally modernize the horizontal site | The user explicitly approved a complete redesign and wants Kyoto to remain as the theme rather than preserving existing widgets. |
| Adopt "Kyoto Material" | Pure traditional Kyoto; generic M3 portfolio; cinematic night-only experience | It combines recognizable identity with a coherent, accessible component and interaction system. |
| Use normal vertical scrolling | Preserve virtual horizontal scrolling; create a WebGL journey | Native scrolling improves usability, accessibility, mobile parity, navigation reliability, and maintenance. |
| Make projects the primary content | Lead with biography; lead with decorative scene | Portfolio visitors need evidence of work quickly. |
| Use four seasonal themes | Light only; dark only; two selectable palettes | Four complete schemes provide atmosphere and chronological progression without unnecessary theme complexity. |
| Express Kyoto abstractly | Literal themed cards and temple illustrations throughout | Material, color, proportion, and light create a more contemporary and durable identity. |
| Prefer dedicated project routes | Generic project modal; external links only | Routes support deeper storytelling, sharing, browser history, and accessibility when content is available. |
| Use adaptive window classes | One mobile breakpoint; separate desktop and mobile implementations | Adaptive composition reduces duplicated behavior and handles the range between phone and large desktop. |
| Treat motion as state communication | Permanent particles and ambient loops | Concentrated motion is clearer, calmer, faster, and easier to make accessible. |
