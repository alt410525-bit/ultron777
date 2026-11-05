package com.ultron.android.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ultron.android.core.tools.ToolContext
import com.ultron.android.core.tools.ToolRegistry
import com.ultron.android.core.tools.ToolResult
import com.ultron.android.data.ConnectorSecrets
import com.ultron.android.data.SettingsStore
import com.ultron.android.tools.buildToolRegistry
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Skill store ke items (Installed list ke saath sync). */
private val SKILL_STORE = listOf(
    "calendar" to "Calendar helper",
    "news" to "Daily news",
    "focus" to "Focus mode"
)

private fun connectorKey(id: String): String? = when (id) {
    "c_github" -> ConnectorSecrets.GITHUB
    "c_notion" -> ConnectorSecrets.NOTION
    "c_telegram" -> ConnectorSecrets.TELEGRAM
    else -> null
}

private fun connectorName(id: String): String =
    id.removePrefix("c_").replaceFirstChar { it.uppercase() }

private fun toast(ctx: Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

/** Ek tool chalao aur uska text result lo. */
private suspend fun runTool(registry: ToolRegistry, ctx: Context, id: String, args: JsonObject): String {
    val tool = registry.find(id) ?: return "Tool nahi mila: $id"
    return try {
        when (val r = tool.execute(args, ToolContext(ctx) { false })) {
            is ToolResult.Ok -> r.text
            is ToolResult.Error -> "Error: ${r.message}"
            is ToolResult.NeedsPermission -> "Permission chahiye: ${r.permissions.joinToString()}"
        }
    } catch (e: Exception) {
        "Error: ${e.message ?: "kuch gadbad hui"}"
    }
}

@Composable
fun SettingsSection(
    section: String,
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val appCtx = context.applicationContext
    val settings = remember(context) { SettingsStore(appCtx) }
    val registry = remember(context) { buildToolRegistry(settings) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    // version badhne par section dobara bante hain, taaki toggles turant dikhein
    var version by remember { mutableIntStateOf(0) }
    val def = remember(section, settings, version) { SettingsRegistry.section(section, settings) }

    var dialog by remember { mutableStateOf<String?>(null) }
    var dialogText by remember { mutableStateOf("") }
    var dialogResult by remember { mutableStateOf("") }
    var choiceItem by remember { mutableStateOf<SettingItem?>(null) }

    fun bump() {
        version++
    }

    fun wrap(item: SettingItem): SettingItem = item.copy(
        onToggle = { v -> item.onToggle(v); bump() },
        onValueChange = { v -> item.onValueChange(v); bump() },
        onSelect = { v -> item.onSelect(v); bump() }
    )

    fun onAction(id: String) {
        val key = connectorKey(id)
        when {
            id == "perm" -> onOpenPermissions()
            id == "bk_export" -> {
                dialog = "export"
                dialogResult = "Backup ban raha hai…"
                scope.launch {
                    dialogResult = runTool(registry, appCtx, "ai_backup_create", JsonObject(emptyMap()))
                }
            }
            id == "bk_restore" -> {
                dialog = "restore"
                dialogText = ""
                dialogResult = ""
            }
            key != null -> {
                dialog = id
                dialogText = ""
                dialogResult = if (ConnectorSecrets.get(appCtx, key).isNotBlank()) {
                    "Connected hai. Naya token daaloge to purana badal jaayega."
                } else {
                    "Token daalo, phir Ultron ye account use kar payega."
                }
            }
            id == "sk_store" -> {
                dialog = "skills"
            }
        }
    }

    SettingsScaffold(title = def?.title ?: "Settings", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            def?.groups?.forEachIndexed { index, group ->
                item(key = "group_$index") {
                    SettingCard(
                        header = group.header,
                        items = group.items.map { wrap(it) },
                        onAction = { id -> onAction(id) },
                        onChoice = { choiceItem = it }
                    )
                }
            }
        }
    }

    // ---- Choice dialog (radio list) ----
    choiceItem?.let { item ->
        AlertDialog(
            onDismissRequest = { choiceItem = null },
            title = { Text(item.title) },
            text = {
                Column {
                    item.choices.forEach { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    item.onSelect(c)
                                    bump()
                                    choiceItem = null
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(selected = c == item.selected, onClick = null)
                            Text(c, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { choiceItem = null }) { Text("Band karo") }
            }
        )
    }

    // ---- Action dialogs (backup, restore, connector token, skill store) ----
    val d = dialog
    if (d != null) {
        val title = when {
            d == "export" -> "Backup banao"
            d == "restore" -> "Backup restore"
            d == "skills" -> "Skill store"
            else -> connectorName(d)
        }
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    when {
                        d == "export" -> Text(dialogResult, style = MaterialTheme.typography.bodySmall)
                        d == "restore" -> {
                            OutlinedTextField(
                                value = dialogText,
                                onValueChange = { dialogText = it },
                                label = { Text("Backup JSON paste karo") },
                                minLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (dialogResult.isNotBlank()) {
                                Text(
                                    dialogResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        d == "skills" -> {
                            SKILL_STORE.forEach { (id, name) ->
                                val on = settings.installedSkills.contains(id)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
                                    Text(name, modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = on,
                                        onCheckedChange = { checked ->
                                            settings.installedSkills = if (checked) {
                                                settings.installedSkills + id
                                            } else {
                                                settings.installedSkills - id
                                            }
                                            bump()
                                        }
                                    )
                                }
                            }
                        }
                        else -> {
                            OutlinedTextField(
                                value = dialogText,
                                onValueChange = { dialogText = it },
                                label = { Text("${connectorName(d)} token") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                dialogResult,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                when (d) {
                    "export" -> TextButton(onClick = {
                        clipboard.setText(AnnotatedString(dialogResult))
                        toast(appCtx, "Copy ho gaya")
                    }) { Text("Copy karo") }
                    "restore" -> TextButton(onClick = {
                        scope.launch {
                            dialogResult = runTool(
                                registry,
                                appCtx,
                                "ai_backup_restore",
                                JsonObject(mapOf("json" to JsonPrimitive(dialogText)))
                            )
                        }
                    }) { Text("Restore karo") }
                    "skills" -> TextButton(onClick = { dialog = null }) { Text("Theek hai") }
                    else -> TextButton(onClick = {
                        val key = connectorKey(d)
                        if (key != null && dialogText.isNotBlank()) {
                            ConnectorSecrets.set(appCtx, key, dialogText.trim())
                            toast(appCtx, "${connectorName(d)} save ho gaya")
                            bump()
                            dialog = null
                        } else {
                            toast(appCtx, "Pehle token daalo")
                        }
                    }) { Text("Save karo") }
                }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Band karo") }
            }
        )
    }
}
