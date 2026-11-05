package com.ultron.android.tools.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.ultron.android.core.tools.*
import com.ultron.android.data.SettingsStore
import kotlinx.serialization.json.JsonObject

class BatteryStatusTool : Tool {
    override val id = "system_battery_status"
    override val category = ToolCategory.SYSTEM
    override val title = "Battery status"
    override val description = "Phone ki battery percentage aur charging state batao."
    override val params = emptyList<ToolParam>()

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val bm = ctx.appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val state = if (bm.isCharging) "charging ho rahi hai" else "charging nahi ho rahi"
        return ToolResult.Ok("Battery $pct%, $state")
    }
}

class PermissionsStatusTool : Tool {
    override val id = "system_permissions_status"
    override val category = ToolCategory.SYSTEM
    override val title = "Permissions status"
    override val description = "Batao kaun si permissions allow hain aur kaun si nahi (mic, SMS, call, location, notifications)."
    override val params = emptyList<ToolParam>()

    private val checks = listOf(
        "Microphone" to Manifest.permission.RECORD_AUDIO,
        "Contacts" to Manifest.permission.READ_CONTACTS,
        "SMS bhejna" to Manifest.permission.SEND_SMS,
        "SMS padhna" to Manifest.permission.READ_SMS,
        "Call lagana" to Manifest.permission.CALL_PHONE,
        "Call log" to Manifest.permission.READ_CALL_LOG,
        "Location" to Manifest.permission.ACCESS_FINE_LOCATION,
        "Notifications" to Manifest.permission.POST_NOTIFICATIONS
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val lines = checks.joinToString("\n") { (label, perm) ->
            val granted = ContextCompat.checkSelfPermission(ctx.appContext, perm) ==
                PackageManager.PERMISSION_GRANTED
            "$label: ${if (granted) "allowed" else "not allowed"}"
        }
        return ToolResult.Ok(lines)
    }
}

class BatterySaverSetTool(private val settings: SettingsStore) : Tool {
    override val id = "system_battery_saver_set"
    override val category = ToolCategory.SYSTEM
    override val title = "Battery saver"
    override val description = "Ultron ka battery saver on ya off karo."
    override val params = listOf(ToolParam("enabled", "boolean", "true = on, false = off", required = true))

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val enabled = args["enabled"]?.toString()?.toBooleanStrictOrNull()
            ?: return ToolResult.Error("enabled true ya false hona chahiye")
        settings.batterySaver = enabled
        return ToolResult.Ok("Battery saver ${if (enabled) "on" else "off"} kar diya")
    }
}

class SystemToolModule(private val settings: SettingsStore) : ToolModule {
    override val tools: List<Tool> = listOf(
        BatteryStatusTool(),
        PermissionsStatusTool(),
        BatterySaverSetTool(settings)
    )
}
