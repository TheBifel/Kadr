# Перевірки Kadr

Середовище: існуючий Android AVD, Android API35 x86_64, launcher Pixel. Дані AVD не очищалися. Логи й знімки нижче — локальні файли в `artifacts/`, поза Git.

## 0.1.2 — 2026-11-13

- Збірка APK і androidTest, lint без помилок: `build-final-network.log`. Чотири unit-тести пройшли.
- Повний instrumentation запуск: `instrumentation-final.log`, **OK (13 tests)**. Перевірено actual HTML/CSS/JS, bundled Vue, Canvas, async fetch зі змінною HTTP fixture, збереження PNG/часу після помилок і timeout, незалежність проєктів, restore remap, заборону stale publish, виконання періодичного WorkManager, en/uk та fallback en, Auto light/dark, pending/cache, реальні натискання в редакторі й clipboard.
- Пресет → прев’ю → збереження на справжньому домашньому віджеті: `before-real-preset.json`, `real-preset-saved.json`. ID проєкту й binding збережені, код і кадр змінилися лише після збереження.
- Натискання native ↻ без запуску Activity: `before-final-native-refresh.json`, `after-final-native-refresh.json`, скріншот `screenshots/08-native-refresh-012.png`.
- Реальне перемикання системної теми: `system-light.json`, `system-dark.json`, скріншоти `09-system-light-widget.png`, `10-system-dark-widget.png`. Український UI: `11-ukrainian-editor.png`; тестовий override мови згодом прибрано.
- Реальний `adb reboot` із вимкненими Wi-Fi/data: `before-offline-reboot.json` → `after-offline-reboot-settled.json`. Код, налаштування й binding збережені; локальна сторінка оновилася офлайн (lastSuccess 23:53:39.909 → 23:55:22.601 UTC). API PNG/hash і час останнього успіху незмінні. `reboot-top-activity.txt` підтверджує launcher у foreground; редактор для відновлення не відкривався. OS jobs відновлені: `jobs-after-reboot-settled.txt`. Скріншот `13-offline-reboot-widget.png`. Мережу після перевірки увімкнуто.
- Реальний resize: `before-resize.json` → `after-resize.json`, висота кадру 370 → 479 px, код і binding незмінні. Скріншот `15-resized-widget.png`. Два домашні віджети перевірялися раніше (`two-widgets.json`); restore remap перевірений окремим тестом.

Межі: AC06 підтверджений реальним виконанням періодичного WorkManager й наявністю OS jobs; окремого примусового запуску через `cmd jobscheduler run` не виконано. Відновлення хмарного backup на іншому пристрої, Direct Boot, поведінка фізичних телефонів/OEM та тривале енергоспоживання не перевірені. Пізніший користувацький обсяг: решту перевірок на власному пристрої користувач бере на себе.

APK `/tank/files/Kadr-0.1.2.apk`, SHA256 `90bf3c31bb9d4ccb8cfd2d061d86b7ffb38b4271dae33fc8c4bed3c8088ee2ac` — звірено після копіювання.

## 0.1.3 — видалення, 2026-11-14

Вимога FR19: смітничок у кожному рядку, підтвердження/скасування, очищення коду/кадру/bindings/work, вибір іншого проєкту та збереження порожнього списку після видалення останнього.

- `build-013-delete.log`: APK + androidTest + lint, **BUILD SUCCESSFUL**.
- `instrumentation-013-delete.log`: **OK (3 tests)**, 38.684 s. Реальні натискання: скасування зберігає проєкт/binding; підтвердження видаляє потрібний проєкт/PNG/binding і завершує manual/periodic work; інший проєкт і його binding незмінні; активний редактор переходить на збережений проєкт. Пізні `fail`/`publish` не відновлюють видалені дані. В ізольованому репозиторії повторна ініціалізація після видалення останнього не створює стартовий проєкт. Також повторено попередній UI-сценарій preset/save/copy після зміни початкового стану редактора.
- Скріншот меню: `screenshots/16-my-widgets-trash.png`. Домашній placeholder після видалення перевірений кодом оновлення Glance, але в цьому запуску UI-тест використовує ізольовані тестові bindings; поведінку фактичної картки перевіряє користувач.

APK `/tank/files/Kadr-0.1.3.apk`, SHA256 `98b9f4123fa9e65a9a9ada79dc7441f9f6e0666f5331ca6bf1b982c2f1a5c8a8` — звірено після копіювання на NAS.

## 0.1.4 — overlay та англійські ШІ-інструкції, 2026-11-14

- FR20: Glance Box накладає час і ↻ поверх повного кадру; із розміру HTML більше не віднімається 48 dp. Прев’ю використовує той самий принцип. Область 144 × 56 включає крайові відступи й кнопку 48 × 48. Автоматичного padding у renderer немає; нові starter pages самі враховують куток. Наявний код користувача не переписується.
- ШІ-копіювання завжди складається з `ai-en.txt` та `sdk-en.txt`. Кнопка й окреме копіювання контракту локалізовані en/uk. Український AI asset видалено.
- `build-014-overlay.log`: APK + androidTest + lint, **BUILD SUCCESSFUL**.
- `instrumentation-014-overlay.log`: **OK (3 tests)**, 18.446 s. Рендер 320 × 220 CSS px перевіряє повну висоту, `Kadr.controls`, обидві CSS-змінні й нульовий автоматичний padding; синій фон доходить до обох нижніх кутів bitmap. Перевірено однакові англійські ШІ-інструкції для en/uk/fr, локалізований окремий контракт, реальні clipboard/save/preview натискання.
- `overlay-preset-saved.json`: новий приклад явно застосовано до тестового домашнього віджета. Кадр 482 × 480 px. `before-overlay-native-refresh.json` → `after-overlay-native-refresh.json`: tap native ↻ успішно оновлює той самий binding/код; `overlay-refresh-top.txt` підтверджує foreground launcher без Activity. `17-overlay-home.png` і `18-overlay-refreshed.png` у screenshots показують HTML-текст ліворуч від керування.
- `build-014-final.log`: фінальна збірка й lint після вирівнювання часу поруч із кнопкою, **BUILD SUCCESSFUL**. Після встановлення фінального APK й завершення package refresh повторено фактичний tap ↻: `before-overlay-final-refresh.json` → `after-overlay-final-refresh.json`, новий успішний час, ті самі проєкти/bindings, foreground launcher (`overlay-final-refresh-top.txt`). Скріншот `19-overlay-final.png`.

APK `/tank/files/Kadr-0.1.4.apk`, SHA256 `90c46adc5e4af7d8d4f0cf24d9249bfc8333d9407453d8d3bea951326cc745e7`.
