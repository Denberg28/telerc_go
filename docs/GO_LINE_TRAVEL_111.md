# TeleRC Go 1.0.11

The position light now follows touch position along the visible axis line.
The center is neutral, the halfway tick is half stick travel, and each line
endpoint is full stick travel. The surrounding circle provides touch space.
Moving beyond a line endpoint keeps the selected maximum command.

Previously, touch input already used the visible line length, but the position
light displayed the channel command after sensitivity and expo. At 25%
sensitivity, full finger travel therefore lit only the first quarter of the
line. The marker and the touch scale now agree. Sensitivity controls the
requested command magnitude while the marker shows finger position.

The existing 8% neutral zone and Go receiver response curve apply.
For a halfway touch with expo at zero, the expected PWM requests are:

| Sensitivity | Positive direction | Negative direction |
| --- | --- | --- |
| 25% | 1565 µs | 1435 µs |
| 50% | 1610 µs | 1390 µs |
| 100% | 1702 µs | 1298 µs |

Native checks dispatch touches at the actual line ticks and verify marker
position, channel output and movement of the Test rover at 25%, 50%, 75% and
100% travel. They cover both axes and directions, sensitivity 25/50/100%, and
the lowest and highest stored expo settings.

Install the signed prerelease manually over Go 1.0.10. The production package
and signer are retained with version code 1007. Keep the previous APK because
an Android downgrade may require uninstalling and losing app data.

## Phone and rover acceptance

1. Open Test and start practice. Touch halfway from center to each line end.
   Confirm the light sits at that tick and the rover moves or turns.
2. Repeat at the quarter and full line positions, both directions and all three
   sensitivity settings. The light should reach the line endpoint even at
   25% sensitivity. Release and pause must return the controls to neutral.
3. With wheels raised and an independent cutoff available, connect and
   explicitly arm. Repeat on the physical rover, watching the numeric readout.
   Confirm the receiver observes the command, then measure actual motor onset.
4. Check release, lost touch, disconnect and radio takeover at low speed.

The software checks establish Test response. Physical motor starting duty,
friction, receiver configuration and installed phone behavior remain hardware
acceptance items. No firmware target changes in this update.
