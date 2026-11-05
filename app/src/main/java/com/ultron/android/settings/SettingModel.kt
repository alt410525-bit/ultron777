package com.ultron.android.settings

import androidx.annotation.StringRes

/**
 * A single row in a settings section. The [kind] decides which control
 * Compose renders (toggle, text, choice, password, nav).
 */
data class SettingItem(
    val id: String,
    val kind: Kind,
    val title: String,
    val sub: String = "",
    val choices: List<String> = emptyList(),
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,

    /** Toggle switch state. */
    val checked: Boolean = false,
    val onToggle: (Boolean) -> Unit = {},

    /** Text field state. */
    val value: String = "",
    val onValueChange: (String) -> Unit = {},
    val hint: String = "",

    /** Choice selection. */
    val selected: String = "",
    val onSelect: (String) -> Unit = {}
) {
    enum class Kind { TOGGLE, TEXT, PASSWORD, CHOICE, NAV, INFO, BUTTON }
}

/** A settings screen = a group of items under a header. */
data class SettingGroup(
    val header: String = "",
    val items: List<SettingItem>
)

/** A whole settings section (one screen). */
data class SettingSectionDef(
    val id: String,
    val title: String,
    val groups: List<SettingGroup>
)
