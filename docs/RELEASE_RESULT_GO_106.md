# TeleRC Go 1.0.6 — published signed prerelease

Published 29 October 2026 (Philippine time):
[go-v1.0.6](https://github.com/Denberg28/telerc_go/releases/tag/go-v1.0.6).
[Download production APK](https://github.com/Denberg28/telerc_go/releases/download/go-v1.0.6/TeleRC-Go-v1.0.6.apk).

Test immediately before Phone connect opens the offline rover game/free-drive
preview. Steer/Drive captions share centering, heights and gaps. The Test/Controls
button stays on one line at enlarged text. Opening Test disconnects/cancels live
control; Controls returns without reconnecting or arming.

## Identity and update

Production package `io.github.denberg28.telerc.go`, version 1.0.6/code 1002.
Exact APK/source/tag commit: `d66e4751b050caae7c9b53ef4e9235b153f78aa7`.
Production signer retained from 1.0.5:
`5e57bd9bb3d10d97e5abdad4c51dbf5282708db73f1844fac272403952f9f5ec`.
APK: 1,099,060 bytes; SHA256:
`fd8a6bb54fb514d0c752c6e7db03eff3079e5539934f1e8bdc013fb93c3f41a2`.
Source ZIP SHA256:
`9cb9296917c9fa8b3a0bb75db191c35f46f1ba03b254da1fa89c83e22782c76c`.

Install the production APK over existing production Go; no uninstall or firmware
reflash. The independent Candidate preview has a different package/data and
does not serve as the production upgrade. Actual phone upgrade/settings retention
still need device acceptance. The stable in-app feed skips prereleases, so
download/install this one manually. Retain the previous APK; downgrade may
require uninstall/data loss.

## Final gates and evidence

| Gate | Status | Evidence and limit |
| --- | --- | --- |
| A source/build | PASS — build verified | Go debug/release build/lint and native-test APK; full-app debug build/lint. |
| B behavior | PASS — software tested | 34 Go + 162 full-app JVM tests, zero failures/errors/skips; 18 packaging tests. |
| C regression | PASS — cloud verified | Shared-source checks, normal host regression and default ASan/UBSan/LeakSanitizer. |
| D integration | PASS — simulator tested | API35 gallery regression; Test isolation/socket cancellation, Game/Test/Reset/music/lifecycle, symmetric captions and single-line page action at 1.0/1.3 text in 640x360dp landscape. Final screenshots visually reviewed. |
| E phone/HIL | BLOCKED — not hardware tested | Actual phone install/gallery providers and restrained-wheel acceptance remain pending. |
| F delivery | PASS — signed prerelease | Retained certificate, code 1002 > 1001, package/signature/alignment/ZIP, clean exact-source archive and all 11 hosted assets/digests verified. |

Producers and publication:
- [Final software/native checks40855741152](https://github.com/Denberg28/telerc_go/actions/runs/40855741152), product source above.
- [Retained-signer build40855768830](https://github.com/Denberg28/TeleRC/actions/runs/40855768830), workflow commit `23c82095097fd1945a67ecffe1e99f2cb62e4dcf`.
- [Publication40856345126](https://github.com/Denberg28/telerc_go/actions/runs/40856345126), publication commit `986de55d0ca0caeddae330ec5813ab281f9356e9`.

The initial source passed checks; screenshot review caught a wrapped Controls
caption at enlarged text. The final source widens that action and enforces one
line. It was rebuilt, retested, signed and published; earlier preview binaries
were not substituted for the production APK.

The release contains APK, exact source, test evidence/screenshots, build and
signing provenance, acceptance guide and checksums. Every uploaded draft asset
was downloaded and compared byte for byte before publication. Final public
release/tag/asset metadata was rechecked. No signing key/private configuration
was exported. Stable/field qualification is not claimed.

Earlier build-blocked, local-only and publication-pending notes are superseded
by this result. Next: perform the phone/raised-wheel sequence in
[TEST_GUIDE.md](https://github.com/Denberg28/telerc_go/releases/download/go-v1.0.6/TEST_GUIDE.md).
