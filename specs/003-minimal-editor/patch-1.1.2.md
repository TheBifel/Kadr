# Kadr 1.1.2 — adaptive-layout AI instructions

Requested 2026-11-15 after a card page failed before creating its items because
fixed minimum heights/gaps left fewer slots than requested categories. Package
versionName **1.1.2**, versionCode **9**.

The English AI prompt now explicitly requires filtering before packing,
measured CSS viewport/header geometry, density-rounding tolerance, per-region
capacity with the native corner reserved, and progressive card-to-row fallback.
It distinguishes intentional ellipsis from required-content overflow and
requires specific constraint diagnostics with the original Error/cause.

`AssetContent.aiInstructions()` still combines the canonical English prompt and
English SDK in either app language. This patch changes prompt assets and package
metadata; the renderer, diagnostics and clipboard behavior remain as in 1.1.1.

Build only the debug APK. No test suites or emulator runs at the owner's request.
Package/distribution evidence is recorded after assembly.

## Package evidence

Debug APK assembly passed (`artifacts/1.1.2/build.log`). Package metadata is
1.1.2/code 9; the packaged AI prompt exactly matches its canonical source.
Signing certificate matches the existing 1.1.0 release, and the distribution
copy's digest matches the built APK. No test suites/emulator runs were repeated.
Source/version tag are updated in Git. APK SHA-256: `d09eb3bd1713700d9c9302973a852f2613da5a9d6142501f5346646ecb2248c6`.
