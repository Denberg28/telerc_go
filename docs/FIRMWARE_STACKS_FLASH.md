# Flash the selected ESP32-S3 motor stack

Use this only with an ESP32-S3 development board with at least 4 MB flash.
Disconnect motor/battery power and motor leads. Power the controller by USB.
Keep the previous known-good firmware and its wiring guide for rollback.

## Option A: precompiled binaries

Install Python and esptool 5.1.0 on your computer:

```sh
python -m pip install esptool==5.1.0
python flash.py --verify-only
python flash.py --port COM5
```

Replace COM5 with your board's port (Linux example `/dev/ttyACM0`). On Windows,
`py` can be used instead of `python`. If auto-reset cannot enter download mode,
hold BOOT, tap RESET, release BOOT, and try again. The helper verifies the image
SHA-256 values before opening the port. No whole-flash erase is requested.
The command writes the bootloader, partition table, OTA boot-selection metadata
and application. It does not deliberately erase the NVS partition at 0x9000;
SpringCar stores travel timings in NVS; the 4WD stack has no saved steering calibration. A different previous partition
layout is not guaranteed to preserve all existing data.

| File | Address |
| --- | --- |
| firmware/bootloader.bin | 0x0 |
| firmware/partitions.bin | 0x8000 |
| firmware/boot_app0.bin | 0xe000 |
| firmware/application.bin | 0x10000 |

Flash mode DIO, 80 MHz flash, 4 MB, default partition, PSRAM disabled; native
USB CDC enabled. The application image identifies ESP32-S3 (chip ID 9).
Never write application.bin alone at address 0x0. No merged whole-flash image
is provided. Recheck wiring with power removed, then follow README.md.

## Option B: Arduino IDE/source

Install Arduino-ESP32 3.3.2. Open the single sketch contained under `bridge/`:
`TeleRC_Go_4WD/TeleRC_Go_4WD.ino` for four drive motors, or
`TeleRC_Go_SpringCar/TeleRC_Go_SpringCar.ino` for rear drive/front steering.
Choose ESP32S3 Dev Module, 4 MB, default partition, DIO, native hardware USB CDC
on boot, PSRAM disabled. Never substitute the other layout's sketch.

No app update is required. Verify the motor mapping/power and restrained-wheel
procedure in README.md before connecting the motor leads. SpringCar calibration
is in docs/SPRING_CAR.md; a normal image update preserves its NVS timing record.
