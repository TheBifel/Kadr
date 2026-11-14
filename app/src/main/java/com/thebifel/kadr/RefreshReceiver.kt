package com.thebifel.kadr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.thebifel.kadr.di.appContainer
import com.thebifel.kadr.platform.LocaleSupport
import com.thebifel.kadr.platform.runAsync

class RefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("project") ?: return
        Toast.makeText(
                context,
                LocaleSupport.localized(context).getString(R.string.update_requested),
                Toast.LENGTH_SHORT,
            )
            .show()
        runAsync { context.appContainer.requestRefresh(id) }
    }
}
