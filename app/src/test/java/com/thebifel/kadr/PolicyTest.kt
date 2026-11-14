package com.thebifel.kadr
import org.junit.Assert.*
import org.junit.Test
class PolicyTest {
    @Test fun scheduleCannotPromiseUnsupportedIntervals() { assertFalse(Policy.validInterval(1));assertFalse(Policy.validInterval(14));assertTrue(Policy.validInterval(0));assertTrue(Policy.validInterval(15));assertTrue(Policy.validInterval(1440)) }
    @Test fun largeWidgetHasBoundedBitmapMemory() { for(w in listOf(-100,140,320,640,9999))for(h in listOf(-100,70,160,640,9999)){val (rw,rh)=Policy.renderSize(w,h);assertTrue(rw in 1..720);assertTrue(rh in 1..480);assertTrue(rw*rh*4<=1_382_400)} }
    @Test fun staleRenderCannotReplaceNewEditorRevision() { assertTrue(Policy.canPublish(8,8));assertFalse(Policy.canPublish(8,9));assertFalse(Policy.canPublish(8,-1)) }
    @Test fun backgroundApiJobsDeclareNetworkWhileLocalPagesCanRunOffline() {
        assertTrue(Policy.needsNetwork("<html><script>fetch('https://example.com')</script></html>"))
        assertFalse(Policy.needsNetwork("<meta name=\"kadr-network\" content=\"none\">"))
        assertFalse(Policy.needsNetwork("<META content='NONE' name='kadr-network'>"))
    }

}
