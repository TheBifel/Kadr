# 002 — Architecture and Kadr 1.0.0

Date: 2026-11-14. User-approved scope: refactor the working app, document its purpose and architecture, publish a public GitHub repository and a 1.0.0 release with a built debug APK. Google Play is outside this request.

## Requirements

- A pure Kotlin/JVM `:domain` module owns models, policies, repository/scheduler/renderer/widget ports and shared use cases. It must not depend on Android, Compose, WebView, JSON or WorkManager.
- Android `:app` provides data adapters, platform integrations and presentation. Dependencies point toward domain; a single application container wires implementations. Activities and receivers delegate to use cases. Compose renders immutable ViewModel state and emits events; composables do not write storage or schedule work.
- Split editor, reusable components, dialogs, theme, widget configuration, widget rendering, receivers, scheduler, WebView rendering and persistence into focused files. Format code consistently and name responsibilities explicitly.
- Preserve package/application ID, persisted preference keys/JSON layout, frame files, widget receiver/activity component names, WorkManager worker class name and unique-work names. An upgrade must preserve existing projects/bindings/cache and scheduled work.
- Preserve all 001 behavior: draft preview vs saved publish, current-project preset, copy/delete, system theme/language, English-only AI instructions, overlay controls, offline cache, reboot recovery and stale-publication protection.
- `versionName=1.0.0`, increasing versionCode. Build a debug APK; no private signing key enters Git or the APK.
- README explains the problem, examples, user workflow, architecture, build/test/install steps and platform limits. Include an architecture diagram and contribution/build guidance. Build instructions work outside the original host.
- Public GitHub `TheBifel/Kadr` and release `v1.0.0` with debug APK, checksum and release notes. Preserve the existing local history if its secret review passes. Inspect current tree, history and packaged assets before pushing. Any needed credentials come from environment variables or GitHub secrets, not committed configuration. Do not publish operational logs, device data, local properties or keystores.

## Acceptance

1. Domain tests run on the JVM without an emulator, exercise save/delete/refresh behavior including stale/deleted projects, failure cache preservation and cancellation.
2. Build, domain/app tests and lint pass; existing instrumentation scenarios remain valid after package/import changes.
3. Upgrade the existing emulator app without clearing data: projects, bindings and last-success frame remain; native refresh works without opening an Activity. Verify reboot after the structural changes.
4. Public repository and release resolve; downloadable APK matches the locally verified checksum. README contains no host-specific setup requirement or secrets.

## Decisions

Use manual constructor injection for this small app, not a DI framework. Keep Android adapters in one app module while enforcing the inner boundary with a separate JVM module. Do not change the storage engine or add product features during this refactor. Pin existing Android component and Worker names as compatibility entry points.
