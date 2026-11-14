#!/usr/bin/env bash
set -euo pipefail
KADR_AVD="${1:?Usage: scripts/emulator.sh AVD_NAME}"
KADR_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:?Set ANDROID_HOME or ANDROID_SDK_ROOT}}"
exec "$KADR_SDK/emulator/emulator" -avd "$KADR_AVD" -no-window -no-audio -gpu swiftshader -no-boot-anim -camera-back none -camera-front none -cores 2 -memory 2560 -no-snapshot-load -no-snapshot-save
