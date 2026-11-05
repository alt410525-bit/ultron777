package com.ultron.android.tools

import com.ultron.android.core.tools.ToolRegistry
import com.ultron.android.data.SettingsStore
import com.ultron.android.tools.ai.AiToolModule
import com.ultron.android.tools.assistant.AssistantToolModule
import com.ultron.android.tools.connectors.ConnectorToolModule
import com.ultron.android.tools.developer.DeveloperToolModule
import com.ultron.android.tools.device.DeviceToolModule
import com.ultron.android.tools.messaging.MessagingToolModule
import com.ultron.android.tools.personal.PersonalToolModule
import com.ultron.android.tools.system.SystemToolModule
import com.ultron.android.tools.utility.UtilityToolModule

/**
 * Naya batch aaye to yahan module add karo.
 * Naya tool likhne ka tarika: kisi module ki list mein SpecTool(...) { args, ctx -> ... } jodo.
 * Id unique honi chahiye (registry start par check karta hai).
 */
fun buildToolRegistry(settings: SettingsStore): ToolRegistry = ToolRegistry(
    listOf(
        MessagingToolModule(),
        ConnectorToolModule(),
        PersonalToolModule(settings),
        AssistantToolModule(settings),
        SystemToolModule(settings),
        DeviceToolModule(),
        DeveloperToolModule(),
        UtilityToolModule(),
        AiToolModule(settings),
        Batch3ConnectorModule(),
        Batch3DeveloperModule(),
        Batch3UtilityModule(),
        Batch3DeviceModule(),
        Batch3MessagingModule()
    )
)
