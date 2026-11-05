package com.ultron.android.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import com.ultron.android.data.SettingsStore

/**
 * The full settings tree, mirroring Maya's hub. Every section Ultron has
 * is declared here. Add a new setting by appending a [SettingItem] to the
 * right group — the UI renders it automatically.
 */
object SettingsRegistry {

    data class Section(
        val id: String,
        val title: String,
        val sub: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
        val builder: (SettingsStore) -> SettingSectionDef
    )

    fun hubSections(settings: SettingsStore): List<Section> = listOf(
        Section("assistant", "Assistant", "Persona, awaaz, bhasha", Icons.Default.Android) { s ->
            SettingSectionDef("assistant", "Assistant", listOf(
                SettingGroup("Ultron", listOf(
                    toggle(s, "a_name_toggle", "Proactive Ultron", "Khud se baat shuru kare", s.proactiveEnabled) { s.proactiveEnabled = it },
                    toggle(s, "a_mem_toggle", "Khud se yaad rakhne do", "Jo kaam ki baatein pakadta hai unhe save karta hai", s.memoryAuto) { s.memoryAuto = it },
                    toggle(s, "a_echo", "Echo guard", "Apni hi awaaz ka jawab nahi deta", s.echoGuard) { s.echoGuard = it }
                )),
                SettingGroup("Naam", listOf(
                    text(s, "a_name", "Assistant ka naam", "Tum use kya bulate ho", s.assistantName, "jaise Ultron, Jarvis…") { s.assistantName = it },
                    toggle(s, "a_name_t2", "Screen jagana", "Zaroorat ho to screen jagaaye", s.screenWake) { s.screenWake = it }
                ))
            ))
        },
        Section("personal", "Personal", "Naam, music, Gemini key", Icons.Default.Person) { s ->
            SettingSectionDef("personal", "Personal", listOf(
                SettingGroup("Tum", listOf(
                    text(s, "p_name", "Tumhara naam", "Ultron tumhe kya bulaye (khaali = \"dost\")", s.userName, "jaise Rahul") { s.userName = it },
                    choice(s, "p_gender", "Tum ho", "Taaki sahi shabd bole", listOf("Ladka", "Ladki"), s.gender.capitalize()) { s.gender = it.lowercase() },
                    text(s, "p_phone", "Phone number", "Optional", s.phoneNumber, "+91 98765 43210") { s.phoneNumber = it }
                )),
                SettingGroup("Gemini API key", listOf(
                    password(s, "p_key", "Gemini API key", "Ultron ka dimaag isi se chalta hai", if (s.geminiKey.isBlank()) "" else "••••${s.geminiKey.takeLast(4)}", "GA/RESthw5rJ… ya AIza…") { s.geminiKey = it }
                )),
                SettingGroup("Music", listOf(
                    text(s, "p_song", "Favourite gaana", "Jo bajayega", s.favoriteSong, "jaise 'Kesariya'") { s.favoriteSong = it },
                    text(s, "p_mapp", "Isme bajao", "Default music app", s.musicApp, "YouTube Music") { s.musicApp = it }
                ))
            ))
        },
        Section("messages", "Messages aur WhatsApp", "Alerts, auto-reply, calls", Icons.Default.Notifications) { s ->
            SettingSectionDef("messages", "Messages aur WhatsApp", listOf(
                SettingGroup("Calls", listOf(
                    toggle(s, "m_call_ann", "Aane wali calls batao", "", s.callAnnounce) { s.callAnnounce = it },
                    toggle(s, "m_call_spk", "Call uthaye to loudspeaker", "", s.callSpeaker) { s.callSpeaker = it },
                    toggle(s, "m_driving", "Driving mode", "Har call reject, caller ko SMS", s.drivingMode) { s.drivingMode = it }
                )),
                SettingGroup("Auto-reply", listOf(
                    toggle(s, "m_auto", "WhatsApp messages ka reply karo", "", s.autoReply) { s.autoReply = it },
                    toggle(s, "m_auto_asleep", "Sirf jab Ultron so raha ho", "", s.autoReplyOnlyAsleep) { s.autoReplyOnlyAsleep = it }
                )),
                SettingGroup("Social", listOf(
                    toggle(s, "m_reels", "Instagram Auto scroll", "Reels khud aage badhein", s.reelsAutoscroll) { s.reelsAutoscroll = it }
                ))
            ))
        },
        Section("connectors", "Connectors", "GitHub, Notion, Telegram", Icons.Default.Storage) { s ->
            SettingSectionDef("connectors", "Connectors", listOf(
                SettingGroup("Jude hue accounts", listOf(
                    nav("c_github", "GitHub", "Connect karo"),
                    nav("c_notion", "Notion", "Connect karo"),
                    nav("c_telegram", "Telegram", "Connect karo")
                ))
            ))
        },
        Section("skills", "Skills", "Playbooks aur skill store", Icons.Default.Star) { s ->
            SettingSectionDef("skills", "Skills", listOf(
                SettingGroup("Installed", listOf(
                    info("sk_none", if (s.installedSkills.isEmpty()) "Koi skill installed nahi" else s.installedSkills.sorted().joinToString(", "), "Skill store se add karo")
                )),
                SettingGroup("Store", listOf(
                    nav("sk_store", "Skill store", "Online playbooks dekho")
                ))
            ))
        },
        Section("advanced", "Advanced", "Behaviour, safety, permissions", Icons.Default.Settings) { s ->
            SettingSectionDef("advanced", "Advanced", listOf(
                SettingGroup("Look aur feel", listOf(
                    nav("adv_theme", "Theme", "Colours, font, text size aur corners"),
                    nav("adv_appearance", "Appearance", "Orb style, colour aur size"),
                    nav("adv_behaviour", "Behaviour", "Floating orb, echo guard, boot pe start")
                )),
                SettingGroup("Safety aur access", listOf(
                    nav("adv_screenlock", "Screen lock", "Phone jagana, aur tumhare liye unlock"),
                    nav("adv_faces", "Chehre", "Pehchane, sirf isi phone pe"),
                    nav("adv_sos", "Emergency SOS", "Contacts, siren aur kya bheja jaata hai"),
                    nav("adv_touchguard", "Touch Guard", "Tum door ho tab phone pe nazar"),
                    nav("perm", "Permissions", "Features ke liye zaroori access do")
                )),
                SettingGroup("System", listOf(
                    nav("adv_guardian", "Voice Guardian", "Sirf tumhari awaaz ka jawab de"),
                    nav("adv_awareness", "Awareness", "Selective attention, aur chup kyun"),
                    nav("adv_typing", "Typing", "Editors me insaan jaisi typing"),
                    nav("adv_events", "Event triggers", "Khud se kya batata hai")
                ))
            ))
        },
        Section("battery", "Battery saver", "Battery kam kharch ho", Icons.Default.Warning) { s ->
            SettingSectionDef("battery", "Battery saver", listOf(
                SettingGroup("Saver", listOf(
                    toggle(s, "b_master", "Battery saver ON", "Sabse zyada battery khane wale hisse band", s.batterySaver) { s.batterySaver = it }
                )),
                SettingGroup("Saver me bhi chalu rakho", listOf(
                    toggle(s, "b_wake", "\"Hey Ultron\" sunta rahe", "", s.batteryKeepWake) { s.batteryKeepWake = it },
                    toggle(s, "b_orb", "3D character rakho", "Home pe orb ki jagah", s.batteryKeepOrb) { s.batteryKeepOrb = it },
                    toggle(s, "b_guard", "Voice Guardian check kare", "", s.batteryKeepGuardian) { s.batteryKeepGuardian = it }
                ))
            ))
        },
        Section("backup", "Backup", "Yaadein aur chats", Icons.Default.Storage) { s ->
            SettingSectionDef("backup", "Backup", listOf(
                SettingGroup("Data", listOf(
                    nav("bk_export", "Export karo", "Yaadein aur chats save"),
                    nav("bk_restore", "Restore karo", "Pichhla backup wapas"),
                    info("bk_where", "Device pe data", "Sab kuch isi phone pe rehta hai")
                ))
            ))
        },
        Section("secret", "Secret settings", "Partner mode (18+)", Icons.Default.Security) { s ->
            SettingSectionDef("secret", "Secret settings", listOf(
                SettingGroup("Partner mode", listOf(
                    choice(s, "sec_level", "Partner mode", "Ultron kitna kareeb hai", listOf("Friendly", "Romantic", "Intense (18+)"), s.partnerMode) { s.partnerMode = it }
                )),
                SettingGroup("Age confirmation", listOf(
                    text(s, "sec_year", "Janam ka saal", "Intense ke liye", s.birthYear, "jaise 1998") { s.birthYear = it }
                ))
            ))
        },
        Section("optional", "Optional", "Maps / Places", Icons.Default.Wallpaper) { s ->
            SettingSectionDef("optional", "Optional", listOf(
                SettingGroup("Integrations", listOf(
                    password(s, "o_maps", "Maps / Places API key", "Jagah dhoondhne ke liye (optional)", s.mapsKey, "AIza…") { s.mapsKey = it },
                    password(s, "o_search", "Search API key", "Behtar search sources (optional)", s.searchKey, "API key…") { s.searchKey = it },
                    password(s, "o_image", "Image generation key", "AI images (optional)", s.imageKey, "API key…") { }
                ))
            ))
        },
        Section("interface", "Interface", "App ki bhasha aur theme", Icons.Default.Face) { s ->
            SettingSectionDef("interface", "Interface", listOf(
                SettingGroup("Theme", listOf(
                    choice(s, "i_theme", "Style", "Theme rang", listOf("Dark", "Light"), s.themeStyle) { s.themeStyle = it },
                    choice(s, "i_lang", "App ki bhasha", "Screens ki bhasha", listOf("System", "Hindi", "English"), s.uiLanguage) { s.uiLanguage = it }
                )),
                SettingGroup("Baat-cheet bhasha", listOf(
                    choice(s, "i_clang", "Ultron ki bhasha", "Kaunsi bhasha bolta hai", listOf("Hindi", "English", "Hinglish"), s.language) { s.language = it }
                ))
            ))
        },
        Section("privacy", "Privacy Policy", "Data kaise store hota hai", Icons.Default.Lock) { s ->
            SettingSectionDef("privacy", "Privacy Policy", listOf(
                SettingGroup("Policy", listOf(
                    info("pri_1", "Device pe data", "Sab kuch isi phone pe rehta hai — koi server nahi"),
                    info("pri_2", "Awaaz", "Mic audio siwaye Gemini API ke kisi ko nahi jaata"),
                    info("pri_3", "Analytics", "Na analytics, na trackers, na ads"),
                    info("pri_4", "Control", "Kabhi bhi yaadein delete kar sakte ho")
                ))
            ))
        }
    )

    private fun toggle(s: SettingsStore, id: String, t: String, sub: String, checked: Boolean, on: (Boolean) -> Unit) =
        SettingItem(id, SettingItem.Kind.TOGGLE, t, sub, checked = checked, onToggle = on)

    private fun text(s: SettingsStore, id: String, t: String, sub: String, value: String, hint: String, on: (String) -> Unit) =
        SettingItem(id, SettingItem.Kind.TEXT, t, sub, value = value, hint = hint, onValueChange = on)

    private fun password(s: SettingsStore, id: String, t: String, sub: String, value: String, hint: String, on: (String) -> Unit) =
        SettingItem(id, SettingItem.Kind.PASSWORD, t, sub, value = value, hint = hint, onValueChange = on)

    private fun choice(s: SettingsStore, id: String, t: String, sub: String, choices: List<String>, selected: String, on: (String) -> Unit) =
        SettingItem(id, SettingItem.Kind.CHOICE, t, sub, choices = choices, selected = selected, onSelect = on)

    private fun nav(id: String, t: String, sub: String) =
        SettingItem(id, SettingItem.Kind.NAV, t, sub)

    private fun info(id: String, t: String, sub: String) =
        SettingItem(id, SettingItem.Kind.INFO, t, sub)

    fun section(id: String, settings: SettingsStore): SettingSectionDef? =
        hubSections(settings).firstOrNull { it.id == id }?.builder?.invoke(settings)
}
