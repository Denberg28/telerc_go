# TeleRC Go 1.0.12 — proportional joystick response

Baseline: `889a211` on main, Go 1.0.11 / code 1007. Work branch:
`fix/go-proportional-joystick`. Acceptance: 25/50/75/100% line travel must
produce the corresponding fraction of the selected usable receiver demand,
within one PWM quantization step, on the first Test simulation frame. Both
axes/directions, 25–100% independent sensitivities, releases and pointer loss
are covered. The fixed ±8% touch neutral guard remains.

## Findings and changes

The previous tests accepted any nonzero movement. They missed magnitude errors
and lag. At 25% sensitivity the previous maximum was 100 µs from neutral;
a 35 µs receiver neutral window leaves only 65/365 = 17.8% usable demand.
Expo, when stored, further softened intermediate positions even though Go has
no expo control. Test then applied a different 25 µs window and an acceleration
blend (22.4% of target on a 16 ms first frame; roughly 144 ms to 90%).

Go now uses a direct linear curve, applies sensitivity to usable demand, and
adds receiver compensation afterward. At 50% travel and 50% sensitivity the
requested receiver demand is 25%, independent of stored legacy expo. Each axis
retains its independent sensitivity. The position light still follows finger
travel along the line. Touches beyond endpoints clamp. The center guard is
neutral; beyond it the line percentage applies directly without travel rescaling.
No motor start boost is invented.

| Sensitivity | Half-line positive | Full-line positive | Full usable demand |
| --- | --- | --- | --- |
| 25% | 1581 µs | 1626 µs | 25% |
| 50% | 1626 µs | 1718 µs | 50% |
| 75% | 1672 µs | 1809 µs | 75% |
| 100% | 1718 µs | 1900 µs | 100% |

Negative commands are symmetric around 1500 µs. Fractions are relative to
neutral-to-endpoint travel. Values assume Go's existing conservative 35 µs
receiver window. Test now decodes that same window and applies direct track
demand on the next simulation frame, including neutral and reversal. It does
not simulate physical motor acceleration. Live RC sends use the latest axes
every 20 ms instead of 50 ms, with a 5 ms receive timeout instead of 20 ms;
no queue or command replay is added. Actual device/network latency is unmeasured.

The full TeleRC app retains its old curve, receiver decoding and smoothing
through default parameters. Shared source copies remain byte-identical. No
firmware changes or reflash. Existing 25 µs mini receivers have a small response
offset compared with the 35 µs conservative Test model; FC deadband, PWM
configuration and physical friction can further change motor onset. No wheel
speed or measured steering-angle feedback is supplied by the position light.

## Acceptance on phone and restrained rover

1. Install the matching-package/signer APK over Go 1.0.11; keep existing app data.
   Start Test. Set both sensitivities to 100%, then 50%, then 25%.
2. Touch 25%, 50%, 75% and the endpoint in each direction on both sticks. The
   marker must follow the finger and Test must respond on its next frame without
   slowly catching up. Half travel must deliver half the selected strength.
3. Reverse and release: Test demand must reverse/neutralize on its next frame.
   Pause, loss of touch and lifecycle changes must neutralize; resume never
   inherits held motion. Run both axes together and let the finger drift sideways.
4. Raise and restrain wheels, connect and explicitly ARM with an independent
   cutoff available. Repeat at low sensitivity, observe actual PWM requests and
   measure motor onset/current. Check release, stale telemetry, disconnect and
   radio takeover. Physical motor slew/reversal guards still apply in firmware.
5. If the receiver sees the new low commands but the motor remains still, measure
   starting PWM under load before tuning firmware; app percentages cannot
   guarantee motor RPM or steering angle.

Rollback: retain 1.0.11 source/APK. Android may reject a lower version code;
prefer rebuilding the old product with a higher code and the retained signer
rather than clearing saves. Candidate uses its own package/signature; it cannot
upgrade the production package and has separate preferences/saves.

## Verification record

- PASS — reviewed and host tested: all 64 Go JVM unit tests, including four new
  magnitude/first-frame response regressions, compiled and ran with Kotlin
  2.1.20/JVM target 17 and JUnit 4.13.2. This invokes the repository's actual
  pure Kotlin sources directly without Gradle; it is not an Android build.
- PASS — host tested: 18 release source/package regression checks, full bridge
  host suite, shared-source and motor-core synchronization, whitespace checks.
- PASS with limitation — full bridge AddressSanitizer/UndefinedBehaviorSanitizer
  suite with leak detection disabled. Default LeakSanitizer cannot operate in
  this sandbox (fatal ptrace/process inspection restriction); leak checking is
  BLOCKED and remains required in CI.
- BLOCKED — Android debug/release builds/lint and native MotionEvent/rendered
  marker/emulator tests. The required Gradle command failed to download its
  distribution. Direct Maven access allowed the JVM check above only.
- BLOCKED — public branch push by automatic approval review: current request
  did not explicitly authorize public source publication. No remote branch,
  PR, workflow run, APK or release was created. The source and checks are
  committed locally, ready for an authorized push and existing CI workflow.
- BLOCKED — physical installation and restrained-wheel HIL: no device or rover
  attached. No stable promotion or hardware reliability claim.

Next concrete check: authorize the public review-branch push, run the prepared
Android/native CI, inspect actual first-frame/rendered marker evidence, then
build and verify a same-signer APK before physical acceptance.
