package com.thebifel.kadr.domain

import com.thebifel.kadr.domain.model.WidgetPolicy
import org.junit.Assert.*
import org.junit.Test

class PolicyTest {
    @Test
    fun scheduleCannotPromiseUnsupportedIntervals() {
        assertFalse(WidgetPolicy.validInterval(1))
        assertFalse(WidgetPolicy.validInterval(14))
        assertTrue(WidgetPolicy.validInterval(0))
        assertTrue(WidgetPolicy.validInterval(15))
        assertTrue(WidgetPolicy.validInterval(1440))
    }

    @Test
    fun largeWidgetHasBoundedBitmapMemory() {
        for (w in listOf(-100, 140, 320, 640, 9999)) for (h in listOf(-100, 70, 160, 640, 9999)) {
            val (rw, rh) = WidgetPolicy.renderSize(w, h)
            assertTrue(rw in 1..720)
            assertTrue(rh in 1..480)
            assertTrue(rw * rh * 4 <= 1_382_400)
        }
    }

    @Test
    fun staleRenderCannotReplaceNewEditorRevision() {
        assertTrue(WidgetPolicy.canPublish(8, 8))
        assertFalse(WidgetPolicy.canPublish(8, 9))
        assertFalse(WidgetPolicy.canPublish(8, -1))
    }

    @Test
    fun backgroundApiJobsDeclareNetworkWhileLocalPagesCanRunOffline() {
        assertTrue(
            WidgetPolicy.needsNetwork("<html><script>fetch('https://example.com')</script></html>")
        )
        assertFalse(WidgetPolicy.needsNetwork("<meta name=\"kadr-network\" content=\"none\">"))
        assertFalse(WidgetPolicy.needsNetwork("<META content='NONE' name='kadr-network'>"))
    }
}
