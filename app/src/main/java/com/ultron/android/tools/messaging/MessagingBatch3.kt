package com.ultron.android.tools.messaging

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.ultron.android.core.tools.*
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun ToolContext.hasPermB3(p: String): Boolean =
    ContextCompat.checkSelfPermission(appContext, p) == PackageManager.PERMISSION_GRANTED

private val smsTimeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM HH:mm")

class SmsSearchTool : Tool {
    override val id = "messaging_sms_search"
    override val category = ToolCategory.MESSAGING
    override val title = "SMS search"
    override val description = "Phone ke SMS mein text dhoondho. Sirf tumhare apne SMS, latest pehle. Count default 5, max 10."
    override val params = listOf(
        ToolParam("query", "string", "Kya dhoondhna hai", required = true),
        ToolParam("count", "integer", "Kitne results, 1 se 10", required = false)
    )
    override val permissions = listOf(Manifest.permission.READ_SMS)

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        if (!ctx.hasPermB3(Manifest.permission.READ_SMS)) return ToolResult.NeedsPermission(permissions)
        val query = args.str("query")?.trim()?.takeIf { it.isNotEmpty() } ?: return ToolResult.Error("query nahi mila")
        val limit = (args.int("count") ?: 5).coerceIn(1, 10)
        val cursor = ctx.appContext.contentResolver.query(
            Uri.parse("content://sms"),
            arrayOf("address", "body", "date"),
            "body LIKE ?",
            arrayOf("%$query%"),
            "date DESC"
        ) ?: return ToolResult.Error("SMS padh nahi paye")

        val lines = mutableListOf<String>()
        cursor.use { c ->
            val addrIdx = c.getColumnIndexOrThrow("address")
            val bodyIdx = c.getColumnIndexOrThrow("body")
            val dateIdx = c.getColumnIndexOrThrow("date")
            while (c.moveToNext() && lines.size < limit) {
                val from = c.getString(addrIdx).orEmpty()
                val body = c.getString(bodyIdx).orEmpty().take(160)
                val time = smsTimeFmt.format(Instant.ofEpochMilli(c.getLong(dateIdx)).atZone(ZoneId.systemDefault()))
                lines.add("$time · $from: $body")
            }
        }
        if (lines.isEmpty()) return ToolResult.Ok("'$query' wala koi SMS nahi mila")
        return ToolResult.Ok(lines.joinToString("\n"))
    }
}

class ContactSearchTool : Tool {
    override val id = "messaging_contact_search"
    override val category = ToolCategory.MESSAGING
    override val title = "Contact search"
    override val description = "Naam se contact dhoondho aur number dikhao."
    override val params = listOf(
        ToolParam("name", "string", "Contact ka naam ya uska hissa", required = true)
    )
    override val permissions = listOf(Manifest.permission.READ_CONTACTS)

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        if (!ctx.hasPermB3(Manifest.permission.READ_CONTACTS)) return ToolResult.NeedsPermission(permissions)
        val name = args.str("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return ToolResult.Error("naam nahi mila")
        val cursor = ctx.appContext.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        ) ?: return ToolResult.Error("Contacts padh nahi paye")

        val lines = mutableListOf<String>()
        cursor.use { c ->
            val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext() && lines.size < 10) {
                lines.add("${c.getString(nameIdx).orEmpty()}: ${c.getString(numIdx).orEmpty()}")
            }
        }
        if (lines.isEmpty()) return ToolResult.Ok("'$name' naam ka contact nahi mila")
        return ToolResult.Ok(lines.joinToString("\n"))
    }
}

/**
 * Naya contact add karne ka form Contacts app mein khulta hai. Hum seedha contact nahi likhte,
 * user khud save karega.
 */
class ContactAddTool : Tool {
    override val id = "messaging_contact_add"
    override val category = ToolCategory.MESSAGING
    override val title = "Contact add karo"
    override val description = "Contacts app mein naya contact form khol do, naam aur number pehle se bhara hua. User save karega."
    override val params = listOf(
        ToolParam("name", "string", "Contact ka naam", required = true),
        ToolParam("phone", "string", "Phone number", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val name = args.str("name")?.trim()?.takeIf { it.isNotEmpty() } ?: return ToolResult.Error("naam nahi mila")
        val phone = args.str("phone")?.filter { it.isDigit() || it == '+' }?.takeIf { it.length >= 5 }
            ?: return ToolResult.Error("phone number galat hai")
        val intent = Intent(ContactsContract.Intents.Insert.ACTION)
            .setType(ContactsContract.RawContacts.CONTENT_TYPE)
            .putExtra(ContactsContract.Intents.Insert.NAME, name)
            .putExtra(ContactsContract.Intents.Insert.PHONE, phone)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.appContext.startActivity(intent)
            ToolResult.Ok("Contacts mein '$name' ka form khol diya")
        } catch (e: ActivityNotFoundException) {
            ToolResult.Error("Is phone par Contacts app nahi mila")
        }
    }
}
