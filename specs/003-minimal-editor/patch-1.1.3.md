# Kadr 1.1.3 — native widget corner slider

Requested 2026-11-15. Package versionName **1.1.3**, versionCode **10**.

The corner setting is now an integer slider from 0 to 48 dp with a live value
and localized accessibility labels. It controls the outer native widget surface,
including the background, cached frame and overlay controls. It does not alter
HTML cards. The existing root Glance cornerRadius clips the complete widget;
the preview uses that same project value for its actual outer surface instead
of presenting it inside a separate fixed-rounded decorative frame.

Radius changes update preview clipping immediately and do not invalidate the
cached HTML bitmap or require another fetch/render. Save persists the radius
and updates the home-screen widget through the existing save/refresh path.
The persisted integer `radius` key and default 24 dp remain; old 0/16/24 values
are valid in the extended 0–48 range. SDK/AI instructions explain native shaping.

Build only the debug APK, with no test suites/emulator runs at the owner's
request. Existing preview-state regression source now expects a 17 dp native
radius edit to retain the current picture. Package evidence follows assembly.

## Package evidence

Debug APK assembly and source formatting completed successfully
(`artifacts/1.1.3/build.log`). Metadata confirms 1.1.3/code 10; signing certificate
matches 1.1.2, and the distribution copy matches the APK digest. Local inspection
of the pinned Glance 1.2.0 implementation confirms its root corner-radius modifier
sets clip-to-outline and the preferred outline radius on API31+. No test suites
or emulator checks were rerun; actual slider/launcher interaction remains
unverified on a device. Source and version tag are updated in Git.

APK SHA-256: `41f841f5578b7b7e0605fa8c912aa587d6afcd6261c7faa90aaf9a05f1b96f8d`.
