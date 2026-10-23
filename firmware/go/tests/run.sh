#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
flags=(-std=c++17 -Wall -Wextra -Werror -DTELERC_HOST_TEST=1 -Itests/stubs)
if [[ "${SANITIZE:-0}" == 1 ]]; then flags+=(-fsanitize=address,undefined -fno-omit-frame-pointer); fi
out="$(mktemp /tmp/mini-rover-test.XXXXXX)"
trap 'rm -f "$out"' EXIT
for api in 2 3; do
  g++ "${flags[@]}" -DESP_ARDUINO_VERSION_MAJOR="$api" tests/test_wifi.cpp -o "$out"
  "$out"
  g++ "${flags[@]}" -DESP_ARDUINO_VERSION_MAJOR="$api" tests/test_go_handshake.cpp -o "$out"
  "$out"
  g++ "${flags[@]}" -DESP_ARDUINO_VERSION_MAJOR="$api" tests/test_4wd_outputs.cpp -o "$out"
  "$out" mapping
  for pin in {4..11}; do
    "$out" attach "$pin"
    "$out" drift "$pin"
    "$out" idle "$pin"
  done
done
for driver in 1 2 3 4 5; do
  args=(-DTELERC_DRIVER="$driver")
  if [[ "$driver" == 4 ]]; then args+=(-DTELERC_SUPPLY_MAX_MV=12000 -DTELERC_MOTOR_RATED_MV=12000 -DTELERC_BTS_LOGIC_VERIFIED=1); fi
  g++ "${flags[@]}" "${args[@]}" tests/test_profiles.cpp -o "$out"
  "$out"
done
printf 'PASS: Wi-Fi protocol, Go handshake, four motors, eight PWM legs and fault stops\n'
