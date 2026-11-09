package com.attendo.feature.home

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
import com.attendo.core.designsystem.motion.IosMotion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.Subject
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
                        val spec = tween<Float>(260, easing = IosMotion.Easing)
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
                                                    launch { x.animateTo(0f, IosMotion.bouncy()) }
                                                    y.animateTo(0f, IosMotion.bouncy())
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            launch { x.animateTo(0f, IosMotion.bouncy()) }
                                            y.animateTo(0f, IosMotion.bouncy())
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
    modifier: Modifier = Modifier,
    stamps: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    val subject = item.summary.subject
    val summary = item.summary
    Box(
        modifier
            .fillMaxWidth(0.88f)
            .height(410.dp)
            .shadow(14.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(AttendoColors.Ink),
    ) {
        SubjectPosterArt(subject, Modifier.fillMaxSize())
        // Bottom scrim for legible text, like a profile photo caption.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xE6151515))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Text(
                subject.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AttendoColors.Cream,
            )
            val isLab = subject.name.contains("lab", ignoreCase = true)
            Text(
                listOfNotNull(subject.code.ifBlank { null }, if (isLab) "Lab" else null, "Target ${subject.targetPercent}%")
                    .joinToString("  ·  "),
                style = MaterialTheme.typography.labelLarge,
                color = AttendoColors.Green,
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
                color = AttendoColors.Cream.copy(alpha = 0.85f),
            )
        }
        stamps()
    }
}

/** Generated flat-polygon poster used as the card's "profile photo" (unique per subject code). */
@Composable
private fun SubjectPosterArt(subject: Subject, modifier: Modifier) {
    val palettes = listOf(
        listOf(Color(0xFFF2542D), Color(0xFFF28AC9), Color(0xFF2E9C9A), Color(0xFFF2C84B)),
        listOf(Color(0xFF2B5CE6), Color(0xFFE63946), Color(0xFFF1FAEE), Color(0xFFFFB703)),
        listOf(Color(0xFF50A65C), Color(0xFF232323), Color(0xFFF8FAED), Color(0xFFE0399A)),
        listOf(Color(0xFFE76F51), Color(0xFF264653), Color(0xFFE9C46A), Color(0xFF2A9D8F)),
        listOf(Color(0xFF7B2CBF), Color(0xFFFF9E00), Color(0xFF3A86FF), Color(0xFFFFD6E0)),
    )
    val seed = (subject.code.ifBlank { subject.name }).hashCode()
    val p = palettes[Math.floorMod(seed, palettes.size)]
    val initials = subject.name.split(" ").filter { it.isNotBlank() && it.first().isUpperCase() }
        .take(2).joinToString("") { it.first().toString() }.ifBlank { subject.name.take(2).uppercase() }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = java.util.Random(seed.toLong())
            drawRect(p[0])
            drawRect(p[1], Offset(0f, 0f), Size(w * (0.3f + r.nextFloat() * 0.25f), h * 0.32f))
            drawRect(p[2], Offset(w * (0.7f + r.nextFloat() * 0.1f), 0f), Size(w * 0.3f, h * 0.6f))
            drawRect(p[3], Offset(0f, h * 0.62f), Size(w * 0.35f, h * 0.2f))
            repeat(5) { i -> // thin poster stripes
                drawRect(p[(i + 1) % 4].copy(alpha = 0.8f), Offset(w * (0.05f + i * 0.045f), h * 0.36f), Size(w * 0.02f, h * 0.2f))
            }
            drawCircle(AttendoColors.Cream, radius = w * 0.3f, center = Offset(w * 0.55f, h * 0.36f))
        }
        Text(
            initials,
            fontSize = 92.sp,
            fontWeight = FontWeight.Black,
            color = AttendoColors.Ink,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 92.dp).graphicsLayer { translationX = 18.dp.toPx() },
        )
    }
}

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
