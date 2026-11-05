package com.ultron.android.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Ek clickable / non-clickable row: title, sub, aur right side par control. */
@Composable
fun SettingRow(
    title: String,
    sub: String = "",
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        trailing()
    }
}

/**
 * Ek item ka control. [onAction] NAV / BUTTON rows ke liye (id bhejta hai),
 * [onChoice] CHOICE rows ke liye (dialog kholne ke liye).
 */
@Composable
fun SettingControl(
    item: SettingItem,
    onAction: (String) -> Unit = {},
    onChoice: (SettingItem) -> Unit = {}
) {
    when (item.kind) {
        SettingItem.Kind.TOGGLE -> SettingRow(item.title, item.sub) {
            Switch(checked = item.checked, onCheckedChange = item.onToggle)
        }
        SettingItem.Kind.NAV, SettingItem.Kind.BUTTON -> SettingRow(
            item.title,
            item.sub,
            onClick = { onAction(item.id) }
        ) {
            Text(
                text = ">",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        SettingItem.Kind.INFO -> SettingRow(item.title, item.sub) { }
        SettingItem.Kind.TEXT -> SettingRow(item.title, item.sub) {
            TextField(
                value = item.value,
                onValueChange = item.onValueChange,
                placeholder = { Text(item.hint, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.widthIn(max = 180.dp),
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
        SettingItem.Kind.PASSWORD -> SettingRow(item.title, item.sub) {
            TextField(
                value = item.value,
                onValueChange = item.onValueChange,
                placeholder = { Text(item.hint, style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.widthIn(max = 180.dp),
                shape = RoundedCornerShape(8.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
        SettingItem.Kind.CHOICE -> SettingRow(
            item.title,
            item.sub,
            onClick = { onChoice(item) }
        ) {
            Text(
                text = item.selected,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

/** Ek group ka card. */
@Composable
fun SettingCard(
    header: String,
    items: List<SettingItem>,
    onAction: (String) -> Unit = {},
    onChoice: (SettingItem) -> Unit = {}
) {
    Column {
        if (header.isNotEmpty()) {
            Text(
                text = header,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) {
            Column {
                items.forEachIndexed { i, item ->
                    SettingControl(item, onAction, onChoice)
                    if (i < items.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}
