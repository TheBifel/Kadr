#!/usr/bin/env bash
# Optional local convenience; Gradle only needs a configured JDK and Android SDK.
KADR_PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$KADR_PROJECT_DIR/.env.local" ]]; then
    set -a
    . "$KADR_PROJECT_DIR/.env.local"
    set +a
fi
export JAVA_HOME="${JAVA_HOME:?Set JAVA_HOME to JDK 17}"
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
export PATH="$JAVA_HOME/bin:${ANDROID_HOME:+$ANDROID_HOME/platform-tools:}$PATH"
