package com.attendo.feature.attendance

import com.attendo.core.designsystem.component.CircleMascot
import com.attendo.core.designsystem.component.TabBarClearance
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Icon
import com.attendo.core.designsystem.component.AttendoIcons
import androidx.compose.ui.graphics.vector.ImageVector
import com.attendo.core.designsystem.theme.Corner
import com.attendo.core.designsystem.theme.Space
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendo.core.designsystem.component.Mood
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.motion.rememberHaptics
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AttendanceStatus
import com.attendo.core.model.CourseCatalog
import com.attendo.core.model.Physics
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** What a swipe (or its button) records. */
internal enum class SwipeAction(val status: AttendanceStatus, val count: Int = 1) {
    PRESENT(AttendanceStatus.PRESENT),
    ABSENT(AttendanceStatus.ABSENT),
    NO_CLASS(AttendanceStatus.CANCELLED),

    /** Swipe up: the teacher took two classes back to back and both were attended. */
    PRESENT_DOUBLE(AttendanceStatus.PRESENT, 2),
}

/**
 * Dating-app style card stack for the selected day:
 * swipe right = Present, left = Absent, down = No class, up = Present ×2 (a double class).
 * Buttons do the same, plus undo.
 *
 * Fluid like iOS (WWDC18 "Designing Fluid Interfaces"): the card tracks the finger 1:1 from
 * wherever it is, a flick counts by where it is heading (momentum projection), the throw or the
 * spring back continues at the finger's speed, and a card springing back can be caught mid-way.
 */
@Composable
internal fun SwipeDeck(
    cards: List<SubjectDayItem>,
    totalToday: Int,
    canUndo: Boolean,
    onSwipe: (subjectId: String, status: AttendanceStatus, count: Int) -> Unit,
    onUndo: () -> Unit,
    colorFor: (subjectId: String) -> Color,
    modifier: Modifier = Modifier,
    /** Room from the deck's top down to the bottom of the screen; the cards shrink so the buttons stay above the tab bar. */
    availableHeight: Dp? = null,
) {
    val haptics = rememberHaptics()
    val deckHeight = availableHeight
        ?.let { (it - TabBarClearance - DeckControlsHeight).coerceIn(MinDeckHeight, MaxDeckHeight) }
        ?: MaxDeckHeight
    val compact = deckHeight < CompactDeckHeight
    var requested by remember { mutableStateOf<SwipeAction?>(null) }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(deckHeight), contentAlignment = Alignment.Center) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val top = cards.firstOrNull()
            if (top == null) {
                AllCaughtUp(totalToday)
            } else {
                key(top.summary.subject.id) {
                    // X and Y are separate springs so each keeps its own velocity (WWDC18).
                    val x = remember { Animatable(0f) }
                    val y = remember { Animatable(0f) }
                    val scope = rememberCoroutineScope()
                    val thresholdX = widthPx * 0.28f
                    val thresholdY = heightPx * 0.2f
                    // Read only inside graphicsLayer blocks, so dragging redraws without recomposing.
                    fun progress() = maxOf(abs(x.value) / thresholdX, abs(y.value) / thresholdY).coerceIn(0f, 1f)
                    var committing by remember { mutableStateOf(false) }
                    var dragged by remember { mutableStateOf(false) }

                    /** The action a card resting at (px, py) stands for, along its dominant axis. */
                    fun actionAt(px: Float, py: Float): SwipeAction? =
                        if (abs(px) / thresholdX >= abs(py) / thresholdY) {
                            when {
                                px > thresholdX -> SwipeAction.PRESENT
                                px < -thresholdX -> SwipeAction.ABSENT
                                else -> null
                            }
                        } else {
                            when {
                                py > thresholdY -> SwipeAction.NO_CLASS
                                py < -thresholdY -> SwipeAction.PRESENT_DOUBLE
                                else -> null
                            }
                        }

                    /** Throws the card off-screen, continuing at the finger's [velocity] (no seam). */
                    suspend fun commit(action: SwipeAction, velocity: Velocity = Velocity.Zero) {
                        committing = true
                        haptics.confirm()
                        val horizontal = action == SwipeAction.PRESENT || action == SwipeAction.ABSENT
                        val axis = if (horizontal) x else y
                        val sign = if (action == SwipeAction.PRESENT || action == SwipeAction.NO_CLASS) 1f else -1f
                        val target = sign * (if (horizontal) widthPx else heightPx) * 1.5f
                        var reported = false
                        fun report() {
                            if (!reported) {
                                reported = true
                                onSwipe(top.summary.subject.id, action.status, action.count)
                            }
                        }
                        // Hand over to the next card as soon as this one is out of view.
                        axis.animateTo(target, Motion.settle(), initialVelocity = if (horizontal) velocity.x else velocity.y) {
                            if (abs(value) >= abs(target) * 0.75f) report()
                        }
                        report()
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
                            compact,
                            Modifier.graphicsLayer {
                                val p = progress()
                                val s = 0.92f + 0.08f * p
                                scaleX = s
                                scaleY = s
                                translationY = (1f - p) * 18.dp.toPx()
                            },
                        )
                    }
                    SubjectProfileCard(
                        top,
                        colorFor(top.summary.subject.id),
                        compact,
                        Modifier
                            .graphicsLayer {
                                translationX = x.value
                                translationY = y.value
                                rotationZ = x.value / widthPx * 14f
                            }
                            // Touching a card that is still springing back catches it where it is;
                            // letting go without dragging lets it settle.
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    dragged = false
                                    if (!committing) {
                                        scope.launch {
                                            x.stop()
                                            y.stop()
                                        }
                                    }
                                    waitForUpOrCancellation()
                                    if (!dragged && !committing && (x.value != 0f || y.value != 0f)) {
                                        scope.launch {
                                            launch { x.animateTo(0f, Motion.release()) }
                                            y.animateTo(0f, Motion.release())
                                        }
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                val tracker = VelocityTracker()
                                var dragX = 0f
                                var dragY = 0f
                                var armed: SwipeAction? = null
                                detectDragGestures(
                                    onDragStart = {
                                        dragged = true
                                        // Start from what's on screen, not from rest.
                                        dragX = x.value
                                        dragY = y.value
                                        armed = actionAt(dragX, dragY)
                                        tracker.resetTracking()
                                    },
                                    onDrag = { change, drag ->
                                        if (committing) return@detectDragGestures
                                        change.consume()
                                        dragX += drag.x
                                        dragY += drag.y
                                        tracker.addPosition(change.uptimeMillis, Offset(dragX, dragY))
                                        scope.launch {
                                            x.snapTo(dragX)
                                            y.snapTo(dragY)
                                        }
                                        // A tick when letting go here would start (or stop) counting.
                                        val now = actionAt(dragX, dragY)
                                        if (now != armed) {
                                            armed = now
                                            haptics.tick()
                                        }
                                    },
                                    onDragEnd = {
                                        if (committing) return@detectDragGestures
                                        val v = tracker.calculateVelocity()
                                        // Decide by where the flick is heading, not where it stopped.
                                        val action = actionAt(
                                            dragX + Physics.project(v.x, Physics.FAST_DECELERATION),
                                            dragY + Physics.project(v.y, Physics.FAST_DECELERATION),
                                        )
                                        scope.launch {
                                            if (action != null) {
                                                commit(action, v)
                                            } else {
                                                launch { x.animateTo(0f, Motion.release(), initialVelocity = v.x) }
                                                y.animateTo(0f, Motion.release(), initialVelocity = v.y)
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        if (committing) return@detectDragGestures
                                        scope.launch {
                                            launch { x.animateTo(0f, Motion.release()) }
                                            y.animateTo(0f, Motion.release())
                                        }
                                    },
                                )
                            },
                        stamps = {
                            Stamp("PRESENT", AttendoColors.Green, -14f, { (x.value / thresholdX).coerceIn(0f, 1f) }, Alignment.TopStart)
                            Stamp("ABSENT", AttendoColors.Pink, 14f, { (-x.value / thresholdX).coerceIn(0f, 1f) }, Alignment.TopEnd)
                            Stamp("NO CLASS", AttendoColors.Blue, 0f, { (y.value / thresholdY).coerceIn(0f, 1f) }, Alignment.Center)
                            Stamp("PRESENT ×2", AttendoColors.Green, 0f, { (-y.value / thresholdY).coerceIn(0f, 1f) }, Alignment.BottomCenter)
                        },
                    )
                }
            }
        }

        Text(
            if (cards.isEmpty()) " " else "${cards.size} of $totalToday left",
            style = MaterialTheme.typography.labelSmall,
            color = AttendoColors.Muted,
            modifier = Modifier.padding(top = Space.S, bottom = Space.S),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.S), verticalAlignment = Alignment.CenterVertically) {
            RoundAction(AttendoIcons.Undo, AttendoColors.Yellow, 46.dp, enabled = canUndo, label = "Undo last mark", onClick = onUndo)
            RoundAction(AttendoIcons.Cross, AttendoColors.Pink, 62.dp, enabled = cards.isNotEmpty(), label = "Absent") {
                requested = SwipeAction.ABSENT
            }
            RoundAction(AttendoIcons.ArrowDown, AttendoColors.Blue, 50.dp, enabled = cards.isNotEmpty(), label = "No class") {
                requested = SwipeAction.NO_CLASS
            }
            RoundAction(AttendoIcons.Check, AttendoColors.Green, 62.dp, enabled = cards.isNotEmpty(), label = "Present") {
                requested = SwipeAction.PRESENT
            }
            RoundAction(null, AttendoColors.Green, 50.dp, text = "×2", enabled = cards.isNotEmpty(), label = "Present for a double class") {
                requested = SwipeAction.PRESENT_DOUBLE
            }
        }
    }
}

/** A subject shown like a dating-profile card: poster "photo", name, and an attendance "bio". */
@Composable
private fun SubjectProfileCard(
    item: SubjectDayItem,
    color: Color,
    /** A short deck (small screens): smaller abbreviation so the details still fit. */
    compact: Boolean,
    modifier: Modifier = Modifier,
    stamps: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    val subject = item.summary.subject
    val summary = item.summary
    val onCard = contentColorFor(color)
    Box(
        modifier
            .fillMaxWidth(0.88f)
            .fillMaxHeight(0.95f)
            .shadow(14.dp, Corner.Shape)
            .clip(Corner.Shape)
            .background(color),
    ) {
        // Big abbreviation as the card's "face".
        Text(
            CourseCatalog.shortName(subject),
            fontSize = if (compact) 44.sp else 64.sp,
            lineHeight = if (compact) 46.sp else 66.sp,
            fontWeight = FontWeight.Black,
            color = onCard,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = Space.L).padding(bottom = if (compact) Space.XXL + Space.M else Space.XXL * 2),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(Space.L)) {
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
            Spacer(Modifier.height(Space.XS))
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

/** Tallest the card area gets (big screens), and the shortest before it would stop reading well. */
private val MaxDeckHeight = 430.dp
private val MinDeckHeight = 280.dp

/** Below this the card text shrinks. */
private val CompactDeckHeight = 360.dp

/** The count line and the row of round buttons under the cards. */
private val DeckControlsHeight = 112.dp

/** Ten clearly different colours; shuffled once with a fixed seed so they look random but stay stable. */
private val CardPalette: List<Color> = listOf(
    Color(0xFFF2542D), Color(0xFF2B5CE6), Color(0xFF50A65C), Color(0xFFE0399A), Color(0xFFF2C84B),
    Color(0xFF232323), Color(0xFF2A9D8F), Color(0xFF00B4D8), Color(0xFF8C5E3C), Color(0xFF3D405B),
).shuffled(kotlin.random.Random(2026))

/** Colour for the subject at [index] in the subject list: distinct for the first ten subjects. */
internal fun cardColor(index: Int): Color = CardPalette[Math.floorMod(index, CardPalette.size)]


@Composable
private fun androidx.compose.foundation.layout.BoxScope.Stamp(
    text: String,
    color: Color,
    angle: Float,
    /** Read in the draw phase: the stamp fades with the drag without recomposing. */
    alpha: () -> Float,
    alignment: Alignment,
) {
    Text(
        text,
        color = color,
        fontSize = 30.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        modifier = Modifier
            .align(alignment)
            .padding(Space.XL)
            .graphicsLayer {
                this.alpha = alpha()
                rotationZ = angle
            }
            .border(4.dp, color, Corner.Shape)
            .padding(horizontal = Space.S, vertical = Space.XXS),
    )
}

@Composable
private fun RoundAction(
    icon: ImageVector?,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean,
    label: String,
    /** Shown instead of an icon ("×2"). */
    text: String? = null,
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
        val tint = if (enabled) color else AttendoColors.CreamDeep
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.45f))
        } else {
            Text(text.orEmpty(), color = tint, fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AllCaughtUp(total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircleMascot(Modifier.size(160.dp), mood = Mood.Happy)
        Text("All caught up!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            if (total == 0) "Add subjects to start swiping." else "All $total subjects are marked for this day.",
            style = MaterialTheme.typography.bodyMedium,
            color = AttendoColors.Muted,
        )
    }
}
