package com.ultron.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.ultron.android.data.SettingsStore

class UltronApplication : Application() {
    val settings: SettingsStore by lazy { SettingsStore(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ultron session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ultron ke saath session chalta rehta hai"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "ultron_session"
    }
}
