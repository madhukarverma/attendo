package com.attendo.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign

/** Background of the launch screen; the artwork's edges are this exact colour so it blends in. */
val LaunchBackground = Color(0xFF1A1D1A)

private object LaunchPalette {
    val Coat = Color(0xFF2A2C2A)
    val CoatLight = Color(0xFF323532)
    val Lapel = Color(0xFF222422)
    val Sweater = Color(0xFF232823)
    val Collar = Color(0xFF2D322D)
    val Skin = Color(0xFFE6D5BE)
    val SkinShade = Color(0xFFC9B8A4)
    val Hair = Color(0xFF1F2A2B)
    val HairLight = Color(0xFF34413F)
    val Frame = Color(0xFF111311)
    val Pin = Color(0xFF50A65C)
}

/**
 * Original minimalist flat-vector portrait for the launch screen: a calm student in a dark coat and
 * turtleneck, round glasses, tidy side-parted hair and a green Attendo pin, on [LaunchBackground].
 */
@Composable
fun LaunchArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        fun p(x: Float, y: Float) = Offset(x * w, y * h)
        fun poly(color: Color, vararg pts: Pair<Float, Float>, round: Float = 0f) {
            val path = Path().apply {
                moveTo(pts[0].first * w, pts[0].second * h)
                pts.drop(1).forEach { (x, y) -> lineTo(x * w, y * h) }
                close()
            }
            drawPath(path, color)
            if (round > 0f) drawPath(path, color, style = Stroke(round * w, join = StrokeJoin.Round))
        }

        // Coat and shoulders
        poly(LaunchPalette.Coat, 0.04f to 1f, 0.09f to 0.71f, 0.27f to 0.61f, 0.40f to 0.575f, 0.60f to 0.575f, 0.73f to 0.61f, 0.91f to 0.71f, 0.96f to 1f, round = 0.03f)
        poly(LaunchPalette.CoatLight, 0.73f to 0.61f, 0.91f to 0.71f, 0.94f to 0.88f, 0.80f to 0.72f)
        // Turtleneck sweater
        poly(LaunchPalette.Sweater, 0.38f to 0.575f, 0.62f to 0.575f, 0.58f to 1f, 0.42f to 0.98f)
        poly(LaunchPalette.Collar, 0.40f to 0.525f, 0.60f to 0.525f, 0.625f to 0.60f, 0.375f to 0.60f, round = 0.015f)
        // Lapels
        poly(LaunchPalette.Lapel, 0.27f to 0.61f, 0.39f to 0.585f, 0.44f to 0.82f, 0.35f to 0.73f)
        poly(LaunchPalette.Lapel, 0.73f to 0.61f, 0.61f to 0.585f, 0.56f to 0.82f, 0.65f to 0.73f)
        drawCircle(LaunchPalette.Pin, radius = 0.02f * w, center = p(0.335f, 0.70f))

        // Neck, ears, head (two-tone faceted)
        poly(LaunchPalette.SkinShade, 0.44f to 0.44f, 0.56f to 0.44f, 0.575f to 0.54f, 0.425f to 0.54f)
        drawOval(LaunchPalette.SkinShade, topLeft = p(0.315f, 0.30f), size = Size(0.05f * w, 0.07f * h))
        drawOval(LaunchPalette.SkinShade, topLeft = p(0.635f, 0.30f), size = Size(0.05f * w, 0.07f * h))
        poly(LaunchPalette.Skin, 0.36f to 0.22f, 0.64f to 0.22f, 0.66f to 0.34f, 0.62f to 0.435f, 0.54f to 0.485f, 0.46f to 0.485f, 0.38f to 0.435f, 0.34f to 0.34f, round = 0.02f)
        poly(LaunchPalette.SkinShade, 0.57f to 0.24f, 0.645f to 0.24f, 0.66f to 0.34f, 0.62f to 0.435f, 0.55f to 0.48f, 0.60f to 0.36f)

        // Hair: rounded, side-parted
        poly(
            LaunchPalette.Hair,
            0.335f to 0.32f, 0.335f to 0.205f, 0.40f to 0.125f, 0.50f to 0.10f, 0.61f to 0.115f, 0.675f to 0.18f,
            0.68f to 0.31f, 0.645f to 0.235f, 0.56f to 0.205f, 0.44f to 0.215f, 0.38f to 0.25f, 0.36f to 0.31f,
            round = 0.02f,
        )
        poly(LaunchPalette.HairLight, 0.44f to 0.13f, 0.56f to 0.12f, 0.50f to 0.165f)

        // Round glasses, eyes, small smile
        val frame = Stroke(0.011f * w)
        drawCircle(LaunchPalette.Frame, radius = 0.048f * w, center = p(0.435f, 0.33f), style = frame)
        drawCircle(LaunchPalette.Frame, radius = 0.048f * w, center = p(0.565f, 0.33f), style = frame)
        drawLine(LaunchPalette.Frame, p(0.483f, 0.33f), p(0.517f, 0.33f), 0.009f * w)
        drawCircle(LaunchPalette.Hair, radius = 0.011f * w, center = p(0.435f, 0.333f))
        drawCircle(LaunchPalette.Hair, radius = 0.011f * w, center = p(0.565f, 0.333f))
        drawArc(
            LaunchPalette.SkinShade,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = p(0.47f, 0.395f),
            size = Size(0.06f * w, 0.035f * h),
            style = Stroke(0.01f * w, cap = StrokeCap.Round),
        )
    }
}

/** Greyed-out "developed by" line with the author's GitHub. */
@Composable
fun DeveloperCredit(modifier: Modifier = Modifier, color: Color = Color(0xFF8C8E82)) {
    Text(
        "Developed by @madhukarverma · github.com/madhukarverma",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
