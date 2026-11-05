package com.ultron.android.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ultron.android.R
import com.ultron.android.UltronApplication

/**
 * "Hey Ultron" wake-word foreground service.
 *
 * This keeps a microphone foreground service alive (like Maya's standby
 * wake-word mode). A real wake-word detector (e.g. Porcupine/ONNX) would
 * replace [isWakePhraseDetected]; this stub listens briefly and toggles
 * the "listening" side-effect so the app can demonstrate the flow.
 */
class WakeWordService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notification = NotificationCompat.Builder(this, UltronApplication.CHANNEL_ID)
                .setContentTitle("Ultron")
                .setContentText("\"Hey Ultron\" sun raha hoon…")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            notification = NotificationCompat.Builder(this)
                .setContentTitle("Ultron")
                .setContentText("\"Hey Ultron\" sun raha hoon…")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build()
            startForeground(1001, notification)
        }
        return START_STICKY
    }

    private fun isWakePhraseDetected(audioFrame: ByteArray): Boolean {
        // Placeholder: true wake-word detection hooks here
        // (Porcupine keyword "ultron" or a custom ONNX model).
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
