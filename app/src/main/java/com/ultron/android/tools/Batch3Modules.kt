package com.ultron.android.tools

import com.ultron.android.core.tools.Tool
import com.ultron.android.core.tools.ToolModule
import com.ultron.android.tools.connectors.GithubBuildFailuresTool
import com.ultron.android.tools.connectors.GithubCommentIssueTool
import com.ultron.android.tools.connectors.GithubCommitHistoryTool
import com.ultron.android.tools.developer.CodeCleanTool
import com.ultron.android.tools.developer.JsonToKotlinTool
import com.ultron.android.tools.developer.NoteReadTool
import com.ultron.android.tools.developer.NoteSaveTool
import com.ultron.android.tools.developer.PortStatusTool
import com.ultron.android.tools.developer.WebhookTestTool
import com.ultron.android.tools.device.AlarmSetTool
import com.ultron.android.tools.device.CalendarAddTool
import com.ultron.android.tools.device.SensorReadTool
import com.ultron.android.tools.device.ShareTextTool
import com.ultron.android.tools.device.TimerSetTool
import com.ultron.android.tools.messaging.ContactAddTool
import com.ultron.android.tools.messaging.ContactSearchTool
import com.ultron.android.tools.messaging.SmsSearchTool
import com.ultron.android.tools.utility.PingTool
import com.ultron.android.tools.utility.TextDecryptTool
import com.ultron.android.tools.utility.TextEncryptTool

/** Batch 3: 20 naye tools. Har module ToolCatalog mein register hota hai. */
class Batch3ConnectorModule : ToolModule {
    override val tools: List<Tool> = listOf(
        GithubCommitHistoryTool(),
        GithubCommentIssueTool(),
        GithubBuildFailuresTool()
    )
}

class Batch3DeveloperModule : ToolModule {
    override val tools: List<Tool> = listOf(
        CodeCleanTool(),
        WebhookTestTool(),
        PortStatusTool(),
        NoteSaveTool(),
        NoteReadTool(),
        JsonToKotlinTool()
    )
}

class Batch3UtilityModule : ToolModule {
    override val tools: List<Tool> = listOf(
        TextEncryptTool(),
        TextDecryptTool(),
        PingTool()
    )
}

class Batch3DeviceModule : ToolModule {
    override val tools: List<Tool> = listOf(
        AlarmSetTool(),
        TimerSetTool(),
        ShareTextTool(),
        CalendarAddTool(),
        SensorReadTool()
    )
}

class Batch3MessagingModule : ToolModule {
    override val tools: List<Tool> = listOf(
        SmsSearchTool(),
        ContactSearchTool(),
        ContactAddTool()
    )
}
