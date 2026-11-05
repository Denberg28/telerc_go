#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
flags=(-std=c++17 -Wall -Wextra -Werror -DTELERC_HOST_TEST=1 -Istubs)
if [[ ${SANITIZE:-0} == 1 ]]; then flags+=(-fsanitize=address,undefined -fno-omit-frame-pointer -fno-pie -no-pie); fi
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
for api in 2 3; do
  for test in test_wifi test_go_handshake test_4wd_outputs test_profiles; do
    g++ "${flags[@]}" -DESP_ARDUINO_VERSION_MAJOR="$api" "$test.cpp" -o "$out/$test"
  done
  "$out/test_wifi"
  "$out/test_go_handshake"
  for mode in mapping mask wdt sequence;do "$out/test_4wd_outputs" "$mode";done
  for pin in {4..11}; do
    "$out/test_4wd_outputs" attach "$pin"
    "$out/test_4wd_outputs" drift "$pin"
    "$out/test_4wd_outputs" idle "$pin"
  done
  "$out/test_profiles"
done
printf 'PASS Go 4WD: protocol, ownership, 4 motors, 8 PWM legs and latched faults\n'
