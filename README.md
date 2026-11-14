# Kadr

Android-віджет з програмованим статичним контентом: HTML + CSS + JavaScript → збережений кадр. Код виконується лише під час оновлення. Vue підтримується всередині сторінки.

Версія 0.1.3, Android 12+. Інтерфейс — Jetpack Compose, оболонка віджета — Jetpack Glance. Мова системна: українська для `uk`, англійська для решти мов; перемикача немає. Назва Kadr не перекладається. Застосунок слідує системній темі, кожен віджет має Auto / Light / Dark.

## Використання

1. Створити проєкт, обрати пресет або вставити власну HTML-сторінку. Через меню доступні імпорт/експорт HTML, контракт SDK та копіювання інструкцій для ШІ.
2. «Переглянути» будує лише прев’ю чернетки. «Зберегти й побудувати кадр» застосовує зміни до поточного проєкту та вже прив’язаного віджета. Пресет не створює іншого проєкту; для цього є «Новий віджет».
3. Налаштувати оформлення та інтервал, додати на домашній екран. Натискання кадру відкриває редактор, ↻ біля часу запускає фонове оновлення. Час змінюється лише після успішної побудови.

У «Мої віджети» біля кожного проєкту є смітничок. Після підтвердження видаляються код, кадр, розклад і прив’язки. Картка на домашньому екрані переходить до вибору сторінки; її можна прибрати вручну. Видалення останнього проєкту залишає порожній список, новий автоматично не створюється.

Контент виконується під час оновлення й завершується викликом `Kadr.ready()`. Після кадру WebView знищується. HTML-кнопки не інтерактивні у статичному bitmap. Мережеві сторінки використовують HTTPS і звичайні правила CORS. Vue включений локально. Канонічний [SDK-контракт](app/src/main/assets/contracts/sdk-uk.txt) та [ШІ-інструкції](app/src/main/assets/contracts/ai-uk.txt) мають англійські відповідники в тому самому каталозі.

## Оновлення і збереження

WorkManager: вручну або кожні 15/30/60/180/360/1440 хвилин, неточно за правилами Android. Ручне оновлення expedited з fallback; Wi-Fi/battery обмеження розкладу його не блокують. Для API потрібна доступна мережа. Повністю локальна сторінка може додати `<meta name="kadr-network" content="none">`, щоб будуватися офлайн.

Код, прив’язки та останній успішний PNG зберігаються у приватному постійному сховищі. Збої JavaScript, мережі й timeout не замінюють останній кадр. Після звичайного reboot/unlock кадр відновлюється без відкриття редактора. Після системного Force stop Android може блокувати фонову роботу до ручного запуску; Direct Boot до першого розблокування не входить у цю версію.

## Збірка і перевірки

```bash
. scripts/env.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --console=plain --no-daemon --max-workers=1
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r com.thebifel.kadr.test/androidx.test.runner.AndroidJUnitRunner
```

Toolchain: JDK17, Gradle9.3.1, AGP9.1.0, compile/target36, min31. `scripts/env.sh` використовує наявний локальний SDK/JDK. Наявний AVD не очищувати. [Рішення про background renderer і AndroidX Web](specs/001-programmable-widget/adr-webcontent.md).

Spec-driven development: [специфікація](specs/001-programmable-widget/spec.md), [план](specs/001-programmable-widget/plan.md), [задачі](specs/001-programmable-widget/tasks.md), [результати перевірок](specs/001-programmable-widget/verification.md). Логи, знімки стану й скріншоти збережені локально в `artifacts/` (ігноруються Git). Перевірка API35 x86_64 не доводить поведінку всіх фізичних пристроїв/OEM.

Канонічний код: `/home/aiagent/kadr`, CT107 ai-agent-hub на Proxmox. Попередній HTML-мокап — лише дизайн-референс: `/home/aiagent/workspace/tasks/web/2026-11-13-html-widget/dist/index.html`.
