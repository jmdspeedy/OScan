# OScan release and signing guide

GitHub release APKs are signed with a persistent OScan release certificate. The private key must never be committed or attached to a release.

## Signing material

The maintainer's local backup is stored in the ignored `.signing/` directory:

- `oscan-release.jks` — the private signing key
- `credentials.properties` — the keystore alias and passwords

Back up both files together in a secure offline location. Losing either file can prevent future APKs from upgrading installations signed by the current certificate.

GitHub Actions requires these encrypted repository secrets:

- `OSCAN_KEYSTORE_BASE64`
- `OSCAN_KEYSTORE_PASSWORD`
- `OSCAN_KEY_ALIAS`
- `OSCAN_KEY_PASSWORD`

The public certificate SHA-256 fingerprint is recorded in the project README so users can verify release continuity.

## Release process

1. Update `versionCode` and `versionName` in `android-app/build.gradle.kts`.
2. Run the JVM tests, Android lint, and a locally signed release build.
3. Verify all four ABI-specific APKs (`arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`) with Android SDK `apksigner`.
4. Commit and push the exact release candidate.
5. Create and push a `vMAJOR.MINOR.PATCH` tag, such as `v0.8.1`.
6. Confirm the `Build APK` workflow succeeds.
7. Download each release APK and verify its included SHA-256 checksum before announcing the release.

The workflow refuses to build a tagged release when any required signing secret is missing. Pushes and pull requests run tests, lint, a debug build, and packaging checks without access to signing secrets. Tagged builds publish only signed ABI-specific release APKs and their checksums.
