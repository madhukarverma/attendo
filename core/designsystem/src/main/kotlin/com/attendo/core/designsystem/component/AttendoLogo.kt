package com.attendo.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Attendo brand mark: an original flat-polygon portrait (a student in a black suit and green tie in
 * front of a cream halo) over bold colour blocks, in a modern poster style. Drawn in code in
 * unit coordinates so it scales cleanly to any size.
 */
@Composable
fun AttendoLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) { drawAttendoLogo() }
}

private object LogoPalette {
    val Orange = Color(0xFFF2542D)
    val Pink = Color(0xFFF28AC9)
    val Teal = Color(0xFF2E9C9A)
    val Yellow = Color(0xFFF2C84B)
    val Halo = Color(0xFFF8FAED)
    val Suit = Color(0xFF1C1C1C)
    val Lapel = Color(0xFF3A3A3A)
    val Shirt = Color(0xFFF8FAED)
    val Tie = Color(0xFF50A65C)
    val Skin = Color(0xFFF0BFA0)
    val SkinShade = Color(0xFFD99A7C)
    val Neck = Color(0xFFE2A988)
    val Hair = Color(0xFF151515)
}

fun DrawScope.drawAttendoLogo() {
    val w = size.width
    val h = size.height
    fun p(x: Float, y: Float) = Offset(x * w, y * h)
    fun poly(color: Color, vararg pts: Pair<Float, Float>) {
        val path = Path().apply {
            moveTo(pts[0].first * w, pts[0].second * h)
            pts.drop(1).forEach { (x, y) -> lineTo(x * w, y * h) }
            close()
        }
        drawPath(path, color)
    }

    // Background colour blocks
    drawRect(LogoPalette.Orange)
    drawRect(LogoPalette.Pink, topLeft = p(0f, 0f), size = Size(0.45f * w, 0.34f * h))
    drawRect(LogoPalette.Teal, topLeft = p(0.78f, 0f), size = Size(0.22f * w, 0.56f * h))
    drawRect(LogoPalette.Yellow, topLeft = p(0f, 0.72f), size = Size(0.26f * w, 0.28f * h))
    poly(LogoPalette.Teal.copy(alpha = 0.55f), 0.78f to 0.56f, 1f to 0.56f, 1f to 0.7f)

    // Halo
    drawCircle(LogoPalette.Halo, radius = 0.27f * w, center = p(0.5f, 0.39f))

    // Suit, shirt, lapels, tie
    poly(LogoPalette.Suit, 0.16f to 1f, 0.22f to 0.73f, 0.37f to 0.63f, 0.63f to 0.63f, 0.78f to 0.73f, 0.84f to 1f)
    poly(LogoPalette.Shirt, 0.42f to 0.63f, 0.58f to 0.63f, 0.5f to 0.82f)
    poly(LogoPalette.Lapel, 0.37f to 0.63f, 0.42f to 0.63f, 0.48f to 0.84f, 0.40f to 0.74f)
    poly(LogoPalette.Lapel, 0.63f to 0.63f, 0.58f to 0.63f, 0.52f to 0.84f, 0.60f to 0.74f)
    poly(LogoPalette.Tie, 0.48f to 0.645f, 0.52f to 0.645f, 0.535f to 0.79f, 0.5f to 0.83f, 0.465f to 0.79f)

    // Neck and head (two-tone faceted shading)
    poly(LogoPalette.Neck, 0.45f to 0.52f, 0.55f to 0.52f, 0.56f to 0.64f, 0.44f to 0.64f)
    poly(LogoPalette.Skin, 0.40f to 0.30f, 0.60f to 0.30f, 0.62f to 0.44f, 0.55f to 0.555f, 0.45f to 0.555f, 0.38f to 0.44f)
    poly(LogoPalette.SkinShade, 0.5f to 0.30f, 0.60f to 0.30f, 0.62f to 0.44f, 0.55f to 0.555f, 0.5f to 0.555f, 0.53f to 0.44f)

    // Hair: neat rounded bob
    poly(
        LogoPalette.Hair,
        0.36f to 0.44f, 0.36f to 0.28f, 0.43f to 0.205f, 0.57f to 0.205f, 0.64f to 0.28f,
        0.64f to 0.44f, 0.61f to 0.33f, 0.52f to 0.29f, 0.42f to 0.31f, 0.39f to 0.37f,
    )

    // Minimal features
    val stroke = 0.012f * w
    drawLine(LogoPalette.Hair, p(0.435f, 0.405f), p(0.465f, 0.405f), stroke, StrokeCap.Round)
    drawLine(LogoPalette.Hair, p(0.535f, 0.405f), p(0.565f, 0.405f), stroke, StrokeCap.Round)
    drawLine(LogoPalette.SkinShade, p(0.48f, 0.49f), p(0.52f, 0.49f), stroke * 0.8f, StrokeCap.Round)
}
