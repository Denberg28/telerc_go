# Go 1.0.7 — latest TeleRC maze Test

Baseline: published Go1.0.6 product `d66e4751b050caae7c9b53ef4e9235b153f78aa7`,
main `ad8fddb0e011c84ba7c56500fc6ac2540b8a75cf`.
Target: Go1.0.7/code1003, same production package and signer; app-only.

The owner supplied a screenshot of TeleRC's maze Test. Go1.0.6 had incorrectly
adapted the older arcade road game from TeleRC main. This update replaces it
with the latest **released** TeleRC v1.1.0rc1 Test, product
`03b5b69ff39487678d25a8023c5438a773ca3d51`.
The existing shared MazePractice/Market/Environment/Renderer/View/RoverKinematics
and popup layout source blobs were compared against that release and are exact.

## Behavior and compatibility

Test remains immediately before Phone connect on Go's control screen. Entering
it disconnects/cancels live authority and opens the full-screen three-column maze
layout: Test menu / Steer / Start practice, central level/world/timer/course with
Levels and Restart level, Market / Drive / Clear times. The Test menu includes
Controls and Settings; Android Back also returns to disconnected Controls.

The released20 levels, worlds, collision/three-bump rule, start/pause/resume,
mandatory failure restart, next-level unlock, persistent best/clear times,
finite coin supply, replay offers/rewards and cosmetic Market are preserved.
No game input reaches GoLink: the panel has no transport reference. Opening any
practice popup, settings or backgrounding pauses/neutralizes practice. Return or
reconnect never enables/arms live control. Maze state across a view rebuild stays
paused; app restart loads the saved level, completion, wallet and cosmetics with
fresh neutral practice. Different TeleRC/Go Android packages keep separate data;
this does not transfer saved progress from the full TeleRC installation.

GoMazeSession uses the original maze_practice preference keys/walletv3 schema;
completion, replay reward and offer removal share one preference transaction.
The private wallpaper and main control layout/connection/signing remain. No
firmware update. Android downgrade may require uninstall/data loss; retain the
prior signed APK and avoid uninstalling to update.

## Acceptance and gates

| Gate | Status before final cloud run | Evidence / required check |
| --- | --- | --- |
| A build | PASS local build/lint | Both-app regression and Go debug/release lint/build/native test compilation passed locally. Final cloud checks remain required. |
| B behavior | Local57 Go +162 full-app JVM tests PASS | Reuse released maze physics/economy/replay regressions. |
| C regression | Shared sync PASS; host PASS | Default cloud sanitizers and18 source/package tests remain required. |
| D integration | NOT TESTED | Native screenshot-like level12/14-coin fixture at1.0/1.3 text: actual joystick input, pause, Market buy/remove/equip, real-frame clear/record/unlock, Levels, three-bump restart, lifecycle, persistence, no live transmissions. |
| E phone/HIL | BLOCKED | Actual phone installation/settings and restrained-wheel isolation pending. |
| F delivery | NOT TESTED | Same signer as1.0.6, increased code1003, exact source/tag, class inventory, permissions, alignment/ZIP, hashes and public assets. |

Native fixtures seed emulator preferences only; real users start at their saved
Go maze level or level1. The screenshot's level12 is not a forced starting level.

Phone check: update the production app in place; confirm wallpaper/connection and
sensitivity. With wheels raised and independent cutoff, open Test during pending
ARM/low-speed travel and confirm the existing stop/pause behavior and canceled
arming. Play the maze, use dialogs, background/relaunch and return to Controls;
no rover reconnect/motion/arming may occur until explicit connection and ARM.
Check Test menu and all controls/captions at normal and enlarged text, game saves
and actual gallery providers. Local balance torque may remain on balancers;
Test entry does not prove motor power is off.

Next: inspect cloud native screenshots/results, build with retained production
signer and publish an explicitly limited prerelease after software/package gates
pass. Final producer results supersede pending software statuses only.
