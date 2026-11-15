# Build and test

Requirements: JDK 17, Android SDK platform 36 and a Bash-compatible shell (Linux, macOS or WSL). Gradle 9.3.1 is supplied by the wrapper; AGP 9.1.0, Kotlin 2.2.10 and runtime dependencies are pinned in Gradle files.

Install SDK components using Android Studio or the official command-line tools:

```bash
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

Set `JAVA_HOME` to your JDK and `ANDROID_HOME` to your SDK. Alternatively copy `.env.example` to ignored `.env.local`, fill in your paths, then source `scripts/env.sh`. Android Studio's ignored `local.properties` is also supported by Gradle. No host-specific SDK path is required.

```bash
./gradlew :domain:test :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
./gradlew checkKotlinFormatting
```

Use `./gradlew formatKotlin` for deterministic Kotlin/Gradle formatting (ktfmt, Kotlin style). CI runs checks and uploads a debug APK artifact. CI debug signatures can differ from the maintainer's downloadable release; an in-place Android update requires the same signing certificate.

## Emulator checks

Start an Android 12+ AVD, for example using `scripts/emulator.sh AVD_NAME`. Set `ANDROID_SERIAL` if multiple devices are attached.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.thebifel.kadr.test/androidx.test.runner.AndroidJUnitRunner
```

Acceptance tests use isolated test projects and remove fixtures afterward. Do not clear real app data when testing an upgrade. Capture settings/bindings/cache before upgrading; check native ↻ from the launcher, resize and an actual offline reboot after unlock. `scripts/device_state.py` records metadata and HTML/PNG hashes rather than page source; keep reports local because metadata can still be personal.

## Signing and environment variables

The default build uses the standard local Android debug keystore, outside the repository. To use a specific external debug key, set all of:

- `KADR_DEBUG_KEYSTORE_PATH`: path to the keystore outside the checkout.
- `KADR_DEBUG_STORE_PASSWORD`
- `KADR_DEBUG_KEY_ALIAS`
- `KADR_DEBUG_KEY_PASSWORD`

These values are read from the environment. Keystores, `.env.local`, tokens and `local.properties` must not be committed or attached to a release. The GitHub workflow needs no app API credentials. Keep any future CI credentials in repository secrets, not YAML literals.

## Release

Build and verify the APK, copy it under a versioned filename, generate a SHA-256 checksum and attach both to the matching release. Downloads are debug builds, not Play Store packages. Check packaged assets and compare the signing certificate with the previous release before distributing an update.

See [1.1.5 patch notes](../specs/003-minimal-editor/patch-1.1.5.md) for the actual tested environment and limits.
