# TeleRC Go 1.0.9

Offline Test displays only STEER and DRIVE above its translucent joysticks.
Removing the channel text and letting each joystick use its full side column
gives the dials more room on short or narrow landscape layouts. The dials keep
their axis guides and position lights. This change is scoped to offline Test.

The production APK uses version code 1005. Install it manually over Go 1.0.8
with the retained production signing certificate. The stable updater skips
prereleases. No firmware reflash is needed. Retain the previous APK because an
Android downgrade may require uninstalling and losing app data.

## Phone acceptance

1. Upgrade over Go 1.0.8 and check your wallpaper, settings and Test progress.
2. Open Test at the default and enlarged Android text sizes. Confirm both
   headings fit and each dial is centered with room around its outer edge.
3. Start practice and move each axis. Check the indicator tracks the stick and
   returns to neutral on release. Pause, then exit through Test → Controls.
4. Confirm Controls returns disconnected. With wheels raised and an independent
   cutoff available, check steering, drive, release and disconnect at low speed.

Physical phone and restrained-wheel acceptance are pending.
