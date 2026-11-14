# Kadr 1.1.0 verification

Date: 2026-11-14. Package `com.thebifel.kadr`, versionCode **7**. JDK 17, SDK 36, API35 x86_64 emulator with gesture navigation and existing launcher widgets. Detailed logs, project metadata and intermediate screenshots remain local ignored artifacts under `artifacts/1.1.0/`.

## Implementation

The approved [003 specification](spec.md) is implemented in the existing Compose presentation layer. Domain/storage formats, scheduling, SDK and Android component identities are unchanged. Source of the approved design: the user's iterative local mockup review; the selected implementation is its single-screen variant.

Preview refresh now has its own successful timestamp and does not mark an unchanged project dirty or publish to the home screen. Preview matching includes source, effective theme, radius and locale. Outdated renders cannot clear the stale status for newer code; a failed preview retains its previous successful image/time. Known source signatures of cached frames prevent renaming and scheduling-only saves from invalidating the picture.

## Automated checks

- `build-final.log` and `build-gesture.log`: formatting, debug APK/test APK and Android lint passed. **16 JVM domain tests** passed / remained up to date with unchanged domain sources.
- `instrumentation.log`: **19 Android tests passed**, 48.03s. Includes real WebView/Vue/Canvas/fetch, failure cache retention, scheduling, persistence/remapping, theme/locale, deletion isolation, actual editor preset/save/clipboard actions and three new preview-state regressions.
- The three new regressions cover unchanged preview without publishing, delayed preview versus newer edits and failure retention, and rename/schedule-only save versus visual changes.
- Final gesture-adjusted APK: **19/19 Android tests passed**, 53.456s (`instrumentation-final.log`).

## Upgrade and device flows

- `before-upgrade.json` / `after-upgrade.json`: in-place update from installed 1.0.0 preserved all original project IDs, settings, HTML hashes and widget bindings. Existing successful frames were present. Signing certificate SHA-256 is identical to 1.0.0 (`previous-cert.txt`, `current-cert.txt`). No app data was cleared.
- `manual-ux.log`: create via My widgets, choose the public power template, rename with the real keyboard, Save, Add to home screen and confirm the actual launcher pin dialog. The bound project ID is retained in a local fixture report.
- Actual edge swipe opens the left drawer; Android Back closes it. The initial unrestricted edge attempt conflicted with Android's system Back gesture. The final implementation excludes only a 200dp-high middle-left region, preserving normal system gestures elsewhere; the real-device-input retry passed.
- `native-check.log`, `native-refresh-top.txt`, `after-native-refresh.json`: native refresh advanced the newly placed widget's successful timestamp without opening MainActivity. Tapping its frame then opened exactly its bound project (`tap-project-top.txt`).
- Ukrainian/dark drawer and editor, system pin dialog and keyboard screenshots are local; public example screenshots in `docs/images/` show the final editor and launcher.

## Package and publication

- Gitleaks 8.30.1: zero findings in the public source candidate and extracted APK assets/DEX strings (`secrets-source.json`, `secrets-apk.json`). No credentials, private signing keys, emulator state or local reports are distributed.
- Debug APK SHA-256: `c0aee683fe647f6f23376fe6f94e489e2952f2437bce6b31b3dc0ac046e3a625`.
- Release assets: `Kadr-1.1.0-debug.apk`, `SHA256SUMS`, `LICENSE`, `NOTICE`. Publication and anonymous download verification are recorded after the release is created.

## Limits

Tested on the API35 emulator, not physical devices/OEM launchers. The middle-left swipe area is deliberately bounded to coexist with Android gesture navigation. The API35 1.0.0 offline reboot recovery verification remains applicable to the unchanged recovery/storage/scheduler code; an additional reboot was not performed for this UI release. No Play Store package or Direct Boot support is claimed.
