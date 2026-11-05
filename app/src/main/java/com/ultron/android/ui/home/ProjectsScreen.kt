package com.ultron.android.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Project(
    val id: Long,
    val name: String,
    val desc: String,
    val progress: Int,
    val status: String,
    val next: String
)

/**
 * In-memory project list (demo se liya gaya). Process band hote hi reset ho jayegi.
 * Persist karna ho to Room / DataStore mein move karna hoga.
 */
object ProjectStore {
    val projects = mutableStateListOf(
        Project(1, "Ultron", "Voice assistant with tools for calls, messages and GitHub.", 72, "Active", "Finish the permission screen and build the APK."),
        Project(2, "Yesmin AI", "Companion app built on Gemini.", 40, "Paused", "Fix the chat history bug."),
        Project(3, "Kotlin practice", "Small Compose exercises every week.", 25, "Active", "Build a todo list with Compose.")
    )
    private var nextId = 4L

    fun add(name: String) {
        projects.add(Project(nextId++, name, "New project.", 0, "Active", "Write the first task."))
    }

    fun update(id: Long, transform: (Project) -> Project) {
        val i = projects.indexOfFirst { it.id == id }
        if (i >= 0) projects[i] = transform(projects[i])
    }
}

private val STATUSES = listOf("Active", "Paused", "Done")

@Composable
fun ProjectsScreen(onAskUltron: (String) -> Unit) {
    var newName by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = ProjectStore.projects.firstOrNull { it.id == selectedId }

    Column(modifier = Modifier.padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("New project name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val n = newName.trim()
                    if (n.isNotEmpty()) {
                        ProjectStore.add(n)
                        newName = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Add") }
        }

        Text(
            "ALL PROJECTS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.navigationBarsPadding()) {
            items(ProjectStore.projects, key = { it.id }) { p ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedId = p.id }
                        .padding(vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            p.status.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { p.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${p.progress}% complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                HorizontalDivider()
            }
        }
    }

    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { selectedId = null }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding()) {
                Text(selected.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    selected.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text("PROGRESS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 20.dp))
                LinearProgressIndicator(
                    progress = { selected.progress / 100f },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    OutlinedButton(onClick = {
                        ProjectStore.update(selected.id) { it.copy(progress = (it.progress - 10).coerceAtLeast(0)) }
                    }) { Text("Minus 10") }
                    Button(onClick = {
                        ProjectStore.update(selected.id) { it.copy(progress = (it.progress + 10).coerceAtMost(100)) }
                    }) { Text("Plus 10") }
                }

                Text("STATUS", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    STATUSES.forEach { s ->
                        FilterChip(
                            selected = selected.status == s,
                            onClick = { ProjectStore.update(selected.id) { it.copy(status = s) } },
                            label = { Text(s) }
                        )
                    }
                }

                Text("NEXT STEP", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 20.dp))
                Text(selected.next, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))

                Button(
                    onClick = {
                        selectedId = null
                        onAskUltron("Show status of ${selected.name}")
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp)
                ) { Text("Ask Ultron about ${selected.name}") }
            }
        }
    }
}
