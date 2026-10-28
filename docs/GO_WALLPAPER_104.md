# TeleRC Go 1.0.4 — custom gallery wallpaper

Baseline: `Denberg28/telerc_go` main commit
`409c5154c9a588e35c71c1f9a726c2f2779c05ee`, Go 1.0.3/code 4.
Candidate: 1.0.4/code 5, same `io.github.denberg28.telerc.go` application ID.
No firmware, protocol, connection, authority, joystick mapping or updater-feed change.

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
| A — source/build | Reviewed; cloud pending | Shared-source checks PASS; local wrapper blocked by network and missing SDK. Debug/release builds and lint requested in CI. |
| B — behavior | Cloud pending | Go policy unit tests; native synthetic gallery picker, cancellation, valid/corrupt/oversize import and rollback. |
| C — regression | Host PASS; cloud pending | Normal bridge host suite PASS. Local LeakSanitizer blocked by denied /proc task inspection; default sanitizers remain required in CI. |
| D — integration | Cloud pending | Native wallpaper rendering, restart/rebuild persistence, restore-default persistence, normal/1.3 text on short landscape sheets. |
| E — phone/HIL | BLOCKED — not hardware tested | No attached phone/rover. Actual gallery providers, upgrade/signing, interruption and restrained-wheel no-auto-rearm checks remain. |
| F — delivery | Review source pending | Candidate code 5; retained signer is external. No release/stable promotion or installable retained-signer APK claimed. |

The baseline independently deleted `firmware/go` (including its tests). CI no
longer invokes those nonexistent paths; bridge checks and all Android tests stay
required. This app-only change does not restore or qualify deleted firmware.

## Phone acceptance, installation and rollback

1. On an actual phone, install a retained-signer 1.0.4 APK over 1.0.3 and verify
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
   existing stop/disarm behavior, centered axes and cancelled pending ARM. Return,
   cancel, import, background or recreate the Activity; nothing may auto-rearm.
   Verify receiver-selected operation remains passive as before.

No rover firmware reflash is needed. Restore default rolls back wallpaper only.
For an app rollback retain the signed 1.0.3 APK; Android downgrade may require
uninstall/data loss. Do not erase receiver or motor configuration.

Next check: inspect both final CI jobs, visually inspect their screenshots and
record their commit/run IDs here before presenting this candidate as build verified.
