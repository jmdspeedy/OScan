# Vault Feature Specification

**Product:** OScan  
**Feature:** Vault  
**Status:** Draft v0.1  
**Target:** Android, offline/local-first  
**Last updated:** 2026-07-24

## 1. Summary

Vault is a protected area inside OScan for sensitive documents. A user creates a Vault passcode, then moves, imports, or scans documents into Vault. Vault document content and sensitive metadata are encrypted at rest and are unavailable until the user unlocks Vault with that passcode.

Vault is not a standard OScan folder with a hidden route. It is a separate encrypted storage boundary. When Vault is locked, OScan must not load or display Vault document names, thumbnails, page previews, search results, folder placement, favorites, or deleted items.

The feature remains fully offline. OScan has no account-based passcode recovery and no remote copy of the encryption key.

## 2. Product promise

User-facing promise:

> Documents in Vault are encrypted on this device and can be opened in OScan only after Vault is unlocked with your passcode.

Required qualification in setup and Help:

> If you forget your passcode, OScan cannot recover your Vault documents. Resetting Vault permanently deletes them.

OScan must not claim that Vault provides secure deletion, protects a compromised device while Vault is unlocked, or prevents a recipient from retaining an explicitly exported copy.

## 3. Goals

- Require the user-created Vault passcode before Vault content is decrypted.
- Encrypt every durable Vault document asset, including originals, processed pages, and thumbnails.
- Encrypt sensitive Vault metadata, including document names and page metadata.
- Keep Vault content out of Home, folders, Recents, favorites, search, normal Trash, Android recents previews, and logs while locked.
- Preserve the existing local-first model: no account, server, analytics, or network dependency.
- Support safe, recoverable movement of documents into and out of Vault.
- Automatically return Vault to a locked state when the security session ends.
- Make failure states explicit and avoid plaintext leftovers after interrupted operations.

## 4. Non-goals for the first release

- Cloud sync, remote backup, web access, or cross-device Vault transfer.
- Account-based passcode reset or administrator recovery.
- Multiple vaults, shared vaults, or per-document passcodes.
- Biometric-only access. Biometric quick unlock may be considered later, but the Vault passcode remains the recovery-independent credential.
- Plausible deniability, a decoy Vault, or hiding that a Vault has been configured.
- Protection against screen photography, a compromised operating system, malicious accessibility services, kernel/root compromise, or memory inspection while Vault is unlocked.
- Guaranteed forensic secure deletion on flash storage.

## 5. Threat model

### 5.1 In scope

Vault is designed to protect content from:

- A person who can open OScan on an unlocked device but does not know the Vault passcode.
- Casual inspection through Home, search, folders, Trash, app switcher previews, notifications, logs, or file browsing.
- Offline inspection of copied OScan app files where the attacker does not have the passcode.
- Accidental plaintext exposure caused by normal preview, edit, export, or crash-recovery flows.
- Modification or corruption of encrypted files, which must be detected before plaintext is used.

### 5.2 Out of scope

Vault does not promise protection from:

- An attacker controlling the device OS, OScan process, or input method while the user enters the passcode.
- Runtime capture after the user has unlocked Vault.
- A weak passcode subjected to sustained offline guessing.
- Plaintext copies the user explicitly exports or shares outside OScan.

These boundaries must inform QA and product copy. “Encrypted” and “passcode protected” are accurate; “unbreakable,” “secure deletion,” and “zero-knowledge backup” are not.

## 6. Core user experience

### 6.1 Entry point

- Add a `Vault` entry to Home's secondary Browse actions, alongside Folders and Trash.
- The entry uses a lock icon and the label `Vault`.
- When locked, the entry shows `Locked` and no document count, names, or preview stack.
- Vault is not a fourth primary navigation destination; Home, Scan, and Me remain unchanged.

### 6.2 First-time setup

1. The user opens Vault.
2. OScan explains:
   - Vault content is encrypted locally.
   - The passcode is required to access it.
   - OScan cannot recover a forgotten passcode.
   - Exported copies are outside Vault protection.
3. The user creates a passcode and confirms it.
4. The user acknowledges the no-recovery warning.
5. OScan creates the Vault cryptographic material and opens an empty Vault.

Passcode requirements:

- Accept a numeric passcode of 6–12 digits for the first release.
- Reject common sequences and a single repeated digit, such as `123456` or `111111`.
- Do not trim, log, persist, include in crash reports, or retain the passcode as a `String` longer than required.
- Offer a visible `Show passcode` control on setup only, with an accessible state description.
- Never use the passcode itself as an encryption key or store a reversible copy of it.

The UI should encourage a longer passcode and explain that a short numeric code is easier to guess.

### 6.3 Unlock

- Opening a configured, locked Vault shows a dedicated passcode screen.
- Unlock succeeds only when the passcode-derived key successfully authenticates and unwraps the Vault master key.
- There is no separate boolean “passcode is correct” gate that can unlock the UI without completing the cryptographic operation.
- A failed attempt shows `Incorrect passcode` without revealing partial information.
- The passcode field clears after each failed attempt and when the screen is left.
- Pasting into the passcode field is disabled for the numeric first-release flow.
- Android back navigation exits to the previous OScan screen without changing Vault state.

Attempt throttling:

- After 5 consecutive failures, delay the next attempt by 30 seconds.
- Increase the delay after further failures, capped at 1 hour.
- Persist the failure count and next-allowed-attempt timestamp in integrity-protected local state.
- Reset the counter after a successful unlock.
- Treat throttling as a usability and casual-attack defense, not as protection against offline guessing of extracted ciphertext.

### 6.4 Unlocked Vault

The unlocked screen follows OScan's existing library patterns:

- Grid/list presentation and local name search.
- Sort by modified date, created date, and name.
- Open, rename, favorite, multi-select, export/share, move out of Vault, and move to Vault Trash.
- Scan or import directly into Vault.
- Add, reorder, rotate, re-crop, change treatment, and remove pages.
- A persistent app-bar lock action labeled `Lock Vault`.

Vault content is a separate collection:

- Vault favorites do not appear in Home favorites.
- Vault items never appear in Home Recent or All documents.
- Vault search runs only while unlocked and does not join normal library search.
- Search text and decrypted indexes are cleared when Vault locks.

### 6.5 Auto-lock

Vault locks and clears its active key/session when any of the following occurs:

- The user taps `Lock Vault`.
- OScan leaves the foreground.
- The device screen turns off or the device locks.
- The app process is recreated or terminated.
- An unrecoverable cryptographic or integrity error occurs.

The first release uses immediate background locking. A configurable grace period may be considered later after security review.

Navigating between screens inside foreground OScan does not automatically lock Vault, but Vault content must disappear immediately when the session is locked.

### 6.6 Forgotten passcode and reset

- The unlock screen provides `Forgot passcode?`.
- OScan states that the passcode and documents cannot be recovered.
- The only recovery action is `Reset and delete Vault`.
- Reset requires a two-step, count-neutral destructive confirmation. It must not reveal document names or the number of documents.
- Reset deletes encrypted Vault files, the encrypted manifest, wrapped keys, salts, KDF parameters, and attempt-throttling state, then returns to first-time setup.
- The UI must say that flash storage does not permit a guarantee of forensic secure erasure.

### 6.7 Change passcode

- Available under `Me > Settings > Vault` and from the unlocked Vault overflow menu.
- Requires the current Vault passcode, even during an existing unlocked session.
- The user enters and confirms a new passcode.
- OScan derives a new passcode key and re-wraps the same Vault master key. Document assets are not re-encrypted.
- The old wrapping record is removed only after the new wrapping record is durably written and verified.

### 6.8 Disable Vault

Disabling Vault requires the current passcode and an unlocked Vault. The user must choose one of:

- `Move documents to library and disable`: decrypt and migrate all active and trashed Vault documents into the standard local library, then remove Vault configuration.
- `Delete Vault and documents`: permanently remove the encrypted repository and configuration.

The operation must be resumable and must never silently delete documents after a partial migration.

## 7. Document lifecycle

### 7.1 Move an existing document into Vault

- Expose `Move to Vault` in a document overflow menu and selection mode.
- Require Vault setup or unlock before migration begins.
- Encrypt the document name, metadata, originals, processed pages, and thumbnails into a new Vault document record.
- Verify that every encrypted asset can be authenticated and decrypted before deleting the standard-library database row and plaintext asset directory.
- Use a transaction journal so a crash can resume or roll back without losing the only valid copy.
- Do not expose the document in both libraries after a successful migration.
- If migration fails, keep the original document intact and remove incomplete encrypted outputs.

Bulk moves report progress and item-specific failures without exposing names after Vault relocks.

### 7.2 Move a document out of Vault

- Require an unlocked Vault.
- Let the user choose `Unfiled` or an existing standard folder.
- Decrypt into a staged standard-library document, verify all assets, commit the Room graph, and only then delete the encrypted Vault copy.
- Warn that the moved document will no longer be protected by Vault encryption.

### 7.3 Create content in Vault

- Scanning or importing from unlocked Vault defaults the destination to Vault.
- Capture/import working files may exist briefly in app-private cache during processing, but must never be placed in shared storage.
- Encrypt durable assets before declaring save success.
- Clean related working files after successful encryption, cancel, failure, auto-lock, and process-recovery cleanup.
- A cleanup failure is surfaced and retried at next launch; OScan must not claim the operation is fully secured until cleanup completes.

### 7.4 Vault Trash

- Vault has a separate encrypted Trash accessible only while unlocked.
- Deleting a Vault document moves it to Vault Trash and preserves its encrypted content.
- Restore, permanent delete, and Empty Vault Trash require an unlocked Vault.
- Vault items never enter or appear in normal Trash.
- The first release uses manual emptying, matching the standard library.

## 8. Security and cryptography requirements

### 8.1 Key hierarchy

- Generate one random 256-bit Vault Master Key (VMK) using a cryptographically secure random generator.
- Derive a 256-bit Passcode-Derived Key (PDK) from the user passcode with a unique random salt and a memory-hard password KDF. Argon2id is preferred.
- Benchmark KDF parameters on supported devices and target approximately 250–500 ms per unlock on a representative mid-range device without risking out-of-memory failures. Persist the algorithm, version, salt, and parameters so they can be upgraded.
- Use the PDK only to encrypt and authenticate the VMK wrapping record.
- Use independently derived subkeys, with explicit domain separation, for document content and metadata. Do not reuse one key for unrelated cryptographic roles.
- Hold the unwrapped VMK/subkeys only in the in-memory unlocked session and release references on lock. Use mutable byte/character arrays where practical and overwrite them before release.
- Do not store the raw VMK, PDK, passcode, or plaintext verifier on disk.

The implementation may add a device-bound Android Keystore wrapping layer as defense in depth, but it must not replace the user passcode cryptographic check. Keystore keys must be non-exportable and restricted to their intended operation. Vault data must be excluded from Android Auto Backup unless and until a tested portable backup format exists.

### 8.2 Asset encryption

- Encrypt each durable asset independently using AES-256-GCM.
- Generate a fresh, unpredictable 96-bit nonce for every encryption operation. Never reuse a nonce with the same key.
- Store a versioned envelope containing only non-secret fields needed to decrypt: format version, algorithm identifier, key identifier/version, nonce, ciphertext length, and authentication tag.
- Bind the Vault ID, document ID, page ID, asset kind, and envelope version as authenticated additional data (AAD) so ciphertext cannot be silently swapped between records.
- Authenticate the complete asset before handing plaintext to an image decoder or PDF exporter.
- Treat a tag failure, truncated file, unexpected length, or unsupported version as corruption. Do not show partial plaintext.
- Use streaming encryption/decryption with bounded memory for full-resolution and multi-page assets.

Android's current guidance recommends AES in GCM mode and Android Keystore for stronger key protection. The deprecated `androidx.security.crypto.EncryptedFile` API must not be the foundation of a new implementation. See [Android cryptography guidance](https://developer.android.com/privacy-and-security/cryptography), [Android Keystore](https://developer.android.com/privacy-and-security/keystore), and [Jetpack Security release notes](https://developer.android.com/jetpack/androidx/releases/security).

### 8.3 Metadata

The following are sensitive and must be encrypted:

- Document and page names.
- Original, processed, and thumbnail bytes.
- Page dimensions, rotation, crop points, ordering, and treatment.
- Folder-like grouping inside Vault, if added later.
- Favorite, created, modified, and deletion timestamps.
- Export history, if ever introduced.

While locked, the normal Room database may contain only the minimum non-sensitive information needed to show that Vault is configured. It must not contain Vault document rows, names, asset paths, thumbnails, counts, or timestamps.

Recommended first-release persistence:

- Keep standard documents in the existing Room database and `files/documents` store.
- Keep Vault documents under a separate `files/vault` encrypted repository.
- Store Vault document metadata in a versioned encrypted manifest or encrypted Vault-specific database opened only for an unlocked session.
- Keep only Vault format/configuration version, KDF parameters, salt, wrapping envelope, and throttling state outside the encrypted repository.

### 8.4 Plaintext handling

- Never write decrypted thumbnails or pages to the normal document store.
- Prefer streaming decryption directly into decoders and exporters.
- If a plaintext temporary file is unavoidable, use a dedicated app-private no-backup cache directory, use an opaque random name, restrict its lifetime, and delete it on completion, lock, cancellation, startup recovery, and process exit where possible.
- Do not log document names, passcodes, decrypted paths, raw URIs, keys, nonces, or plaintext.
- Redact Vault-related crash breadcrumbs and analytics. OScan currently has no telemetry and Vault must not introduce any.
- Clear decoded image caches, Compose state containing Vault metadata, search queries, viewer state, and generated PDFs when locking.
- Do not place Vault content on the clipboard.

### 8.5 Display protection

- Apply Android secure-window protection while passcode, Vault list, document viewer, editor, or export preparation is visible.
- App-switcher/recents previews must be blank or use a neutral OScan privacy screen.
- Block screenshots and non-secure display mirroring on Vault screens where the platform supports it.
- Accessibility remains supported; do not disable TalkBack. Sensitive content descriptions are produced only while unlocked and are removed on lock.

## 9. Export and sharing

Export is an explicit transition out of the Vault security boundary:

- Require an unlocked Vault at the moment export begins.
- Before export/share, show: `The exported copy will not be protected by Vault.`
- For Storage Access Framework export, stream the decrypted PDF directly to the user-selected destination where possible.
- For Android sharing, create a short-lived app-private plaintext PDF exposed through the existing `FileProvider`.
- Grant read access only to the selected recipient through Android's URI permission mechanism.
- Delete share-cache files when the share flow ends where observable, on Vault lock, on next app start, and via age-based cleanup.
- Never reuse a prior share artifact after Vault has locked.
- Locking during export cancels the operation and cleans staged plaintext. If bytes have already been committed to a user-selected destination or recipient, OScan cannot revoke that copy.

## 10. Data integrity, migration, and recovery

- All writes use write-to-new-file, flush/close, authenticate/verify, then atomic rename where supported.
- Maintain a small versioned transaction journal for move-in, move-out, rewrap, and disable operations.
- Startup recovery runs before Vault can unlock and resolves incomplete transactions without loading content into the normal library.
- Never overwrite the last valid wrapping record or manifest in place.
- A corrupt asset affects that document/page and is reported clearly; it must not silently reset or delete the whole Vault.
- A corrupt wrapping record or manifest presents a non-destructive `Vault cannot be opened` state with retry and Help. Reset remains an explicit destructive action.
- Schema and cryptographic format migrations require fixtures from every released format version and rollback-safe testing.

## 11. Accessibility and visual design

- Follow the existing OScan Material 3 system, adaptive layouts, 48dp targets, visible focus, high-contrast support, reduced motion, and 200% font scaling.
- Use direct language: `Unlock Vault`, `Incorrect passcode`, `Lock Vault`, and `Move out of Vault`.
- Do not rely on a lock icon or color alone to convey locked/unlocked state.
- The passcode field exposes one logical accessible control and announces errors once.
- Lockout feedback includes the remaining wait time and does not update more than once per second.
- Destructive reset and disable flows remain reachable and understandable with TalkBack, keyboard, switch access, and large text.
- Avoid decorative “military-grade” security imagery or unqualified security claims.

## 12. Functional requirements

| ID | Requirement |
|---|---|
| VLT-001 | A user must create and confirm a compliant passcode before adding Vault content. |
| VLT-002 | A locked Vault must not return document content or sensitive metadata to UI/repository callers. |
| VLT-003 | Successful unlock must depend on authenticated cryptographic unwrapping with the entered passcode. |
| VLT-004 | Every durable original, processed image, thumbnail, and sensitive metadata record must be encrypted and authenticated at rest. |
| VLT-005 | Moving into or out of Vault must be atomic, resumable, and retain one verified valid copy through commit. |
| VLT-006 | Vault must auto-lock on background, screen-off/device-lock, process recreation, and explicit lock. |
| VLT-007 | Locking must clear decrypted UI state, caches, search state, temporary files, and active key references. |
| VLT-008 | Vault items must not appear in Home, folders, normal search, normal favorites, Recents, or normal Trash. |
| VLT-009 | Vault export/share must warn that the resulting copy is unprotected and minimize plaintext lifetime. |
| VLT-010 | Forgotten-passcode reset must disclose irreversible loss and require explicit destructive confirmation. |
| VLT-011 | Changing the passcode must rewrap the VMK without re-encrypting all document assets. |
| VLT-012 | Authentication failures must be throttled and must reveal no Vault metadata. |
| VLT-013 | Cryptographic integrity failures must fail closed and never display partial content. |
| VLT-014 | Vault content and keys must be excluded from automatic device backup. |
| VLT-015 | Vault screens must prevent screenshots and sensitive app-switcher previews where Android supports it. |

## 13. Acceptance criteria

Vault is ready for release when all of the following pass:

### Security boundary

- With Vault locked, automated repository/UI tests cannot enumerate Vault IDs, names, counts, timestamps, thumbnails, or page metadata.
- Searching the standard library for a Vault document name returns no result.
- Inspecting Room and app files after adding a known test document finds no plaintext name or recognizable original/processed/thumbnail bytes in the Vault repository.
- Editing the unlock UI state or bypassing navigation cannot produce a usable VMK or decrypted document.
- Changing any ciphertext, nonce, tag, AAD identifier, or length causes a closed integrity error.
- Reusing an encrypted asset under another document/page identifier fails authentication.

### Lifecycle

- Correct passcode unlocks; incorrect passcodes do not.
- Five failed attempts activate the specified delay, which survives process restart and device time changes without becoming shorter.
- Backgrounding, screen-off, process recreation, and explicit lock all remove Vault content from view and require the passcode again.
- A process kill at every migration journal step recovers with either the original or the verified migrated document intact.
- Changing the passcode invalidates the old passcode and preserves all documents.
- Forgotten-passcode reset removes the old Vault and permits clean setup.

### Plaintext containment

- Cancel, failure, lock, and process-recovery tests leave no readable preview, decoded page, export, or share-cache artifact beyond documented unavoidable platform destinations.
- Android Auto Backup rules exclude the Vault repository and key/configuration artifacts.
- Screenshots and app-switcher previews do not contain Vault content.
- Release logging contains no passcodes, keys, document names, raw URIs, or decrypted file paths.

### Compatibility and quality

- The feature works offline from setup through unlock, edit, export, lock, and reset.
- Large multi-page documents are encrypted/decrypted with bounded memory and without blocking Compose rendering.
- Vault routes work at 320dp width, landscape, expanded layouts, and 200% font scale.
- TalkBack, keyboard/D-pad, switch access, high contrast, and reduced motion flows pass.
- Cryptography and storage design receive an independent security review before release.

## 14. Test plan

### Unit tests

- Passcode validation and weak-pattern rejection.
- KDF parameter serialization and version migration.
- Key derivation, wrapping, rewrapping, and wrong-passcode failure.
- AES-GCM envelope round trips, unique nonces, AAD binding, truncation, corruption, and unsupported versions.
- Attempt-throttling progression and reset.
- Locked repository guards and memory-session expiry.

### Integration tests

- Move in/out for single-page, multi-page, rotated, re-cropped, favorited, foldered, and trashed documents.
- Crash injection at every staged-write and transaction-journal step.
- Scan/import directly to Vault and cleanup after success/failure/cancel.
- Viewer, editor, PDF export, and share with auto-lock races.
- Startup cleanup of stale plaintext and incomplete encrypted writes.
- Database and envelope migration fixtures.

### Security verification

- Static scan for hard-coded keys, insecure cipher modes, passcode logging, raw Vault file exposure, and backup-rule omissions.
- Dynamic inspection of app-private storage before unlock, while unlocked, after lock, and after export.
- Verify keys/nonces are generated with a cryptographically secure random source.
- Verify GCM nonce uniqueness under concurrency and retries.
- Attempt navigation/authentication bypasses and direct component launches.
- Validate rooted-device and compromised-runtime limitations against the documented threat model.

### UI tests

- Setup, confirmation mismatch, weak passcode, unlock, error, lockout, timer, explicit lock, and reset.
- Empty, populated, selection, search, no-results, Vault Trash, and corruption states.
- Screenshot/app-switcher protection.
- TalkBack descriptions and announcements without metadata leakage while locked.

## 15. Delivery plan

1. **Security foundation:** format definitions, key lifecycle, KDF benchmark, encrypted asset/manifest store, backup exclusions, and cryptographic tests.
2. **Locked repository boundary:** Vault session, locked-state guards, auto-lock triggers, cache cleanup, and transaction journal.
3. **Setup and unlock:** first-time education, passcode setup, unlock, throttling, reset, change-passcode, and secure-window behavior.
4. **Library integration:** Vault entry, unlocked collection, viewer/editor, move in/out, Vault Trash, search, sorting, and selection.
5. **Capture and export:** scan/import directly to Vault, streaming PDF export/share, plaintext cleanup, and lock-race handling.
6. **Hardening:** migration fault injection, performance profiling, accessibility/adaptive-layout audit, security review, and release copy review.

Do not ship a UI-only Vault before the encrypted repository and locked query boundary are complete.

## 16. Product decisions recorded by this draft

- One Vault per local OScan installation.
- A 6–12 digit user-created passcode is mandatory.
- No recovery exists; reset deletes Vault content.
- Immediate auto-lock on background is the first-release default.
- Vault has separate encrypted favorites, search, and Trash.
- Document names, timestamps, page metadata, and thumbnails are treated as sensitive.
- Explicit export/share is allowed after a warning and creates a copy outside Vault protection.
- Biometrics and portable backup are deferred.
- Vault uses a separate encrypted repository rather than placing locked rows in the existing plaintext Room library.

## 17. Open implementation decisions

The following require a short engineering/security spike before implementation:

- Select and audit the Argon2id implementation that supports OScan's minimum API level.
- Establish KDF memory/time parameters across low-, mid-, and high-end supported devices.
- Choose between a versioned encrypted manifest and a Vault-specific encrypted database.
- Decide whether to add a second Android Keystore device-bound wrapping layer and define its key-invalidation behavior.
- Confirm the most reliable secure-window and app-switcher behavior across supported Android/API and OEM versions.
- Define the maximum plaintext-free streaming path supported by the current image decoder and PDF exporter.

