# TeleRC Go 1.0.6 — offline Test and control captions

Baseline: `999a4800d5f8783e34334aa591bdcdd95fdb05b3`, released Go 1.0.5/code1001.
Go 1.0.6/code1002 preserves the production package and signing configuration.
Only Go UI/game sources, native tests, candidate packaging and these notes change.
No firmware/protocol change or board compilation is applicable.

The Test action immediately before Phone connect opens TeleRC's arcade rover
course, with Game/Test-drive switching, Reset and music. CH1 steers and CH2 drives
in both the local game and the free-drive preview. The three small game classes
come from the full-app Test drive implementation (also present in the current
TeleRC main reference `67b2cf5137989c1a6a65a4b02af960105d04afa3`); the Go copy
corrects the old preview's CH3 caption to CH2. No map/GPS feature is required.
High score uses the private `game` preferences. Controls returns to the ordinary
rover screen without reconnecting or arming. Android Back has the same behavior.

Entry disconnects/cancels live authority before changing mode. Phone connect is
disabled in Test, and game inputs target TestDriveView only. Backgrounding,
closing or opening settings stops animation/audio and neutralizes inputs. Resume
never restores held joystick input or music. Test is an approximate simulation.

Steer and Drive share equal width/padding, fixed 28dp single-line captions,
center alignment, 8dp caption-to-stick spacing, 12dp header/body gap and equal
bottom spacing. Native checks inspect actual rendered bounds at 1.0/1.3 text on
a 640x360dp landscape, plus game switching, socket cancellation and lifecycle.

| Gate | Current status | Evidence / next check |
| --- | --- | --- |
| A source/build | PASS — build verified locally | Java17/SDK35; both-app tests/debug build and lint; Go release build/lint and UI-test APK compilation. |
| B behavior | PASS unit tests; native NOT TESTED | 34 Go + 162 full-app JVM tests, zero failures/errors/skips. Native input/lifecycle and geometry checks await emulator. |
| C regression | PASS locally with limits | Shared-core checks, normal host suite and 18 archive tests pass. ASan/UBSan passes with local leak check disabled; default LeakSanitizer is blocked by /proc/ptrace. CI retains default sanitizers. |
| D integration | NOT TESTED | Native checks compile; execution requires an emulator/device. No local KVM/device is attached. |
| E phone/HIL | BLOCKED | No attached phone/rover; follow acceptance below. |
| F delivery | PASS — local Candidate packaged | Separate debug-signed candidate identity/code1002, APK signature/ZIP/alignment verified. Production signing and GitHub publication remain pending. |

Build artifacts and workflow results supersede pending software evidence only.
A debug Candidate is a separate app with separate data, not a production upgrade.

## Phone / restrained-wheel acceptance

1. Retain the previous production APK. Install a same-signer production update
   over it and confirm saved wallpaper/connection/sensitivity/high score.
2. With wheels raised and an independent power cutoff, connect and begin low
   speed travel or pending ARM, then press Test. Confirm neutral/release and
   canceled arming, with the profile's existing stop behavior. A balancer may
   retain local balance torque; this action does not prove motor power is off.
3. Move both game sticks, switch Test drive/Game, reset and toggle music. Confirm
   no live rover motion or reconnect throughout. Return via Controls/Back; manual
   connection and ARM must be required.
4. Open Settings, background/resume and close/reopen while holding game sticks
   and playing music. No held input/audio may resume. Check normal and enlarged
   text; labels and all header actions must fit symmetrically.

Rollback: use Controls to leave Test. An APK downgrade may require uninstall and
lose app data; retain the existing production installation when using Candidate.
Next: complete cloud checks, inspect captured game/control screens, then perform
actual phone installation and the raised-wheel acceptance above.

## Local handoff — 29 October 2026

Implementation is committed locally on `feat/go-test-and-control-spacing`.
Automatic approval review rejected the attempted GitHub push: it classified this
turn as implementation/testing authorization without publication authorization.
No remote branch/PR/build/release is claimed. Do not bypass that rejection through
another write path. The source package preserves the completed work; after the
owner authorizes publication, push this branch and run the existing PR checks.

Local command results: `python scripts/sync-go-core.py --check`,
`python bridge/sync_motor_core.py --check`, `bridge/tests/run.sh`,
`python3 -m unittest discover -s scripts -p 'test_*release*.py' -v` all PASS.
`SANITIZE=1 bridge/tests/run.sh` is BLOCKED by LeakSanitizer /proc access;
`ASAN_OPTIONS=detect_leaks=0 SANITIZE=1 bridge/tests/run.sh` PASS.
The required combined Android command was attempted and BLOCKED while downloading
Gradle (`java.net.SocketException: Network is unreachable`); no Android test,
lint, APK, native geometry or visual verification result is claimed.

## Completed local build — supersedes initial build-blocked notes

A proxy-aware toolchain resolved the initial download/JRE/certificate blockers.
The pinned Gradle8.11.1 distribution checksum was verified; Java17 JDK and
Android SDK35/Build Tools35.0.0 were installed in temporary tooling only.
All required Android tasks passed on source `da8fb1b`:
`:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`,
`:go:testDebugUnitTest :go:lintDebug :go:lintRelease :go:assembleDebug
:go:assembleRelease :go:assembleDebugAndroidTest`.
Go34 and full-app162 JVM tests passed with zero failure/error/skip.
Lint completed without errors (existing/ordinary warnings remain).

A subsequent `:go:assembleDebug -PcandidateInstall=true` produces the independently
installable preview `io.github.denberg28.telerc.go.candidate`, version1.0.6/code1002.
APK signature verification, 16KiB ZIP alignment and ZIP integrity pass.
SHA256 `76b0aabf1b26d09cfad8ea03cc13fce2b8447d0acd00c266e953b452a490fc8e`.
Install Candidate alongside the current production Go; it has separate settings
and data. The production release variant was built unsigned, is not an upgrade
artifact, and is deliberately not delivered. No retained private signing input
was available or exported. Native/device/HIL checks and default leak checking
remain blocked as stated above; the native test APK compiles but was not run.

The source package includes the successful build log, candidate build log, JVM
XML results and lint XML reports under `verification/`. Public upload remains
blocked by automatic approval review pending explicit owner authorization.
