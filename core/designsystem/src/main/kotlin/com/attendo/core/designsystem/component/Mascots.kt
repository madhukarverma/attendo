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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.drawscope.withTransform
import com.attendo.core.designsystem.motion.LocalAnimationMode
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AnimationMode
import kotlin.math.cos
import kotlin.math.sin

enum class Mood { Happy, Neutral, Sad }

/** Face colour: always dark, since it sits on the mascot's coloured body in every theme. */
private val FaceInk = Color(0xFF232323)

/** Draws the simple mascot face: two eyes and a mouth whose shape follows [mood]. */
fun DrawScope.drawMascotFace(center: Offset, size: Float, mood: Mood, color: Color = FaceInk) {
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
    Mood.Happy -> AttendoColors.GreenFill
    Mood.Neutral -> Color(0xFF9DB35A)
    Mood.Sad -> Color(0xFFF08080)
}

/**
 * The mascots follow the three parts of a DNA nucleotide, one per tab: a pentagon (the pentose
 * sugar) on Home, a circle (the phosphate) on Classes and a rectangle (the nitrogenous base) on
 * Tasks.
 */
private enum class MascotShape { Pentagon, Circle, Rectangle }

/**
 * Meet Penta, the rounded pentagon who lives on the Home screen. (He never introduces himself in the
 * app; his name is a codebase-only easter egg.) Penta is happy when every subject is at or above its
 * target and sad otherwise. [color] null follows the mood.
 */
@Composable
fun PentagonMascot(modifier: Modifier = Modifier, mood: Mood = Mood.Happy, color: Color? = null) =
    Mascot(MascotShape.Pentagon, modifier, mood, color)

/** The Classes mascot: a round phosphate on two legs. */
@Composable
fun CircleMascot(modifier: Modifier = Modifier, mood: Mood = Mood.Happy, color: Color? = null) =
    Mascot(MascotShape.Circle, modifier, mood, color)

/** The Tasks mascot: a rectangular base on two legs. */
@Composable
fun RectangleMascot(modifier: Modifier = Modifier, mood: Mood = Mood.Happy, color: Color? = null) =
    Mascot(MascotShape.Rectangle, modifier, mood, color)

/**
 * Every mascot idles by mood (DEC-022: values are read only in the draw phase):
 * - Happy: a small hop with a squash on landing; the shadow shrinks as it rises.
 * - Sad: a slow sway from the feet, like a sigh.
 * - Neutral: gentle breathing.
 * With Animations set to None it's drawn still and no loop runs.
 */
@Composable
private fun Mascot(shape: MascotShape, modifier: Modifier, mood: Mood, color: Color?) {
    val still = LocalAnimationMode.current == AnimationMode.NONE
    val body = if (still) rememberUpdatedState(color ?: moodColor(mood)) else animateColorAsState(color ?: moodColor(mood), tween(600), label = "mascotColor")
    val idle = if (still) null else rememberIdle(mood)
    Canvas(modifier) {
        val s = size.minDimension
        val cx = size.width / 2
        val ground = size.height * 0.86f
        val g = when (shape) {
            MascotShape.Pentagon -> Geometry(base = size.height * 0.44f + s * 0.38f * 0.85f, shadow = 0.75f)
            MascotShape.Circle, MascotShape.Rectangle -> Geometry(base = ground, shadow = 0.6f)
        }
        val m = idle?.frame(s) ?: Frame.Still
        drawShadow(cx, ground, s * g.shadow * (1f - m.lift / (s * 0.2f)))
        withTransform({
            translate(top = -m.lift)
            rotate(m.angle, pivot = Offset(cx, g.base))
            scale(scaleX = m.scaleX, scaleY = m.scaleY, pivot = Offset(cx, g.base))
        }) {
            when (shape) {
                MascotShape.Pentagon -> drawPentagon(s, cx, body.value, mood)
                MascotShape.Circle -> drawCircleBody(s, cx, ground, body.value, mood)
                MascotShape.Rectangle -> drawRectangleBody(s, cx, ground, body.value, mood)
            }
        }
    }
}

private class Geometry(val base: Float, val shadow: Float)

private fun DrawScope.drawPentagon(s: Float, cx: Float, color: Color, mood: Mood) {
    val c = Offset(cx, size.height * 0.44f)
    val r = s * 0.38f
    val pts = (0 until 5).map { i ->
        val a = Math.toRadians(-90.0 + 72.0 * i)
        Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
    }
    drawRoundedPolygon(pts, color, s * 0.1f)
    drawMascotFace(Offset(c.x, c.y + s * 0.06f), s * 0.42f, mood)
}

private fun DrawScope.drawLegs(s: Float, cx: Float, hip: Float, ground: Float, spread: Float) {
    val stroke = s * 0.03f
    drawLine(AttendoColors.Ink, Offset(cx - spread * 0.55f, hip), Offset(cx - spread * 0.8f, ground + s * 0.02f), stroke, StrokeCap.Round)
    drawLine(AttendoColors.Ink, Offset(cx + spread * 0.55f, hip), Offset(cx + spread * 0.85f, ground), stroke, StrokeCap.Round)
}

private fun DrawScope.drawCircleBody(s: Float, cx: Float, ground: Float, color: Color, mood: Mood) {
    val r = s * 0.29f
    val c = Offset(cx, size.height * 0.1f + r)
    drawLegs(s, cx, c.y + r * 0.8f, ground, r * 0.6f)
    drawCircle(color, r, c)
    drawMascotFace(Offset(c.x, c.y + s * 0.02f), s * 0.36f, mood)
}

private fun DrawScope.drawRectangleBody(s: Float, cx: Float, ground: Float, color: Color, mood: Mood) {
    val w = s * 0.64f
    val h = s * 0.46f
    val top = size.height * 0.16f
    drawLegs(s, cx, top + h, ground, w * 0.35f)
    drawRoundRect(color, Offset(cx - w / 2, top), Size(w, h), CornerRadius(s * 0.05f))
    drawMascotFace(Offset(cx, top + h * 0.52f), s * 0.36f, mood)
}

/** One frame of the idle loop, in px and degrees. */
private class Frame(val lift: Float, val angle: Float, val scaleX: Float, val scaleY: Float) {
    companion object {
        val Still = Frame(0f, 0f, 1f, 1f)
    }
}

/** The running idle loop; [frame] is called from draw blocks only. */
private class Idle(
    private val bounce: State<Float>,
    private val sway: State<Float>,
    private val happy: State<Float>,
    private val sad: State<Float>,
    private val neutral: State<Float>,
) {
    fun frame(s: Float): Frame {
        val b = bounce.value
        val lift = b * s * 0.035f * happy.value // ~6 dp at 170 dp: "a very small amount"
        val squash = (1f - b) * 0.03f * happy.value // slight squash at the bottom of the hop
        val breathe = 1f + (b - 0.5f) * 0.02f * neutral.value
        return Frame(lift, sway.value * 6f * sad.value, (1f + squash) * breathe, (1f - squash) * breathe)
    }
}

@Composable
private fun rememberIdle(mood: Mood): Idle {
    val loop = rememberInfiniteTransition(label = "mascot")
    // Smooth mode slows the idle loops and uses a symmetric ease so there's no visible "kick".
    val slow = Motion.idleSlowdown
    val easing = if (slow > 1f) Motion.Easing else FastOutSlowInEasing
    val bounce = loop.animateFloat(0f, 1f, infiniteRepeatable(tween((900 * slow).toInt(), easing = easing), RepeatMode.Reverse), label = "bounce")
    val sway = loop.animateFloat(-1f, 1f, infiniteRepeatable(tween((1600 * slow).toInt(), easing = easing), RepeatMode.Reverse), label = "sway")
    // Blend the motion amount when the mood changes instead of jumping.
    val happy = animateFloatAsState(if (mood == Mood.Happy) 1f else 0f, tween(500), label = "happy")
    val sad = animateFloatAsState(if (mood == Mood.Sad) 1f else 0f, tween(500), label = "sad")
    val neutral = animateFloatAsState(if (mood == Mood.Neutral) 1f else 0f, tween(500), label = "neutral")
    return Idle(bounce, sway, happy, sad, neutral)
}

/** Big tilted capsule mascot on the dark onboarding screen; it breathes and rocks gently. */
@Composable
fun BlobMascot(modifier: Modifier = Modifier, mood: Mood = Mood.Happy, color: Color = AttendoColors.GreenFill) {
    val idle = if (LocalAnimationMode.current == AnimationMode.NONE) null else rememberIdle(Mood.Neutral)
    val rock = if (idle == null) null else {
        val loop = rememberInfiniteTransition(label = "blob")
        loop.animateFloat(-1f, 1f, infiniteRepeatable(tween((2400 * Motion.idleSlowdown).toInt(), easing = Motion.Easing), RepeatMode.Reverse), label = "rock")
    }
    Canvas(modifier) {
        val m = idle?.frame(size.minDimension) ?: Frame.Still
        val h = size.height * 0.62f
        val pivot = Offset(size.width * 0.5f, size.height * 0.5f)
        withTransform({ scale(m.scaleX, m.scaleY, pivot) }) {
            // The body rocks a little around its tilt; the face stays upright, as before.
            rotate(degrees = 22f + (rock?.value ?: 0f) * 2.5f, pivot = pivot) {
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
}
