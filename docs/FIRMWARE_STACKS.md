# TeleRC Go firmware stacks — MX1508 / ESP32-S3

Choose the archive that matches the physical layout. Existing TeleRC Go remains
unchanged. These are standalone Wi-Fi motor controllers; no flight controller,
LoRa gateway or receiver is required. Hardware/field qualification is pending.

| Stack | Motors and behavior | ESP32-S3 GPIO input pairs |
| --- | --- | --- |
| 4WD 1.3.0 | Four drive motors; left/right differential mixing and pivot steering | LF 4/5, LR 6/7, RF 8/9, RR 10/11 |
| SpringCar 0.2.0 | Rear drive + front two-wire steering with centering spring; estimated travel, brake and return | Rear drive 4/5; front steering 8/9 |

Both use **two MX1508 boards**. For 4WD, each board drives its two separate
motors/channels. For SpringCar, use one channel per board and leave the others
unconnected. Never parallel driver outputs. GPIO numbers mean chip GPIO labels.
Never flash one layout and leave the other layout's wiring connected.

## Power and connection

The builds expect **regulated 5 V motor power from the 2S battery**. Select a
buck and wiring for measured startup/stall current of all active motors. Check
actual motor/driver ratings. Power the development board through its documented
5 V input or USB; never put raw 2S voltage on 5V/3V3. Connect all grounds. Use
input pull-downs and motor/driver decoupling described in SPRING_CAR.md. Driver
PWM caps do not regulate the raw motor supply voltage.

Join **TeleRC-MiniRover**, password **telerc123**, stay connected without internet.
Wi-Fi endpoint **192.168.4.1:14550**, system 1. One controller at a time. Center
both sticks, tap ARM and wait for the rover-confirmed state. CH1=steer, CH2=drive.
Fresh neutral and explicit ARM are required after a stop/reconnect; no auto-rearm.

## 4WD behavior

All four channels are independently driven. Full app input reaches a 242/255
PWM ceiling (95%), 20 kHz carrier. Mixing normalizes combined steer/drive so
outputs remain within the cap. Neutral coasts; opposite direction requires at
least 30 ms coast. Four motors/eight PWM legs are validated at startup and
periodically checked through LEDC register readback. PWM failures latch motor
outputs off until reboot. This is register checking, not motor/current feedback.

## SpringCar behavior and recalibration

A joystick percentage requests an **estimated position**. The motor runs for a
calibrated interval, then electrically brakes instead of continually rotating.
Changing the target moves by an estimated difference. Release requests a timed
powered return, followed by 200 ms spring-finishing coast. Holding a turn does
not disarm the vehicle. Rear drive is independent.

With only two wires, there is no measured angle or detected endpoint. Electrical
braking damps motion but cannot statically lock against the spring; physical
position can drift. If braking cannot hold your mechanism, add a potentiometer,
encoder or servo. Full calibration and limitations: **docs/SPRING_CAR.md**.

Center reset: DISARM, hold **BOOT for two seconds**, release and let the spring
center; explicitly ARM again. Or send `CENTER` over USB serial at 115200 baud.
While DISARMED, use newline-terminated `CAL 350 350 180 180` to save left-out,
right-out, left-return and right-return milliseconds (50–1200 each). `CAL?` shows
current timings in diagnostics. Checked NVS storage survives normal updates.
These defaults are starting values, not physical measurements of your linkage.

## Stops and acceptance

Both stacks disarm/coast after 500 ms without a fresh valid command, on station
loss, DISARM, BOOT or PWM faults. Discovery/heartbeat do not feed the motor timer.
Valid-CRC duplicate/backward frames cannot refresh commands or replay ARM. Peer
IP/source port are locked. CRC/order are not cryptographic authentication.
A 1 s task watchdog is a secondary reset mechanism; measure actual output-off
and reboot behavior. BOOT is a software stop; keep a battery cutoff reachable.

Before ground driving, raise/restrain wheels and verify:

1. Motor leads disconnected: all driver inputs LOW on boot/reset, correct 5 V
   supply, pull-downs, and no output before explicit ARM.
2. Correct motor mapping and polarity with short low-power commands. On 4WD,
   verify forward/reverse, both pivots and mixed steer/drive across all motors.
3. Neutral, DISARM, BOOT, app background/close, Wi-Fi loss and reconnect: no
   unexpected motion or automatic ARM. Measure stopping/current/rail dips.
4. On SpringCar, calibrate movement/return and verify actual centering, brake
   drift and temperature; do not mistake the EST diagnostic for a sensor angle.
5. Record actual board, motor/driver models, current, voltage, temperature and
   stopping times. Hardware/endurance/field tests were not performed here.

## Build, flash and rollback

Read **FLASH.md** in the selected archive. It contains source, tests, four images,
manifest, SHA-256 checksums and a digest-checked flash helper. Flash only the
selected layout with motor/battery power disconnected. A merged whole-flash
image is deliberately excluded. NVS is preserved by the standard offsets.

Source: `bridge/TeleRC_Go_4WD` and `bridge/TeleRC_Go_SpringCar`. Build with
Arduino CLI 1.3.1 and Arduino-ESP32 3.3.2. From a clean checkout:

```sh
python3 scripts/build_motor_stacks.py --output /tmp/go-firmware-stacks --commit "$(git rev-parse HEAD)"
```

Use a fresh output directory. Rebuild after changing pins, power, polarity or
travel timing; repeat the affected checks. Roll back by disconnecting motor
power and reflashing your retained known-good firmware with its matching wiring.
The two layout archives are not interchangeable. App installation is unchanged.
