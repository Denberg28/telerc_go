# TeleRC Go 1.0.5 — gallery wallpaper prerelease

**Settings → Background → Choose image** imports a custom photo from Android's
gallery/document picker. The photo stays saved across app restarts. **Restore
default** brings back the original ship background. Cancel, corrupt files and
images over 20 MiB keep the previous wallpaper. The compact settings and preview
fit short landscape screens and large text.

## Install

Download **TeleRC-Go-v1.0.5.apk**. This is the production package
`io.github.denberg28.telerc.go`, Android version 1.0.5/code 1001. The retained
production signing certificate is verified against the previous 1.0.4/code 1000
APK, so it supports an in-place upgrade while retaining Go settings. Android
still requires its normal installation approval. Actual device installation and
gallery-provider acceptance remain to be checked on the user's phone.

This APK is separate from the debug-signed **TeleRC Go Candidate** preview. Do not
uninstall the working production Go app to install this update. Candidate data
is independent and does not automatically move into production.

## Compatibility and checks

- App-only update; **no firmware reflash is needed**. Retain the firmware and
  wiring already qualified for your rover. The existing
  [v1.0.0 firmware/profile bundle](https://github.com/Denberg28/telerc_go/releases/tag/v1.0.0)
  remains available; this release does not relabel or rebuild its firmware.
- Retains the released 1.0.4 runtime, including deliberate balancer resume,
  neutral qualification and local-balance-preserving pause. Opening settings or
  the gallery stops phone travel and cancels pending ARM; returning never
  automatically enables travel or re-arms.
- 23 Go JVM tests; Android debug/release builds and lint; full-app regression;
  shared-source and bridge/default sanitizer checks; native API 35 wallpaper
  import/cancel/invalid-file/persistence/restore and normal/large-text layout
  checks. The release packager has 18 additional source/archive exclusion tests.
- APK package/version, increasing code, retained signer, alignment, ZIP integrity,
  permission scope, exact source snapshot and SHA-256 are checked before publish.

The release includes the signed APK, exact source archive, verification and
signing provenance, build information, test evidence and **SHA256SUMS.txt**. No
keystore, pairing keys, private configuration or local build inputs are included.

## Limits and rollback

This is a **prerelease**, not a stable or field-validation claim. Actual phone
gallery providers, upgrade/settings retention and restrained-wheel interruption
checks remain pending. Follow the executable phone/raised-wheel acceptance in
`GO_WALLPAPER_105.md`, included as the release guide. No rover safety or timing
performance is established by emulator tests.

The existing stable in-app update feed is unchanged and skips prereleases.
Install this GitHub prerelease manually. Keep the previous signed 1.0.4 APK.
Restoring the bundled background needs only **Restore default**; an Android app
downgrade may require uninstalling and losing app data. Do not erase receiver,
motor or firmware configuration to roll back an app wallpaper change.
