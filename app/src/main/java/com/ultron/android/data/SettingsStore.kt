package com.ultron.android.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Ultra-secure preferences store. The Gemini API key is stored in
 * encrypted storage (Android Keystore-backed EncryptedSharedPreferences),
 * mirroring Maya's privacy model — keys never leave the device.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "ultron_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ---- Gemini key ----
    var geminiKey: String
        get() = prefs.getString(KEY_GEMINI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI, value).apply()

    // ---- Personal ----
    var userName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var assistantName: String
        get() = prefs.getString(KEY_ANAME, "Ultron") ?: "Ultron"
        set(value) = prefs.edit().putString(KEY_ANAME, value).apply()

    var gender: String
        get() = prefs.getString(KEY_GENDER, "male") ?: "male"
        set(value) = prefs.edit().putString(KEY_GENDER, value).apply()

    var phoneNumber: String
        get() = prefs.getString(KEY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE, value).apply()

    var favoriteSong: String
        get() = prefs.getString(KEY_SONG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SONG, value).apply()

    var musicApp: String
        get() = prefs.getString(KEY_MUSIC_APP, "YouTube Music") ?: "YouTube Music"
        set(value) = prefs.edit().putString(KEY_MUSIC_APP, value).apply()

    // ---- Voice / persona ----
    var selectedVoice: String
        get() = prefs.getString(KEY_VOICE, "ultron_default") ?: "ultron_default"
        set(value) = prefs.edit().putString(KEY_VOICE, value).apply()

    var persona: String
        get() = prefs.getString(KEY_PERSONA, "Mature") ?: "Mature"
        set(value) = prefs.edit().putString(KEY_PERSONA, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANG, "Hindi") ?: "Hindi"
        set(value) = prefs.edit().putString(KEY_LANG, value).apply()

    // ---- Behaviour toggles ----
    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE, value).apply()

    var proactiveEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_PROACTIVE, value).apply()

    var memoryAuto: Boolean
        get() = prefs.getBoolean(KEY_MEMORY_AUTO, true)
        set(value) = prefs.edit().putBoolean(KEY_MEMORY_AUTO, value).apply()

    var updatesBackground: Boolean
        get() = prefs.getBoolean(KEY_UPDATES, true)
        set(value) = prefs.edit().putBoolean(KEY_UPDATES, value).apply()

    var startupBoot: Boolean
        get() = prefs.getBoolean(KEY_BOOT, true)
        set(value) = prefs.edit().putBoolean(KEY_BOOT, value).apply()

    var echoGuard: Boolean
        get() = prefs.getBoolean(KEY_ECHO, true)
        set(value) = prefs.edit().putBoolean(KEY_ECHO, value).apply()

    var mediaMode: Boolean
        get() = prefs.getBoolean(KEY_MEDIA, false)
        set(value) = prefs.edit().putBoolean(KEY_MEDIA, value).apply()

    var floatingOrb: Boolean
        get() = prefs.getBoolean(KEY_ORB, true)
        set(value) = prefs.edit().putBoolean(KEY_ORB, value).apply()

    var screenWake: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_WAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_WAKE, value).apply()

    var partnerMode: String
        get() = prefs.getString(KEY_PARTNER, "Friendly") ?: "Friendly"
        set(value) = prefs.edit().putString(KEY_PARTNER, value).apply()

    // ---- Theme ----
    var themeStyle: String
        get() = prefs.getString(KEY_THEME, "Dark") ?: "Dark"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var uiLanguage: String
        get() = prefs.getString(KEY_UI_LANG, "System") ?: "System"
        set(value) = prefs.edit().putString(KEY_UI_LANG, value).apply()

    // ---- Battery saver ----
    var batterySaver: Boolean
        get() = prefs.getBoolean(KEY_BATTERY, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY, value).apply()

    var batteryKeepWake: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_WAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_WAKE, value).apply()

    var batteryKeepOrb: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_ORB, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_ORB, value).apply()

    var batteryKeepGuardian: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_GUARDIAN, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_GUARDIAN, value).apply()

    // ---- Auto-reply ----
    var autoReply: Boolean
        get() = prefs.getBoolean(KEY_AUTOREPLY, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTOREPLY, value).apply()

    var autoReplyOnlyAsleep: Boolean
        get() = prefs.getBoolean(KEY_AUTOREPLY_ASLEEP, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTOREPLY_ASLEEP, value).apply()

    var drivingMode: Boolean
        get() = prefs.getBoolean(KEY_DRIVING, false)
        set(value) = prefs.edit().putBoolean(KEY_DRIVING, value).apply()

    var callAnnounce: Boolean
        get() = prefs.getBoolean(KEY_CALL_ANNOUNCE, true)
        set(value) = prefs.edit().putBoolean(KEY_CALL_ANNOUNCE, value).apply()

    var callSpeaker: Boolean
        get() = prefs.getBoolean(KEY_CALL_SPEAKER, false)
        set(value) = prefs.edit().putBoolean(KEY_CALL_SPEAKER, value).apply()

    var reelsAutoscroll: Boolean
        get() = prefs.getBoolean(KEY_REELS, false)
        set(value) = prefs.edit().putBoolean(KEY_REELS, value).apply()

    // ---- Secret settings ----
    var adultConfirmed: Boolean
        get() = prefs.getBoolean(KEY_ADULT, false)
        set(value) = prefs.edit().putBoolean(KEY_ADULT, value).apply()

    var birthYear: String
        get() = prefs.getString(KEY_BIRTH_YEAR, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BIRTH_YEAR, value).apply()

    // ---- Onboarding, optional keys, skills ----
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    var mapsKey: String
        get() = prefs.getString(KEY_MAPS_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MAPS_KEY, value).apply()

    var searchKey: String
        get() = prefs.getString(KEY_SEARCH_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SEARCH_KEY, value).apply()

    var imageKey: String
        get() = prefs.getString(KEY_IMAGE_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_IMAGE_KEY, value).apply()

    var installedSkills: Set<String>
        get() = prefs.getStringSet(KEY_SKILLS, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SKILLS, value.toSet()).apply()

    companion object {
        private const val KEY_GEMINI = "gemini_key"
        private const val KEY_NAME = "user_name"
        private const val KEY_ANAME = "assistant_name"
        private const val KEY_GENDER = "gender"
        private const val KEY_PHONE = "phone"
        private const val KEY_SONG = "favorite_song"
        private const val KEY_MUSIC_APP = "music_app"
        private const val KEY_VOICE = "voice"
        private const val KEY_PERSONA = "persona"
        private const val KEY_LANG = "language"
        private const val KEY_WAKE = "wake_word"
        private const val KEY_PROACTIVE = "proactive"
        private const val KEY_MEMORY_AUTO = "memory_auto"
        private const val KEY_UPDATES = "updates"
        private const val KEY_BOOT = "boot"
        private const val KEY_ECHO = "echo"
        private const val KEY_MEDIA = "media"
        private const val KEY_ORB = "orb"
        private const val KEY_SCREEN_WAKE = "screen_wake"
        private const val KEY_PARTNER = "partner"
        private const val KEY_THEME = "theme"
        private const val KEY_UI_LANG = "ui_lang"
        private const val KEY_BATTERY = "battery"
        private const val KEY_BATTERY_WAKE = "battery_wake"
        private const val KEY_BATTERY_ORB = "battery_orb"
        private const val KEY_BATTERY_GUARDIAN = "battery_guardian"
        private const val KEY_AUTOREPLY = "autoreply"
        private const val KEY_AUTOREPLY_ASLEEP = "autoreply_asleep"
        private const val KEY_DRIVING = "driving"
        private const val KEY_CALL_ANNOUNCE = "call_announce"
        private const val KEY_CALL_SPEAKER = "call_speaker"
        private const val KEY_REELS = "reels"
        private const val KEY_ADULT = "adult"
        private const val KEY_BIRTH_YEAR = "birth_year"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_MAPS_KEY = "maps_key"
        private const val KEY_SEARCH_KEY = "search_key"
        private const val KEY_IMAGE_KEY = "image_key"
        private const val KEY_SKILLS = "installed_skills"
    }
}
