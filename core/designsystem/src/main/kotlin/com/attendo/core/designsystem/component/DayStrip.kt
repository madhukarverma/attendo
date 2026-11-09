package com.attendo.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.attendo.core.designsystem.motion.IosMotion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.theme.AttendoColors
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.remember
import java.time.format.TextStyle
import java.util.Locale

/**
 * Horizontal day picker ("WE 12  TH 13 ... MO 17"). The selected day is a dark pill; days that have
 * any attendance marked get a soft cream circle.
 */
@Composable
fun DayStrip(
    days: List<LocalDate>,
    selected: LocalDate,
    markedDays: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    /** Optional coloured marker under a day (academic calendar events). */
    dots: Map<LocalDate, Color> = emptyMap(),
) {
    // Days interleaved with a month label wherever a new month begins (and at the very start).
    val entries: List<Any> = remember(days) {
        buildList {
            days.forEachIndexed { i, d ->
                if (i == 0 || d.dayOfMonth == 1) add(YearMonth.from(d))
                add(d)
            }
        }
    }
    val state = rememberLazyListState()
    LaunchedEffect(days.firstOrNull()) {
        val idx = entries.indexOf(selected)
        if (idx >= 0) state.scrollToItem((idx - 3).coerceAtLeast(0))
    }
    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(entries, key = { if (it is LocalDate) it.toEpochDay() else "m$it" }) { entry ->
            if (entry is YearMonth) {
                MonthSeparator(entry)
                return@items
            }
            val day = entry as LocalDate
            val isSelected = day == selected
            val pill by animateColorAsState(
                if (isSelected) AttendoColors.Ink else AttendoColors.Cream,
                animationSpec = IosMotion.bouncy(),
                label = "dayPill",
            )
            Column(
                Modifier
                    .iosPress(PressStyle.Scale, onClickLabel = day.toString()) { onSelect(day) }
                    .clip(RoundedCornerShape(50))
                    .background(pill)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .width(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) AttendoColors.Cream else AttendoColors.Muted,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> pill
                                day in markedDays -> AttendoColors.CreamDeep
                                else -> pill
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AttendoColors.Cream else AttendoColors.Ink,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dots[day] ?: Color.Transparent),
                )
            }
        }
    }
}

/** Thin divider with the month name written vertically, e.g. "SEP". */
@Composable
private fun MonthSeparator(month: YearMonth) {
    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(1.dp).height(56.dp).background(AttendoColors.CreamDeep))
        Column(Modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase().forEach { ch ->
                Text(ch.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AttendoColors.Green)
            }
        }
    }
}
