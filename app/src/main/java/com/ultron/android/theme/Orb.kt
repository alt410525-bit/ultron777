package com.ultron.android.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private val OrbBlue = Color(0xFF3B6FB6)
private val OrbBlueSoft = Color(0xFF9DBBE6)

/**
 * Glass HUD orb jaisa Maya reference mein hai: tick ring, rotating arcs,
 * crosshairs aur glass core. Awaaz aaye to volume ke hisaab se phoolta hai.
 */
@Composable
fun Orb(
    modifier: Modifier = Modifier,
    diameter: Dp = 280.dp,
    active: Boolean = false,
    volume: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
        label = "pulse"
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000), RepeatMode.Restart),
        label = "spin"
    )
    val scale = if (active) 1f + volume.coerceIn(0f, 1f) * 0.12f else pulse

    Canvas(modifier = modifier.size(diameter)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension / 2f * scale

        // Halka blue glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(OrbBlueSoft.copy(alpha = 0.6f), Color.Transparent),
                center = c,
                radius = base * 1.2f
            ),
            radius = base * 1.2f,
            center = c
        )

        // Glass core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.95f), Color(0xFFE3EDFB), Color(0xFFBCD2EE)),
                center = Offset(c.x - base * 0.25f, c.y - base * 0.3f),
                radius = base * 0.95f
            ),
            radius = base * 0.9f,
            center = c
        )

        // Patli bahar wali ring
        drawCircle(
            color = OrbBlue.copy(alpha = 0.25f),
            radius = base,
            center = c,
            style = Stroke(width = 1.dp.toPx())
        )

        // Tick marks (72 total, har 6th bada)
        for (i in 0 until 72) {
            val a = Math.toRadians(i * 5.0 - 90.0)
            val major = i % 6 == 0
            val inner = base * (if (major) 0.88f else 0.94f)
            val outer = base * 0.99f
            val cosA = cos(a).toFloat()
            val sinA = sin(a).toFloat()
            drawLine(
                color = OrbBlue.copy(alpha = if (major) 0.6f else 0.3f),
                start = Offset(c.x + inner * cosA, c.y + inner * sinA),
                end = Offset(c.x + outer * cosA, c.y + outer * sinA),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Crosshair plus marks
        listOf(-90.0, -35.0, 0.0, 90.0).forEach { deg ->
            val a = Math.toRadians(deg)
            val px = c.x + base * 1.08f * cos(a).toFloat()
            val py = c.y + base * 1.08f * sin(a).toFloat()
            val arm = 7.dp.toPx()
            val w = 1.5.dp.toPx()
            drawLine(OrbBlue.copy(alpha = 0.7f), Offset(px - arm, py), Offset(px + arm, py), w)
            drawLine(OrbBlue.copy(alpha = 0.7f), Offset(px, py - arm), Offset(px, py + arm), w)
        }

        // Ghoomte hue arcs
        rotate(degrees = spin, pivot = c) {
            arc(base * 0.82f, c, 200f, 75f, OrbBlue.copy(alpha = 0.9f), 7.dp.toPx())
            arc(base * 0.82f, c, 285f, 45f, OrbBlue.copy(alpha = 0.6f), 4.dp.toPx())
            arc(base * 0.82f, c, 50f, 50f, OrbBlue.copy(alpha = 0.6f), 4.dp.toPx())
            arc(base * 0.90f, c, 320f, 100f, Color.White.copy(alpha = 0.95f), 9.dp.toPx())
        }
    }
}

private fun DrawScope.arc(
    radius: Float,
    center: Offset,
    start: Float,
    sweep: Float,
    color: Color,
    width: Float
) {
    drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = width, cap = StrokeCap.Round)
    )
}
