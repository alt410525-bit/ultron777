package com.ultron.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultron.android.chat.ChatScreen
import com.ultron.android.chat.ChatViewModel
import com.ultron.android.chat.UiPhase
import com.ultron.android.theme.Orb
import com.ultron.android.theme.UltronPrimary
import com.ultron.android.ui.components.GlassIconButton
import com.ultron.android.ui.components.StatusBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAB_HOME = 0
private const val TAB_SCAN = 1
private const val TAB_YAADEIN = 2
private const val TAB_CHAT = 3

/**
 * Main shell: top bar (menu + title), tab content, aur bottom navigation
 * (Home, Scan, mic, Yaadein, Chat). Menu (3 lines) mein saari settings hain
 * jo pehle gear button se khulti thin.
 */
/**
 * Main shell: top bar (menu + title), tab content, aur bottom navigation.
 * Hamburger (3 lines) Menu screen kholta hai, jahan saari settings, tools aur permissions hain.
 */
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onOpenMenu: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    val phase by viewModel.phase.collectAsState()
    val micError by viewModel.micError.collectAsState()
    val recognizerActive by viewModel.recognizerActive.collectAsState()
    val recognizerVolume by viewModel.recognizerVolume.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE6EEFB), Color(0xFFC7D8F2))))
            .statusBarsPadding()
    ) {
        TopBar(onMenu = onOpenMenu)

        micError?.let { err ->
            StatusBanner(
                message = err,
                isError = true,
                onDismiss = { viewModel.clearMicError() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                TAB_HOME -> HomeContent(
                    orbActive = recognizerActive || phase is UiPhase.Speaking,
                    volume = recognizerVolume,
                    onChip = { prompt ->
                        viewModel.sendText(prompt)
                        tab = TAB_CHAT
                    }
                )
                TAB_SCAN -> ComingSoon("Scan jald aayega")
                TAB_YAADEIN -> ComingSoon("Yaadein jald aayega")
                else -> ChatScreen(viewModel = viewModel)
            }
        }

        BottomNav(
            selected = tab,
            onSelect = { tab = it },
            onMic = {
                if (phase is UiPhase.Listening) viewModel.stopSession() else viewModel.startListening()
                tab = TAB_CHAT
            }
        )
    }
}

@Composable
private fun TopBar(onMenu: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(icon = Icons.Default.Menu, description = "Menu", onClick = onMenu)
        Spacer(Modifier.width(18.dp))
        Text(
            text = "Ultron",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun HomeContent(orbActive: Boolean, volume: Float, onChip: (String) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000L)
        }
    }
    val date = Date(now)
    val dayNumber = SimpleDateFormat("d", Locale.getDefault()).format(date)
    val dayName = SimpleDateFormat("EEE, MMM", Locale.getDefault()).format(date)
    val clock = SimpleDateFormat("hh:mm", Locale.getDefault()).format(date)
    val amPm = SimpleDateFormat("a", Locale.getDefault()).format(date)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Orb(active = orbActive, volume = volume)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AI ASSISTANT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "U.L.T.R.O.N",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF2E4A7A),
                                Color(0xFF4F7CC0),
                                Color(0xFF7C5CC4),
                                Color(0xFFC05CA8)
                            )
                        ),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "BOLO, KAISE HELP KARUN?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeChip("Songs", Icons.Default.MusicNote, Modifier.weight(1f)) {
                onChip("Mera favorite gaana chalao")
            }
            HomeChip("Study", Icons.Default.MenuBook, Modifier.weight(1f)) {
                onChip("Padhai mein help karo")
            }
            HomeChip("Journal", Icons.Default.Edit, Modifier.weight(1f)) {
                onChip("Aaj ka journal likhna hai")
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                modifier = Modifier.weight(1f),
                label = "Weather",
                icon = Icons.Default.Cloud,
                value = "--°",
                sub = "No data"
            )
            InfoCard(
                modifier = Modifier.weight(1f),
                label = "Today",
                icon = Icons.Default.CalendarToday,
                value = dayNumber,
                sub = dayName
            )
            InfoCard(
                modifier = Modifier.weight(1f),
                label = "Time",
                icon = Icons.Default.Schedule,
                value = clock,
                sub = amPm
            )
        }

        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun HomeChip(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(52.dp)
            .shadow(6.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    value: String,
    sub: String
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .height(150.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit, onMic: () -> Unit) {
    val pill = RoundedCornerShape(36.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(10.dp, pill)
                .clip(pill)
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem("Home", Icons.Default.Home, selected == TAB_HOME, Modifier.weight(1f)) {
                onSelect(TAB_HOME)
            }
            NavItem("Scan", Icons.Default.CenterFocusWeak, selected == TAB_SCAN, Modifier.weight(1f)) {
                onSelect(TAB_SCAN)
            }
            Spacer(Modifier.width(64.dp))
            NavItem("Yaadein", Icons.Default.Bookmark, selected == TAB_YAADEIN, Modifier.weight(1f)) {
                onSelect(TAB_YAADEIN)
            }
            NavItem("Chat", Icons.Default.Chat, selected == TAB_CHAT, Modifier.weight(1f)) {
                onSelect(TAB_CHAT)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .size(72.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(UltronPrimary, Color(0xFF60A5FA))))
                .clickable(onClick = onMic),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Mic",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) UltronPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun ComingSoon(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
