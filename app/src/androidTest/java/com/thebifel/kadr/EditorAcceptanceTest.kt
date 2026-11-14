package com.thebifel.kadr

import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.MotionEvent
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EditorAcceptanceTest {
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private fun nodes(): List<AccessibilityNodeInfo> {
        val root=instrumentation.uiAutomation.rootInActiveWindow ?: return emptyList()
        val list=mutableListOf(root);var index=0
        while(index<list.size){val n=list[index++];for(i in 0 until n.childCount)n.getChild(i)?.let { list.add(it) }}
        return list
    }
    private fun click(label: String,scroll: Boolean=true) {
        val deadline=System.currentTimeMillis()+25_000
        while(System.currentTimeMillis()<deadline) {
            val all=nodes()
            val anr=all.firstOrNull { it.text?.contains("isn't responding")==true || it.text?.contains("не відповіда")==true }
            if(anr!=null) {
                check(anr.text?.contains("Kadr",ignoreCase=true)!=true) { "Kadr ANR: ${anr.text}" }
                var close=all.firstOrNull { it.text?.toString()=="Close app" }
                while(close!=null && !close.isClickable)close=close.parent
                close?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(500);continue
            }
            val target=all.firstOrNull { (it.text?.toString()==label || it.contentDescription?.toString()==label) && Rect().also { r -> it.getBoundsInScreen(r) }.let { !it.isEmpty } }
            if(target!=null) {
                val bounds=Rect().also { target.getBoundsInScreen(it) }
                val time=SystemClock.uptimeMillis()
                val down=MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY(),0)
                val up=MotionEvent.obtain(time,time+60,MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY(),0)
                instrumentation.uiAutomation.injectInputEvent(down,true)
                instrumentation.uiAutomation.injectInputEvent(up,true)
                down.recycle();up.recycle();Thread.sleep(400);return
            }
            if(scroll)all.firstOrNull { it.isScrollable && it.actionList.any { a->a.id==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD } }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            Thread.sleep(250)
        }
        java.io.File(context.filesDir,"ui-failure.txt").writeText(nodes().joinToString("\n") { "${it.className} visible=${it.isVisibleToUser} scroll=${it.isScrollable} text=${it.text?.take(160)}" })
        instrumentation.uiAutomation.takeScreenshot()?.let { image -> java.io.File(context.filesDir,"ui-failure.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) };image.recycle() }
        error("UI control not found: $label")
    }
    private fun waitUntil(condition: ()->Boolean) {
        val deadline=System.currentTimeMillis()+25_000
        while(!condition()){check(System.currentTimeMillis()<deadline){"Timed out"};Thread.sleep(200)}
    }
    @Test fun trashCancelsOrDeletesOnlyTheNamedProjectAndUnbindsItsWidgets() {
        val repo=Repository(context)
        val p=Project(id="test-delete-"+UUID.randomUUID(),name="Delete acceptance",html="<html><head><meta name='kadr-network' content='none'></head><body><script>Kadr.ready()</script></body></html>",interval=15)
        val survivor=p.copy(id="test-keep-"+UUID.randomUUID(),name="Keep acceptance",interval=0)
        repo.save(p);repo.save(survivor);repo.bind(90011,p.id);repo.bind(90012,survivor.id)
        val initial=Bitmap.createBitmap(20,10,Bitmap.Config.ARGB_8888)
        repo.publish(p,initial);initial.recycle()
        val file=repo.frameFile(p.id)!!
        Schedule.configure(context,p);Schedule.now(context,p)
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).putExtra("project",p.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
        val c=LocaleSupport.localized(context)
        try {
            instrumentation.waitForIdleSync()
            click(c.getString(R.string.my_widgets)+" ▾",scroll=false)
            click(c.getString(R.string.delete_widget_name,p.name))
            click(c.getString(R.string.cancel),scroll=false)
            assertNotNull(repo.get(p.id));assertEquals(p.id,repo.binding(90011))
            click(c.getString(R.string.delete_widget_name,p.name))
            click(c.getString(R.string.delete_action),scroll=false)
            waitUntil { repo.get(p.id)==null && repo.binding(90011)==null }
            assertEquals(0L,repo.result(p.id).success);assertFalse(file.exists())
            assertEquals(survivor,repo.get(survivor.id));assertEquals(survivor.id,repo.binding(90012))
            val wm=WorkManager.getInstance(context)
            waitUntil { listOf("manual.","periodic.").all { prefix -> wm.getWorkInfosForUniqueWork(prefix+p.id).get().all { it.state.isFinished } } }
            // A late render or error must never recreate the removed frame state.
            repo.fail(p.id,"late failure",p.revision)
            val bitmap=Bitmap.createBitmap(20,10,Bitmap.Config.ARGB_8888)
            assertFalse(repo.publish(p,bitmap));bitmap.recycle()
            assertEquals(FrameState(),repo.result(p.id))
            assertFalse(nodes().any { it.contentDescription?.toString()==c.getString(R.string.delete_widget_name,p.name) })
            click(c.getString(R.string.close),scroll=false)
            assertTrue(nodes().any { it.text?.toString()==repo.all().first().name })
        }finally {
            instrumentation.runOnMainSync { activity.finish() }
            Schedule.delete(context,p.id);Schedule.delete(context,survivor.id)
        }
    }
    @Test fun presetPreviewsThenPublishesToTheSameWidgetAndCopiesInstructions() {
        val repo=Repository(context)
        val p=Project(id="test-ui-"+UUID.randomUUID(),name="Editor acceptance",html="<html><head></head><body><script>Kadr.ready()</script></body></html>",interval=0,autoTheme=false)
        repo.save(p);repo.bind(90010,p.id)
        val initial=Bitmap.createBitmap(20,10,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        repo.publish(p,initial);initial.recycle()
        val before=repo.result(p.id).success;val count=repo.all().size
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).putExtra("project",p.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
        val c=LocaleSupport.localized(context)
        try {
            instrumentation.waitForIdleSync()
            click(c.getString(R.string.content),scroll=false)
            click(c.getString(R.string.choose_template))
            click(c.getString(R.string.preset_blank),scroll=false)
            repeat(4) {
                nodes().firstOrNull { it.isScrollable && it.actionList.any { a->a.id==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD } }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                Thread.sleep(200)
            }
            waitUntil { nodes().any { it.text?.toString()==c.getString(R.string.draft_preview) } }
            assertEquals(p.html,repo.get(p.id)!!.html);assertEquals(before,repo.result(p.id).success)
            click(c.getString(R.string.save_build))
            waitUntil { repo.result(p.id).success>before && repo.result(p.id).revision==repo.get(p.id)!!.revision }
            assertEquals(p.id,repo.binding(90010));assertEquals(count,repo.all().size)
            assertNotEquals(p.html,repo.get(p.id)!!.html)
            click("⋮",scroll=false)
            click(c.getString(R.string.contract_title),scroll=false)
            click(c.getString(R.string.copy_contract),scroll=false)
            val clipboard=context.getSystemService(ClipboardManager::class.java)
            waitUntil { clipboard.primaryClip?.getItemAt(0)?.text?.contains("Kadr.ready()") == true }
            assertEquals(ContractText.contract(c),clipboard.primaryClip!!.getItemAt(0).text.toString())
            click(c.getString(R.string.close),scroll=false)
            click("⋮",scroll=false)
            click(c.getString(R.string.copy_ai),scroll=false)
            waitUntil { clipboard.primaryClip?.getItemAt(0)?.text?.toString()==ContractText.aiInstructions(c) }
        }finally {
            instrumentation.runOnMainSync { activity.finish() }
            repo.unbind(90010)
            WorkManager.getInstance(context).cancelUniqueWork("manual.${p.id}").result.get()
            WorkManager.getInstance(context).cancelUniqueWork("periodic.${p.id}").result.get()
            repo.delete(p.id)
        }
    }
}
