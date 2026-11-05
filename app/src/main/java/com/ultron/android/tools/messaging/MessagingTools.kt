package com.ultron.android.tools.messaging

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.ultron.android.core.tools.*
import kotlinx.serialization.json.JsonObject

private fun ToolContext.hasPermission(p: String): Boolean =
    ContextCompat.checkSelfPermission(appContext, p) == PackageManager.PERMISSION_GRANTED

private fun ToolContext.launch(intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    appContext.startActivity(intent)
}

private fun String.digitsAndPlus(): String = filter { it.isDigit() || it == '+' }

class SendSmsTool : Tool {
    override val id = "messaging_send_sms"
    override val category = ToolCategory.MESSAGING
    override val title = "SMS bhejo"
    override val description = "Kisi number par SMS bhejo. Bhejne se pehle user se haan karwana zaroori hai."
    override val params = listOf(
        ToolParam("number", "string", "Receiver ka phone number", required = true),
        ToolParam("text", "string", "SMS ka text", required = true)
    )
    override val risk = RiskLevel.HIGH
    override val permissions = listOf(Manifest.permission.SEND_SMS)

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val number = args.str("number")?.digitsAndPlus() ?: return ToolResult.Error("number nahi mila")
        val text = args.str("text") ?: return ToolResult.Error("text nahi mila")
        if (!ctx.hasPermission(Manifest.permission.SEND_SMS)) return ToolResult.NeedsPermission(permissions)
        @Suppress("DEPRECATION")
        SmsManager.getDefault().sendTextMessage(number, null, text, null, null)
        return ToolResult.Ok("SMS $number ko bhej diya")
    }
}

class ReadRecentSmsTool : Tool {
    override val id = "messaging_read_recent_sms"
    override val category = ToolCategory.MESSAGING
    override val title = "Recent SMS padho"
    override val description = "Phone ke latest SMS padh kar sunao. count optional hai, default 5."
    override val params = listOf(
        ToolParam("count", "integer", "Kitne SMS chahiye, 1 se 10", required = false)
    )
    override val permissions = listOf(Manifest.permission.READ_SMS)

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        if (!ctx.hasPermission(Manifest.permission.READ_SMS)) return ToolResult.NeedsPermission(permissions)
        val count = (args.int("count") ?: 5).coerceIn(1, 10)
        val lines = mutableListOf<String>()
        ctx.appContext.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
            null, null, "${Telephony.Sms.DATE} DESC"
        )?.use { c ->
            while (c.moveToNext() && lines.size < count) {
                val from = c.getString(0).orEmpty()
                val body = c.getString(1).orEmpty().take(120)
                lines += "$from: $body"
            }
        }
        return if (lines.isEmpty()) ToolResult.Ok("Koi SMS nahi mila")
        else ToolResult.Ok(lines.joinToString("\n"))
    }
}

class MissedCallsTool : Tool {
    override val id = "messaging_missed_calls"
    override val category = ToolCategory.MESSAGING
    override val title = "Missed calls"
    override val description = "Latest missed calls ki list batao."
    override val params = emptyList<ToolParam>()
    override val permissions = listOf(Manifest.permission.READ_CALL_LOG)

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        if (!ctx.hasPermission(Manifest.permission.READ_CALL_LOG)) return ToolResult.NeedsPermission(permissions)
        val lines = mutableListOf<String>()
        ctx.appContext.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER),
            "${CallLog.Calls.TYPE} = ?",
            arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
            "${CallLog.Calls.DATE} DESC"
        )?.use { c ->
            while (c.moveToNext() && lines.size < 10) {
                val name = c.getString(0)?.takeIf { it.isNotBlank() } ?: c.getString(1).orEmpty()
                lines += name
            }
        }
        return if (lines.isEmpty()) ToolResult.Ok("Koi missed call nahi hai")
        else ToolResult.Ok("Missed calls: " + lines.joinToString(", "))
    }
}

class DialNumberTool : Tool {
    override val id = "messaging_dial_number"
    override val category = ToolCategory.MESSAGING
    override val title = "Dialer kholo"
    override val description = "Dialer app mein number bhar kar khol do. Call user khud dabayega."
    override val params = listOf(
        ToolParam("number", "string", "Phone number", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val number = args.str("number")?.digitsAndPlus().orEmpty()
        if (number.isEmpty()) return ToolResult.Error("number nahi mila")
        ctx.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        return ToolResult.Ok("Dialer mein $number bhar diya")
    }
}

class MakeCallTool : Tool {
    override val id = "messaging_make_call"
    override val category = ToolCategory.MESSAGING
    override val title = "Call lagao"
    override val description = "Kisi number par seedha call lagao. Pehle user se haan karwana zaroori hai."
    override val params = listOf(
        ToolParam("number", "string", "Phone number", required = true)
    )
    override val risk = RiskLevel.HIGH
    override val permissions = listOf(Manifest.permission.CALL_PHONE)

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val number = args.str("number")?.digitsAndPlus().orEmpty()
        if (number.isEmpty()) return ToolResult.Error("number nahi mila")
        if (!ctx.hasPermission(Manifest.permission.CALL_PHONE)) return ToolResult.NeedsPermission(permissions)
        ctx.launch(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
        return ToolResult.Ok("$number ko call laga raha hoon")
    }
}

class WhatsAppSendTool : Tool {
    override val id = "messaging_whatsapp_send"
    override val category = ToolCategory.MESSAGING
    override val title = "WhatsApp message"
    override val description = "WhatsApp mein message draft karke khol do. Number do to us chat mein khulega. Send user khud dabayega."
    override val params = listOf(
        ToolParam("text", "string", "Message ka text", required = true),
        ToolParam("number", "string", "Receiver ka number (optional)", required = false)
    )
    override val risk = RiskLevel.MEDIUM

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val text = args.str("text") ?: return ToolResult.Error("text nahi mila")
        val digits = args.str("number")?.filter { it.isDigit() }.orEmpty()
        val intent = if (digits.isNotEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=${Uri.encode(text)}"))
        } else {
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        }
        intent.setPackage("com.whatsapp")
        return try {
            ctx.launch(intent)
            ToolResult.Ok("WhatsApp khol diya. Send tum dabao.")
        } catch (e: ActivityNotFoundException) {
            ToolResult.Error("WhatsApp install nahi hai")
        }
    }
}

class MessagingToolModule : ToolModule {
    override val tools: List<Tool> = listOf(
        SendSmsTool(),
        ReadRecentSmsTool(),
        MissedCallsTool(),
        DialNumberTool(),
        MakeCallTool(),
        WhatsAppSendTool()
    )
}
