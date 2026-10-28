#!/usr/bin/env bash
set -euo pipefail
PKG=io.github.denberg28.telerc.go
mkdir -p go/build/wallpaper-evidence
capture() {
 for name in go-wallpaper-custom go-1.0-settings go-1.3-settings go-1.0-background go-1.3-background go-failed; do
  adb exec-out run-as "$PKG" cat "files/$name.png" > "go/build/wallpaper-evidence/$name.png" 2>/dev/null || true
 done
 adb logcat -d > go/build/wallpaper-evidence/logcat.txt
}
trap capture EXIT
adb install -r go/build/outputs/apk/debug/go-debug.apk
adb install -r go/build/outputs/apk/androidTest/debug/go-debug-androidTest.apk
adb shell am instrument -r -w -e wallpaperOnly true "$PKG.test/io.github.denberg28.telerc.GoInstrumentation" | tee go/build/wallpaper-evidence/result.txt
grep 'INSTRUMENTATION_CODE: -1' go/build/wallpaper-evidence/result.txt
grep 'PASS: gallery image picker' go/build/wallpaper-evidence/result.txt
