# TeleRC balancer SBRB-inspired controller (v0.3.1 development)

Upstream reference: [jaguilar/sbrb](https://github.com/jaguilar/sbrb), James Aguilar (2026), MIT License. This is an independent adaptation of its derivative-on-measurement and conditional anti-windup concepts; no source files were copied.

Changes: isolated pitch controller; first-sample initialized gyro-rate low-pass (12 ms time constant); explicit anti-windup; reset of filter and integrator on disarm, fault and successful arm. Existing 250 Hz timing, 160/255 bench PWM cap, 300 ms sustained saturation fault, MPU calibration, direction checks, STOP, Wi-Fi protocol and 350 ms travel watchdog remain unchanged.

Not adopted: wheel-speed PID, yaw-rate feedback, motor encoder feedback, battery-voltage compensation and 500 Hz task timing. SBRB's N20 motor encoder hardware, TB6612FNG and battery ADC do not match the TeleRC TT motor / MX1508 platform. Do not infer motor speed from PWM.

Validation: this commit is a source-only development candidate. Run `bridge/TT_Balancer_ESP32S3/tests/run.sh` and `bridge/TT_Balancer_ESP32S3/tests/run_sbrb_tests.sh`; perform actual Arduino-ESP32 3.x target compile and supported-wheel hardware-in-the-loop checks before any stable release. New filtering may change pitch response; tune gains from conservative values and confirm motor direction before arming. Keep an emergency motor-power cutoff available.

Rollback: use the unchanged `main` branch or the previous firmware release; do not flash this development branch without bench validation.
