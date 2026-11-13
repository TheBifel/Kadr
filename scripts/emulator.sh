#!/usr/bin/env bash
set -euo pipefail
cd /home/aiagent
export XDG_RUNTIME_DIR=/home/aiagent/.android/kadr-runtime
mkdir -p "$XDG_RUNTIME_DIR"
chmod 700 "$XDG_RUNTIME_DIR"
exec /home/aiagent/.local/share/bubnilka-toolchain/android-sdk/emulator/emulator @bubnilka_api35 -no-window -no-audio -gpu swiftshader -no-boot-anim -camera-back none -camera-front none -cores 2 -memory 2560 -no-snapshot-load -no-snapshot-save
