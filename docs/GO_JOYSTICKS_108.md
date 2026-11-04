# TeleRC Go 1.0.8

Controls and offline Test now show translucent dials, a cyan position light
along each commanded axis, a white neutral ring, and a muted disabled dash.
The light follows the local channel request after sensitivity and expo.
Android losing the controlling pointer releases the stick to neutral and
requires a new touch. Channel mapping, output limits, arming and radio
authority retain their existing behavior. The shared input fix also compiles
in full TeleRC, whose classic drawing style remains its default.

The production APK uses the same package and signing certificate as Go 1.0.7
with an increased version code of 1004. Install over your existing Go
installation. No firmware reflash is needed. The stable in-app feed skips
prereleases, so install this build manually. A downgrade may require
uninstalling and lose app data. Retain your prior signed APK.

## Phone acceptance

1. Install over Go 1.0.7. Verify your wallpaper, settings and Test progress.
2. Open Test and start practice. Try both dials against bright and dark
   wallpapers. Compare the light to the channel readout across the axis.
3. Release each stick and repeat while adding or removing a second finger.
   The controlling finger's release must return that axis to 1500. Start a
   fresh touch to move again. Verify pause and Settings clear the active lights.
4. With wheels raised and an independent cutoff, connect and explicitly arm.
   Repeat at low speed. Verify neutral on release and the disabled indication
   when live control is revoked. Confirm reconnect requires explicit arming.

Physical upgrade and restrained-wheel results are pending. This build is
eligible for prerelease after the software and signing gates pass.
