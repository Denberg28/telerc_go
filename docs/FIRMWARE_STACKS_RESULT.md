# Published Go motor firmware stacks 0.3.0-rc1

[GitHub prerelease](https://github.com/Denberg28/telerc_go/releases/tag/go-firmware-stacks-v0.3.0-rc1).
Source `d5975e17dc1e59c8d4ac13d0fbf92f5b778055af`, merged by
[PR 16](https://github.com/Denberg28/telerc_go/pull/16).
Published by [gated build 42130103024](https://github.com/Denberg28/telerc_go/actions/runs/42130103024).

| Gate | Final result |
| --- | --- |
| A source/build | PASS — both actual ESP32-S3 layouts compiled with Arduino CLI 1.3.1 and Arduino-ESP32 3.3.2. Both-app debug/release compilation/lint passed; full-app release is deliberately unsigned only in this firmware CI. Product app/signing sources are unchanged. |
| B behavior | PASS — 4WD mixing/pivots/all motors and SpringCar estimated travel/braking/return/calibration host checks. |
| C regression | PASS — legacy/new host suites with full default ASan/UBSan/LeakSanitizer in GitHub; source parity and package regressions. This supersedes the local sandbox leak-check restriction for released source. |
| D integration | PASS — host ownership/nonce/CRC/source-port/ARM/fault checks and unchanged native Go UI/gallery/joystick regression. Actual phone-to-driver hardware integration remains pending. |
| E hardware/field | BLOCKED — physical current, voltage dips, stopping latency, steering drift/return/temperature and endurance remain unmeasured. |
| F delivery | PASS — two distinct published ZIPs and checksum file. Downloaded bytes match both GitHub asset digests and SHA256SUMS; archive CRC, source hashes, layout manifests and flash addresses verified. |

| Asset | SHA-256 |
| --- | --- |
| TeleRC-Go-4WD-v1.3.0-Candidate.zip | `6b338d53750ad047577c5adb68ac5809dd5dab87a3b6027630f45c2342813379` |
| TeleRC-Go-SpringCar-v0.2.0-Candidate.zip | `35fcaee4c0cdf2798ba21667e5db36441bbf88c2dbc75dc801c2e31ae02759c2` |
| SHA256SUMS.txt | `35bd969e7f0fc4d18f6dec914e7ff65f355ba47a57d7f54c8a66698706722d4e` |

4WD: GPIO pairs LF4/5, LR6/7, RF8/9, RR10/11. SpringCar: rear drive4/5,
front steering8/9, saved four-time recalibration and BOOT center reference.
Both expect regulated 5 V motor power from the 2S pack. Existing TeleRC Go
remains unchanged. Each ZIP includes source/tests, four separate images, manifest,
flashing helper and wiring/acceptance instructions. No merged whole-flash image.

SpringCar estimates position without feedback; its electrical brake can be
back-driven by the spring. Holding a turn does not disarm, but a fixed physical
angle is not guaranteed. Recalibrate and verify the mechanism before normal use.

Next concrete check: disconnect motor leads and flash the matching layout, verify
LOW driver inputs on boot/reset and the 5 V motor rail, then execute the raised-
wheel steps in [FIRMWARE_STACKS.md](FIRMWARE_STACKS.md). Retain known-good firmware
and layout-matching wiring for rollback. This prerelease is not field-qualified.
