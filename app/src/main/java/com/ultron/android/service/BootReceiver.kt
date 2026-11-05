package com.ultron.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ultron.android.data.SettingsStore

/** Starts the wake-word service after boot if the user enabled it. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = SettingsStore(context.applicationContext)
        if (settings.wakeWordEnabled) {
            val s = Intent(context, WakeWordService::class.java)
            context.startForegroundService(s)
        }
    }
}
