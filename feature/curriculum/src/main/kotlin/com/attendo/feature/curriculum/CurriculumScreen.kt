package com.attendo.feature.curriculum

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.Curriculum
import com.attendo.core.model.CurriculumCourse
import com.attendo.core.model.CurriculumUnit

/** Header items above the course list (title, overview). */
private const val HEADER_ITEMS = 2

/**
 * The programme curriculum, course by course: contact hours, course type, assessment, the five
 * units with their topics, and the course outcomes. [focusCode] opens on that course, expanded.
 */
@Composable
fun CurriculumScreen(curriculum: Curriculum, focusCode: String?, onClose: () -> Unit) {
    val listState = rememberLazyListState()
    val collapse by rememberCollapseFraction(listState)
    val focusIndex = focusCode?.let { code -> curriculum.courses.indexOfFirst { it.code.equals(code, ignoreCase = true) } } ?: -1
    var expanded by rememberSaveable { mutableStateOf(if (focusIndex >= 0) setOf(curriculum.courses[focusIndex].code) else emptySet<String>()) }
    LaunchedEffect(Unit) { if (focusIndex > 0) listState.scrollToItem(HEADER_ITEMS + focusIndex) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        CollapsingTopBar(title = "Curriculum", collapseFraction = { collapse }, onBack = onClose, backLabel = "Back")
        BounceContainer(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { LargeTitle("Curriculum") }
                item { Overview(curriculum) }
                items(curriculum.courses, key = { it.code }) { course ->
                    CourseCard(
                        course = course,
                        expanded = course.code in expanded,
                        onToggle = {
                            expanded = if (course.code in expanded) expanded - course.code else expanded + course.code
                        },
                    )
                }
                item {
                    Text(
                        "Source: MCA Detailed Curriculum, National Institute of Technology, Tiruchirappalli. " +
                            "Labs (CA701, CA703) aren't covered by it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendoColors.Muted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Overview(curriculum: Curriculum) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AttendoColors.Hero)
            .padding(22.dp),
    ) {
        Text("NIT TIRUCHIRAPPALLI", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
        Spacer(Modifier.height(6.dp))
        Text(curriculum.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = AttendoColors.Green)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Courses", curriculum.courses.size.toString())
            Stat("Contact hours", curriculum.totalHours.toString())
            Stat("Units", curriculum.courses.sumOf { it.units.size }.toString())
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, color = AttendoColors.OnHero)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
    }
}

@Composable
private fun CourseCard(course: CurriculumCourse, expanded: Boolean, onToggle: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AttendoColors.CreamCard),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .iosPress(PressStyle.Highlight, onClickLabel = if (expanded) "Collapse ${course.name}" else "Expand ${course.name}", onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    course.code,
                    style = MaterialTheme.typography.labelMedium,
                    color = AttendoColors.OnAccent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AttendoColors.GreenFill)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${course.contactHours} h · ${course.type} · ${course.assessment}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AttendoColors.Muted,
                )
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = AttendoColors.Muted,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(Motion.settle()) + fadeIn(Motion.settle()),
            exit = shrinkVertically(Motion.settle()) + fadeOut(Motion.settle()),
        ) {
            CourseDetails(course, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
        }
    }
}

/** A course's units with topic chips, then its outcomes. Also used on the subject screen. */
@Composable
fun CourseDetails(course: CurriculumCourse, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        course.units.forEach { UnitBlock(it) }
        if (course.outcomes.isNotEmpty()) {
            Column {
                Text("Course outcomes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                course.outcomes.forEach { outcome ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Box(
                            Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AttendoColors.Green),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(outcome, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnitBlock(unit: CurriculumUnit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Unit ${unit.number}",
                style = MaterialTheme.typography.labelMedium,
                color = AttendoColors.Green,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(unit.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            unit.topics.forEach { topic ->
                Text(
                    topic,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AttendoColors.Cream)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}
