# TeleRC Go 1.0.7 — published latest maze Test

Published 29 October 2026 (Philippine time):
[GitHub prerelease](https://github.com/Denberg28/telerc_go/releases/tag/go-v1.0.7) ·
[Download signed APK](https://github.com/Denberg28/telerc_go/releases/download/go-v1.0.7/TeleRC-Go-v1.0.7.apk).

This corrects Go1.0.6's older arcade road game. Test now matches the maze from
released TeleRC v1.1.0rc1 and the owner's screenshot: 20 themed levels, Market
vehicles/accessories, bounded coins/replay rewards, three-bump restart, best/clear
times and saved Go progress. Test entry remains before Phone connect; the maze
has the original three-column layout and symmetric Steer/Drive captions.

## Identity and installation

Production package `io.github.denberg28.telerc.go`, version 1.0.7/code 1003.
Exact product/source/tag commit: `a177c599d002dc3db8c3f232dcac457738b773a2`.
Maze model/view/renderer blobs were verified against released TeleRC product
`03b5b69ff39487678d25a8023c5438a773ca3d51`.

Production certificate retained from 1.0.6:
`5e57bd9bb3d10d97e5abdad4c51dbf5282708db73f1844fac272403952f9f5ec`.
APK: 1,111,096 bytes; SHA256:
`023277808ce63cfe985c204ae2fcb609af750c400817128278b1ba7824fb1723`.
Source ZIP SHA256:
`4f149b61da573157e6ea31d9970f4cbffadaeeb89f2cc2d6c3f0ed46781e64a5`.

Install over the existing production Go without uninstalling. No firmware
reflash. Actual phone upgrade and settings retention remain device acceptance
items. The separate full TeleRC app has separate Android data; its level/coins
are not copied automatically. Go starts at its saved maze level or level 1.
The screenshot's level 12/14-coin state is an emulator fixture only.
The stable in-app feed skips prereleases; download/install this one manually.
Retain 1.0.6; an Android downgrade may require uninstall/data loss.

## Final gates

| Gate | Status | Evidence / limit |
| --- | --- | --- |
| A source/build | PASS — build verified | Both-app debug regression/build/lint; Go debug/release lint/build and native-test APK. |
| B behavior | PASS — software tested | 57 Go + 162 full-app JVM tests, zero failures/errors/skips; 18 packaging checks. |
| C regression | PASS — cloud verified | Shared-core parity, normal host checks and default ASan/UBSan/LeakSanitizer. |
| D integration | PASS — simulator tested | Gallery regression; screenshot-like level 12/14-coin maze at 1.0/1.3 text; actual joystick dispatch, pause, Market buy/remove/equip, real-frame clear/records/unlock, Levels, mandatory restart, lifecycle, restart persistence and no live transmissions. Screenshots visually reviewed. |
| E phone/HIL | BLOCKED — not hardware tested | Actual phone install/gallery providers and restrained-wheel acceptance remain pending. |
| F delivery | PASS — signed prerelease | Retained signer, code 1003 > 1002, correct maze classes and absent old TestDriveView, permissions/alignment/ZIP, clean exact source, all 11 hosted asset bytes/digests verified. |

Sources and producers:
- [PR10](https://github.com/Denberg28/telerc_go/pull/10).
- [Software/native checks40860059065](https://github.com/Denberg28/telerc_go/actions/runs/40860059065).
- [Retained signing40860127856](https://github.com/Denberg28/TeleRC/actions/runs/40860127856), workflow commit `fe3e4743bd5aa1f972ee4067f451e48846392b18`.
- [Verified publication40860749514](https://github.com/Denberg28/telerc_go/actions/runs/40860749514), publication commit `e8663202d644f7cec6abcbe17652c2023d78cda2`.

Publication verified every draft asset by downloading and comparing its bytes,
then rechecked the public release/tag and digests. Release contains signed APK,
source, test evidence/screenshots, build/signing provenance, acceptance guide and
checksums. No private signing input was exported. Stable/field qualification is
not claimed. Pending software statuses in the scope notes are superseded.

Next: follow [TEST_GUIDE.md](https://github.com/Denberg28/telerc_go/releases/download/go-v1.0.7/TEST_GUIDE.md)
on the actual phone with restrained wheels. Test entry disconnects/cancels live
authority; leaving Test never reconnects or arms. Practice dialogs/backgrounding
pause and neutralize simulation. Balancer local torque may persist; do not infer
motor power-off from Test entry.
