package com.attendo.feature.attendance

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendo.core.designsystem.component.Mood
import com.attendo.core.designsystem.component.TriangleMascot
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.CourseCatalog
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dating-app style card stack for the selected day:
 * swipe right = Present, left = Absent, down = No class. Buttons do the same, plus undo.
 */
@Composable
internal fun SwipeDeck(
    cards: List<SubjectDayItem>,
    totalToday: Int,
    canUndo: Boolean,
    onSwipe: (subjectId: String, status: AttendanceStatus) -> Unit,
    onUndo: () -> Unit,
    colorFor: (subjectId: String) -> Color,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var requested by remember { mutableStateOf<AttendanceStatus?>(null) }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(430.dp), contentAlignment = Alignment.Center) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val top = cards.firstOrNull()
            if (top == null) {
                AllCaughtUp(totalToday)
            } else {
                key(top.summary.subject.id) {
                    val x = remember { Animatable(0f) }
                    val y = remember { Animatable(0f) }
                    val scope = rememberCoroutineScope()
                    val thresholdX = widthPx * 0.28f
                    val thresholdY = heightPx * 0.2f
                    val progress = maxOf(abs(x.value) / thresholdX, y.value.coerceAtLeast(0f) / thresholdY).coerceIn(0f, 1f)

                    suspend fun commit(status: AttendanceStatus) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val spec = tween<Float>(260, easing = Motion.Easing)
                        when (status) {
                            AttendanceStatus.PRESENT -> x.animateTo(widthPx * 1.6f, spec)
                            AttendanceStatus.ABSENT -> x.animateTo(-widthPx * 1.6f, spec)
                            AttendanceStatus.CANCELLED -> y.animateTo(heightPx * 1.6f, spec)
                        }
                        onSwipe(top.summary.subject.id, status)
                    }

                    LaunchedEffect(requested) {
                        requested?.let {
                            requested = null
                            commit(it)
                        }
                    }

                    // Card underneath grows into place as the top card is dragged away.
                    cards.getOrNull(1)?.let { next ->
                        SubjectProfileCard(
                            next,
                            colorFor(next.summary.subject.id),
                            Modifier.graphicsLayer {
                                val s = 0.92f + 0.08f * progress
                                scaleX = s
                                scaleY = s
                                translationY = (1f - progress) * 18.dp.toPx()
                            },
                        )
                    }
                    SubjectProfileCard(
                        top,
                        colorFor(top.summary.subject.id),
                        Modifier
                            .graphicsLayer {
                                translationX = x.value
                                translationY = y.value
                                rotationZ = x.value / widthPx * 14f
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDrag = { change, drag ->
                                        change.consume()
                                        scope.launch {
                                            x.snapTo(x.value + drag.x)
                                            y.snapTo((y.value + drag.y).coerceAtLeast(-heightPx * 0.15f))
                                        }
                                    },
                                    onDragEnd = {
                                        scope.launch {
                                            when {
                                                x.value > thresholdX -> commit(AttendanceStatus.PRESENT)
                                                x.value < -thresholdX -> commit(AttendanceStatus.ABSENT)
                                                y.value > thresholdY -> commit(AttendanceStatus.CANCELLED)
                                                else -> {
                                                    launch { x.animateTo(0f, Motion.bouncy()) }
                                                    y.animateTo(0f, Motion.bouncy())
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            launch { x.animateTo(0f, Motion.bouncy()) }
                                            y.animateTo(0f, Motion.bouncy())
                                        }
                                    },
                                )
                            },
                        stamps = {
                            Stamp("PRESENT", AttendoColors.Green, -14f, (x.value / thresholdX).coerceIn(0f, 1f), Alignment.TopStart)
                            Stamp("ABSENT", AttendoColors.Pink, 14f, (-x.value / thresholdX).coerceIn(0f, 1f), Alignment.TopEnd)
                            Stamp("NO CLASS", AttendoColors.Blue, 0f, (y.value / thresholdY).coerceIn(0f, 1f), Alignment.Center)
                        },
                    )
                }
            }
        }

        Text(
            if (cards.isEmpty()) " " else "${cards.size} of $totalToday left  ·  ← absent   ↓ no class   present →",
            style = MaterialTheme.typography.labelSmall,
            color = AttendoColors.Muted,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundAction("↺", AttendoColors.Yellow, 46.dp, enabled = canUndo, label = "Undo last swipe", onClick = onUndo)
            RoundAction("✕", AttendoColors.Pink, 62.dp, enabled = cards.isNotEmpty(), label = "Absent") {
                requested = AttendanceStatus.ABSENT
            }
            RoundAction("↓", AttendoColors.Blue, 50.dp, enabled = cards.isNotEmpty(), label = "No class") {
                requested = AttendanceStatus.CANCELLED
            }
            RoundAction("✓", AttendoColors.Green, 62.dp, enabled = cards.isNotEmpty(), label = "Present") {
                requested = AttendanceStatus.PRESENT
            }
        }
    }
}

/** A subject shown like a dating-profile card: poster "photo", name, and an attendance "bio". */
@Composable
private fun SubjectProfileCard(
    item: SubjectDayItem,
    color: Color,
    modifier: Modifier = Modifier,
    stamps: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    val subject = item.summary.subject
    val summary = item.summary
    val onCard = contentColorFor(color)
    Box(
        modifier
            .fillMaxWidth(0.88f)
            .height(410.dp)
            .shadow(14.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(color),
    ) {
        // Big abbreviation as the card's "face".
        Text(
            CourseCatalog.shortName(subject),
            fontSize = 64.sp,
            lineHeight = 66.sp,
            fontWeight = FontWeight.Black,
            color = onCard,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp).padding(bottom = 90.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Text(
                subject.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = onCard,
            )
            Text(
                listOfNotNull(
                    subject.code.ifBlank { null },
                    if (CourseCatalog.isLab(subject)) "Lab" else null,
                    "Target ${subject.targetPercent}%",
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.labelLarge,
                color = onCard.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    summary.held == 0 -> "No classes marked yet. Swipe to start the streak!"
                    summary.isOnTrack ->
                        "${summary.percent.roundToInt()}% · ${summary.present}/${summary.held} attended · can miss ${summary.classesCanSkip()}"
                    else ->
                        "${summary.percent.roundToInt()}% · ${summary.present}/${summary.held} attended · needs ${summary.classesNeeded()} in a row"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = onCard.copy(alpha = 0.85f),
            )
        }
        stamps()
    }
}

/**
 * Dark text on light cards, light text on dark ones. Fixed colours (not the theme's ink/background)
 * because the card colours themselves don't change with the theme.
 */
private fun contentColorFor(background: Color): Color =
    if (background.luminance() > 0.45f) Color(0xFF232323) else Color(0xFFF8FAED)

/** Ten clearly different colours; shuffled once with a fixed seed so they look random but stay stable. */
private val CardPalette: List<Color> = listOf(
    Color(0xFFF2542D), Color(0xFF2B5CE6), Color(0xFF50A65C), Color(0xFFE0399A), Color(0xFFF2C84B),
    Color(0xFF7B2CBF), Color(0xFF2A9D8F), Color(0xFF00B4D8), Color(0xFF8C5E3C), Color(0xFF3D405B),
).shuffled(kotlin.random.Random(2026))

/** Colour for the subject at [index] in the subject list: distinct for the first ten subjects. */
internal fun cardColor(index: Int): Color = CardPalette[Math.floorMod(index, CardPalette.size)]


@Composable
private fun androidx.compose.foundation.layout.BoxScope.Stamp(
    text: String,
    color: Color,
    angle: Float,
    alpha: Float,
    alignment: Alignment,
) {
    if (alpha <= 0f) return
    Text(
        text,
        color = color,
        fontSize = 30.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        modifier = Modifier
            .align(alignment)
            .padding(28.dp)
            .graphicsLayer {
                this.alpha = alpha
                rotationZ = angle
            }
            .border(4.dp, color, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun RoundAction(
    glyph: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .iosPress(PressStyle.Scale, enabled = enabled, onClickLabel = label, onClick = onClick)
            .shadow(if (enabled) 6.dp else 0.dp, CircleShape)
            .clip(CircleShape)
            .background(AttendoColors.Cream)
            .border(2.dp, if (enabled) color else AttendoColors.CreamDeep, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            color = if (enabled) color else AttendoColors.CreamDeep,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AllCaughtUp(total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TriangleMascot(Modifier.size(160.dp), mood = Mood.Happy)
        Text("All caught up!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            if (total == 0) "Add subjects to start swiping." else "All $total subjects are marked for this day.",
            style = MaterialTheme.typography.bodyMedium,
            color = AttendoColors.Muted,
        )
        Spacer(Modifier.width(1.dp))
    }
}
