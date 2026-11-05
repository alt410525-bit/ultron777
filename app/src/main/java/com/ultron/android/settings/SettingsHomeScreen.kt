package com.ultron.android.settings

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.android.data.SettingsStore

/** Reusable screen scaffold: top bar + content. Status bar aur nav bar ke bahar rehta hai. */
@Composable
fun SettingsScaffold(
    title: String,
    onBack: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}

/** Settings home — the hub, exactly like Maya's settings list. */
@Composable
fun SettingsHomeScreen(
    onBack: () -> Unit,
    onOpenSection: (String) -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember(context) { SettingsStore(context.applicationContext) }
    val sections = remember(settings) { SettingsRegistry.hubSections(settings) }

    SettingsScaffold(title = "Settings", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "Ultron",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            item {
                HubCard(title = "Assistant", sub = "Persona, awaaz, bhasha",
                    onClick = { onOpenSection("assistant") })
                HubCard(title = "Personal", sub = "Naam, gemini key, music",
                    onClick = { onOpenSection("personal") })
                HubCard(title = "Messages aur WhatsApp", sub = "Alerts, auto-reply, calls",
                    onClick = { onOpenSection("messages") })
            }
            item {
                Text(text = "Jude hue accounts", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
            }
            item {
                HubCard(title = "Connectors", sub = "GitHub, Notion, Telegram",
                    onClick = { onOpenSection("connectors") })
                HubCard(title = "Skills", sub = "Playbooks aur skills",
                    onClick = { onOpenSection("skills") })
            }
            item {
                Text(text = "Kaam aur messages", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
            }
            item {
                HubCard(title = "Advanced", sub = "Behaviour, safety, permissions",
                    onClick = { onOpenSection("advanced") })
                HubCard(title = "Permissions", sub = "Features ke liye zaroori access",
                    onClick = onOpenPermissions)
                HubCard(title = "Battery saver", sub = "Battery kam kharch ho",
                    onClick = { onOpenSection("battery") })
            }
            item {
                Text(text = "Yaadein aur data", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp))
            }
            item {
                HubCard(title = "Backup", sub = "Yaadein aur chats export/restore",
                    onClick = { onOpenSection("backup") })
                HubCard(title = "Secret settings", sub = "Partner mode (18+)",
                    onClick = { onOpenSection("secret") })
                HubCard(title = "Interface", sub = "Theme aur bhasha",
                    onClick = { onOpenSection("interface") })
                HubCard(title = "Optional", sub = "Maps / Places",
                    onClick = { onOpenSection("optional") })
                HubCard(title = "Privacy Policy", sub = "Data kaise store hota hai",
                    onClick = { onOpenSection("privacy") })
            }
        }
    }
}

@Composable
fun HubCard(title: String, sub: String, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                if (sub.isNotEmpty()) {
                    Text(sub, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
