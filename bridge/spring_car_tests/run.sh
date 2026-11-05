#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
flags=(-std=c++17 -Wall -Wextra -Werror -Istubs)
if [[ ${SANITIZE:-0} == 1 ]]; then flags+=(-g -fno-omit-frame-pointer -fsanitize=address,undefined -fno-pie -no-pie); fi
# 3.3.2 is the actual target; core-2 API coverage is host-only compatibility.
for core in 2 3; do
  c++ "${flags[@]}" -DESP_ARDUINO_VERSION_MAJOR="$core" test_car.cpp -o "$out/car"
  "$out/car"
  "$out/car" watchdog
  "$out/car" pwm
done
