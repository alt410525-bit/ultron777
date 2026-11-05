package com.ultron.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** GitHub, Telegram, Notion ke tokens. Android Keystore se encrypted. */
object ConnectorSecrets {
    private const val FILE = "ultron_connectors"

    const val GITHUB = "github_token"
    const val TELEGRAM = "telegram_bot_token"
    const val NOTION = "notion_token"

    private fun prefs(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(ctx: Context, key: String): String = prefs(ctx).getString(key, "").orEmpty()

    fun set(ctx: Context, key: String, value: String) {
        prefs(ctx).edit().putString(key, value).apply()
    }
}
