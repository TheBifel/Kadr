# План реалізації

1. Специфікація й acceptance matrix до реалізації.
2. Kotlin + Jetpack Compose UI, AGP 9.1.0 / Gradle 9.3.1 з наявного toolchain. WorkManager для persistent work.
3. Репозиторій проєктів, binding та атомарних кадрів; версії конфігурації й mutex.
4. Software WebView на main looper поза Activity, обмежений SDK, явна готовність і завершення процесу рендерингу. Перевірити найризикованіший background render instrumentation-тестом.
5. Jetpack Glance Image + native overlay часу й оновлення, конфігурація та pin, resize/restore/boot.
6. Український редактор, прев’ю, HTML/CSS/JS, оформлення й розклад; starter pages, bundled Vue.
7. Unit/instrumentation: fetch fixture, Vue, різні кадри, помилки/timeout, bindings, version mismatch, worker.
8. Емулятор: launcher pin, refresh, OS job, кілька віджетів, справжній reboot. Скріншоти кожного значущого етапу.
9. Фіксація перевірок, APK, README та канонічного посилання в knowledge.

Build and emulator setup are documented in [Building](../../docs/BUILDING.md). Preserve existing AVD data when testing an upgrade.
