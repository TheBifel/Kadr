# Kadr 1.1.4 — apply corner radius to the launcher frame

Requested 2026-11-15 after 1.1.3 changed preview corners but not the home-screen
widget, including when the owner opened the bound widget and pressed Save.
Package versionName **1.1.4**, versionCode **11**.

The launcher now receives a display-only ARGB bitmap with the selected rounded
alpha mask. It is composed from the original square cache, current widget bounds
and saved radius; placeholder/background pixels use the same shape. The outer
Glance surface is transparent and declares a square clipped background so an
additional default outline does not cover the transparent corners. Native time
and refresh controls remain separate and interactive in their reserved area.

The rounded frame is memoized by source file, project, radius, bounds and theme.
The original persisted PNG is not modified, allowing both larger and smaller
radii immediately from cache without fetching HTML. Display masking uses Glance's
current layout size, preserving the selected radius in dp after bitmap scaling.
Source content keeps its aspect ratio inside the shaped background, and the
completed surface fills the widget.
Storage keys, bindings and the 0–48 dp slider are unchanged.

This replaces the previous reliance on the Glance parent outline alone. The
exact launcher-specific cause on the owner's device is not confirmed. AOSP
[launcher corner handling](https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/src/com/android/launcher3/widget/BaseLauncherAppWidgetHostView.java)
provides context for background/outline opt-out; the new radius is visible in
image alpha rather than depending solely on preferred-outline propagation.

Build only the APK; no test-suite or emulator runs at the owner's request.
Actual interaction on the owner's launcher remains unverified. Package evidence
follows assembly.

## Package evidence

APK assembly and Kotlin formatting completed successfully
(`artifacts/1.1.4/build.log`). Packaged metadata confirms 1.1.4/code 11, and the
signing certificate matches 1.1.3. The distribution copy was checked against the
local APK SHA-256. No test suites or emulator checks were run. Source and the
version tag are updated in Git.

APK SHA-256: `f710f69fee77becff1ab3f7cafa6b855dd87235eff2e00532f080b418ebf7546`.
