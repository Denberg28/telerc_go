#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
cli=${ARDUINO_CLI:-arduino-cli}
if ! "$cli" core list | grep -Eq 'esp32:esp32[[:space:]]+3\.3\.2[[:space:]]'; then
  echo 'Install the pinned core: arduino-cli core install esp32:esp32@3.3.2' >&2
  exit 1
fi
out=${1:-spring-car-build}
if [[ -e "$out" ]]; then echo 'Use a fresh output directory.' >&2; exit 1; fi
"$cli" compile --fqbn 'esp32:esp32:esp32s3:CDCOnBoot=cdc,USBMode=hwcdc,FlashSize=4M,PartitionScheme=default,PSRAM=disabled,FlashMode=dio' \
  --warnings default --output-dir "$out" bridge/TeleRC_Go_SpringCar
