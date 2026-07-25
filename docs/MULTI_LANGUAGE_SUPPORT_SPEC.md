# Multi-language Support — Product and Technical Specification

**Status:** Draft  
**Target:** OScan Android app  
**Initial languages:** English, Simplified Chinese, Japanese  
**Locale tags:** `en`, `zh-CN`, `ja`

## 1. Summary

OScan will let a user choose the language used by the app independently from the device language. The initial release supports:

| Choice shown in UI | Native label | Locale tag | Behavior |
|---|---|---|---|
| System default | System default | empty locale list | Follow the device/app language selected in Android Settings |
| English | English | `en` | Force English |
| Simplified Chinese | 简体中文 | `zh-CN` | Force Simplified Chinese |
| Japanese | 日本語 | `ja` | Force Japanese |

The setting applies to all OScan-owned UI text, accessibility descriptions, dialogs, errors, notifications, and formatted quantities. User content—document names, folder names, display name, imported filenames, and OCR/document pixels—is never translated.

The implementation must use Android string resources and AndroidX per-app locale APIs. It must not introduce an in-memory translation dictionary or branch on the selected language inside Composables.

## 2. Goals

- Let the user change OScan's display language from the existing **Me → Settings** area.
- Apply the selected language immediately across the active activity.
- Persist the selection across process death, app updates, and device restarts.
- Follow the device locale when **System default** is selected.
- Fully localize English, Simplified Chinese, and Japanese UI and accessibility text.
- Preserve existing app data and all unrelated preferences.
- Use quantity resources and format arguments so grammar and word order can differ by language.

## 3. Non-goals

- Translating user-created names, saved documents, folder names, filenames, or vault content.
- OCR, handwriting recognition, or document-content translation.
- Right-to-left language support in the first release. The resource design must not prevent it later.
- Downloadable language packs or server-managed translations.
- Locale-specific scanning or image-processing behavior.
- Translating third-party license text. The surrounding About UI is translated; license bodies remain as supplied.

## 4. User experience

### 4.1 Entry point

Add a new row to **Me → Settings**, immediately after **Appearance** and before **Storage**:

- Leading icon: `Icons.Default.Language`
- Title: **Language**
- Subtitle:
  - **System default** when following the device
  - The selected language's native name otherwise (`English`, `简体中文`, or `日本語`)
- Tap action: open the Language settings screen

### 4.2 Language settings screen

Add a full-screen settings route consistent with the existing Capture, Appearance, and Storage screens.

**Top app bar**

- Title: **Language**
- Back button with localized accessibility description

**Introductory text**

- Heading: **App language**
- Supporting text: **Choose the language used in OScan.**

**Selection list**

Display four full-width, single-selection rows:

1. System default
2. English
3. 简体中文
4. 日本語

Each row contains:

- A radio button
- The language name (language names should remain in their native form in every locale)
- For System default only, secondary text showing the currently resolved device language, for example **Currently: English**

The whole row is tappable and has a minimum 48 dp touch target. The selected row exposes the correct selected semantics to accessibility services.

### 4.3 Selection behavior

1. The user taps a language.
2. Persist the selection through the locale API.
3. Apply the locale immediately.
4. Android recreates the activity as needed; the user returns to the Language screen with the new language selected.
5. Do not show a confirmation dialog, toast, or manual restart prompt.

If the selected language is already active, do nothing.

When **System default** is selected, clear the app-specific locale list rather than copying the current device locale. This allows later device-language changes to flow through automatically.

### 4.4 Android system settings

Declare the supported locales in `res/xml/locales_config.xml` and reference it from the application manifest. On Android 13+, this makes English, Simplified Chinese, and Japanese available in the system's per-app Language settings. A change made there and a change made inside OScan must represent the same underlying app-locale state.

## 5. Localization architecture

### 5.1 Resource layout

Create these resource sets:

```text
android-app/src/main/res/
├── values/strings.xml          # English source of truth
├── values-zh-rCN/strings.xml   # Simplified Chinese
├── values-ja/strings.xml       # Japanese
└── xml/locales_config.xml      # en, zh-CN, ja
```

English is the default resource set. Every key in the default `strings.xml` must exist in both translated files before release, except keys explicitly marked `translatable="false"`.

Use:

- `stringResource(R.string.key)` in Composables.
- `pluralStringResource(R.plurals.key, count, count)` for visible Compose quantities.
- `context.getString(...)` / `resources.getQuantityString(...)` outside Compose.
- Positional format arguments (`%1$s`, `%2$d`) wherever values are interpolated.
- `<xliff:g>` placeholders when it improves translator context.
- `translatable="false"` for product name `OScan`, filename tokens such as `{DATE}`, URLs, and technical identifiers.

Do not concatenate translated fragments. For example, replace `"$count items in Trash"` with a complete plural resource, and replace `"Page ${position + 1} of ${total}"` with one formatted string.

### 5.2 Locale model

Add:

```kotlin
enum class AppLanguage(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    SIMPLIFIED_CHINESE("zh-CN"),
    JAPANESE("ja")
}
```

`languageTag == null` maps to `LocaleListCompat.getEmptyLocaleList()`. All other values map through `LocaleListCompat.forLanguageTags(languageTag)`.

Language display names must come from string resources, not an enum `label` property. The enum is stable persisted/domain identity only.

### 5.3 Source of truth and persistence

Use `AppCompatDelegate.setApplicationLocales(...)` as the single source of truth for the active locale.

- Add AndroidX AppCompat.
- Change `MainActivity` from `FragmentActivity` to `AppCompatActivity`; this retains `FragmentActivity` behavior required by biometric prompts.
- Enable AndroidX automatic locale storage with the `AppLocalesMetadataHolderService` manifest metadata/service entry for Android 12 and lower.
- On Android 13+, Android's framework locale manager persists app locales.
- Read the current selection with `AppCompatDelegate.getApplicationLocales()`.

Do **not** duplicate the locale in `UserPreferencesStore`. Dual persistence can drift when the user changes OScan's language from Android Settings. The locale API is authoritative.

If product requirements later demand analytics-free observation through `LibraryUiState`, add a `LocaleRepository` that observes configuration changes; do not mirror the setting into DataStore.

### 5.4 Locale application

Add `AppLocaleController` with two operations:

```kotlin
interface AppLocaleController {
    fun currentLanguage(): AppLanguage
    fun setLanguage(language: AppLanguage)
}
```

`AndroidAppLocaleController` converts between `AppLanguage` and `LocaleListCompat`, invokes `AppCompatDelegate.setApplicationLocales`, and treats any unsupported external locale as `SYSTEM` for the in-app selection UI.

The controller is created in `AppContainer` and passed into the app shell. The Language screen receives immutable state plus an `onLanguageSelected` callback. It must not call global locale APIs directly.

### 5.5 State and lifecycle

The selected value should be refreshed from `AppLocaleController.currentLanguage()` whenever the Language screen enters composition and after activity recreation. Because the activity is recreated on an actual locale change, a separate long-lived Flow is unnecessary for version one.

Do not add `android:configChanges="locale|layoutDirection"` to suppress recreation. Standard activity recreation ensures resources, Compose, accessibility text, and any non-Compose Android UI are all refreshed consistently.

## 6. String migration scope

All user-facing literals in `android-app/src/main` must move to resources. This includes:

- Visible Compose `Text`, button labels, headings, field labels, menu items, empty states, and dialog copy.
- Icon/image `contentDescription` values.
- Accessibility announcements and semantic descriptions.
- ViewModel/repository error messages that reach the UI.
- Enum/model display labels currently stored as English strings.
- Camera guidance, scan progress, crop/review states, export status, vault states, and library messages.
- Document counts, folder counts, page counts, and trash counts.
- Date/time presentation that is currently assembled manually.

Internal logs, exception diagnostics that never reach the user, database values, preference keys, enum names, filter IDs, and test fixture text do not need translation.

### 6.1 Domain labels

Remove localized display text from enums:

- `AccentTheme.label`
- `PdfPageSize.label`
- `JpegQuality.label`
- `FilterType.displayName` where it is used by Android UI
- `DocumentSort.label()`
- `SessionPageStatus.readableLabel()`

Replace them with Android-side resource mapping functions annotated `@Composable` or taking `Resources`. Keep enum names and stored values unchanged to avoid preference/data migrations.

### 6.2 Errors emitted below the UI

Prefer typed error/status values from ViewModels and repositories, with localization at the UI boundary. Where a complete typed-error refactor is too large for this feature, pass a string resource ID plus format arguments. Do not localize exceptions deep in `core-engine`, because that module is platform-neutral.

## 7. Exact file change plan

### New files

| File | Required change |
|---|---|
| `android-app/src/main/res/values/strings.xml` | English source strings, plurals, format arguments, translator comments, non-translatable constants |
| `android-app/src/main/res/values-zh-rCN/strings.xml` | Complete Simplified Chinese translation |
| `android-app/src/main/res/values-ja/strings.xml` | Complete Japanese translation |
| `android-app/src/main/res/xml/locales_config.xml` | Declare `en`, `zh-CN`, and `ja` |
| `android-app/src/main/kotlin/com/oscan/android/data/preferences/AppLanguage.kt` | Stable language enum |
| `android-app/src/main/kotlin/com/oscan/android/localization/AppLocaleController.kt` | Locale controller interface and Android implementation |
| `android-app/src/main/kotlin/com/oscan/android/ui/LanguageSettingsScreen.kt` | Language selection UI |
| `android-app/src/test/kotlin/com/oscan/android/localization/AppLocaleControllerTest.kt` | Locale-tag conversion and empty-list/system tests |
| `android-app/src/test/kotlin/com/oscan/android/ui/LocalizationResourcesTest.kt` | Resource parity and format/plural validation |
| `android-app/src/androidTest/kotlin/com/oscan/android/LanguageSettingsTest.kt` | Selection, recreation, persistence, and screen translation tests |

### Existing configuration and application files

| File | Required change |
|---|---|
| `android-app/build.gradle.kts` | Add `androidx.appcompat:appcompat`; keep existing Compose/resource support |
| `android-app/src/main/AndroidManifest.xml` | Set `android:localeConfig="@xml/locales_config"` and configure AndroidX locale auto-storage for pre-33 devices |
| `android-app/src/main/kotlin/com/oscan/android/MainActivity.kt` | Extend `AppCompatActivity`; keep biometric-compatible behavior and existing Compose setup |
| `android-app/src/main/kotlin/com/oscan/android/OScanApplication.kt` | Construct and expose `AppLocaleController` from `AppContainer` |

### Navigation, settings, and preference files

| File | Required change |
|---|---|
| `android-app/src/main/kotlin/com/oscan/android/ui/AppShell.kt` | Add `SettingsSubRoute.LANGUAGE`, route rendering, Language row/icon/subtitle, localized navigation and all existing literals |
| `android-app/src/main/kotlin/com/oscan/android/ui/ScannerApp.kt` | Pass the locale controller into `AppShell`; migrate scanner flow strings and accessibility announcements |
| `android-app/src/main/kotlin/com/oscan/android/ui/SettingsScreens.kt` | Migrate every settings string; replace enum `.label` usage with resource mappings |
| `android-app/src/main/kotlin/com/oscan/android/data/preferences/UserPreferences.kt` | Remove English enum label properties while preserving enum constants and persisted names |
| `android-app/src/main/kotlin/com/oscan/android/ui/LibraryViewModel.kt` | Replace user-visible raw error/status strings with typed/resource-backed UI messages; no language setter is added here |
| `android-app/src/test/kotlin/com/oscan/android/data/preferences/UserPreferencesStoreTest.kt` | Confirm existing preferences remain unchanged; no locale DataStore key is introduced |

### Remaining UI surfaces requiring string migration

| File | Localization work |
|---|---|
| `android-app/src/main/kotlin/com/oscan/android/ui/LibraryScreens.kt` | Library, folders, trash, dialogs, sorting, counts, actions, errors |
| `android-app/src/main/kotlin/com/oscan/android/ui/StartScreen.kt` | Start/empty-state copy and actions |
| `android-app/src/main/kotlin/com/oscan/android/ui/CameraScreen.kt` | Camera controls, guidance, permissions, accessibility |
| `android-app/src/main/kotlin/com/oscan/android/ui/CropScreen.kt` | Crop controls and accessibility |
| `android-app/src/main/kotlin/com/oscan/android/ui/PreviewScreen.kt` | Treatment names, actions, accessibility |
| `android-app/src/main/kotlin/com/oscan/android/ui/PageEditorScreen.kt` | Editor actions, statuses, dialogs |
| `android-app/src/main/kotlin/com/oscan/android/ui/ExportSuccessScreen.kt` | Export result/actions |
| `android-app/src/main/kotlin/com/oscan/android/ui/vault/VaultScreens.kt` | Vault setup, unlock, move, warnings, errors, accessibility |
| `android-app/src/main/kotlin/com/oscan/android/ui/AdaptiveAccessibility.kt` | Any user-facing semantics or announcements |
| `android-app/src/main/kotlin/com/oscan/android/ui/ScannerViewModel.kt` | Replace user-visible status/error strings with typed/resource-backed messages |
| `android-app/src/main/kotlin/com/oscan/android/ui/CameraViewModel.kt` | Replace user-visible permission/camera errors with typed/resource-backed messages |
| `android-app/src/main/kotlin/com/oscan/android/ui/PageEditorViewModel.kt` | Replace user-visible save/process errors with typed/resource-backed messages |
| `android-app/src/main/kotlin/com/oscan/android/ui/vault/VaultViewModel.kt` | Replace user-visible vault errors/statuses with typed/resource-backed messages |

Files in `core-engine` require no locale-specific logic. If Android currently displays a raw `core-engine` exception, map it to a localized Android error at the ViewModel boundary.

## 8. Translation rules

- Use concise, natural product language; do not translate word-for-word when that sounds unnatural.
- Keep `OScan`, file extensions, `{DATE}`, `PDF`, `JPEG`, `A4`, and `US Letter` unchanged unless a conventional localized presentation exists.
- Use Simplified Chinese terminology for Mainland China (`zh-CN`), not Traditional Chinese.
- Japanese UI should use standard Japanese product terminology and full-width Japanese punctuation where natural.
- Avoid embedding English capitalization assumptions in layouts or tests.
- Add translator comments for ambiguous nouns such as “Scan,” “Magic,” “Vault,” “Crop,” and “Treatment.”
- Use locale-aware number/date formatting. Do not hard-code English separators or plural suffixes.
- User-visible default `Local Workspace` should become a resource-derived default for new installs. Existing persisted custom/default display names must not be silently rewritten in this feature.

## 9. Accessibility and layout requirements

- Every translated content description must convey the same action, not merely translate the visible noun.
- TalkBack announcements must use complete localized sentences and plural resources.
- Verify Chinese and Japanese glyph fallback under the existing typography.
- No text may clip at system font scale 200%.
- Settings rows and action groups must wrap vertically rather than truncate essential actions.
- Preserve semantic selection state for language radio rows.
- Keep layout start/end based; do not introduce left/right assumptions.

## 10. Failure and edge cases

- **Unsupported device locale:** default English resources are used.
- **System default resolves to Chinese/Japanese:** the System row remains selected; subtitle reports the resolved language.
- **Locale changed in Android Settings while OScan is stopped:** next launch reflects it.
- **Locale changed in Android Settings while OScan is alive:** configuration recreation refreshes resources and the selected row.
- **Unsupported locale tag restored externally:** treat the in-app choice as System default while Android falls back through resources.
- **Process death during switch:** AndroidX/framework persistence restores the selected locale.
- **Upgrade from an older OScan build:** locale list is empty, so existing users initially follow their device language; all existing DataStore/Room data remains valid.
- **User-entered text:** retain exactly as entered across language changes.

## 11. Testing and acceptance criteria

### 11.1 Automated tests

- Unit-test all `AppLanguage` ↔ locale-list conversions.
- Verify System maps to an empty locale list.
- Verify unsupported/multiple locale lists have a deterministic fallback.
- Verify every English string/plural key exists in Chinese and Japanese.
- Verify translated format placeholders have the same type and index set as English.
- Compose UI test: Language row appears after Appearance and opens the new screen.
- Compose UI test: each choice is selectable and exposes selected semantics.
- Instrumentation test: choosing Japanese recreates the activity and displays Japanese text.
- Instrumentation test: choosing Chinese persists after activity and process recreation.
- Instrumentation test: choosing System clears the explicit app locale.
- Instrumentation test: changing language in system settings is reflected in-app on Android 13+.

### 11.2 Manual matrix

Test on API 26, API 32, API 33, and API 34 with:

- Device languages English, Simplified Chinese, Japanese, and one unsupported language.
- OScan setting System, English, Simplified Chinese, and Japanese.
- Light/dark themes and each navigation destination.
- Font scales 100%, 130%, and 200%.
- TalkBack for the language list, camera controls, crop controls, review actions, and vault.
- Cold launch, warm launch, rotation/configuration recreation, process death, and app upgrade.

### 11.3 Release acceptance criteria

- Language row and screen match Section 4.
- Switching among all four choices requires no app restart prompt.
- Every OScan-owned screen listed in Section 7 renders in the selected language.
- No user data changes when the language changes.
- No hard-coded user-facing English remains under `android-app/src/main`.
- Chinese and Japanese resource sets have 100% key coverage and valid placeholders.
- Quantity strings produce correct output for 0, 1, and multiple values.
- The Android 13+ per-app Language page lists exactly the supported languages.
- All existing unit/instrumentation tests pass, plus the tests in this spec.

## 12. Implementation sequence

1. Add AppCompat dependency, manifest locale configuration, locale controller, and `AppCompatActivity` base.
2. Add `AppLanguage`, the Language screen, route, and Me settings row.
3. Create the English resource catalog and migrate settings/navigation strings.
4. Migrate every remaining UI and accessibility literal; convert dynamic counts to plurals and errors to typed/resource-backed messages.
5. Add complete Chinese and Japanese resources.
6. Add parity, controller, and UI tests.
7. Run the API/language/accessibility manual matrix and obtain native-language review.

## 13. Open product decisions

These do not block the technical design, but must be resolved before translation sign-off:

- Whether the persisted default display name **Local Workspace** should remain English for upgraded users or be migrated only when it still exactly equals the old default.
- Whether the marketing term **Magic** should remain branded in English or use a localized product term.
- Whether Traditional Chinese (`zh-TW`) is planned soon; it is not included in this release and must not silently fall back to Simplified Chinese as if it were equivalent.

