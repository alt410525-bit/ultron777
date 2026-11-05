package com.ultron.android.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ultron.android.data.SettingsStore
import com.ultron.android.settings.SettingCard
import com.ultron.android.settings.SettingItem
import com.ultron.android.settings.SettingsScaffold
import com.ultron.android.theme.UltronPrimary

private data class MenuGroup(val title: String, val rows: List<Triple<String, String, String>>)

/** id, title, sub. id ka matlab MenuScreen ke onAction mein dekho. */
private val MENU_GROUPS = listOf(
    MenuGroup(
        "Ultron",
        listOf(
            Triple("section:assistant", "Assistant", "Persona, awaaz, bhasha"),
            Triple("section:personal", "Personal", "Naam, Gemini key, music"),
            Triple("section:messages", "Messages aur WhatsApp", "Alerts, auto-reply, calls")
        )
    ),
    MenuGroup(
        "Tools aur access",
        listOf(
            Triple("tools", "Tools", "Ultron kya kya kar sakta hai"),
            Triple("perm", "Permissions", "Features ke liye zaroori access")
        )
    ),
    MenuGroup(
        "Jude hue accounts",
        listOf(
            Triple("section:connectors", "Connectors", "GitHub, Notion, Telegram"),
            Triple("section:skills", "Skills", "Playbooks aur skills")
        )
    ),
    MenuGroup(
        "Kaam aur messages",
        listOf(
            Triple("section:advanced", "Advanced", "Behaviour, safety, system"),
            Triple("section:battery", "Battery saver", "Battery kam kharch ho")
        )
    ),
    MenuGroup(
        "Yaadein aur data",
        listOf(
            Triple("section:backup", "Backup", "Yaadein aur chats export/restore"),
            Triple("section:secret", "Secret settings", "PIN lock"),
            Triple("section:interface", "Interface", "Theme aur bhasha"),
            Triple("section:optional", "Optional", "Maps aur API keys"),
            Triple("section:privacy", "Privacy Policy", "Data kaise store hota hai"),
            Triple("hub", "Settings (sab ek jagah)", "Poori settings ki list")
        )
    )
)

/**
 * 3-line menu. Yahan se har settings section, tools list aur permissions khulte hain.
 * [onAction] ko Nav handle karta hai.
 */
@Composable
fun MenuScreen(onBack: () -> Unit, onAction: (String) -> Unit) {
    val context = LocalContext.current
    val settings = remember(context) { SettingsStore(context.applicationContext) }
    SettingsScaffold(title = "Menu", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                ProfileCard(
                    name = settings.userName.ifBlank { "Dost" },
                    onClick = { onAction("section:personal") }
                )
            }
            items(MENU_GROUPS, key = { it.title }) { group ->
                SettingCard(
                    header = group.title,
                    items = group.rows.map { (id, title, sub) ->
                        SettingItem(id = id, kind = SettingItem.Kind.NAV, title = title, sub = sub)
                    },
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(UltronPrimary, Color(0xFF7C5CC4))))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Profile aur Gemini key yahan",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
    }
}
