package com.ultron.android.tools.device

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.ultron.android.core.tools.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.sqrt

/** Intent launch helper. Koi app na ho to error, crash nahi. */
private fun launchDev3(ctx: ToolContext, intent: Intent, ok: String): ToolResult = try {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.appContext.startActivity(intent)
    ToolResult.Ok(ok)
} catch (e: ActivityNotFoundException) {
    ToolResult.Error("Is phone par ye kaam karne wala app nahi mila")
}

class AlarmSetTool : Tool {
    override val id = "device_set_alarm"
    override val category = ToolCategory.DEVICE
    override val title = "Alarm set karo"
    override val description = "Clock app mein alarm set karo (24 ghante format). User clock app mein confirm karega."
    override val params = listOf(
        ToolParam("hour", "integer", "Ghanta, 0 se 23", required = true),
        ToolParam("minute", "integer", "Minute, 0 se 59 (default 0)", required = false),
        ToolParam("label", "string", "Alarm ka label (optional)", required = false)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val hour = args.int("hour") ?: return ToolResult.Error("hour nahi mila")
        if (hour !in 0..23) return ToolResult.Error("hour 0 se 23 ke beech hona chahiye")
        val minute = (args.int("minute") ?: 0).coerceIn(0, 59)
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, minute)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        args.str("label")?.let { intent.putExtra(AlarmClock.EXTRA_MESSAGE, it) }
        return launchDev3(ctx, intent, "Alarm settings khol diya: $hour:${minute.toString().padStart(2, '0')}")
    }
}

class TimerSetTool : Tool {
    override val id = "device_set_timer"
    override val category = ToolCategory.DEVICE
    override val title = "Timer set karo"
    override val description = "Clock app mein timer set karke start karo (minutes mein)."
    override val params = listOf(
        ToolParam("minutes", "integer", "Kitne minute, 1 se 720", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val minutes = args.int("minutes") ?: return ToolResult.Error("minutes nahi mila")
        if (minutes !in 1..720) return ToolResult.Error("minutes 1 se 720 ke beech hona chahiye")
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        return launchDev3(ctx, intent, "$minutes minute ka timer start kar diya")
    }
}

class ShareTextTool : Tool {
    override val id = "device_share_text"
    override val category = ToolCategory.DEVICE
    override val title = "Text share karo"
    override val description = "Koi text share sheet se kisi bhi app mein bhejo. User app chunega aur khud send karega."
    override val params = listOf(
        ToolParam("text", "string", "Share karne wala text", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val text = args.str("text")?.takeIf { it.isNotBlank() } ?: return ToolResult.Error("text nahi mila")
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        return launchDev3(ctx, Intent.createChooser(send, "Share karo"), "Share menu khol diya")
    }
}

class CalendarAddTool : Tool {
    override val id = "device_calendar_add"
    override val category = ToolCategory.DEVICE
    override val title = "Calendar event banao"
    override val description = "Calendar app mein event ka form khol do, title, start aur duration pehle se bhara hua. User save karega."
    override val params = listOf(
        ToolParam("title", "string", "Event ka title", required = true),
        ToolParam("start", "string", "Shuru kab, format yyyy-MM-dd HH:mm", required = true),
        ToolParam("durationMinutes", "integer", "Kitne minute (default 60)", required = false)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val title = args.str("title")?.takeIf { it.isNotBlank() } ?: return ToolResult.Error("title nahi mila")
        val startRaw = args.str("start") ?: return ToolResult.Error("start nahi mila")
        val start = try {
            LocalDateTime.parse(startRaw.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } catch (e: Exception) {
            return ToolResult.Error("start ka format yyyy-MM-dd HH:mm hona chahiye")
        }
        val duration = (args.int("durationMinutes") ?: 60).coerceIn(1, 24 * 60)
        val zone = ZoneId.systemDefault()
        val begin = start.atZone(zone).toInstant().toEpochMilli()
        val end = begin + duration * 60_000L
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        return launchDev3(ctx, intent, "Calendar mein '$title' ka form khol diya")
    }
}

class SensorReadTool : Tool {
    override val id = "device_sensor_read"
    override val category = ToolCategory.DEVICE
    override val title = "Sensor value padho"
    override val description = "Ek baar sensor value padho: light (lux), compass (approx heading), accelerometer (m/s²)."
    override val params = listOf(
        ToolParam("sensor", "string", "light, compass ya accelerometer", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val kind = args.str("sensor")?.trim()?.lowercase() ?: return ToolResult.Error("sensor nahi mila")
        val type = when (kind) {
            "light" -> Sensor.TYPE_LIGHT
            "compass" -> Sensor.TYPE_MAGNETIC_FIELD
            "accelerometer" -> Sensor.TYPE_ACCELEROMETER
            else -> return ToolResult.Error("sensor light, compass ya accelerometer hona chahiye")
        }
        val sm = ctx.appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(type) ?: return ToolResult.Error("Is phone mein ye sensor nahi hai")

        val values = withTimeoutOrNull(3000L) {
            suspendCancellableCoroutine<FloatArray> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sm.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values.copyOf())
                    }
                    override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
                }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                cont.invokeOnCancellation { sm.unregisterListener(listener) }
            }
        } ?: return ToolResult.Error("Sensor se value nahi mili, dobara try karo")

        return ToolResult.Ok(
            when (kind) {
                "light" -> "Roshni: ${values[0].toInt()} lux"
                "compass" -> {
                    // Tilt compensation nahi hai, isliye ye sirf approximate heading hai.
                    var heading = Math.toDegrees(atan2(values[1].toDouble(), values[0].toDouble()))
                    if (heading < 0) heading += 360.0
                    "Approx heading: ${heading.toInt()} degree (phone flat rakhne par sahi aata hai)"
                }
                else -> {
                    val mag = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
                    "x=${"%.2f".format(values[0])}, y=${"%.2f".format(values[1])}, z=${"%.2f".format(values[2])} (total ${"%.2f".format(mag)} m/s²)"
                }
            }
        )
    }
}
