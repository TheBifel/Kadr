package com.thebifel.kadr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thebifel.kadr.di.appContainer
import com.thebifel.kadr.platform.runAsync

class RecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action !in
                listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
        )
            return
        runAsync { context.appContainer.reconcile(refresh = true) }
    }
}
