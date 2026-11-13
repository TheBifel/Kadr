#!/usr/bin/env bash
export JAVA_HOME="${JAVA_HOME:-/home/aiagent/.local/share/bubnilka-toolchain/jdk-17}"
export ANDROID_HOME="${ANDROID_HOME:-/home/aiagent/.local/share/bubnilka-toolchain/android-sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
