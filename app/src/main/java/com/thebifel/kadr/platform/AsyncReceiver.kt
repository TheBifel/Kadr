package com.thebifel.kadr.platform

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Keep a receiver alive while its bounded, non-rendering delegation finishes. */
fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            withTimeout(8_000) { block() }
        } finally {
            pending.finish()
        }
    }
}
