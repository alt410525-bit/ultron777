package com.ultron.android.ui.tools

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ultron.android.core.tools.RiskLevel
import com.ultron.android.core.tools.Tool
import com.ultron.android.core.tools.ToolCategory
import com.ultron.android.core.tools.ToolContext
import com.ultron.android.core.tools.ToolResult
import com.ultron.android.data.SettingsStore
import com.ultron.android.settings.SettingsScaffold
import com.ultron.android.tools.buildToolRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Saare tools, category ke hisaab se. Tool par tap karke chala sakte ho. */
@Composable
fun ToolsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appCtx = context.applicationContext
    val settings = remember(context) { SettingsStore(appCtx) }
    val registry = remember(context) { buildToolRegistry(settings) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Tool?>(null) }

    val all = registry.all()
    val filtered = all.filter { t ->
        query.isBlank() || "${t.title} ${t.description} ${t.id}".contains(query.trim(), ignoreCase = true)
    }
    val grouped = ToolCategory.entries
        .map { cat -> cat to filtered.filter { it.category == cat }.sortedBy { it.title } }
        .filter { it.second.isNotEmpty() }

    SettingsScaffold(title = "Tools (${all.size})", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Tool dhoondho") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Text(
                        text = "Har tool ka kaam, zaroori permission aur risk yahan hai. Tool par tap karo aur chalao.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                grouped.forEach { (cat, list) ->
                    item(key = cat.name) {
                        Column {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                list.forEachIndexed { i, tool ->
                                    ToolRow(tool) { selected = tool }
                                    if (i < list.size - 1) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { tool ->
        ToolDialog(tool = tool, appCtx = appCtx, onDismiss = { selected = null })
    }
}

@Composable
private fun ToolRow(tool: Tool, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RiskBadge(tool.risk)
                if (tool.permissions.isNotEmpty()) {
                    Text(
                        text = tool.permissions.joinToString { it.substringAfterLast('.') },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RiskBadge(risk: RiskLevel) {
    val (label, color) = when (risk) {
        RiskLevel.LOW -> "Safe" to Color(0xFF1E8E5A)
        RiskLevel.MEDIUM -> "Dhyan se" to Color(0xFFE08A1E)
        RiskLevel.HIGH -> "Confirm lagega" to Color(0xFFD64545)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun ToolDialog(tool: Tool, appCtx: Context, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val values = remember(tool.id) { mutableStateMapOf<String, String>() }
    var result by remember(tool.id) { mutableStateOf<String?>(null) }
    var error by remember(tool.id) { mutableStateOf<String?>(null) }
    var running by remember(tool.id) { mutableStateOf(false) }
    var confirmRequest by remember(tool.id) {
        mutableStateOf<Pair<String, CompletableDeferred<Boolean>>?>(null)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (tool.permissions.isNotEmpty()) {
                    Text(
                        text = "Zaroori permission: " + tool.permissions.joinToString { it.substringAfterLast('.') },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (tool.params.isEmpty()) {
                    Text(
                        text = "Is tool ko kisi input ki zaroorat nahi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                tool.params.forEach { p ->
                    OutlinedTextField(
                        value = values[p.name].orEmpty(),
                        onValueChange = { values[p.name] = it },
                        label = { Text("${p.name}${if (p.required) " *" else ""}") },
                        supportingText = { Text(p.description) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                result?.let {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Band karo") }
                    Button(
                        enabled = !running,
                        onClick = {
                            val built = buildArgs(tool, values)
                            val args = built.first
                            if (args == null) {
                                error = built.second
                            } else {
                                error = null
                                running = true
                                scope.launch {
                                    val confirm: suspend (String) -> Boolean = { question ->
                                        val deferred = CompletableDeferred<Boolean>()
                                        confirmRequest = question to deferred
                                        val ok = deferred.await()
                                        confirmRequest = null
                                        ok
                                    }
                                    result = try {
                                        when (val r = tool.execute(args, ToolContext(appCtx, confirm))) {
                                            is ToolResult.Ok -> r.text
                                            is ToolResult.Error -> "Error: ${r.message}"
                                            is ToolResult.NeedsPermission ->
                                                "Permission chahiye: ${r.permissions.joinToString()}"
                                        }
                                    } catch (e: Exception) {
                                        "Error: ${e.message ?: "kuch gadbad hui"}"
                                    }
                                    running = false
                                }
                            }
                        }
                    ) {
                        Text(if (running) "Chal raha hai…" else "Chalao")
                    }
                }
            }
        }
    }

    // Risky tools ke liye pakka confirm
    confirmRequest?.let { (question, deferred) ->
        AlertDialog(
            onDismissRequest = { deferred.complete(false) },
            title = { Text("Pakka karo") },
            text = { Text(question) },
            confirmButton = {
                TextButton(onClick = { deferred.complete(true) }) { Text("Haan, chalao") }
            },
            dismissButton = {
                TextButton(onClick = { deferred.complete(false) }) { Text("Nahi") }
            }
        )
    }
}

/** Form ki values ko tool ke parameter types mein badlo. */
private fun buildArgs(tool: Tool, values: Map<String, String>): Pair<JsonObject?, String?> {
    val out = mutableMapOf<String, JsonElement>()
    for (p in tool.params) {
        val raw = values[p.name]?.trim().orEmpty()
        if (raw.isEmpty()) {
            if (p.required) return Pair(null, "${p.name} bharna zaroori hai")
            continue
        }
        val value: JsonElement? = when (p.type) {
            "integer" -> raw.toIntOrNull()?.let { JsonPrimitive(it) }
            "number" -> raw.toDoubleOrNull()?.let { JsonPrimitive(it) }
            "boolean" -> JsonPrimitive(raw.lowercase() in setOf("true", "haan", "ha", "yes", "1"))
            else -> JsonPrimitive(raw)
        }
        if (value == null) return Pair(null, "${p.name} sahi format mein do (${p.type})")
        out[p.name] = value
    }
    return Pair(JsonObject(out), null)
}
