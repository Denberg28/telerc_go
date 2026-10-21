# TeleRC Go repository migration — 22 October 2026

Destination: https://github.com/Denberg28/telerc_go

Latest app baseline: TeleRC Go **1.0.3**, version code **4**,
package `io.github.denberg28.telerc.go`.

Imported source snapshot: `Denberg28/TeleRC` commit
`6694e6aab5857948ba796e4c1c943df647f82abb`.
Published product source: `e209341c47273e8fccce90dbd8d8daafdcf7a390`.
The subsequent snapshot includes the release qualification record.

The Android product sources, package/version, resources, Gradle wrapper and
matching standalone four-wheel firmware are preserved byte for byte. The `app/`
module and shared bridge/test utilities remain because Go reuses resources and
the shared-source checks compare both modules. Build Go with `:go` tasks.

Historical workflows are retained under `docs/legacy-workflows/` as reference.
The active workflow checks both modules and firmware host regressions; it does
not publish or replace a release. Existing signing keys remain external. The
new repository needs the existing signing identity configured before any future
signed release; credentials and keystores are excluded.

The existing APK and release assets remain at
https://github.com/Denberg28/TeleRC/releases/tag/go-v1.0.3 .
The installed 1.0.3 updater still reads that repository. This migration preserves
that working feed; moving it requires a future app update and an accessible
release feed. No release, visibility change, APK rebuild or hardware change is
part of this migration. The original repository is retained for compatibility.

Verification: imported file hashes and executable modes are compared with the
source snapshot. Shared-core checks and firmware host regressions are rerun.
Android CI supplies fresh build/test/lint results in this repository. Original
qualification remains recorded in `docs/TESTING.md` and release BUILD_INFO.
Physical phone/rover HIL remains pending as recorded in the source baseline.

Rollback: retain the existing 1.0.3 APK and firmware. Repository migration alone
does not alter installed apps, preferences, receiver bindings or rover firmware.

Next check: inspect the new repository's Android CI result; configure its retained
release signing identity before a future signed build.

Migration verification results: source/permission comparisons and both shared-core
checks PASS. Normal bridge and Go firmware host regressions PASS. Default
LeakSanitizer runs are BLOCKED in this local runtime because /proc task inspection
is denied; the new GitHub runner reruns these checks without relaxing sanitizers.
Fresh Android build/lint verification is delegated to the imported CI workflow.

Follow-up on 22 October: hosted checks failed before any steps started; no
successful new-repository Android CI is claimed. The TT-car work records actual
local ESP32-S3 build evidence in `GO_TT_CAR_BUILD_RESULT.md`.
