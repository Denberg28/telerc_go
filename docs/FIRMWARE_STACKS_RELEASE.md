Separate build-verified ESP32-S3/MX1508 firmware packages for existing TeleRC Go:

| Download | Layout |
| --- | --- |
| TeleRC-Go-4WD-v1.3.0-Candidate.zip | Four drive motors, differential/pivot steering; GPIO pairs 4/5, 6/7, 8/9, 10/11 |
| TeleRC-Go-SpringCar-v0.2.0-Candidate.zip | Rear drive GPIO4/5, front two-wire steering GPIO8/9; timed estimated movement, brake, return and saved recalibration |

Both include source, tests, four separate flash images, installation guide,
manifest and SHA-256 checksums. Existing app remains unchanged. Both expect a
regulated 5 V motor rail from the 2S battery. Use the archive matching your wiring.

SpringCar: hold steering without a timed disarm; release for calibrated return.
Disarm, hold BOOT two seconds and release to reset the assumed center. USB serial
`CAL leftOut rightOut leftBack rightBack` saves travel/return times. Two-wire
steering is an estimate: braking cannot guarantee a static angle against the spring.

Publication is gated by host/full sanitizers, archive regressions, Android
regressions/lint/build and two actual pinned Arduino-ESP32 3.3.2 target builds.
Physical motor/current/temperature, brake drift and spring return remain untested.
This is a hardware-unvalidated prerelease, not a stable/field qualification.
