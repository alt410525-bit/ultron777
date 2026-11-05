package com.ultron.android.tools.utility

import com.ultron.android.core.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase se AES-256-GCM encryption. Output: base64(salt[16] + iv[12] + ciphertext).
 * Salt aur IV har baar random hote hain, isliye same text ka output alag aata hai.
 */
private object TextCryptoC3 {
    private const val ITERATIONS = 120_000
    private const val SALT_LEN = 16
    private const val IV_LEN = 12

    private fun deriveKey(pass: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(pass.toCharArray(), salt, ITERATIONS, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    fun encrypt(text: String, pass: String): String {
        val rnd = SecureRandom()
        val salt = ByteArray(SALT_LEN).also { rnd.nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { rnd.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(pass, salt), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(salt + iv + ct)
    }

    fun decrypt(b64: String, pass: String): String {
        val all = Base64.getDecoder().decode(b64.trim())
        require(all.size > SALT_LEN + IV_LEN) { "Encrypted data chhota hai ya kharab hai" }
        val salt = all.copyOfRange(0, SALT_LEN)
        val iv = all.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val ct = all.copyOfRange(SALT_LEN + IV_LEN, all.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(pass, salt), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }
}

class TextEncryptTool : Tool {
    override val id = "util_text_encrypt"
    override val category = ToolCategory.UTILITY
    override val title = "Text encrypt"
    override val description = "Text ko passphrase se encrypt karo (AES-256-GCM). Output base64 mein milega."
    override val params = listOf(
        ToolParam("text", "string", "Jo text encrypt karna hai", required = true),
        ToolParam("passphrase", "string", "Passphrase, ise yaad rakhna zaroori hai", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val text = args.str("text") ?: return ToolResult.Error("text nahi mila")
        val pass = args.str("passphrase")?.takeIf { it.length >= 6 }
            ?: return ToolResult.Error("passphrase kam se kam 6 characters ka ho")
        val out = withContext(Dispatchers.Default) { TextCryptoC3.encrypt(text, pass) }
        return ToolResult.Ok(out)
    }
}

class TextDecryptTool : Tool {
    override val id = "util_text_decrypt"
    override val category = ToolCategory.UTILITY
    override val title = "Text decrypt"
    override val description = "Pehle se encrypt kiya hua base64 text passphrase se wapas kholo."
    override val params = listOf(
        ToolParam("cipher", "string", "Encrypted base64 text", required = true),
        ToolParam("passphrase", "string", "Wahi passphrase jo encrypt karte waqt diya tha", required = true)
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val cipher = args.str("cipher") ?: return ToolResult.Error("cipher text nahi mila")
        val pass = args.str("passphrase") ?: return ToolResult.Error("passphrase nahi mila")
        return try {
            ToolResult.Ok(withContext(Dispatchers.Default) { TextCryptoC3.decrypt(cipher, pass) })
        } catch (e: Exception) {
            ToolResult.Error("Passphrase galat hai ya data kharab hai")
        }
    }
}

class PingTool : Tool {
    override val id = "util_ping"
    override val category = ToolCategory.UTILITY
    override val title = "Ping (latency)"
    override val description = "Kisi host ka latency naap-ti hai. ICMP ping ki jagah TCP connect time use hota hai (port 443 default)."
    override val params = listOf(
        ToolParam("host", "string", "Host, jaise google.com", required = true),
        ToolParam("port", "integer", "Port, default 443", required = false)
    )
    override val permissions = listOf(android.Manifest.permission.INTERNET)

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val host = args.str("host")?.trim()?.takeIf { it.isNotBlank() } ?: return ToolResult.Error("host nahi mila")
        val port = (args.int("port") ?: 443).takeIf { it in 1..65535 } ?: return ToolResult.Error("port galat hai")
        val times = withContext(Dispatchers.IO) {
            (1..3).map {
                val start = System.nanoTime()
                try {
                    Socket().use { it.connect(InetSocketAddress(host, port), 3000) }
                    (System.nanoTime() - start) / 1_000_000.0
                } catch (e: Exception) {
                    null
                }
            }
        }
        val ok = times.filterNotNull()
        if (ok.isEmpty()) return ToolResult.Error("$host tak connect nahi ho paya")
        val avg = ok.average()
        return ToolResult.Ok(
            "$host: ${ok.joinToString(", ") { "%.0f ms".format(it) }} (avg %.0f ms, ${ok.size}/3 success)".format(avg)
        )
    }
}
