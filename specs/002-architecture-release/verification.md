# Kadr 1.0.0 verification

Date: 2026-11-14. Environment: JDK 17, Android SDK 36, API35 x86_64 emulator and an existing launcher widget. Device data was preserved across upgrades. Detailed logs and device snapshots remain local ignored artifacts, not published application data.

## Build and tests

- `build-100-final.log`: formatting check, domain tests, APK/test APK and Android lint passed. `build-100-licensed.log`: final APK rebuilt with Apache license and NOTICE assets.
- Domain: **16 JVM tests** (4 policy, 12 use-case scenarios). Covers source revision advancement, cache preservation, deletion/binding isolation, scheduler coordination, cancellation, stale/deleted publication and per-project serialization.
- `instrumentation-100.log`: **16 Android tests passed**, 70.07 seconds. Real WebView pixels, Vue/Canvas/fetch, error/timeout cache retention, persistence/remap, WorkManager, system themes/locales, full viewport/overlay contract, clipboard and editor preset/save/delete touch flows.

## Upgrade and lifecycle

- In-place upgrade from 0.1.4 to 1.0.0: existing projects, HTML hashes, settings, bindings and successful frame cache retained (`before-100-upgrade.json` / `after-100-upgrade.json`).
- Native refresh on the launcher: successful new frame with the same projects/bindings and no Activity (`before-100-native-refresh.json`, `after-100-native-refresh.json`, `100-refresh-top.txt`).
- Final licensed APK installed, Wi-Fi/data disabled, actual `adb reboot`, boot/unlock. No Kadr Activity opened for recovery. Local HTML rebuilt offline, API frame/hash and success timestamp retained, project configuration/bindings unchanged; OS WorkManager jobs restored (`before-100-reboot.json`, `after-100-reboot.json`, `100-reboot-top.txt`, `100-reboot-jobs.txt`). Network restored afterward.
- Public demo screenshots: [widget](../../docs/images/widget.png), [editor](../../docs/images/editor.png).

## Package and secrets

- Gitleaks 8.30.1: zero findings in Git history, the current public source candidate and extracted APK assets/DEX strings. Reports are local and redacted. No personal widget state, local properties, environment files or private signing keys are packaged/published.
- Final APK signature verifies and its certificate matches 0.1.4, supporting in-place updates. The private key stays outside Git. Optional external signing configuration is read from environment variables.
- Final debug APK SHA-256: `0bd676a4cb563033f043399b3da4a1e923e93e513211d275339ca67dac79758c`.

- [GitHub CI](https://github.com/TheBifel/Kadr/actions/runs/43601757704) passed on a clean hosted runner: explicit Android SDK setup, formatting, JVM tests, both APK builds, lint and artifact upload. Documentation-only changes do not rerun the Android build.

## Distribution and limits

Public repository: [TheBifel/Kadr](https://github.com/TheBifel/Kadr). Release: [v1.0.0](https://github.com/TheBifel/Kadr/releases/tag/v1.0.0), with debug APK, checksum and license/NOTICE. The release is public and published. Its APK was downloaded anonymously from the GitHub release URL and verified with the attached `SHA256SUMS`: identical to the tested local APK. A copy in the local distribution share was also verified against the same checksum.

Physical devices/OEM battery restrictions, long-duration power use, Direct Boot and a cross-device cloud restore were not verified. The release is a debug build, not a Play Store package. The original product verification remains in [001](../001-programmable-widget/verification.md).
