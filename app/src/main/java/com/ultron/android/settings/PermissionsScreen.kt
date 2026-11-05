package com.ultron.android.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** Ek capability: runtime permissions ki list (sab ya kuch nahi). */
private data class PermSpec(
    val key: String,
    val title: String,
    val sub: String,
    val perms: List<String>,
    val required: Boolean = false
)

/** UI ki ek row. */
private data class PermRowData(
    val key: String,
    val title: String,
    val sub: String,
    val granted: Boolean,
    val required: Boolean,
    val request: () -> Unit
)

private class PermController(val rows: List<PermRowData>, val requestAll: () -> Unit)

private fun permSpecs(): List<PermSpec> {
    val media = if (Build.VERSION.SDK_INT >= 33) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    return listOf(
        PermSpec(
            "mic", "Microphone", "Ultron ki awaaz sunne ke liye (zaroori)",
            listOf(Manifest.permission.RECORD_AUDIO), required = true
        ),
        PermSpec(
            "calls", "Calls", "Call laga sake, call log dekh sake",
            listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE)
        ),
        PermSpec(
            "sms", "SMS", "Message padh aur bhej sake",
            listOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS)
        ),
        PermSpec(
            "contacts", "Contacts", "Naam bolo to number dhoondh sake",
            listOf(Manifest.permission.READ_CONTACTS)
        ),
        PermSpec("camera", "Camera", "Scan aur photo ke liye", listOf(Manifest.permission.CAMERA)),
        PermSpec(
            "location", "Location", "Raasta aur nearby jagah ke liye",
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        ),
        PermSpec("media", "Media / Files", "Gallery aur files dhoondh sake", media),
        PermSpec(
            "notif", "Notifications", "Alerts dikhane ke liye",
            if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
        )
    )
}

private fun isGranted(ctx: Context, perm: String): Boolean =
    ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

private fun specGranted(ctx: Context, s: PermSpec): Boolean =
    s.perms.isEmpty() || s.perms.all { isGranted(ctx, it) }

private fun isOverlayAllowed(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx)

private fun isBatteryUnrestricted(ctx: Context): Boolean {
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

private fun openOverlaySettings(ctx: Context) {
    try {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

private fun openBatterySettings(ctx: Context) {
    try {
        ctx.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

/**
 * Sab permission rows + "sab allow karo". Har resume par status dobara padhta hai,
 * taaki Settings se wapas aane par sahi status dikhe.
 */
@Composable
private fun rememberPermController(): PermController {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { tick++ }

    val specs = remember { permSpecs() }

    val rows = remember(tick) {
        val runtime = specs.map { s ->
            PermRowData(
                key = s.key,
                title = s.title,
                sub = s.sub,
                granted = specGranted(ctx, s),
                required = s.required
            ) { launcher.launch(s.perms.toTypedArray()) }
        }
        runtime + listOf(
            PermRowData(
                key = "overlay",
                title = "Dusre apps ke upar dikhna",
                sub = "Floating orb ke liye",
                granted = isOverlayAllowed(ctx),
                required = false
            ) { openOverlaySettings(ctx) },
            PermRowData(
                key = "battery",
                title = "Battery: optimization band",
                sub = "Background mein chalta rahe",
                granted = isBatteryUnrestricted(ctx),
                required = false
            ) { openBatterySettings(ctx) }
        )
    }

    val requestAll: () -> Unit = {
        launcher.launch(specs.flatMap { it.perms }.distinct().toTypedArray())
    }

    return PermController(rows, requestAll)
}

/** Settings > Permissions: kabhi bhi badal sakte ho. */
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val controller = rememberPermController()
    SettingsScaffold(title = "Permissions", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "Ultron ko ye permissions chahiye. Sirf wahi allow karo jo tum chahte ho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            item {
                Button(
                    onClick = controller.requestAll,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) { Text("Sab allow karo") }
            }
            items(controller.rows, key = { it.key }) { row ->
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)) {
                    PermRow(row)
                }
            }
        }
    }
}

/**
 * Pehli baar app kholne par dikhta hai. "Aage badho" dabane par dobara nahi aata
 * (Nav mein onboardingDone save hota hai).
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val controller = rememberPermController()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Ultron ko kuch permissions chahiye",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ye sirf ek baar poochha jaata hai. Kisi bhi permission ko baad mein Menu > Permissions se badal sakte ho.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = controller.requestAll, modifier = Modifier.fillMaxWidth()) {
            Text("Sab allow karo")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(controller.rows, key = { it.key }) { row -> PermRow(row) }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Aage badho")
        }
    }
}

@Composable
private fun PermRow(row: PermRowData) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (row.required) "${row.title} *" else row.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = row.sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (row.granted) {
                Text(
                    text = "Allowed",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                TextButton(onClick = row.request) {
                    Text("Allow karo", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
