package com.attendo.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.motion.LocalAnimationMode
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.model.AnimationMode
import kotlin.math.cos
import kotlin.math.sin

enum class Mood { Happy, Neutral, Sad }

/** Draws the simple mascot face: two eyes and a mouth whose shape follows [mood]. */
fun DrawScope.drawMascotFace(center: Offset, size: Float, mood: Mood, color: Color = AttendoColors.Ink) {
    val stroke = size * 0.07f
    val eyeDx = size * 0.22f
    val eyeTop = center.y - size * 0.22f
    val eyeLen = size * 0.12f
    listOf(-eyeDx, eyeDx).forEach { dx ->
        drawLine(color, Offset(center.x + dx, eyeTop), Offset(center.x + dx, eyeTop + eyeLen), stroke, StrokeCap.Round)
    }
    val mouthW = size * 0.42f
    val mouthTop = center.y + size * 0.02f
    when (mood) {
        Mood.Happy -> drawArc(
            color = color,
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(center.x - mouthW / 2, mouthTop - mouthW / 2),
            size = Size(mouthW, mouthW * 0.9f),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        Mood.Neutral -> drawLine(
            color,
            Offset(center.x - mouthW / 3, mouthTop + size * 0.12f),
            Offset(center.x + mouthW / 3, mouthTop + size * 0.12f),
            stroke,
            StrokeCap.Round,
        )
        Mood.Sad -> drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(center.x - mouthW / 2, mouthTop + size * 0.06f),
            size = Size(mouthW, mouthW * 0.8f),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawRoundedPolygon(points: List<Offset>, color: Color, rounding: Float) {
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, color)
    // Stroking with a round join in the same colour rounds the corners.
    drawPath(path, color, style = Stroke(width = rounding, join = StrokeJoin.Round))
}

private fun DrawScope.drawShadow(centerX: Float, y: Float, width: Float) {
    drawOval(
        color = AttendoColors.CreamDeep,
        topLeft = Offset(centerX - width / 2, y),
        size = Size(width, width * 0.14f),
    )
}

/** Body colour per mood: brand green when happy, olive when neutral, light red when attendance is too low. */
fun moodColor(mood: Mood): Color = when (mood) {
    Mood.Happy -> AttendoColors.Green
    Mood.Neutral -> Color(0xFF9DB35A)
    Mood.Sad -> Color(0xFFF08080)
}

/**
 * Rounded pentagon home mascot, animated by mood:
 * - Happy: small bounce with a little squash on landing; the shadow shrinks as it rises.
 * - Sad: slow left-right sway from the base, like a sigh.
 * - Neutral: gentle "breathing" scale.
 * Colour cross-fades between moods. Animated values are read only in the draw phase, so the
 * animation never triggers recomposition.
 */
@Composable
fun PentagonMascot(modifier: Modifier = Modifier, mood: Mood = Mood.Happy, color: Color? = null) {
    if (LocalAnimationMode.current == AnimationMode.NONE) {
        // Animations off: a still pentagon in the mood colour, no idle loop running.
        StaticPentagon(modifier, mood, color ?: moodColor(mood))
        return
    }
    AnimatedPentagon(modifier, mood, color)
}

@Composable
private fun StaticPentagon(modifier: Modifier, mood: Mood, color: Color) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height * 0.44f)
        val r = s * 0.38f
        drawShadow(c.x, size.height * 0.86f, s * 0.75f)
        val pts = (0 until 5).map { i ->
            val a = Math.toRadians(-90.0 + 72.0 * i)
            Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
        }
        drawRoundedPolygon(pts, color, s * 0.1f)
        drawMascotFace(Offset(c.x, c.y + s * 0.06f), s * 0.42f, mood)
    }
}

@Composable
private fun AnimatedPentagon(modifier: Modifier, mood: Mood, color: Color?) {
    val bodyColor by animateColorAsState(color ?: moodColor(mood), tween(600), label = "mascotColor")
    val loop = rememberInfiniteTransition(label = "mascot")
    // Smooth mode slows the idle loops and uses a symmetric ease so there's no visible "kick".
    val slow = Motion.idleSlowdown
    val easing = if (slow > 1f) Motion.Easing else FastOutSlowInEasing
    // 0..1 phase; each mood maps it differently.
    val bounce by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween((900 * slow).toInt(), easing = easing), RepeatMode.Reverse),
        label = "bounce",
    )
    val sway by loop.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween((1600 * slow).toInt(), easing = easing), RepeatMode.Reverse),
        label = "sway",
    )
    // Smoothly blend the motion amount when the mood changes instead of jumping.
    val happyAmount by animateFloatAsState(if (mood == Mood.Happy) 1f else 0f, tween(500), label = "happy")
    val sadAmount by animateFloatAsState(if (mood == Mood.Sad) 1f else 0f, tween(500), label = "sad")
    val neutralAmount by animateFloatAsState(if (mood == Mood.Neutral) 1f else 0f, tween(500), label = "neutral")

    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height * 0.44f)
        val r = s * 0.38f
        val baseY = c.y + r * 0.85f

        val lift = bounce * s * 0.035f * happyAmount // ~6dp at 170dp: "a very small amount"
        val squash = (1f - bounce) * 0.03f * happyAmount // slight squash at the bottom of the bounce
        val angle = sway * 6f * sadAmount
        val breathe = 1f + (bounce - 0.5f) * 0.02f * neutralAmount

        drawShadow(c.x, size.height * 0.86f, s * 0.75f * (1f - lift / (s * 0.2f)))
        withTransform({
            translate(top = -lift)
            rotate(angle, pivot = Offset(c.x, baseY))
            scale(scaleX = (1f + squash) * breathe, scaleY = (1f - squash) * breathe, pivot = Offset(c.x, baseY))
        }) {
            val pts = (0 until 5).map { i ->
                val a = Math.toRadians(-90.0 + 72.0 * i)
                Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
            }
            drawRoundedPolygon(pts, bodyColor, s * 0.1f)
            drawMascotFace(Offset(c.x, c.y + s * 0.06f), s * 0.42f, mood)
        }
    }
}

/** Green rounded triangle with legs (celebration / detail mascot). */
@Composable
fun TriangleMascot(modifier: Modifier = Modifier, mood: Mood = Mood.Happy, color: Color = AttendoColors.Green) {
    Canvas(modifier) {
        val s = size.minDimension
        val cx = size.width / 2
        val top = size.height * 0.1f
        val base = size.height * 0.62f
        val half = s * 0.36f
        drawShadow(cx, size.height * 0.86f, s * 0.6f)
        // legs
        val legStroke = s * 0.03f
        drawLine(AttendoColors.Ink, Offset(cx - half * 0.3f, base), Offset(cx - half * 0.45f, size.height * 0.88f), legStroke, StrokeCap.Round)
        drawLine(AttendoColors.Ink, Offset(cx + half * 0.3f, base), Offset(cx + half * 0.5f, size.height * 0.86f), legStroke, StrokeCap.Round)
        drawRoundedPolygon(listOf(Offset(cx, top), Offset(cx + half, base), Offset(cx - half, base)), color, s * 0.1f)
        drawMascotFace(Offset(cx, base - s * 0.17f), s * 0.36f, mood)
    }
}

/** Big tilted capsule mascot used on the dark onboarding screen. */
@Composable
fun BlobMascot(modifier: Modifier = Modifier, mood: Mood = Mood.Happy, color: Color = AttendoColors.Green) {
    Canvas(modifier) {
        val h = size.height * 0.62f
        rotate(degrees = 22f, pivot = Offset(size.width * 0.5f, size.height * 0.5f)) {
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width * 0.12f, size.height * 0.2f),
                size = Size(size.width * 1.4f, h),
                cornerRadius = CornerRadius(h / 2, h / 2),
            )
        }
        drawMascotFace(Offset(size.width * 0.34f, size.height * 0.42f), size.minDimension * 0.42f, mood)
    }
}
