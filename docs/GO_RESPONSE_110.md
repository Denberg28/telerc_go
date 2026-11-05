# TeleRC Go 1.0.10

The Go input path now keeps its 8% center neutral zone independent of the
Steering and Drive sensitivity settings. Outside that zone it maps the remaining
touch travel to commands beyond the known 35 µs receiver neutral window, up to
the selected sensitivity limit. At 25%, the endpoints are 1400 and 1600 µs.
At 100%, they are 1100 and 1900 µs. Release still requests exactly 1500 µs.

Both directions use the same rounded magnitude. Movement on the unassigned
touch axis no longer reduces steering or drive range. Go's stored expo setting
retains a linear component of at least 25% to avoid flattening the low range.
The position light continues to display the requested channel value.

Maze practice previously applied a 25 µs receiver dead zone and then another
8% motion dead zone. The second filter suppressed much of the range at low
sensitivity. Maze now uses its receiver dead zone once. This shared maze fix
also applies to the full TeleRC source. Full TeleRC's other input and motion
callers keep their default response mode.

The app mapping covers the included mini receiver's 25 µs and hybrid receiver's
35 µs neutral windows. A flight controller configured with a larger RC dead
zone may still suppress small inputs. Motor friction and loaded starting duty
require physical measurement.

Install the signed prerelease manually over Go 1.0.9. It retains the production
package and signer with version code 1006. Retain the previous APK because an
Android downgrade may require uninstalling and losing app data.

## Acceptance and compatibility

1. In Settings, select 25% Steering and Drive. Return to Test and start practice.
   Move each stick just outside center, then progressively to full travel.
   Verify movement in both directions, including with sideways finger drift.
2. Repeat at 50% and 100%. Each axis should retain a neutral center and respond
   across the rest of its travel. Adjust one slider and check the other axis
   keeps its own endpoint limit. Release and pause must neutralize both axes.
3. With wheels raised and an independent cutoff available, connect, explicitly
   arm, and repeat at low speed. Observe the readout and actual motor response.
   Verify release, lost touch, disconnect and radio takeover revoke phone motion.
4. Check saved sensitivity, wallpaper and Test progress after an in-place upgrade.

Hardware acceptance is pending. No firmware target is changed by this app
update. Follow the existing matched firmware and receiver configuration.
