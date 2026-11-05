# TeleRC Go SpringCar 0.2.0 — bench candidate

Rear single brushed DC drive motor, front single brushed DC steering motor with
mechanical centering spring, two MX1508 boards and one ESP32-S3 development board.
TeleRC Go 1.0.11 remains unchanged. This is a separate standalone Wi-Fi sketch;
it does not replace the four-wheel/skid-steer, flight-controller or gateway code.

## Power and wiring

**The supplied binary expects a regulated 5 V motor rail.** Feed it from the 2S
battery through a buck converter sized for both motors' simultaneous starting
current. A normal 2S Li-ion/LiPo pack reaches 8.4 V full; do not feed that directly
to unidentified 3–6 V motors. PWM duty limiting is not voltage regulation.
The firmware does not measure battery voltage, motor current or wheel angle.
Use the pack's appropriate BMS/low-voltage protection and a battery fuse sized to
the wiring and measured current. Each motor's starting/stall current must fit
its actual driver channel; adding a second board does not increase a channel's
rating. Leave every unused MX1508 channel disconnected; never parallel outputs.

| Connection | Destination |
| --- | --- |
| ESP32-S3 GPIO4 | Rear-drive MX1508 channel input IN1 |
| ESP32-S3 GPIO5 | Rear-drive MX1508 matching input IN2 |
| Rear-drive board matching OUT1/OUT2 | Rear motor's two wires |
| ESP32-S3 GPIO8 | Front-steering MX1508 channel input IN1 |
| ESP32-S3 GPIO9 | Front-steering MX1508 matching input IN2 |
| Steering board matching OUT1/OUT2 | Steering motor's two wires |
| Regulated 5 V motor rail | Both MX1508 motor-supply positive terminals |
| ESP32-S3 GND | Both driver GND terminals and buck/battery negative |
| Regulated 5 V logic supply | Development board's documented 5V/VIN input |
| GPIO0 BOOT button | Built-in stop/disarm input, active low |

Labels differ by board: use the two inputs controlling the same motor output
pair. GPIO numbers mean chip GPIO, not physical header position. GPIO6/7/10/11
are not used. There is no receiver, servo, flight controller or LoRa connection.

Prefer a separate clean 5 V logic regulator branch to prevent motor-induced
brownouts. Do not apply raw 2S voltage to the board's 5V or 3V3 pin. Follow the
specific development board's USB/external-power rules. Add 10 kOhm pull-downs
from each driver input (GPIO4/5/8/9) to GND so reset/unplugged GPIOs remain off.
Place a 100 nF ceramic across each brushed motor and local bulk decoupling near
the drivers (for example 470 uF, at least 16 V), with short motor-current wiring.
These do not replace checking rail dips, actual current, heating and motor ratings.

## Install and connect

1. Disconnect battery/motor power and unplug the driver motor wires for flashing.
2. Target: **ESP32S3 Dev Module**, at least 4 MB flash, DIO, default 4 MB partition,
   native USB CDC enabled, PSRAM disabled. Arduino-ESP32 core **3.3.2**.
3. Open `bridge/TeleRC_Go_SpringCar/TeleRC_Go_SpringCar.ino` in Arduino IDE and
   upload, or use the included binary flashing instructions. No application
   install/update is required. Keep the existing known-good firmware for rollback.
4. With wheels restrained and motor power initially off, power the controller.
   Join **TeleRC-MiniRover**, password **telerc123**. Keep Wi-Fi despite no internet.
   Only one controlling device may connect; close PC TeleRC/other controllers.
5. Open existing TeleRC Go, use Wi-Fi host **192.168.4.1**, UDP **14550**, system 1.
   Connect, center both sticks, tap **ARM**, and wait for confirmed ARMED.
6. Apply motor power only after verifying wiring. Begin with short low commands
   and a reachable battery cutoff. If a direction is reversed, power everything
   off and swap that motor's two output wires, or edit its inversion setting and
   rebuild. Never swap wires with power present.

## Estimated steering and recalibration

CH1 requests an **estimated steering position**; CH2 drives the rear motor.
TeleRC Go and its sensitivity settings remain unchanged. Firmware maps CH1
1100/1900 us to estimated left/right limits, with 1500 us center and +/-25 us
deadband. Percentage follows the app's existing command/sensitivity mapping.

The front motor runs only while the travel model estimates an error. It then
switches to MX1508 electrical braking (both inputs HIGH; both motor outputs LOW).
Holding the same request does not keep issuing a rotation command or disarm the
car. A changed request moves by the estimated difference. On joystick release,
the firmware reverses for an estimated return movement, then coasts for 200 ms
so the spring can finish centering. Rear drive remains independent. DISARM,
BOOT, release of ownership and link/fault stops immediately coast both motors;
they never initiate a powered center return.

**No position feedback is present.** Travel is inferred from elapsed time and
applied PWM, including ramp/reversal pauses. It cannot detect end stops or spring
movement. Electrical braking damps movement; it does NOT lock the shaft at a
static angle against the spring. If the spring back-drives the gearbox while
braked, this version cannot correct the drift. That needs a potentiometer,
encoder or a servo. Do not regard the diagnostic EST value as a measured angle.

Defaults:

| Setting | Value |
| --- | --- |
| Rear drive ceiling | 242/255 (95%), 4-count/5 ms acceleration ramp |
| Steering movement duty | 160/255 (63%), 10-count/5 ms acceleration ramp |
| Steering at estimated target | Electrical brake; no repeated movement pulse |
| Left/right center-to-limit travel | 350/350 ms at the configured steering movement duty |
| Left/right limit-to-center return | 180/180 ms, separately calibrated for spring assistance |
| Spring settling/reference | 200 ms of coast after return or stop; no sensor verification |
| Position tolerance | 1.5% of one-sided estimated travel to limit jitter |
| Reversal coast | At least 30 ms before opposite motor direction |
| PWM carrier | 20 kHz |
| Command timeout | 500 ms without a new valid CH1/CH2 frame disarms/coasts |
| Peer loss | Station loss or 3000 ms lease timeout releases peer |
| Task watchdog | 1000 ms requested loop timeout; actual reset latency is unmeasured |

### Reset the center reference without changing the app

Disarm and release both sticks. Hold the board's **BOOT button for at least two
seconds**, then release it. Both motors remain off and the spring is free to
center; the estimate becomes zero after the settling interval. ARM explicitly
again. This recalibrates the software's assumed center, not a measured endpoint.
The mechanism must actually return straight. If it cannot, repair/recenter the
linkage before trusting the estimate.

The same center reset is available over USB serial at 115200 baud:

```text
CENTER
```

### Set/save travel calibration over USB serial

Calibration is accepted only while **DISARMED**; it never sweeps a motor by
itself. Open the Arduino Serial Monitor at 115200 baud with newline line endings.

```text
CAL 350 350 180 180
CAL?
```

The four numbers are **left outward, right outward, left return, right return**,
in milliseconds for one-sided full travel at steering duty 160. Each must be
50–1200 ms. A successful command saves one checked record in ESP32 NVS, activates
it and resets the center reference. `CAL?` reports the current timings in the
next diagnostic line. `CAL_SAVED` confirms success; `CAL_STORAGE_ERROR` leaves
active timings unchanged. Bad/incomplete/oversized lines are rejected. No flash
writes occur while ARMED. Values survive reboot and normal application flashing
that preserves NVS; erase-flash removes them.

Start with wheels raised and current limited. Use short movements, return to
physical center with BOOT between trials, and compare estimated movement with
visible wheel travel. Increase an outward time if travel falls short; decrease
it if the motor reaches a stop early. Tune each return time independently so
powered return ends near center before the spring finishes. These defaults are
starting values, not measured values for your particular motor. A jam cannot
be detected; keep the cutoff reachable. Voltage/load changes require a new
physical calibration check. Changing steering duty also invalidates the timings.

Motor direction must be correct before tuning. Swap that motor's two output
wires with all power off if necessary. Direction inversion and PWM ceiling are
compile-time options in Config.h; changing them requires rebuilding/testing.

Discovery/heartbeat traffic never keeps motors alive. Duplicate and backward
MAVLink sequences do not refresh control or replay ARM. Peer IP AND source port
are locked; fallback phone source ports are supported. Frame CRCs and the entire
datagram are checked before command side effects. CRC/sequence checks are not
cryptographic authentication; the AP password is the only access restriction.

BOOT is a software stop, not a physical battery disconnect. After a PWM/write/
readback/watchdog-service fault, reboot is required. No reconnect, heartbeat or
reboot can automatically ARM. The task watchdog is a secondary reset mechanism;
actual reset/stop latency requires measurement and external input pull-downs.

## Restrained-wheel acceptance — record each result

No hardware tests were performed during development. Execute these steps with
wheels clear of the floor, the steering linkage visible, an ammeter/current-limit
appropriate to the actual motors, and a reachable battery cutoff:

1. With motor leads disconnected, confirm all four driver inputs are LOW at
   power-up, during reset and after disconnect. Check 5 V rail and input pull-downs.
2. Connect motors with power off. With no ARM, move both app controls: no motor
   may energize. ARM with both centered; no initial motion is permitted.
3. Pulse DRIVE forward/reverse at low sensitivity. Only the rear motor turns;
   neutral must coast it immediately and reversal must include a coast interval.
4. Pulse STEER at a small nonzero percentage. Confirm a finite movement followed
   by braking, not endless rotation. Repeat the same command: no repeated pulse.
   Compare visible travel with EST; the latter is only a model estimate.
5. Increase/decrease steering percentage in both directions while driving with
   wheels raised. Confirm independent rear drive and no steering-duration disarm.
6. Release steering: verify the bounded active reverse movement, then coast and
   actual spring centering. The return may overshoot if timing is wrong: stop,
   recalibrate and repeat. Firmware cannot detect that overshoot by itself.
7. Hold a target and inspect whether the spring back-drives the brake. If it
   drifts, this hardware cannot provide the requested static hold. Add position
   feedback or a servo. Confirm BOOT center reset and saved four-value calibration,
   including persistence after a controller restart. Re-ARM explicitly.
8. While driving slowly with raised wheels, test DISARM, BOOT, app background,
   app force-close and phone Wi-Fi off separately. Measure actual output-off
   latency; command-loss target is <=500 ms plus one tick/scheduling overhead.
   Rediscovery/reconnection alone must not restart either motor.
9. Verify steering release at each mechanical extreme; inspect spring return,
   gearbox back-drive, current, buck rail dips and temperature. Stop on overheating.
10. Run several short low-speed ground maneuvers only after every prior result
    passes. Record controller/driver/motor model, supply, current, stop latency,
    steering return time, ambient and temperatures. Long-term/field reliability
    remains unqualified until actual endurance tests pass.

Serial diagnostics at **115200 baud** report `CH1`, `CH2`, logical `OUT` slots
0=drive/2=steer, LEDC duty registers, `EST` (estimated -1000..1000), `brake`,
`CAL` timings, owner, errors and last stop reason.
`COMMAND_TIMEOUT` means commands expired. `PWM_READBACK`/`PWM_ERROR`/`WDT_RESET_ERROR` require reboot and inspection.
These are register/command diagnostics, not measured electrical or motor feedback.

## Source and rollback

Baseline repo: `a20eac1e05c6422258d3fc11f1659e39a00fc91c`.
Go handshake/output foundation: checked-in `TeleRC-Go-4WD-MiniRover-v1.2.4-firmware.zip`;
its exact SHA-256 and the new source commit are recorded in the package manifest.
The legacy loose MiniRover sketch lacks the current Go ownership contract and
must not be substituted for this sketch. The full original rover/app is unchanged.

Rollback: disconnect motor power, reflash your retained known-good firmware and
restore its matching motor wiring before applying power. The old skid-steer
firmware is NOT suitable for the new front-steering/rear-drive wiring.

Next check: install this candidate on the actual ESP32-S3 and complete steps 1–8,
then calibrate the four travel times and validate actual braking, return,
current and temperature. Do not label this bench candidate field-ready.

Primary references: [MX1508 datasheet](https://uelectronics.com/wp-content/uploads/2018/04/MX1508_Datasheet.pdf),
[Espressif LEDC API](https://docs.espressif.com/projects/arduino-esp32/en/latest/api/ledc.html),
[ESP-IDF 5.5 watchdog API](https://docs.espressif.com/projects/esp-idf/en/release-v5.5/esp32/api-reference/system/wdts.html).

[Espressif Preferences/NVS API](https://docs.espressif.com/projects/arduino-esp32/en/latest/api/preferences.html).
