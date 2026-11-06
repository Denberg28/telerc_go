# TeleRC Go 1.0.12 — published stable release

[Latest stable release](https://github.com/Denberg28/telerc_go/releases/tag/go-v1.0.12)
· [Signed APK](https://github.com/Denberg28/telerc_go/releases/download/go-v1.0.12/TeleRC-Go-v1.0.12.apk)
· [Merged fix PR 17](https://github.com/Denberg28/telerc_go/pull/17).

The release is public, not a draft/prerelease, and is GitHub's Latest release.
Stable channel was explicitly requested by the owner on 6 November 2026.
This channel decision does not establish physical phone, motor or field qualification.

Source/tag: `93d9a0cbcab27a86e662f842e85cd0096de1f907`; merged into main by
`7e7f47981aa89f606febb24be951306da012f99e`. Production package
`io.github.denberg28.telerc.go`, version 1.0.12/code 1008, retained signer SHA-256
`5e57bd9bb3d10d97e5abdad4c51dbf5282708db73f1844fac272403952f9f5ec`.
Install over existing production Go without uninstalling. The existing legacy
in-app update feed is unchanged; use the direct APK download for this release.

## Behavior and matching firmware

Go line travel now controls proportional usable receiver demand after independent
Steer/Drive sensitivity. Hidden legacy expo cannot soften this scale. Test uses
the matched neutral window and applies track demand on its next simulation frame,
including reversal and release. Live latest-value RC cadence is 20 ms. Existing
center neutral, explicit ARM, authority and downstream failsafes remain.

Both firmware archives are rebuilt from the same exact source. Choose the layout
matching the wiring; both include Arduino IDE sketches, four actual ESP32-S3
images, flash helper, wiring, tests, source manifest and checksums. No firmware
reflash is required solely for this app fix. Regulated 5 V motor rail from 2S.

| Asset | Purpose | SHA-256 |
| --- | --- | --- |
| TeleRC-Go-v1.0.12.apk | Production signed Android app | `ee744867319e88973c383325f3c77295eff12ca40db9de2379432a220c85d0cd` |
| TeleRC-Go-4WD-v1.3.0-Candidate.zip | Four drive motors, differential steering | `be0b38527159f1f48094cfbe3b619499075f0f6bfddde82c0857aa5ee034772c` |
| TeleRC-Go-SpringCar-v0.2.0-Candidate.zip | Rear drive + spring-return front steering | `72cf29f4ffb0373c4e7ae0aff7f6e85322164a0524a6cd89c2f23718f5efe631` |

Firmware Candidate filenames and `hardware_validated=false` are retained.
SpringCar steering is an estimate without position feedback; electrical braking
cannot guarantee a fixed angle against the spring. Use saved travel calibration
and verify the physical mechanism before normal operation.

## Final verification

| Gate | Result | Evidence |
| --- | --- | --- |
| A source/build | PASS — build verified | Both-app debug/release builds and lint, exact source archive, increasing APK code and retained signer. |
| B behavior | PASS — JVM tested | 68 Gradle-discovered Go tests, including four new proportional/first-frame tests; zero failures/errors/skips. Earlier direct local compiler run exercised 64 tests. |
| C regression | PASS — cloud verified | Full-app regression, legacy/4WD/SpringCar host suites, default ASan/UBSan/LeakSanitizer, shared/motor source parity and archive regressions. |
| D integration | PASS — emulator tested | API35 real MotionEvent, marker pixels, line fractions, directions, gains, first-frame magnitude, pointer loss, maze/gallery and normal/large-text UI. Actual device integration remains pending. |
| E physical/field | BLOCKED — NOT TESTED | No actual phone upgrade, loaded motor onset/current/temperature, stop latency, spring drift/return or field endurance measurements. |
| F delivery | PASS — owner-selected stable channel | All 17 assets published with source/signing provenance, SHA inventory, evidence and guides. Not a physical readiness claim. |

Producers:
- [Android/native checks 42210956522](https://github.com/Denberg28/telerc_go/actions/runs/42210956522).
- [Retained signing 42211210001](https://github.com/Denberg28/TeleRC/actions/runs/42211210001), signing workflow `668636eca05a5924353c41a7dc3251ea3a0b479f`; key not exported/replaced.
- [Actual firmware builds 42211093815](https://github.com/Denberg28/telerc_go/actions/runs/42211093815), successful firmware job `127869624344`; its earlier publication job rejected the superseded signer run, so only its verified firmware artifact is reused.
- [Final publication 42211497881](https://github.com/Denberg28/telerc_go/actions/runs/42211497881), workflow `326f442b993bfabab4435e10d4ad135c764cb526`.

Native endpoint assertions allow the documented one PWM quantization unit for
pixel-to-axis Float rounding at half-unit ties; proportional first-frame magnitude
and marker checks remain enforced. Earlier 64-test signing expectations were
corrected to all 68 Gradle-discovered tests. Superseded failed producer runs did
not publish a release. Product joystick behavior was unchanged by these harness fixes.

Next concrete check: install over existing Go and verify saved settings/wallpaper/
progress. Then restrain and raise wheels, explicitly ARM at low sensitivity and
execute release, disconnect, stale-link, takeover and physical motor-onset checks
in TEST_GUIDE.md/FIRMWARE_STACKS.md. Retain prior APK/source and layout-matching
firmware for rollback; Android may require a higher-code rebuild to preserve data
when reverting. No whole-flash erase or automatic re-arm is requested.
