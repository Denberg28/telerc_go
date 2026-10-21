# TeleRC Go TT RC car — firmware 0.1.0

ESP32-S3 + one MX1508 dual H-bridge + two TT DC motors:
one rear motor drives the rear axle; one front motor operates the steering linkage.
This is the **bare steering motor, without a position sensor** configuration.
It works with the existing **TeleRC Go 1.0.3** app. No app update is required.

## Wiring

| Connection | ESP32-S3 / destination |
| --- | --- |
| MX1508 steering IN1 | GPIO4 |
| MX1508 steering IN2 | GPIO5 |
| Front steering TT motor | MX1508 motor output A pair |
| MX1508 rear-drive IN3 | GPIO6 |
| MX1508 rear-drive IN4 | GPIO7 |
| Rear drive TT motor | MX1508 motor output B pair |
| MX1508 motor supply | Regulated 5 V, sized for actual motor current |
| ESP32-S3 power | Regulated 5 V input / USB; common ground with driver |
| Local stop | ESP32-S3 BOOT button / GPIO0 |
| Optional ELRS receiver TX | GPIO39, normal CRSF 420000 baud |

Verify the actual module's input/output labels: board layouts differ. Never
connect the two H-bridge output pairs together. Do not power motors through
the ESP32-S3. For a 2S battery, use a suitable buck converter for the motors
and regulated logic power; do not connect the pack directly to TT/MX1508.
Check loaded voltage and motor stall current against the actual driver and
supply ratings; a rear axle under load may need a stronger driver. The firmware
has no current or steering-position measurement.

## Operation

Join **TeleRC-MiniRover**, default password **telerc123**. Open TeleRC Go,
tap Phone connect, center both sticks, then ARM. Default endpoint:
`192.168.4.1:14550`, system 1. Personalize the password in `Config.h` before
shared deployment. The Wi-Fi control protocol is not cryptographically authenticated.

- Right stick / CH2: rear forward/reverse only, proportional duty up to 242/255.
- Left stick / CH1: front steering jog only. A deflection starts one pulse,
  at most **50 ms**. The pulse ends sooner when the stick centers or changes
  direction. Steering duty spans 160..200/255 outside the RC deadband.
- Holding left/right cannot restart or extend the pulse. Return the left stick
  to center continuously for **200 ms**, then deflect again for another jog.
  Rear drive may continue while that steering neutral interval is observed.
- Centering the stick cuts steering power. **It does not center the wheels.**
  Jog in the opposite direction while observing the wheels to center them.
  The firmware does not know steering angle, end stops or physical neutral.
- DISARM, command loss (500 ms software threshold), Wi-Fi peer loss, BOOT stop
  and output faults cut both motor channels. Recovery never arms by itself.
  Cutting PWM commands coast; it does not guarantee immediate mechanical stopping.

The pulse timer bounds commanded energization, not wheel travel or safe stall
time. Begin with the steering linkage disconnected, then verify travel with
short pulses. Use a linkage that cannot rotate continuously or bind the motor.
Repeated jogs can still drive into a mechanical stop. Add position feedback for
proportional angle control and reliable automatic centering. No timed reverse
"centering" or guessed position is used in this firmware.

## Tune and compile

Edit `TTCarSettings.h`: `STEER_PULSE_MS`, `STEER_NEUTRAL_REARM_MS`,
`STEER_MIN_PWM`, `STEER_MAX_PWM`, `INVERT_STEERING`, `INVERT_DRIVE`.
These are mechanical setup values, not measured calibration. Default motor
supply declaration is 5 V, motor rating declaration 6 V; verify your actual motors.
Adjust one setting at a time with drive wheels raised and motor cutoff accessible.

From the repository, create a self-contained Arduino sketch:

```sh
python3 scripts/package-go-tt-car.py --output build/tt-car --commit "$(git rev-parse HEAD)"
```

Open `build/tt-car/TeleRC_Go_TT_Car/TeleRC_Go_TT_Car.ino` in Arduino IDE.
The supplied source ZIP already contains that complete sketch folder; no external
Arduino libraries are required. Install **esp32 by Espressif Systems 3.3.2**;
select **ESP32S3 Dev Module**, USB CDC On Boot enabled, hardware USB, 4 MB flash,
default partitions, PSRAM disabled, DIO flash. These conservative defaults do
not require a particular N8R2/N16R8 PSRAM configuration. Upload at 115200 if the
board is unreliable at higher upload rates. The target is ESP32-S3, not LilyGO T3-S3.

Wi-Fi is the default (`TELERC_GO_ELRS=0`). For a receiver-enabled Arduino IDE
build, add `#define TELERC_GO_ELRS 1` immediately before the profile definition
at the top of the generated `.ino`, or pass `-DTELERC_GO_ELRS=1` to the compiler.
Serial diagnostics use 115200 baud and report the profile/version, channels,
output duties and stop reason. These are command/register values, not feedback.

## Optional ELRS

Use the **WiFi-ELRS** image only when a compatible bound receiver is connected
to GPIO39 with common ground, receiver-rated power and 3.3 V UART signaling.
Normal CRSF: CH1 steering, CH2 centered drive, CH5 ARM, CH6 STOP, CH7 source.
Map RadioMaster Pocket SA to CH7: high (>1800 us) permits phone; low (<1300 us)
selects transmitter; middle inhibits both. Every source change stops/disarms.
For radio arming, center both axes, hold CH5 low for at least 300 ms, then switch
CH5 high. CH6 high stops. Returning to phone requires an explicit app ARM tap.
The same steering pulse/neutral rule applies to both sources. The WiFi image
ignores a receiver; use WiFi-ELRS when transmitter takeover is required.

## Flash compiled images

Use only the image folder matching WiFi or WiFi-ELRS. Flash the supplied
`TeleRC_Go_TT_Car.ino.merged.bin` at **0x0**. For individual images use
bootloader at 0x0, partitions at 0x8000 and application at 0x10000, plus the
toolchain's boot_app0 at 0xe000 if supplied. Prefer the merged image when unsure.
Disconnect motor power during flashing. Source-only ZIPs have no `.bin` files;
compile them in Arduino IDE or use a completed GitHub firmware build artifact.
Verify `SHA256SUMS.txt` and `BUILD_PROFILE.json` before flashing.

## Restrained-wheel acceptance and rollback

1. With motor supply disconnected, confirm pin allocation, common ground,
   regulator settings and independent power cutoff. Lift the car so drive wheels
   are clear. Disconnect the steering linkage for initial direction tests.
2. Power up without the app: both motors must remain off. Connect with a
   deflected stick: ARM must be denied. Center, ARM, then test rear forward,
   neutral, reverse; only the rear motor should move.
3. Keep drive neutral. Jog left/right and check direction. Hold a steering command
   for 2 s: it must stop after one short pulse and remain off. Flip direction
   without neutral: it must remain off. Center for at least 200 ms and retry.
4. Stop and reconnect the linkage. Use shorter/reduced-duty pulses if needed;
   verify both travel limits before running the rear motor. Center wheels manually.
5. Test steer plus drive: expiry of a steering pulse must not change rear speed.
   Confirm DISARM, BOOT, app settings/backgrounding, Wi-Fi loss and command loss
   stop both channels. Reconnect: no automatic motion or arming.
6. If ELRS is fitted, test SA handover/return, held-high CH5, fresh low/high rearm,
   CH6 and receiver loss. Phone observer disconnect must not stop independent RF
   driving; stale phone commands must not seize RF control.
7. Record phone/app/board/driver/receiver versions, actual voltage, pulse travel,
   motor/driver temperature and measured stopping behavior before ground testing.

Hardware acceptance is **NOT TESTED/BLOCKED** until recorded on the actual car.
This is a development firmware, not a hardware-validated stable release.
Retain the previous firmware/configuration for rollback. The four-motor rover
image has different wiring and mixing: it is not a drop-in rollback for this car.
