# ADR — androidx.web alpha01 та фоновий кадр

2026-11-13. Переглянуто офіційний реліз і source jar `androidx.web:web:1.0.0-alpha01` за посиланням користувача.

WebContent відокремлює стан рушія від View/Activity. Підтримка залежить від `WebFeature.WEB_CONTENT`. `attach(context, factory)` потребує Activity context, `DetachedWebContentView` має internal constructor, а load/evaluate API залишаються на View. У першій alpha немає публічного фабричного шляху для створення нової detached-сесії, завантаження сторінки й отримання кадру в cold background worker без Activity. Це висновок із публічного API та вихідного коду, а не твердження про всі можливості майбутніх версій.

Рішення MVP: одноразовий Android WebView з application context на main looper усередині обмеженого WorkManager-завдання. Життєвий цикл закінчується після ready/capture/error/timeout. Не тримати engine постійно й не відкривати Activity з boot receiver. Такий шлях перевірити на реальному емуляторі без відкриття Activity. Alpha-залежність, яка не дає потрібного шляху, до runtime не додається. Renderer відокремлений від UI й scheduler; його можна замінити після появи відповідного API.

Інтерфейс застосунку — Jetpack Compose. Android widget host отримує RemoteViews з bitmap та native footer; це обмеження API домашнього екрана.

Джерела:
- https://developer.android.com/jetpack/androidx/releases/web
- https://developer.android.com/reference/kotlin/androidx/web/WebContent
- https://developer.android.com/reference/kotlin/androidx/web/DetachedWebContentView
- https://developer.android.com/reference/kotlin/androidx/web/WebContentView
- https://dl.google.com/dl/android/maven2/androidx/web/web/1.0.0-alpha01/web-1.0.0-alpha01-sources.jar
