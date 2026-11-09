package com.attendo.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.HandTextStyle
import kotlin.math.PI
import kotlin.math.sin

enum class TailSide { BottomStart, BottomEnd, TopCenter }

/** Rounded speech bubble with handwritten uppercase text, as in the reference designs. */
@Composable
fun SpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    tail: TailSide = TailSide.BottomStart,
    color: Color = AttendoColors.CreamDeep,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (tail == TailSide.TopCenter) BubbleTail(color, pointingUp = true, Modifier.align(Alignment.CenterHorizontally))
        Box(
            Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(color)
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            Text(text.uppercase(), style = HandTextStyle)
        }
        when (tail) {
            TailSide.BottomStart -> BubbleTail(color, false, Modifier.align(Alignment.Start).padding(start = 28.dp))
            TailSide.BottomEnd -> BubbleTail(color, false, Modifier.align(Alignment.End).padding(end = 28.dp))
            TailSide.TopCenter -> Unit
        }
    }
}

@Composable
private fun BubbleTail(color: Color, pointingUp: Boolean, modifier: Modifier) {
    Canvas(modifier.size(width = 18.dp, height = 10.dp)) {
        val p = Path().apply {
            if (pointingUp) {
                moveTo(0f, size.height); lineTo(size.width / 2, 0f); lineTo(size.width, size.height)
            } else {
                moveTo(0f, 0f); lineTo(size.width * 0.2f, size.height); lineTo(size.width, 0f)
            }
            close()
        }
        drawPath(p, color)
    }
}

/** Hand-drawn wavy underline. */
@Composable
fun Squiggle(modifier: Modifier = Modifier, color: Color = AttendoColors.Green) {
    Canvas(modifier.height(14.dp)) {
        val path = Path()
        val waves = 5
        val amp = size.height * 0.3f
        val mid = size.height / 2
        path.moveTo(0f, mid)
        var x = 0f
        while (x <= size.width) {
            path.lineTo(x, mid + amp * sin(x / size.width * waves * 2 * PI).toFloat())
            x += 2f
        }
        drawPath(path, color, style = Stroke(width = size.height * 0.28f, cap = StrokeCap.Round))
    }
}

/** Soft cream card used for list rows. */
@Composable
fun AttendoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .then(if (onClick != null) Modifier.iosPress(PressStyle.Highlight, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Full-width green call-to-action button ("Finish" in the designs). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val container by animateColorAsState(if (enabled) AttendoColors.Green else AttendoColors.CreamDeep, label = "btn")
    Box(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .iosPress(PressStyle.Dim, enabled = enabled, onClick = onClick)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) AttendoColors.Cream else AttendoColors.Muted,
        )
    }
}

/** Integer that animates between values (percentages, counters). */
@Composable
fun AnimatedNumber(value: Int, suffix: String = "", style: TextStyle, color: Color = Color.Unspecified, fontWeight: FontWeight? = null) {
    val shown by animateIntAsState(value, animationSpec = Motion.settle(), label = "number")
    Text("$shown$suffix", style = style, color = color, fontWeight = fontWeight)
}

/** Dark pill bottom bar: green primary action on the left, icon actions on the right. */
@Composable
fun PillBottomBar(
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(AttendoColors.Ink)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .iosPress(PressStyle.Scale, onClick = onPrimary)
                .clip(CircleShape)
                .background(AttendoColors.Green)
                .padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            Text("+ $primaryLabel", color = AttendoColors.Cream, style = MaterialTheme.typography.labelLarge)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly, content = actions)
    }
}

/** Small round icon button for [PillBottomBar]. */
@Composable
fun BarIconButton(onClick: () -> Unit, contentDescription: String, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .iosPress(PressStyle.Dim, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Three-bar "stats" glyph drawn in code (the icon isn't in material-icons-core). */
@Composable
fun StatsGlyph(color: Color = AttendoColors.Cream, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width * 0.18f
        listOf(0.25f to 0.45f, 0.5f to 0.15f, 0.75f to 0.65f).forEach { (x, top) ->
            drawLine(
                color,
                Offset(this.size.width * x, this.size.height * top),
                Offset(this.size.width * x, this.size.height * 0.9f),
                w,
                StrokeCap.Round,
            )
        }
    }
}

/** Stopwatch glyph for the Pomodoro button (not in material-icons-core). */
@Composable
fun TimerGlyph(color: Color = AttendoColors.Cream, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.11f
        val c = Offset(this.size.width / 2, this.size.height * 0.56f)
        val r = s * 0.36f
        drawCircle(color, r, c, style = Stroke(stroke))
        drawLine(color, Offset(c.x - s * 0.12f, s * 0.07f), Offset(c.x + s * 0.12f, s * 0.07f), stroke, StrokeCap.Round)
        drawLine(color, c, Offset(c.x, c.y - r * 0.6f), stroke, StrokeCap.Round)
        drawLine(color, c, Offset(c.x + r * 0.45f, c.y), stroke, StrokeCap.Round)
    }
}

/** Big percentage label in a pill, e.g. "82%". */
@Composable
fun PercentPill(text: String, modifier: Modifier = Modifier, color: Color = AttendoColors.CreamDeep) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(color)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

/** Status dot + label used under list rows ("✱ New", "✓ Done"). */
@Composable
fun StatusLabel(text: String, color: Color, symbol: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(symbol, color = color, style = MaterialTheme.typography.labelMedium)
        Box(Modifier.width(5.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}
