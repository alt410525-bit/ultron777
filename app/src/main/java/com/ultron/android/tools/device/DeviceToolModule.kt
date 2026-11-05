package com.ultron.android.tools.device

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.telephony.SubscriptionManager
import com.ultron.android.core.tools.*

private val DV = ToolCategory.DEVICE

private fun Context.audio() = getSystemService(Context.AUDIO_SERVICE) as AudioManager
private fun Context.connectivity() = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
private fun Context.notifications() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

/** Settings panel kholta hai. Toggles system-level hain, isliye user ko wahan le jaate hain. */
private fun Context.openPanel(action: String, data: Uri? = null): ToolResult {
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (data != null) intent.data = data
    return try {
        startActivity(intent)
        ToolResult.Ok("Settings khol diya. Wahan se toggle kar sakte ho.")
    } catch (e: ActivityNotFoundException) {
        ToolResult.Error("Ye settings screen is phone par nahi mili")
    }
}

private fun Context.canWriteSettings(): Boolean = Settings.System.canWrite(this)

private fun Context.writeSettingsHint(): ToolResult.Error = ToolResult.Error(
    "Ye kaam ke liye 'Modify system settings' permission chahiye. Ultron ke liye wo screen khol du?"
)

private val STREAMS = mapOf(
    "media" to AudioManager.STREAM_MUSIC,
    "ring" to AudioManager.STREAM_RING,
    "alarm" to AudioManager.STREAM_ALARM,
    "call" to AudioManager.STREAM_VOICE_CALL,
    "notification" to AudioManager.STREAM_NOTIFICATION
)

private val SETTINGS_PANELS = mapOf(
    "wifi" to Settings.ACTION_WIFI_SETTINGS,
    "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
    "airplane" to Settings.ACTION_AIRPLANE_MODE_SETTINGS,
    "nfc" to Settings.ACTION_NFC_SETTINGS,
    "hotspot" to "android.settings.TETHER_SETTINGS",
    "display" to Settings.ACTION_DISPLAY_SETTINGS,
    "sound" to Settings.ACTION_SOUND_SETTINGS,
    "dnd" to Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
    "date_time" to Settings.ACTION_DATE_SETTINGS,
    "language" to Settings.ACTION_LOCALE_SETTINGS,
    "accessibility" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
    "default_apps" to Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
    "usage_access" to Settings.ACTION_USAGE_ACCESS_SETTINGS,
    "battery_optimization" to Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
)

@Suppress("DEPRECATION")
private fun Context.vibrator(): Vibrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    else getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

class DeviceToolModule : ToolModule {
    override val tools: List<Tool> = listOf(

        // ---------- Flashlight ----------
        SpecTool(
            id = "device_flashlight_set", category = DV, title = "Flashlight",
            description = "Torch on ya off karo.",
            params = listOf(ToolParam("enabled", "boolean", "true = on, false = off", required = true))
        ) { args, ctx ->
            val on = args.bool("enabled") ?: return@SpecTool ToolResult.Error("enabled true ya false chahiye")
            val cm = ctx.appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val flashId: String? = runCatching {
                cm.cameraIdList.firstOrNull {
                    cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
            }.getOrNull()
            val id = flashId ?: return@SpecTool ToolResult.Error("Is phone mein flashlight nahi mili")
            cm.setTorchMode(id, on)
            ToolResult.Ok("Flashlight ${if (on) "on" else "off"} kar diya")
        },

        // ---------- Volume ----------
        SpecTool(
            id = "device_volume_status", category = DV, title = "Volume status",
            description = "Media, ring, alarm, call aur notification volume ka level batao."
        ) { _, ctx ->
            val am = ctx.appContext.audio()
            ToolResult.Ok(STREAMS.entries.joinToString("\n") { (name, s) ->
                "$name: ${am.getStreamVolume(s)}/${am.getStreamMaxVolume(s)}"
            })
        },
        SpecTool(
            id = "device_volume_set", category = DV, title = "Volume controller",
            description = "Media, ring, alarm, call ya notification volume percent (0-100) mein set karo.",
            params = listOf(
                ToolParam("stream", "string", "media, ring, alarm, call ya notification", required = true),
                ToolParam("percent", "integer", "0 se 100", required = true)
            )
        ) { args, ctx ->
            val name = args.str("stream") ?: return@SpecTool ToolResult.Error("stream chahiye")
            val s = STREAMS[name] ?: return@SpecTool ToolResult.Error("stream media, ring, alarm, call ya notification hona chahiye")
            val pct = (args.int("percent") ?: return@SpecTool ToolResult.Error("percent chahiye")).coerceIn(0, 100)
            val am = ctx.appContext.audio()
            val level = Math.round(pct / 100.0 * am.getStreamMaxVolume(s)).toInt()
            am.setStreamVolume(s, level, 0)
            ToolResult.Ok("$name volume ${pct}% par set kar diya")
        },

        // ---------- Ringer mode ----------
        SpecTool(
            id = "device_audio_mode_set", category = DV, title = "Audio mode",
            description = "Phone ko normal, vibrate ya silent mode mein daalo.",
            params = listOf(ToolParam("mode", "string", "normal, vibrate ya silent", required = true)),
            risk = RiskLevel.MEDIUM
        ) { args, ctx ->
            val mode = when (args.str("mode")) {
                "normal" -> AudioManager.RINGER_MODE_NORMAL
                "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                "silent" -> AudioManager.RINGER_MODE_SILENT
                else -> return@SpecTool ToolResult.Error("mode normal, vibrate ya silent hona chahiye")
            }
            val am = ctx.appContext.audio()
            if (mode == AudioManager.RINGER_MODE_SILENT && !ctx.appContext.notifications().isNotificationPolicyAccessGranted) {
                return@SpecTool ToolResult.Error("Silent ke liye 'Do Not Disturb access' chahiye. Ultron wo screen khol du?")
            }
            am.ringerMode = mode
            ToolResult.Ok("Phone ${args.str("mode")} mode mein hai")
        },

        // ---------- Brightness ----------
        SpecTool(
            id = "device_brightness_status", category = DV, title = "Brightness status",
            description = "Screen brightness percent batao."
        ) { _, ctx ->
            val raw = Settings.System.getInt(ctx.appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
            ToolResult.Ok("Brightness ${Math.round(raw * 100 / 255.0)}%")
        },
        SpecTool(
            id = "device_brightness_set", category = DV, title = "Brightness controller",
            description = "Screen brightness percent (0-100) mein set karo. Manual mode on ho jaata hai.",
            params = listOf(ToolParam("percent", "integer", "0 se 100", required = true)),
            risk = RiskLevel.MEDIUM
        ) { args, ctx ->
            val c = ctx.appContext
            if (!c.canWriteSettings()) return@SpecTool c.writeSettingsHint()
            val pct = (args.int("percent") ?: return@SpecTool ToolResult.Error("percent chahiye")).coerceIn(0, 100)
            val value = Math.round(pct * 255 / 100.0).toInt().coerceIn(1, 255)
            Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            ToolResult.Ok("Brightness $pct% par set kar diya")
        },

        // ---------- Auto-rotation ----------
        SpecTool(
            id = "device_rotation_set", category = DV, title = "Auto-rotation",
            description = "Screen auto-rotate on ya off karo.",
            params = listOf(ToolParam("enabled", "boolean", "true = on, false = off", required = true)),
            risk = RiskLevel.MEDIUM
        ) { args, ctx ->
            val c = ctx.appContext
            val on = args.bool("enabled") ?: return@SpecTool ToolResult.Error("enabled true ya false chahiye")
            if (!c.canWriteSettings()) return@SpecTool c.writeSettingsHint()
            Settings.System.putInt(c.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (on) 1 else 0)
            ToolResult.Ok("Auto-rotate ${if (on) "on" else "off"} kar diya")
        },

        // ---------- Vibration ----------
        SpecTool(
            id = "device_vibrate", category = DV, title = "Vibration",
            description = "Phone ko milliseconds ke liye vibrate karo (default 300ms).",
            params = listOf(ToolParam("ms", "integer", "Kitni der (1-5000 ms)")),
            permissions = listOf(Manifest.permission.VIBRATE)
        ) { args, ctx ->
            val vib = ctx.appContext.vibrator()
            if (!vib.hasVibrator()) return@SpecTool ToolResult.Error("Is phone mein vibration nahi hai")
            val ms = (args.long("ms") ?: 300L).coerceIn(1L, 5000L)
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            ToolResult.Ok("Vibrate kiya ($ms ms)")
        },

        // ---------- Clipboard ----------
        SpecTool(
            id = "device_clipboard_read", category = DV, title = "Clipboard read",
            description = "Clipboard mein jo text hai wo padho. Android 10+ par ye tabhi kaam karta hai jab Ultron screen par khula ho."
        ) { _, ctx ->
            val cb = ctx.appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cb.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
            ToolResult.Ok(text ?: "Clipboard khaali hai, ya Ultron ko abhi padhne ki permission nahi hai.")
        },
        SpecTool(
            id = "device_clipboard_write", category = DV, title = "Clipboard write",
            description = "Clipboard mein text copy karo.",
            params = listOf(ToolParam("text", "string", "Copy karne wala text", required = true))
        ) { args, ctx ->
            val t = args.str("text") ?: return@SpecTool ToolResult.Error("text chahiye")
            val cb = ctx.appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("ultron", t))
            ToolResult.Ok("Clipboard mein copy kar diya")
        },

        // ---------- Settings panels (Wi-Fi, Bluetooth, hotspot, etc.) ----------
        SpecTool(
            id = "device_open_settings", category = DV, title = "Open settings panel",
            description = "Wi-Fi, bluetooth, airplane, hotspot, NFC, display, sound, DND, date/time, language, accessibility, default apps, usage access ya battery optimization ka settings panel kholo. Wi-Fi/bluetooth/airplane jaise toggles Android 10+ par Ultron seedha on/off nahi kar sakta, isliye panel khulta hai.",
            params = listOf(ToolParam("panel", "string", SETTINGS_PANELS.keys.joinToString(", "), required = true))
        ) { args, ctx ->
            val key = args.str("panel") ?: return@SpecTool ToolResult.Error("panel chahiye")
            val action = SETTINGS_PANELS[key] ?: return@SpecTool ToolResult.Error("Panel valid nahi: $key")
            ctx.appContext.openPanel(action)
        },

        // ---------- Network ----------
        SpecTool(
            id = "device_network_status", category = DV, title = "Network status",
            description = "Abhi phone kis network par hai: Wi-Fi, mobile data, ethernet ya koi nahi."
        ) { _, ctx ->
            val cm = ctx.appContext.connectivity()
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            val kind = when {
                caps == null -> "koi network nahi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile data"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                else -> "doosra network"
            }
            ToolResult.Ok("Network: $kind")
        },

        // ---------- Do Not Disturb ----------
        SpecTool(
            id = "device_dnd_status", category = DV, title = "DND status",
            description = "Do Not Disturb on hai ya off batao."
        ) { _, ctx ->
            val nm = ctx.appContext.notifications()
            if (!nm.isNotificationPolicyAccessGranted) return@SpecTool ToolResult.Ok("DND access abhi Ultron ko nahi mila.")
            val on = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            ToolResult.Ok("DND ${if (on) "on" else "off"} hai")
        },
        SpecTool(
            id = "device_dnd_set", category = DV, title = "DND toggle",
            description = "Do Not Disturb on ya off karo. Pehli baar Notification policy access dena padega.",
            params = listOf(ToolParam("enabled", "boolean", "true = on, false = off", required = true)),
            risk = RiskLevel.MEDIUM
        ) { args, ctx ->
            val on = args.bool("enabled") ?: return@SpecTool ToolResult.Error("enabled true ya false chahiye")
            val nm = ctx.appContext.notifications()
            if (!nm.isNotificationPolicyAccessGranted) {
                return@SpecTool ctx.appContext.openPanel(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            }
            nm.setInterruptionFilter(
                if (on) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            ToolResult.Ok("DND ${if (on) "on" else "off"} kar diya")
        },

        // ---------- Apps ----------
        SpecTool(
            id = "device_app_launch", category = DV, title = "App launcher",
            description = "Naam se koi bhi installed app kholo (jaise WhatsApp, Chrome, YouTube).",
            params = listOf(ToolParam("name", "string", "App ka naam", required = true))
        ) { args, ctx ->
            val name = args.str("name") ?: return@SpecTool ToolResult.Error("name chahiye")
            val pm = ctx.appContext.packageManager
            val apps = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
            )
            val labelled = apps.map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
            val match = labelled.firstOrNull { it.first.equals(name, true) }
                ?: labelled.firstOrNull { it.first.contains(name, true) }
                ?: return@SpecTool ToolResult.Error("$name naam ka app nahi mila")
            val launch = pm.getLaunchIntentForPackage(match.second)
                ?: return@SpecTool ToolResult.Error("${match.first} abhi nahi khul sakta")
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.appContext.startActivity(launch)
            ToolResult.Ok("${match.first} khol diya")
        },
        SpecTool(
            id = "device_installed_apps", category = DV, title = "Installed apps",
            description = "Phone par installed launchable apps ki list (max 60)."
        ) { _, ctx ->
            val pm = ctx.appContext.packageManager
            val names = pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
            ).map { it.loadLabel(pm).toString() }.distinct().sortedBy { it.lowercase() }
            ToolResult.Ok("${names.size} apps:\n" + names.take(60).joinToString(", "))
        },

        // ---------- Device info ----------
        SpecTool(
            id = "device_info", category = DV, title = "Device info",
            description = "Phone ka model, brand aur Android version batao."
        ) { _, _ ->
            ToolResult.Ok("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        },
        SpecTool(
            id = "device_memory_usage", category = DV, title = "RAM usage",
            description = "Phone ki RAM kitni total aur kitni free hai."
        ) { _, ctx ->
            val am = ctx.appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
            val totalMb = mi.totalMem / (1024 * 1024)
            val freeMb = mi.availMem / (1024 * 1024)
            ToolResult.Ok("RAM: ${totalMb - freeMb} MB use ho rahi hai, ${freeMb} MB free (total $totalMb MB)")
        },
        SpecTool(
            id = "device_sensors_list", category = DV, title = "Sensors",
            description = "Phone mein kaun se sensors hain: accelerometer, gyroscope, light, magnetometer (compass), proximity."
        ) { _, ctx ->
            val sm = ctx.appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val checks = listOf(
                "Accelerometer" to Sensor.TYPE_ACCELEROMETER,
                "Gyroscope" to Sensor.TYPE_GYROSCOPE,
                "Ambient light" to Sensor.TYPE_LIGHT,
                "Magnetometer (compass)" to Sensor.TYPE_MAGNETIC_FIELD,
                "Proximity" to Sensor.TYPE_PROXIMITY
            )
            ToolResult.Ok(checks.joinToString("\n") { (label, type) ->
                "$label: ${if (sm.getDefaultSensor(type) != null) "available" else "nahi hai"}"
            })
        },

        // ---------- SIM ----------
        SpecTool(
            id = "device_sim_info", category = DV, title = "SIM info",
            description = "Phone mein kitne SIM active hain aur unke carrier naam.",
            permissions = listOf(Manifest.permission.READ_PHONE_STATE)
        ) { _, ctx ->
            val sims = SubscriptionManager.from(ctx.appContext).activeSubscriptionInfoList
            if (sims.isNullOrEmpty()) ToolResult.Ok("Koi active SIM nahi mila.")
            else ToolResult.Ok(sims.joinToString("\n") { "SIM ${it.simSlotIndex + 1}: ${it.carrierName}" })
        },

        // ---------- Location ----------
        SpecTool(
            id = "device_location", category = DV, title = "Location",
            description = "Phone ki last known location (latitude, longitude) batao.",
            risk = RiskLevel.MEDIUM,
            permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        ) { _, ctx ->
            val lm = ctx.appContext.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val loc = listOf(
                android.location.LocationManager.GPS_PROVIDER,
                android.location.LocationManager.NETWORK_PROVIDER
            ).firstNotNullOfOrNull { p -> try { lm.getLastKnownLocation(p) } catch (e: SecurityException) { null } }
                ?: return@SpecTool ToolResult.Error("Location abhi nahi mil rahi. GPS on karke dobara try karo.")
            ToolResult.Ok("Lat ${"%.5f".format(loc.latitude)}, Lng ${"%.5f".format(loc.longitude)}")
        }
    )
}
