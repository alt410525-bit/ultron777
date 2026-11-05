package com.ultron.android.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Ultron light palette: soft blue glass, mirroring the Maya reference screen.
val UltronPrimary = Color(0xFF3B82F6)
val UltronOnPrimary = Color(0xFFFFFFFF)
val UltronBackground = Color(0xFFDCE7F8)
val UltronSurface = Color(0xFFFFFFFF)
val UltronSurfaceVariant = Color(0xFFF1F5FD)
val UltronOnSurface = Color(0xFF1B2A4A)
val UltronOutline = Color(0xFFC5D3EA)

private val UltronColors = lightColorScheme(
    primary = UltronPrimary,
    onPrimary = UltronOnPrimary,
    secondary = Color(0xFF7C5CC4),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFC05CA8),
    background = UltronBackground,
    onBackground = UltronOnSurface,
    surface = UltronSurface,
    onSurface = UltronOnSurface,
    surfaceVariant = UltronSurfaceVariant,
    onSurfaceVariant = Color(0xFF6B7A99),
    outline = UltronOutline,
    error = Color(0xFFD64545)
)

private val UltronShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

/** darkTheme ab use nahi hota: app hamesha light palette dikhata hai. */
@Composable
fun UltronTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UltronColors,
        shapes = UltronShapes,
        content = content
    )
}
