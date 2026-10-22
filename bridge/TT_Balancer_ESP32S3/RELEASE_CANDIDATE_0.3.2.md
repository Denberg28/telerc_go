# TeleRC 2WD Balancer v0.3.2 — software release candidate

Target: ESP32-S3 DevKitC, MPU6050/MPU6500, MX1508, two TT gear motors.
Retains GPIO 4/5 left motor; GPIO 6/7 right motor; GPIO 8/9 I2C; GPIO 12 emergency stop (active LOW).
Arduino ESP32 board core 3.x required.

## Corrections
- Source-IP **and** source-UDP-port peer lock; replies target actual source port (works with TeleRC Go's alternate-port socket fallback).
- Nonzero heartbeat identity and `TELERC_OWN` capability/ownership telemetry for TeleRC Go. The firmware is phone Wi-Fi only; it does **not** implement ELRS takeover. Go's capability flag only identifies a compatible standalone controller, not ELRS presence.
- Neutral dwell is required before an ARM command is accepted. Never auto-arm after restart, Wi-Fi restoration, or a new peer.
- Motor output increases to 205/255 (previously 160); short motor bench test 85/255 (previously 55). Lean command ceiling is 2.5 degrees. Persistent saturation faults only while tilt exceeds the arm-angle threshold, retaining fall, stop, sensor, timing and PWM guards.
- Local balance remains active after phone motion times out; motion request is zeroed immediately. A robot which continues balancing through reconnection is correctly reported ARMED. In TeleRC Go, select DISARM, then perform a new neutral ARM sequence; never force ownership or resume stored throttle.

## Not fixed by firmware
PWM is **not** measured motor current or motor supply voltage. Weak TT motors, MX1508 thermal shutdown or a 2S supply exceeding the board/motor rating cannot be made safe by raising PWM. Validate the motor supply under stall and use a correctly rated converter/driver. Do not stall a TT motor for prolonged periods.

## Validation gates
Run `bridge/TT_Balancer_ESP32S3/tests/run.sh`, compile for ESP32-S3 Arduino 3.x, then bench-test with wheels raised:
1. Disconnect motor power before flashing; set real WPA2 password.
2. Verify motor polarity: local Serial `t`, confirm movement direction, then `v` while upright (or deliberately set `BALANCER_DIRECTIONS_VERIFIED=1` only after a verified bench check).
3. Start upright; observe CAL and SAMPLE. TeleRC Go Wi-Fi connects to `TeleRC-Balancer`, UDP `14550`.
4. Center sticks; ARM. Tilt chassis by hand **while suspended**, confirming output moves wheels toward the fall. If backwards, change motor signs or IMU mount, not KP arbitrarily.
5. Test lost packets, phone reconnect, alternate source port and physical STOP. Reconnect must not resume travel. If it remained balancing, Go may show `Armed elsewhere`; request DISARM before starting a new session.
6. Test low supply, power brownout and thermal behavior. No unattended balancing until these pass.

This is a software candidate; **not physically validated, not a stable hardware release**.
