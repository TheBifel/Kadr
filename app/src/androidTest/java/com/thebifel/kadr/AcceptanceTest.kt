package com.thebifel.kadr

import android.graphics.Bitmap
import android.graphics.Color
import android.webkit.WebResourceResponse
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AcceptanceTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private fun hash(bitmap: Bitmap): String { val out=java.io.ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,out);return MessageDigest.getInstance("SHA-256").digest(out.toByteArray()).joinToString(""){"%02x".format(it)} }
    private fun html(color: String)="""<html><head></head><body style="background:$color;width:100vw;height:100vh"><h1>Реальний кадр</h1><script>Kadr.ready()</script></body></html>"""

    @Test fun ac01_cssAndJavascriptRenderWithoutAnyActivity()=runBlocking {
        val renderer=HtmlRenderer(context)
        val red=renderer.render(Project(html=html("#ff0000")))
        assertEquals(Color.RED,red.getPixel(red.width/2,red.height/2))
        val blue=renderer.render(Project(html=html("#0000ff")))
        assertEquals(Color.BLUE,blue.getPixel(blue.width/2,blue.height/2));assertNotEquals(hash(red),hash(blue))
        red.recycle();blue.recycle()
    }
    @Test fun ac01_bundledVueActuallyBuildsDom()=runBlocking {
        val source="""<html><head><script src="https://kadr.local/vue.global.prod.js"></script></head><body><div id="app"><div :style="{background:color,width:'100vw',height:'100vh'}">{{text}}</div></div><script>Vue.createApp({data:()=>({color:'#00ff00',text:'Vue працює'})}).mount('#app');Vue.nextTick(()=>Kadr.ready())</script></body></html>"""
        val bitmap=HtmlRenderer(context).render(Project(html=source));assertEquals(Color.GREEN,bitmap.getPixel(bitmap.width/2,bitmap.height/2));bitmap.recycle()
    }
    @Test fun ac01_canvasBuildsProgrammableGraphic()=runBlocking {
        val source="""<html><head></head><body><canvas id="c" width="320" height="160"></canvas><script>const x=document.querySelector('#c').getContext('2d');x.fillStyle='#ff00ff';x.fillRect(0,0,320,160);Kadr.ready()</script></body></html>"""
        val bitmap=HtmlRenderer(context).render(Project(html=source));assertEquals(Color.MAGENTA,bitmap.getPixel(bitmap.width/4,bitmap.height/4));bitmap.recycle()
    }
    @Test fun ac02_fetchReceivesChangedHttpResponse()=runBlocking {
        var color="#ff0000"
        val renderer=HtmlRenderer(context) { uri -> if(uri.host=="fixture.test")WebResourceResponse("application/json","UTF-8",200,"OK",mapOf("Access-Control-Allow-Origin" to "*"),ByteArrayInputStream("{\"color\":\"$color\"}".toByteArray()))else null }
        val source="""<html><head></head><body style="width:100vw;height:100vh"><script>(async()=>{const r=await fetch('https://fixture.test/data');if(!r.ok)throw Error('HTTP '+r.status);const d=await r.json();document.body.style.background=d.color;await Kadr.ready()})().catch(e=>Kadr.fail(e.message))</script></body></html>"""
        val a=renderer.render(Project(html=source));color="#0000ff";val b=renderer.render(Project(html=source))
        assertEquals(Color.RED,a.getPixel(a.width/2,a.height/2));assertEquals(Color.BLUE,b.getPixel(b.width/2,b.height/2));assertNotEquals(hash(a),hash(b));a.recycle();b.recycle()
    }
    @Test fun ac03_errorsTimeoutAndFetchFailureKeepLastSuccessfulFrame()=runBlocking {
        val repo=Repository(context);val p=Project(id="test-"+UUID.randomUUID(),html=html("#00ff00"));repo.save(p)
        val bitmap=HtmlRenderer(context).render(p);assertTrue(repo.publish(p,bitmap));bitmap.recycle()
        val before=repo.frameFile(p.id)!!.readBytes();val success=repo.result(p.id).success
        val sources=listOf("<script>throw Error('JS failure')</script>","<script>fetch('https://fixture.test/error').then(r=>{if(!r.ok)throw Error('fetch failure');Kadr.ready()}).catch(e=>Kadr.fail(e.message))</script>","<p>No ready</p>")
        try { for(source in sources) {
            var failed=false
            try { HtmlRenderer(context) { uri -> if(uri.host=="fixture.test")WebResourceResponse("text/plain","UTF-8",503,"Unavailable",mapOf("Access-Control-Allow-Origin" to "*"),ByteArrayInputStream(ByteArray(0)))else null }.render(p.copy(html=source),timeoutMs=1500) }catch(e: Exception){failed=true;repo.fail(p.id,e.message ?: "error")}
            assertTrue("Expected failure",failed);assertArrayEquals(before,repo.frameFile(p.id)!!.readBytes());assertEquals(success,repo.result(p.id).success)
        } }finally{repo.delete(p.id)}
    }
    @Test fun ac05_projectsAndBindingsPersistAndDoNotOverwriteEachOther() {
        val a=Project(id="test-"+UUID.randomUUID(),name="Перший",dark=true,radius=16,interval=60)
        val b=Project(id="test-"+UUID.randomUUID(),name="Другий",html=Templates.blank,interval=0)
        val repo=Repository(context);repo.save(a);repo.save(b)
        try { val reopened=Repository(context);assertEquals(a,reopened.get(a.id));assertEquals(b,reopened.get(b.id));assertNotEquals(a.html,b.html) }finally{repo.delete(a.id);repo.delete(b.id)}
    }
    @Test fun ac08_restoreRebindsIdsAndDeletionPreservesProject() {
        val p=Project(id="test-"+UUID.randomUUID());val repo=Repository(context);repo.save(p);repo.bind(90001,p.id)
        try {repo.remap(intArrayOf(90001),intArrayOf(90002));assertNull(repo.binding(90001));assertEquals(p.id,repo.binding(90002));repo.unbind(90002);assertNotNull(repo.get(p.id))}finally{repo.unbind(90001);repo.unbind(90002);repo.delete(p.id)}
    }
    @Test fun ac09_staleRevisionDoesNotPublish()=runBlocking {
        val repo=Repository(context);val p=Project(id="test-"+UUID.randomUUID(),html=html("#ff0000"));repo.save(p)
        val bitmap=HtmlRenderer(context).render(p)
        try {repo.save(p.copy(revision=2));assertFalse(repo.publish(p,bitmap));assertEquals(0L,repo.result(p.id).success)}finally{bitmap.recycle();repo.delete(p.id)}
    }
    @Test fun ac06_workManagerActuallyRendersAndSchedules() {
        val repo=Repository(context);val p=Project(id="test-"+UUID.randomUUID(),html=html("#0000ff"),interval=15,battery=false);repo.save(p)
        try {
            Schedule.configure(context,p)
            val deadline=System.currentTimeMillis()+30_000
            while(repo.result(p.id).success==0L && System.currentTimeMillis()<deadline)Thread.sleep(200)
            assertTrue(repo.result(p.id).error,repo.result(p.id).success>0)
            assertFalse(WorkManager.getInstance(context).getWorkInfosForUniqueWork("periodic.${p.id}").get().isEmpty())
        }finally{WorkManager.getInstance(context).cancelUniqueWork("periodic.${p.id}").result.get();WorkManager.getInstance(context).cancelUniqueWork("manual.${p.id}").result.get();repo.delete(p.id)}
    }
}
