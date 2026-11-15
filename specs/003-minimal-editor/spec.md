# 003 — Minimal widget editor, Kadr 1.1.0

Approved: 2026-11-14. Release requested by the user: **1.1.0**.

This specification supersedes the editor presentation in 001 FR02/FR08/FR12/FR13. Rendering, scheduling, storage keys, SDK and home screen binding contracts remain compatible with 1.0.0.

## Approved interface

- One scrolling editor, with a fixed bottom action. Project title in the header; hamburger on the left and the existing widget grid button on the right. No overflow or global settings menu.
- The hamburger is a real left navigation drawer, opened by its button or a swipe from the left edge, closed by Back, scrim or a closing swipe. Templates, English-only AI instructions and SDK reference live here, including their detail views. Editing/selecting code must not accidentally open the drawer. On gesture-navigation devices, reserve a 200dp-high region at the middle of the left edge with Android system gesture exclusion, leaving system Back available elsewhere.
- The preview always shows the native timestamp and refresh icon together over its lower-right corner, matching the home screen widget. This button previews the current code without saving or updating the home screen widget, including when there are no unsaved changes. Successful preview updates its timestamp; failure preserves the previous image and successful timestamp.
- No visible “Draft”, build terminology, repeated preview button, marketing copy, Vue hints or permanent explanatory notices. When HTML/theme/corner changes are not represented by the preview, show “Preview not updated” / “Прев’ю не оновлено”. Successful matching preview or published frame clears this status. Renaming and schedule-only changes do not make the picture stale.
- Collapsible code section with **Edit / Import / Export** on the same row. Import opens the system picker and expands code. Export uses the current code, including unsaved edits.
- Collapsible **Widget settings** on this same screen: the current project's name, system/light/dark theme, corner radius, update interval, Wi-Fi and battery options. Rename exists only here. Keep all existing interval choices and platform limits; detailed explanations belong to the drawer reference.
- One **Save / Зберегти** action, disabled without unsaved changes or during a save. It saves configuration and schedules the existing widget's update. After saving a project without a home screen binding, the primary action becomes **Add to home screen**. System pin confirmation remains Android-owned. Already bound projects can add another placement from the widget list; existing independent-copy binding behavior is preserved.
- Widget list contains switching, New widget, per-project trash and placement actions. Delete confirmation names the project; bound projects receive one concise warning that their home screen cards become “Choose a page”. Cancel, deletion isolation and empty-state creation remain supported.
- When a preview error is available, show an accessible copy-error icon next to **!** immediately, even while raw details are collapsed. Copy exactly the current displayed error to the Android clipboard; hide the copy action when the error clears.
- Error diagnostics retain the original message/stack/cause and optional structured details. Kadr adds viewport, element geometry and recent WebView failures; copy includes the retained diagnostic text rather than a 180-character summary. HTML should pass the original Error and describe the particular layout constraint that failed. Diagnostics are bounded at 65,536 characters with an explicit truncation marker.
- Failures use a compact accessible **!** indicator. Raw details expand only when the user requests them. No automatic error dialog or explanatory toast for preview, saving or file operations. Last good frames remain intact. Clipboard confirmation may use the platform clipboard affordance.
- Full Ukrainian and English strings, system language and system app theme. Minimum 48dp icon targets; icon-only actions have accessible names. Narrow screens and IME must retain access to Save and file actions.

## Acceptance

1. On API35, edit/import/template -> preview -> Save updates the same bound project; preview alone changes neither stored HTML nor published frame. Preview refresh on unchanged code does not mark the project dirty.
2. Timestamp remains visible beside the single overlay refresh icon. Stale status tracks visual inputs, clears only for a matching successful image, and survives failed/stale preview attempts.
3. Drawer opens by tap and edge swipe, closes with Back/swipe/scrim; code editing/selection and vertical scrolling work. Drawer reference/templates/AI instructions are usable without a second overflow menu.
4. Create via widget list -> save -> Add to home screen; native launcher placement, native refresh and tap-to-project still work. Bound project deletion warns correctly and preserves other projects.
5. JVM tests, relevant Android instrumentation, formatting and lint pass. Upgrade from signed 1.0.0 preserves projects/bindings/configuration/cache. Capture public demo screens in both languages/themes.
6. Publish signed-compatible debug APK and SHA-256 as GitHub release **v1.1.0**, plus local distribution copy. Verify downloadable bytes and CI. Record actual evidence and any limits in verification.md.
