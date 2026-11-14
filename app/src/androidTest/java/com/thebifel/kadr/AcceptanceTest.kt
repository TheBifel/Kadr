package com.thebifel.kadr

import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebResourceResponse
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.thebifel.kadr.data.content.AssetContent
import com.thebifel.kadr.data.local.PreferencesWidgetRepository
import com.thebifel.kadr.data.local.ProjectJson
import com.thebifel.kadr.data.rendering.BitmapFrame
import com.thebifel.kadr.data.rendering.WebViewFrameRenderer
import com.thebifel.kadr.di.appContainer
import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.platform.AndroidEnvironment
import com.thebifel.kadr.platform.LocaleSupport
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AcceptanceTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun hash(bitmap: Bitmap): String {
        val out = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return MessageDigest.getInstance("SHA-256").digest(out.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    }

    private fun html(color: String) =
        """<html><head></head><body style="background:$color;width:100vw;height:100vh"><h1>Реальний кадр</h1><script>Kadr.ready()</script></body></html>"""

    @Test
    fun ac01_cssAndJavascriptRenderWithoutAnyActivity() = runBlocking {
        val renderer = WebViewFrameRenderer(context)
        val red = renderer.renderBitmap(Project(html = html("#ff0000")))
        assertEquals(Color.RED, red.getPixel(red.width / 2, red.height / 2))
        val blue = renderer.renderBitmap(Project(html = html("#0000ff")))
        assertEquals(Color.BLUE, blue.getPixel(blue.width / 2, blue.height / 2))
        assertNotEquals(hash(red), hash(blue))
        red.recycle()
        blue.recycle()
    }

    @Test
    fun ac01_bundledVueActuallyBuildsDom() = runBlocking {
        val source =
            """<html><head><script src="https://kadr.local/vue.global.prod.js"></script></head><body><div id="app"><div :style="{background:color,width:'100vw',height:'100vh'}">{{text}}</div></div><script>Vue.createApp({data:()=>({color:'#00ff00',text:'Vue працює'})}).mount('#app');Vue.nextTick(()=>Kadr.ready())</script></body></html>"""
        val bitmap = WebViewFrameRenderer(context).renderBitmap(Project(html = source))
        assertEquals(Color.GREEN, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
        bitmap.recycle()
    }

    @Test
    fun ac01_canvasBuildsProgrammableGraphic() = runBlocking {
        val source =
            """<html><head></head><body><canvas id="c" width="320" height="160"></canvas><script>const x=document.querySelector('#c').getContext('2d');x.fillStyle='#ff00ff';x.fillRect(0,0,320,160);Kadr.ready()</script></body></html>"""
        val bitmap = WebViewFrameRenderer(context).renderBitmap(Project(html = source))
        assertEquals(Color.MAGENTA, bitmap.getPixel(bitmap.width / 4, bitmap.height / 4))
        bitmap.recycle()
    }

    @Test
    fun ac02_fetchReceivesChangedHttpResponse() = runBlocking {
        var color = "#ff0000"
        val renderer =
            WebViewFrameRenderer(context) { uri ->
                if (uri.host == "fixture.test")
                    WebResourceResponse(
                        "application/json",
                        "UTF-8",
                        200,
                        "OK",
                        mapOf("Access-Control-Allow-Origin" to "*"),
                        ByteArrayInputStream("{\"color\":\"$color\"}".toByteArray()),
                    )
                else null
            }
        val source =
            """<html><head></head><body style="width:100vw;height:100vh"><script>(async()=>{const r=await fetch('https://fixture.test/data');if(!r.ok)throw Error('HTTP '+r.status);const d=await r.json();document.body.style.background=d.color;await Kadr.ready()})().catch(e=>Kadr.fail(e.message))</script></body></html>"""
        val a = renderer.renderBitmap(Project(html = source))
        color = "#0000ff"
        val b = renderer.renderBitmap(Project(html = source))
        assertEquals(Color.RED, a.getPixel(a.width / 2, a.height / 2))
        assertEquals(Color.BLUE, b.getPixel(b.width / 2, b.height / 2))
        assertNotEquals(hash(a), hash(b))
        a.recycle()
        b.recycle()
    }

    @Test
    fun ac03_errorsTimeoutAndFetchFailureKeepLastSuccessfulFrame() = runBlocking {
        val repo = PreferencesWidgetRepository(context)
        val p = Project(id = "test-" + UUID.randomUUID(), html = html("#00ff00"))
        repo.save(p)
        val bitmap = WebViewFrameRenderer(context).renderBitmap(p)
        assertTrue(repo.publish(p, BitmapFrame(bitmap)))
        bitmap.recycle()
        val before = repo.frameFile(p.id)!!.readBytes()
        val success = repo.result(p.id).success
        val sources =
            listOf(
                "<script>throw Error('JS failure')</script>",
                "<script>fetch('https://fixture.test/error').then(r=>{if(!r.ok)throw Error('fetch failure');Kadr.ready()}).catch(e=>Kadr.fail(e.message))</script>",
                "<p>No ready</p>",
            )
        try {
            for (source in sources) {
                var failed = false
                try {
                    WebViewFrameRenderer(context) { uri ->
                            if (uri.host == "fixture.test")
                                WebResourceResponse(
                                    "text/plain",
                                    "UTF-8",
                                    503,
                                    "Unavailable",
                                    mapOf("Access-Control-Allow-Origin" to "*"),
                                    ByteArrayInputStream(ByteArray(0)),
                                )
                            else null
                        }
                        .renderBitmap(p.copy(html = source), timeoutMs = 1500)
                } catch (e: Exception) {
                    failed = true
                    repo.fail(p.id, e.message ?: "error")
                }
                assertTrue("Expected failure", failed)
                assertArrayEquals(before, repo.frameFile(p.id)!!.readBytes())
                assertEquals(success, repo.result(p.id).success)
            }
        } finally {
            repo.delete(p.id)
        }
    }

    @Test
    fun ac05_projectsAndBindingsPersistAndDoNotOverwriteEachOther() {
        val a =
            Project(
                id = "test-" + UUID.randomUUID(),
                name = "Перший",
                html = AssetContent(context).page("power"),
                dark = true,
                autoTheme = false,
                radius = 16,
                interval = 60,
            )
        val b =
            Project(
                id = "test-" + UUID.randomUUID(),
                name = "Другий",
                html = AssetContent(context).page("blank"),
                interval = 0,
            )
        val repo = PreferencesWidgetRepository(context)
        repo.save(a)
        repo.save(b)
        try {
            val reopened = PreferencesWidgetRepository(context)
            assertEquals(a, reopened.get(a.id))
            assertEquals(b, reopened.get(b.id))
            assertNotEquals(a.html, b.html)
        } finally {
            repo.delete(a.id)
            repo.delete(b.id)
        }
    }

    @Test
    fun ac08_restoreRebindsIdsAndDeletionPreservesProject() {
        val p =
            Project(id = "test-" + UUID.randomUUID(), html = AssetContent(context).page("blank"))
        val repo = PreferencesWidgetRepository(context)
        repo.save(p)
        repo.bind(90001, p.id)
        try {
            repo.remap(intArrayOf(90001), intArrayOf(90002))
            assertNull(repo.binding(90001))
            assertEquals(p.id, repo.binding(90002))
            repo.unbind(90002)
            assertNotNull(repo.get(p.id))
        } finally {
            repo.unbind(90001)
            repo.unbind(90002)
            repo.delete(p.id)
        }
    }

    @Test
    fun ac09_staleRevisionDoesNotPublish() = runBlocking {
        val repo = PreferencesWidgetRepository(context)
        val p = Project(id = "test-" + UUID.randomUUID(), html = html("#ff0000"))
        repo.save(p)
        val bitmap = WebViewFrameRenderer(context).renderBitmap(p)
        try {
            repo.save(p.copy(revision = 2))
            assertFalse(repo.publish(p, BitmapFrame(bitmap)))
            assertEquals(0L, repo.result(p.id).success)
        } finally {
            bitmap.recycle()
            repo.delete(p.id)
        }
    }

    @Test
    fun ac06_workManagerActuallyRendersAndSchedules() {
        val repo = PreferencesWidgetRepository(context)
        val p =
            Project(
                id = "test-" + UUID.randomUUID(),
                html = html("#0000ff"),
                interval = 15,
                battery = false,
            )
        repo.save(p)
        try {
            context.appContainer.scheduler.configure(p)
            val deadline = System.currentTimeMillis() + 30_000
            while (repo.result(p.id).success == 0L && System.currentTimeMillis() < deadline) Thread
                .sleep(200)
            assertTrue(repo.result(p.id).error, repo.result(p.id).success > 0)
            assertFalse(
                WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork("periodic.${p.id}")
                    .get()
                    .isEmpty()
            )
        } finally {
            WorkManager.getInstance(context).cancelUniqueWork("periodic.${p.id}").result.get()
            WorkManager.getInstance(context).cancelUniqueWork("manual.${p.id}").result.get()
            repo.delete(p.id)
        }
    }

    @Test
    fun ac10_systemLanguageAndContractsKeepKadrUntranslated() {
        for (language in listOf("en", "uk", "fr")) {
            val configuration = android.content.res.Configuration(context.resources.configuration)
            configuration.setLocales(android.os.LocaleList.forLanguageTags(language))
            val localized =
                LocaleSupport.localized(context.createConfigurationContext(configuration))
            assertEquals(if (language == "uk") "uk" else "en", LocaleSupport.language(localized))
            assertEquals("Kadr", localized.getString(R.string.app_name))
            val contract = AssetContent(localized).contract()
            assertTrue(contract.contains("Kadr.ready()"))
            assertTrue(contract.contains("Kadr.locale"))
            val english =
                context.assets.open("contracts/sdk-en.txt").bufferedReader().use { it.readText() }
            val instructions = AssetContent(localized).aiInstructions()
            assertTrue(instructions.startsWith("Create a page for Kadr"))
            assertTrue(instructions.contains(english))
            assertTrue(instructions.contains("bottom-right"))
            assertEquals(AssetContent(context).aiInstructions(), instructions)
            assertEquals(
                if (language == "uk") "Копіювати ШІ інструкції" else "Copy AI instructions",
                localized.getString(R.string.copy_ai),
            )
        }
    }

    @Test
    fun ac11_autoThemeUsesTheSystemNightConfiguration() = runBlocking {
        val project =
            Project(
                html =
                    "<html><head></head><body style='width:100vw;height:100vh'><script>Kadr.ready()</script></body></html>",
                autoTheme = true,
            )
        for (dark in listOf(false, true)) {
            val configuration = android.content.res.Configuration(context.resources.configuration)
            configuration.uiMode =
                (configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    (if (dark) android.content.res.Configuration.UI_MODE_NIGHT_YES
                    else android.content.res.Configuration.UI_MODE_NIGHT_NO)
            val themed = context.createConfigurationContext(configuration)
            assertEquals(dark, AndroidEnvironment(themed).current(project).dark)
            val bitmap = WebViewFrameRenderer(themed).renderBitmap(project)
            assertEquals(
                Color.parseColor(if (dark) "#222b1b" else "#f2f4e9"),
                bitmap.getPixel(bitmap.width / 2, bitmap.height / 2),
            )
            bitmap.recycle()
        }
    }

    @Test
    fun overlayContractUsesFullViewportWithoutInjectedPadding() = runBlocking {
        val source =
            """<html><head></head><body style="width:100vw;height:100vh;background:#ff0000"><script>
          const css=getComputedStyle(document.documentElement);
          const valid=Kadr.width===320 && Kadr.height===220 && Kadr.controls.width===144 && Kadr.controls.height===56 && css.getPropertyValue('--kadr-controls-width').trim()==='144px' && css.getPropertyValue('--kadr-controls-height').trim()==='56px' && getComputedStyle(document.body).paddingBottom==='0px';
          document.body.style.background=valid?'#0000ff':'#ff0000';Kadr.ready();
        </script></body></html>"""
        val bitmap = WebViewFrameRenderer(context).renderBitmap(Project(html = source), 320, 220)
        assertEquals(Color.BLUE, bitmap.getPixel(0, bitmap.height - 1))
        assertEquals(Color.BLUE, bitmap.getPixel(bitmap.width - 1, bitmap.height - 1))
        bitmap.recycle()
    }

    @Test
    fun ac12_refreshPendingDoesNotTouchTheCachedFrame() = runBlocking {
        val repo = PreferencesWidgetRepository(context)
        val p = Project(id = "test-" + UUID.randomUUID(), html = html("#ff0000"))
        repo.save(p)
        val bitmap = WebViewFrameRenderer(context).renderBitmap(p)
        repo.publish(p, BitmapFrame(bitmap))
        bitmap.recycle()
        try {
            val before = repo.frameFile(p.id)!!.readBytes()
            val time = repo.result(p.id).success
            repo.markPending(p.id)
            assertTrue(repo.result(p.id).pending > 0)
            assertEquals(time, repo.result(p.id).success)
            assertArrayEquals(before, repo.frameFile(p.id)!!.readBytes())
            repo.fail(p.id, "failure")
            assertEquals(0L, repo.result(p.id).pending)
            assertEquals(time, repo.result(p.id).success)
            val old =
                ProjectJson.encode(p.copy(dark = true, autoTheme = false)).apply {
                    remove("autoTheme")
                }
            val migrated = ProjectJson.decode(old)
            assertFalse(migrated.autoTheme)
            assertTrue(migrated.dark)
        } finally {
            repo.delete(p.id)
        }
    }

    @Test
    fun deletingTheLastProjectKeepsTheRepositoryEmptyAfterInitialization() {
        val prefix = "test-empty-" + UUID.randomUUID()
        val isolated =
            object : android.content.ContextWrapper(context) {
                override fun getSharedPreferences(name: String, mode: Int) =
                    super.getSharedPreferences("$prefix-$name", mode)
            }
        val repo = PreferencesWidgetRepository(isolated)
        try {
            repo.initialize { AssetContent(isolated).starter() }
            assertEquals(1, repo.all().size)
            repo.delete(repo.all().single().id)
            val reopened = PreferencesWidgetRepository(isolated)
            reopened.initialize { AssetContent(isolated).starter() }
            assertTrue(reopened.all().isEmpty())
            assertTrue(reopened.bindings().isEmpty())
        } finally {
            isolated.getSharedPreferences("kadr", 0).edit().clear().commit()
        }
    }
}
