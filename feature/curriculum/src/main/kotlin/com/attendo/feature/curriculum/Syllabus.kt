package com.attendo.feature.curriculum

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendo.core.data.SyllabusRepository
import com.attendo.core.designsystem.component.TimerGlyph
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.CourseCatalog
import com.attendo.core.model.CourseProgress
import com.attendo.core.model.Curriculum
import com.attendo.core.model.CurriculumCourse
import com.attendo.core.model.CurriculumUnit
import com.attendo.core.model.Subject
import com.attendo.core.model.UnitKey
import kotlinx.coroutines.launch

/** The Syllabus page of the Focus tab, wired to the stored progress. */
@Composable
fun SyllabusRoute(
    curriculum: Curriculum,
    syllabus: SyllabusRepository,
    focusCode: String?,
    onFocusSubject: (code: String) -> Unit,
) {
    val covered by syllabus.covered.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    SyllabusPage(
        curriculum = curriculum,
        covered = covered,
        focusCode = focusCode,
        onToggleUnit = { code, unit, done -> scope.launch { syllabus.setCovered(code, unit, done) } },
        onFocusSubject = onFocusSubject,
    )
}

/**
 * The curriculum, made for studying: overall progress, one card per course with a progress ring,
 * and inside each course its units as a timeline. Tapping a unit's circle marks it covered.
 */
@Composable
fun SyllabusPage(
    curriculum: Curriculum,
    covered: Set<UnitKey>,
    focusCode: String?,
    onToggleUnit: (code: String, unit: String, covered: Boolean) -> Unit,
    onFocusSubject: (code: String) -> Unit,
) {
    var expanded by rememberSaveable(focusCode) {
        mutableStateOf(focusCode?.let { setOf(it.uppercase()) } ?: emptySet())
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (curriculum.courses.isEmpty()) {
            Text("No curriculum bundled.", style = MaterialTheme.typography.bodyMedium, color = AttendoColors.Muted)
            return@Column
        }
        Overview(curriculum, covered)
        curriculum.courses.forEachIndexed { index, course ->
            val key = course.code.uppercase()
            CourseCard(
                course = course,
                accent = courseColor(index),
                progress = curriculum.progress(course, covered),
                covered = covered,
                expanded = key in expanded,
                onToggleExpanded = { expanded = if (key in expanded) expanded - key else expanded + key },
                onToggleUnit = onToggleUnit,
                onFocus = { onFocusSubject(course.code) },
            )
        }
        Text(
            "From the MCA Detailed Curriculum, NIT Tiruchirappalli. Tap a unit's circle when you've covered it. " +
                "Labs (CA701, CA703) aren't part of it.",
            style = MaterialTheme.typography.labelSmall,
            color = AttendoColors.Muted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}

/** A subject page's syllabus: progress and the unit timeline for [code], if it's in the curriculum. */
@Composable
fun SubjectSyllabusSection(
    curriculum: Curriculum,
    syllabus: SyllabusRepository,
    code: String,
    onOpenSyllabus: (code: String) -> Unit,
) {
    val course = curriculum.course(code) ?: return
    val covered by syllabus.covered.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()
    val progress = curriculum.progress(course, covered)
    Row(Modifier.padding(top = 24.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Syllabus", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${progress.covered} of ${progress.total} units covered · ${course.contactHours} h",
                style = MaterialTheme.typography.labelSmall,
                color = AttendoColors.Muted,
            )
        }
        TextButton(onClick = { onOpenSyllabus(course.code) }) { Text("All courses", color = AttendoColors.Green) }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AttendoColors.CreamCard)
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 4.dp),
    ) {
        UnitTimeline(course, AttendoColors.Green, covered) { unit, done -> scope.launch { syllabus.setCovered(course.code, unit, done) } }
    }
}

private fun courseColor(index: Int): Color {
    val palette = listOf(AttendoColors.Green, AttendoColors.Blue, AttendoColors.Pink, AttendoColors.Yellow, AttendoColors.Water)
    return palette[Math.floorMod(index, palette.size)]
}

private fun CurriculumCourse.shortName(): String = CourseCatalog.shortName(Subject("", name, code))

@Composable
private fun Overview(curriculum: Curriculum, covered: Set<UnitKey>) {
    val done = curriculum.coveredUnits(covered)
    val total = curriculum.totalUnits
    val fraction by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, Motion.settle(), label = "overall")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(AttendoColors.Hero)
            .padding(20.dp),
    ) {
        Text(curriculum.title.uppercase(), style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$done", style = MaterialTheme.typography.displayLarge.copy(fontSize = 46.sp, lineHeight = 46.sp), color = AttendoColors.OnHero)
            Text(
                " / $total units covered",
                style = MaterialTheme.typography.titleMedium,
                color = AttendoColors.OnHero.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(AttendoColors.OnHero.copy(alpha = 0.15f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(CircleShape)
                    .background(AttendoColors.GreenFill),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Stat("${curriculum.courses.size}", "courses")
            Stat("${curriculum.totalHours}", "contact hours")
            Stat("${curriculum.courses.count { curriculum.progress(it, covered).let { p -> p.total > 0 && p.covered == p.total } }}", "completed")
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AttendoColors.OnHero)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
    }
}

@Composable
private fun CourseCard(
    course: CurriculumCourse,
    accent: Color,
    progress: CourseProgress,
    covered: Set<UnitKey>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleUnit: (code: String, unit: String, covered: Boolean) -> Unit,
    onFocus: () -> Unit,
) {
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, Motion.settle(), label = "chevron")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AttendoColors.CreamCard),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .iosPress(PressStyle.Highlight, onClickLabel = if (expanded) "Collapse ${course.name}" else "Open ${course.name}", onClick = onToggleExpanded)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(progress, accent, 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(course.shortName(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        course.code,
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendoColors.Muted,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AttendoColors.Cream)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Text(
                    course.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = AttendoColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${course.contactHours} h · ${course.units.size} units · ${course.type.substringBefore(" (")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AttendoColors.Muted,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = AttendoColors.Muted,
                modifier = Modifier.rotate(chevron),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(Motion.settle()) + fadeIn(Motion.settle()),
            exit = shrinkVertically(Motion.settle()) + fadeOut(Motion.settle()),
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(AttendoColors.CreamDeep))
                Spacer(Modifier.height(16.dp))
                UnitTimeline(course, accent, covered) { unit, done -> onToggleUnit(course.code, unit, done) }
                if (course.outcomes.isNotEmpty()) Outcomes(course.outcomes)
                Spacer(Modifier.height(14.dp))
                FocusButton(course.shortName(), onFocus)
            }
        }
    }
}

/** Ring showing units covered out of all units, with the count in the middle. */
@Composable
private fun ProgressRing(progress: CourseProgress, accent: Color, size: Dp) {
    val sweep by animateFloatAsState(progress.fraction, Motion.settle(), label = "ring")
    val track = AttendoColors.CreamDeep
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 5.dp.toPx()
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            if (sweep > 0f) {
                drawArc(accent, -90f, 360f * sweep, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Text("${progress.covered}/${progress.total}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

/** Units as a vertical timeline: numbered circles joined by a line, title and topics beside them. */
@Composable
fun UnitTimeline(
    course: CurriculumCourse,
    accent: Color,
    covered: Set<UnitKey>,
    onToggle: (unit: String, covered: Boolean) -> Unit,
) {
    Column {
        course.units.forEachIndexed { index, unit ->
            val done = UnitKey(course.code.uppercase(), unit.number.uppercase()) in covered
            UnitRow(unit, done, accent, isLast = index == course.units.lastIndex) { onToggle(unit.number, !done) }
        }
    }
}

@Composable
private fun UnitRow(unit: CurriculumUnit, done: Boolean, accent: Color, isLast: Boolean, onToggle: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    var open by remember { mutableStateOf(false) }
    val fill by animateColorAsState(if (done) accent else Color.Transparent, Motion.settle(), label = "unitFill")
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(34.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .iosPress(PressStyle.Scale, onClickLabel = if (done) "Mark unit ${unit.number} not covered" else "Mark unit ${unit.number} covered") {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggle()
                    }
                    .background(fill)
                    .border(2.dp, if (done) accent else AttendoColors.CreamDeep, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = AttendoColors.on(accent), modifier = Modifier.size(16.dp))
                } else {
                    Text(unit.number, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            if (!isLast) {
                Box(
                    Modifier
                        .padding(vertical = 4.dp)
                        .width(2.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (done) accent.copy(alpha = 0.6f) else AttendoColors.CreamDeep),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 4.dp else 18.dp)
                .iosPress(PressStyle.Dim, onClickLabel = if (open) "Show fewer topics" else "Show all topics") { open = !open },
        ) {
            Text(
                "UNIT ${unit.number}",
                style = MaterialTheme.typography.labelSmall,
                color = if (done) accent else AttendoColors.Muted,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(unit.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                unit.topics.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = AttendoColors.Muted,
                maxLines = if (open) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!open && unit.topics.size > 4) {
                Text("${unit.topics.size} topics · tap for all", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Green)
            }
        }
    }
}

@Composable
private fun Outcomes(outcomes: List<String>) {
    Column(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AttendoColors.Cream)
            .padding(14.dp),
    ) {
        Text("You'll be able to", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        outcomes.forEachIndexed { i, outcome ->
            Row(Modifier.padding(top = 8.dp)) {
                Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Green, fontWeight = FontWeight.Bold, modifier = Modifier.width(18.dp))
                Text(outcome, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FocusButton(name: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .iosPress(PressStyle.Scale, onClickLabel = "Focus on $name", onClick = onClick)
            .background(AttendoColors.Hero),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerGlyph(color = AttendoColors.OnHero, size = 18.dp)
        Spacer(Modifier.width(10.dp))
        Text("Focus on $name", style = MaterialTheme.typography.labelLarge, color = AttendoColors.OnHero)
    }
}
