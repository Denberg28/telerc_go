# Published Go 1.0.5 prerelease

[Final signed release result](RELEASE_RESULT_GO_105.md): production APK, same
signer, code 1001, 23 Go tests, 18 packaging tests and all 11 release assets
verified. [GitHub release](https://github.com/Denberg28/telerc_go/releases/tag/go-v1.0.5).
No firmware reflash. Actual phone/gallery-provider and restrained-wheel acceptance
remain pending. This result supersedes earlier production-signing-pending and
Candidate-only statements retained below as review history.

# TeleRC Go 1.0.5 — custom gallery wallpaper

Baseline: `Denberg28/telerc_go` main commit
`409c5154c9a588e35c71c1f9a726c2f2779c05ee`, Go 1.0.3/code 4.
Candidate: 1.0.5/code 1001, same `io.github.denberg28.telerc.go` application ID.
Released baseline: ecosystem v1.0.0 contains Android 1.0.4/code 1000;
product source `36323abbebfa0d02b069b4f23cb502607f65e7c0`, retained-signer
publication source `b6b0138191ac3d29e1702982e6006796142718ff`. Main is behind
that published product. The already released GoControl/GoElrs/GoLink/GoStopPolicy
and their tests are carried forward byte for byte, including explicit balancer
resume/neutral qualification and torque-preserving pause. The two corresponding
GoActivity status/drive-ready checks are retained. Version code 1001 upgrades
code 1000; fixture code 1002 is test-only. Production package and external signing
identity remain. No new firmware, protocol or updater-feed change is introduced.

## Use and acceptance

Settings → Background → Choose image opens the Android image document/gallery
picker. Pick a photo to apply it behind the controls. Cancel retains the previous
image; Restore default returns to the bundled ship image. Photos remain offline
in private app storage after closing/restarting the app or deleting the original.
No broad storage/gallery permission is added. Android controls the available
picker providers, including cloud-backed albums when installed.

The full TeleRC importer and bounds policy are byte-synchronized into Go:
20 MiB input limit, 100 MP/dimension validation, 1920-pixel maximum decoded edge,
EXIF orientation, atomic JPEG replacement. Decode/file work stays off the UI
thread and is serialized across Activity replacement. Failed imports retain the
last file and rendered bitmap. Settings use a two-column compact layout so the
new Background entry fits short landscape phones. Background has a preview,
Choose image, Restore default and Done.

Opening Settings/Background/the picker revokes phone drive, resets axes and
cancels queued ARM through the existing stop/lifecycle paths. Import completion
changes only the image view and never enables or arms. ARM/sticks stay disabled
while loading/importing. The bundled photo and transparent controls are retained.

## Verification gates

| Gate | Status | Evidence / limit |
| --- | --- | --- |
| A — source/build | PASS — build verified | Go debug/release builds, debug/release lint, full-app debug build/lint/tests; cloud runs 40753021733 and 40753202767. Release build is unsigned when the retained key is absent. |
| B — behavior | PASS — software tested | 23 Go JVM tests, zero failures/errors/skips; shared importer bounds and retained released stop/resume regressions. |
| C — regression | PASS — cloud verified | Both shared-source checks, normal bridge host suite and default ASan/UBSan/LeakSanitizer checks. Local /proc limitation remains local only. |
| D — integration | PASS — native simulator tested | API 35 gallery-result interception, cancellation, photo render, corrupt/oversize rollback, rebuild/restart persistence, restore-default persistence and fitted normal/1.3 text on short landscape sheets. Final screenshots visually reviewed; no AndroidRuntime exception. |
| E — phone/HIL | BLOCKED — not hardware tested | No attached phone/rover. Actual gallery providers, retained-signer upgrade and restrained-wheel interruption/no-auto-rearm checks remain. |
| F — delivery | PASS — preview packaged; production signing pending | Independent Candidate package/signature/version/alignment/ZIP/checksum verified. Production APK remains unsigned without the retained signing environment; no production release/stable promotion. |

The baseline independently deleted `firmware/go` (including its tests). CI no
longer invokes those nonexistent paths; bridge checks and all Android tests stay
required. This app-only change does not restore or qualify deleted firmware.

## Phone acceptance, installation and rollback

1. On an actual phone, install a retained-signer 1.0.5 APK over 1.0.4 and verify
   connection preferences and sensitivity remain. A CI debug APK has a different
   signer and cannot serve as that upgrade.
2. Choose a landscape image, portrait EXIF photo, local gallery photo and any
   used cloud-provider photo. Check center crop, labels, both sticks and status.
3. Cancel the picker; try corrupt and >20 MiB files. The previous image must stay.
   Restart the app, rotate/change text size, then delete the original gallery photo
   and relaunch; the private imported copy must remain.
4. Restore default, relaunch and confirm the bundled ship remains. Repeat in a
   short landscape display and at 1.3 text size; all actions must remain visible.
5. With raised wheels and a reachable independent power cutoff, begin arming or
   low-speed phone drive, then open Settings/Background/the picker. Confirm the
   existing profile-specific stop/pause behavior, centered axes and cancelled pending ARM. Return,
   cancel, import, background or recreate the Activity; nothing may auto-rearm.
   Verify receiver-selected operation remains passive as before.

No rover firmware reflash is needed. Restore default rolls back wallpaper only.
For an app rollback retain the signed 1.0.4 APK; Android downgrade may require
uninstall/data loss. Do not erase receiver or motor configuration.

## Final software evidence and preview

Verified source head: `c58e30e60c7a8d9819e3abaf0c25a1dacf2b9338`.
Both jobs PASS: https://github.com/Denberg28/telerc_go/actions/runs/40753202767 .
Final screenshots show all settings and Background actions clear and unclipped at
normal text and 1.3 text on the 640 × 360 dp landscape test. The API 35 first-boot
color-overlay relaunch is allowed to settle before the UI harness retains an
Activity; synthetic gallery files come from a separate plain-Java test provider.
The test APK/provider is excluded from production and preview APKs.

Delivered preview: `TeleRC-Go-v1.0.5-Candidate.apk`, 1,321,207 bytes,
`io.github.denberg28.telerc.go.candidate`, version 1.0.5/code 1001.
Producer: https://github.com/Denberg28/telerc_go/actions/runs/40753021733 .
That run verified APK signature, alignment, exact package/version and ZIP CRCs.
Producer checkout: `789e50cd07181485dee3588c332cd7d37653c851` (PR merge checkout
for head `2c7dcb84d485c1527d4f9d1af97e6487181b22d5`). The final head changes only
instrumentation toast timing; production main sources and build configuration
match the preview producer byte for byte.

SHA-256:
`457686d4a351eb0c032cad4e10f9d92a5f1eba2d464a99aaa83eb98a30c8dd8c`.

The Candidate installs alongside production and has separate settings. It is a
debug-signed preview, not an in-place production upgrade. Keep the existing Go
installation/data. A production upgrade still requires the external retained
signing environment, actual phone installation checks and a separate release.

Review source: https://github.com/Denberg28/telerc_go/pull/5 .
The evidence-only documentation commit following this verified head changes no
application, test or workflow code.

Next check: use the Candidate on the actual phone to import a local gallery photo,
relaunch, restore default and exercise the raised-wheel sequence above. Build the
production APK with the retained signer before attempting an in-place upgrade.
