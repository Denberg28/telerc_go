#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
g++ -std=c++17 -Wall -Wextra -Werror -fsanitize=address,undefined -fno-omit-frame-pointer -Istubs sbrb_controller_tests.cpp -o "$tmp/sbrb-tests"
ASAN_OPTIONS=detect_leaks=0 "$tmp/sbrb-tests"
