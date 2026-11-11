package com.attendo.feature.pomodoro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendo.core.data.pomodoro.PomodoroTimerState
import com.attendo.core.designsystem.motion.LocalAnimationMode
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AnimationMode
import com.attendo.core.model.PomodoroKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TICKS = 60
private const val TWO_PI = (2 * PI).toFloat()

/**
 * The timer as a dial (after the user's reference designs): 60 ticks round the edge that fade as
 * time passes, a glowing marker at "now", and a soft disc in the middle that slowly fills with a
 * gently moving liquid while a session runs. Paused, the liquid stands still; with Animations set
 * to None nothing moves at all.
 */
@Composable
internal fun FocusDial(state: PomodoroTimerState, modifier: Modifier = Modifier) {
    val animate = LocalAnimationMode.current != AnimationMode.NONE
    val accent by animateColorAsState(state.kind.color(), label = "dialColor")
    // The timer ticks once a second; animate between ticks so the marker and liquid glide.
    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = if (animate && state.running) tween(1_000, easing = LinearEasing) else snap(),
        label = "dialProgress",
    )
    val phase = remember { Animatable(0f) }
    LaunchedEffect(animate && state.running) {
        if (!(animate && state.running)) return@LaunchedEffect
        while (true) {
            phase.snapTo(phase.value % TWO_PI)
            phase.animateTo(phase.value + TWO_PI, tween(2_800, easing = LinearEasing))
        }
    }
    val track = AttendoColors.CreamDeep
    val muted = AttendoColors.Muted

    Box(modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawTicks(progress, accent, track, showMarker = !state.isFresh) }
        Box(
            Modifier
                .fillMaxSize(0.68f)
                .shadow(18.dp, CircleShape, ambientColor = accent, spotColor = accent)
                .clip(CircleShape)
                .background(AttendoColors.CreamCard),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // A little liquid is always there; it rises to nearly full as the session ends.
                drawLiquid(level = 0.08f + 0.82f * progress, phase = phase.value, accent = accent, bubbles = animate && state.running)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    state.kind.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    formatClock(state.remainingMillis),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp, lineHeight = 56.sp),
                    fontWeight = FontWeight.Bold,
                    color = AttendoColors.Ink,
                )
                Text(
                    when {
                        state.running && state.kind == PomodoroKind.FOCUS -> "minutes left to focus"
                        state.running -> "minutes of break left"
                        state.isFresh -> "ready when you are"
                        else -> "paused"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
            }
        }
    }
}

/** Tick ring: remaining time in the session colour, elapsed time faded, and a glowing marker. */
private fun DrawScope.drawTicks(progress: Float, accent: Color, track: Color, showMarker: Boolean) {
    val radius = size.minDimension / 2
    val c = center
    val elapsedTicks = progress * TICKS
    for (i in 0 until TICKS) {
        val angle = -PI.toFloat() / 2 + TWO_PI * i / TICKS
        val major = i % 5 == 0
        val length = (if (major) 14 else 8).dp.toPx()
        val outer = radius - 4.dp.toPx()
        val start = Offset(c.x + cos(angle) * (outer - length), c.y + sin(angle) * (outer - length))
        val end = Offset(c.x + cos(angle) * outer, c.y + sin(angle) * outer)
        val elapsed = i < elapsedTicks
        drawLine(
            color = if (elapsed) track else accent.copy(alpha = if (major) 1f else 0.75f),
            start = start,
            end = end,
            strokeWidth = (if (major) 3 else 2).dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    if (showMarker) {
        val angle = -PI.toFloat() / 2 + TWO_PI * progress
        val r = radius - 4.dp.toPx() - 7.dp.toPx()
        val p = Offset(c.x + cos(angle) * r, c.y + sin(angle) * r)
        drawCircle(accent.copy(alpha = 0.22f), 12.dp.toPx(), p)
        drawCircle(accent, 5.dp.toPx(), p)
    }
}

/** Liquid filling the disc from the bottom: two offset waves for depth, and rising bubbles. */
private fun DrawScope.drawLiquid(level: Float, phase: Float, accent: Color, bubbles: Boolean) {
    val w = size.width
    val h = size.height
    val surface = h * (1f - level.coerceIn(0f, 1f))
    val amplitude = h * 0.025f

    fun wave(shift: Float, alpha: Float, lift: Float) {
        val path = Path().apply {
            moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                val y = surface + lift + amplitude * sin(TWO_PI * (x / w) * 1.2f + phase + shift)
                lineTo(x, y)
                x += 4f
            }
            lineTo(w, h)
            close()
        }
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(accent.copy(alpha = alpha * 0.6f), accent.copy(alpha = alpha)),
                startY = surface,
                endY = h,
            ),
        )
    }
    wave(shift = PI.toFloat(), alpha = 0.22f, lift = -amplitude)
    wave(shift = 0f, alpha = 0.38f, lift = 0f)

    if (bubbles && level > 0.12f) {
        val depth = h - surface
        listOf(0.22f to 0.0f, 0.38f to 0.35f, 0.55f to 0.7f, 0.68f to 0.15f, 0.8f to 0.5f).forEachIndexed { i, (fx, offset) ->
            val t = ((phase / TWO_PI) * (0.6f + i * 0.12f) + offset) % 1f
            val y = h - t * depth
            val r = (2.5f + (i % 3)) * density
            drawCircle(Color.White.copy(alpha = 0.55f * (1f - t)), r, Offset(w * fx, y))
        }
    }
}

/** "FOCUS", "SHORT BREAK", "LONG BREAK". */
internal fun PomodoroKind.label() = when (this) {
    PomodoroKind.FOCUS -> "FOCUS"
    PomodoroKind.SHORT_BREAK -> "SHORT BREAK"
    PomodoroKind.LONG_BREAK -> "LONG BREAK"
}

internal fun PomodoroKind.color() = if (this == PomodoroKind.FOCUS) AttendoColors.Green else AttendoColors.Blue

internal fun formatClock(millis: Long): String {
    val total = kotlin.math.ceil(millis / 1000.0).toLong()
    return "%02d:%02d".format(total / 60, total % 60)
}

/** Small play / pause glyphs drawn in code (no pause icon in material-icons-core). */
@Composable
internal fun PlayPauseGlyph(playing: Boolean, color: Color, size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        if (playing) {
            val bar = s * 0.24f
            drawRoundRect(color, Offset(s * 0.2f, s * 0.12f), androidx.compose.ui.geometry.Size(bar, s * 0.76f), androidx.compose.ui.geometry.CornerRadius(bar / 3))
            drawRoundRect(color, Offset(s * 0.56f, s * 0.12f), androidx.compose.ui.geometry.Size(bar, s * 0.76f), androidx.compose.ui.geometry.CornerRadius(bar / 3))
        } else {
            val path = Path().apply {
                moveTo(s * 0.28f, s * 0.12f)
                lineTo(s * 0.86f, s * 0.5f)
                lineTo(s * 0.28f, s * 0.88f)
                close()
            }
            drawPath(path, color)
        }
    }
}
