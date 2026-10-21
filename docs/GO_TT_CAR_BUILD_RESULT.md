# TT car 0.1.0 — build result, 22 October 2026

Product source: `2aeb00988bd08a239ad64694ed7390232ea50962`.
Repository review: https://github.com/Denberg28/telerc_go/pull/1 .
ESP32-S3 / MX1508 / rear axle drive + bare TT front steering.
TeleRC Go app 1.0.3 remains compatible; no app rebuild or firmware stable promotion.

| Gate | Result and evidence |
| --- | --- |
| A — actual target build | PASS — Arduino CLI 1.5.1, Espressif core 3.3.2; car WiFi and WiFi-ELRS compile for ESP32-S3, CDC, hardware USB, 4MB/default/DIO/no PSRAM. |
| B — behavior | PASS — host simulated car tests on core 2/3 × Wi-Fi/ELRS: independent rear duty/front jog, 50 ms expiry, held/reversed stick lockout, 200 ms neutral rearm, rollover, source/loss/stop/PWM faults. |
| C — regression | PASS — normal legacy bridge and Go Wi-Fi/RF/BTS host suites; both shared-source checks; actual original MX1508/BTS7960 × Wi-Fi/ELRS builds. Default LeakSanitizer BLOCKED by local /proc inspection; supplementary ASan/UBSan passes with leak inspection disabled, explicitly not a default LeakSanitizer pass. |
| D — integration | PASS — host simulated actual MAVLink receive/arm/output and CRSF source arbitration. Native phone/car integration NOT TESTED. |
| E — HIL | BLOCKED — no actual phone/car/receiver/driver/power/steering-travel/stop-time measurements. Follow `GO_TT_CAR.md`. |
| F — delivery | PASS — two actual image sets, portable sketch, build/source manifest, source-drift refusal, CRC and SHA256 verification. Development package; no stable release. |

The source ZIP/firmware ZIP record the product commit above. Firmware package
contains both merged flash images, individual images and local compilation/host
logs. WiFi application image: 892864 bytes; WiFi-ELRS: 903072 bytes. Each merged
image is 4194304 bytes for the declared 4 MB layout. Flash merged images at 0x0.

Hosted CI failed before any job steps started; step lists were empty and logs
unavailable (initial car run 39667255014). This is not claimed as successful CI.
Local actual-target compilation supplies the build evidence. Android sources
are unchanged; their fresh Gradle checks were network-blocked locally and the
hosted check could not start. The existing app 1.0.3 qualification is retained.

Wheel centering is manual. 50 ms is a software jog limit, not measured safe travel
or stall protection. The next check is linkage-disconnected direction/pulse
testing, then restrained-wheel loss/stop/source tests and measured travel/temperature.
Rollback requires a known matching car image/configuration; the four-wheel
skid-steer image is not suitable for this wiring.
