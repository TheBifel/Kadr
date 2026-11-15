# Kadr 1.1.5 — native surface clipping, original frame

Requested 2026-11-15 after the owner reported poor frame quality in 1.1.4 and
showed a Samsung S21 home-screen widget with rounded corners at a saved 0 dp.
Package versionName **1.1.5**, versionCode **12**.

Acceptance: round the native view containing both the image and controls.
Pass the original decoded frame directly to the image view, without creating a
second bitmap, applying an alpha mask or resizing pixels for the corner setting.
Keep the existing persisted PNG, radius, widget binding and native actions.

Use an explicit XML FrameLayout surface embedded through AndroidRemoteViews.
Its stable `@android:id/background` identifies the actual clipping container;
`clipToOutline` is true from inflation. Set its outline radius in dp on every
composition, including an explicit zero for square corners. All image and control
views are children of that surface. Do not also label a Glance wrapper as the
widget background. Restore the original cached bitmap and ContentScale.Fit.

The XML surface follows Android's background/outline opt-out contract; it does
not depend on altering the captured content. Samsung launcher enforcement is
still device-specific: this change must be confirmed on the owner's phone.
Do not treat APK assembly as evidence of correct Samsung launcher behavior.

Build the APK and verify packaging/signing/distribution only. No test-suite or
emulator runs at the owner's request.

## Package evidence

APK assembly and Kotlin formatting passed (`artifacts/1.1.5/build.log`). Packaged
metadata confirms 1.1.5/code 12 and the signing certificate matches 1.1.4. APK XML
inspection confirms the native FrameLayout has `@android:id/background`, bounds
outline and `clipToOutline=true`. The distribution copy matches the local APK
digest. No emulator or test suites were run; Samsung S21 launcher behavior and
perceived image quality remain unverified on the owner's device.

APK SHA-256: `95c147ebda0d8e2f0319257fde1f596dc931b3c6bbe144179a4a87b273ee2344`.

References: [Android widget corners](https://developer.android.com/develop/ui/views/appwidgets/overview#rounded-corner),
[RemoteViews outline radius](https://developer.android.com/reference/android/widget/RemoteViews#setViewOutlinePreferredRadius(int,float,int)).
