#!/usr/bin/env bash
set -euo pipefail
PKG=io.github.denberg28.telerc.go
mkdir -p go/build/wallpaper-evidence
capture() {
 for name in go-wallpaper-custom go-1.0-settings go-1.3-settings go-1.0-background go-1.3-background go-failed go-test-1.0-controls go-test-1.3-controls go-test-1.0-game go-test-1.3-game go-test-1.0-market go-test-1.3-market go-test-1.0-levels go-test-1.3-levels; do
  adb exec-out run-as "$PKG" cat "files/$name.png" > "go/build/wallpaper-evidence/$name.png" 2>/dev/null || true
 done
 adb logcat -d > go/build/wallpaper-evidence/logcat.txt
}
trap capture EXIT
adb install -r go/build/outputs/apk/debug/go-debug.apk
adb install -r go/build/outputs/apk/androidTest/debug/go-debug-androidTest.apk
# API 35 applies its initial dynamic-color overlays shortly after boot. Wait
# for that known system relaunch before retaining an Activity in the UI harness.
ready=false
for attempt in {1..30}; do
 if adb logcat -d -s ThemeOverlayController | grep -q 'ThemeOverlayController ready with user 0'; then ready=true; break; fi
 sleep 1
done
if [ "$ready" != true ]; then echo 'Initial Android theme setup did not settle'; exit 1; fi
sleep 2
adb shell am instrument -r -w -e wallpaperOnly true "$PKG.test/io.github.denberg28.telerc.GoInstrumentation" | tee go/build/wallpaper-evidence/result.txt
grep 'INSTRUMENTATION_CODE: -1' go/build/wallpaper-evidence/result.txt
grep 'PASS: gallery image picker' go/build/wallpaper-evidence/result.txt

adb shell am instrument -r -w -e testOnly true "$PKG.test/io.github.denberg28.telerc.GoInstrumentation" | tee go/build/wallpaper-evidence/test-result.txt
grep 'INSTRUMENTATION_CODE: -1' go/build/wallpaper-evidence/test-result.txt
grep 'PASS: offline maze Test' go/build/wallpaper-evidence/test-result.txt
