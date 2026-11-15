# Kadr 1.1.1 — detailed preview error copying

Version bump and Git update requested on 2026-11-15. Package
`com.thebifel.kadr`, versionName **1.1.1**, versionCode **8**.

## Problem and behavior

Preview failures had no clipboard action and renderer/persistence shortened
messages to 180 characters. A widget could also replace distinct underlying
layout errors with one generic resize message, losing its original call site.

An accessible copy icon now appears beside the warning action whenever the
preview error area has a current error. It works with collapsed details, copies
the current diagnostic text and disappears when the error clears. English and
Ukrainian labels are included. Android clipboard access remains a platform action.

`Kadr.fail(messageOrError, details?)` accepts the original Error and structured
context while remaining compatible with `fail(string)`. Errors/rejections retain
stacks and causes. Kadr appends failure call site, viewport, theme/locale,
controls, overflow/overlap candidates and recent WebView console/network/HTTP
failures. Geometry diagnostics do not collect page HTML or visible content.
Structured sensitive fields are redacted. Native exception stacks and persisted
errors are retained up to 65,536 characters with an explicit marker if truncated.
The compact preview indicator remains unchanged until details are requested.

The AI instructions now ask pages to preserve the original exception and attach
specific layout constraints instead of replacing every failure with one message.
A private owner-supplied card page was updated separately with capacity/header/
card-overflow reasons and actual geometry; its source/configuration is not part
of this public repository. Intentional percentage ellipsis no longer aborts that
page. Genuine insufficient space still fails and retains the previous frame.

## Validation and limits

Debug APK assembly passed (`artifacts/1.1.1/build-diagnostics.log`). No test suites
or Android emulator checks were rerun at the owner's request. Existing regression
sources were adjusted for detailed messages. The earlier copy-only build evidence
is in [the previous follow-up](verification.md#local-follow-up-copy-preview-error--2026-11-14).

For the newly supplied HTML, a one-off browser diagnosis using synthetic data
reproduced the generic resize failure at 320 × 160. The updated page reports
`LAYOUT_CAPACITY`, item count, per-column capacity and the original stack. The
injected diagnostic collector added viewport and geometry context. No real
credentials, financial values or private page source were recorded in public
artifacts. Native integration on a device remains unverified.

## Package

Metadata confirms versionName **1.1.1** and versionCode **8**. Signing certificate
matches the existing 1.1.0 downloadable build. APK/checksums are in ignored
`artifacts/1.1.1/release/`; the owner-requested distribution copy matched byte for
byte. Source and version tag are updated in Git; APKs/private HTML/configuration
are outside Git.

APK SHA-256: `c57e1e78abdde59b675f91212b504a8764b0de98754f4786299984bab6124454`.
