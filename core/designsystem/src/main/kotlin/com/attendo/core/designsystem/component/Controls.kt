package com.attendo.core.designsystem.component

import com.attendo.core.designsystem.theme.ContinuousShape
import androidx.compose.foundation.shape.CircleShape
import com.attendo.core.designsystem.theme.Space
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import com.attendo.core.designsystem.motion.Motion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** A rounded pill with an optional icon: the app's standard secondary/primary action button. */
@Composable
fun PillButton(
    text: String,
    icon: ImageVector?,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(48.dp)
            .clip(CircleShape)
            .iosPress(PressStyle.Dim, enabled = enabled, onClickLabel = text, onClick = onClick)
            .background(if (enabled) container else AttendoColors.CreamDeep)
            .padding(horizontal = Space.M),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) content else AttendoColors.Muted
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Space.XS))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
    }
}

/**
 * A rounded progress bar whose fill animates in the draw phase only, so a moving bar never
 * re-measures or recomposes (see the animation-performance skill). The fill grows from empty on
 * first show and springs to new values. [vertical] fills from the bottom; [marker] draws a thin
 * tick at that fraction (a target); [minFraction] keeps a sliver visible for zero values.
 */
@Composable
fun ProgressBar(
    fraction: Float,
    fill: Color,
    track: Color,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    minFraction: Float = 0f,
    marker: Float? = null,
    markerColor: Color = AttendoColors.Ink,
    cornerRadius: Dp? = null,
) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(fraction) { shown.animateTo(fraction.coerceIn(0f, 1f), Motion.settle()) }
    Canvas(modifier) {
        val radius = CornerRadius(cornerRadius?.toPx() ?: (minOf(size.width, size.height) / 2))
        drawRoundRect(track, cornerRadius = radius)
        val f = shown.value.coerceAtLeast(minFraction)
        if (f > 0f) {
            if (vertical) {
                drawRoundRect(fill, Offset(0f, size.height * (1f - f)), Size(size.width, size.height * f), radius)
            } else {
                drawRoundRect(fill, Offset.Zero, Size(size.width * f, size.height), radius)
            }
        }
        if (marker != null) {
            val w = 2.dp.toPx()
            drawRect(markerColor, Offset(size.width * marker.coerceIn(0f, 1f) - w / 2, 0f), Size(w, size.height))
        }
    }
}

/**
 * A row that opens another screen: icon, title, one line of explanation and a chevron. Plain, with
 * no card around it, so a list of them reads as one group.
 */
@Composable
fun NavigationRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(ContinuousShape(14.dp))
            .iosPress(PressStyle.Dim, onClickLabel = title, onClick = onClick)
            .padding(vertical = Space.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AttendoColors.Ink, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(Space.M))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AttendoColors.Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AttendoColors.Muted)
        }
        Spacer(Modifier.width(Space.XS))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AttendoColors.Muted)
    }
}

/** A number over a small label, for the figures row on hero cards. */
@Composable
fun HeroStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = AttendoColors.OnHero)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
    }
}

/** Outlined text field on a card: a soft border that turns accent-coloured while typing. */
@Composable
fun attendoFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AttendoColors.Green,
    unfocusedBorderColor = AttendoColors.CreamDeep,
    cursorColor = AttendoColors.Green,
    focusedLabelColor = AttendoColors.Green,
)

/** Text field filled with [container] and no visible border until it's focused. */
@Composable
fun attendoFilledFieldColors(container: Color): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = container,
    unfocusedContainerColor = container,
    focusedBorderColor = AttendoColors.Green,
    unfocusedBorderColor = container,
    cursorColor = AttendoColors.Green,
)

/** Material date picker in the app's colours, working in [LocalDate] (the picker itself uses UTC millis). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendoDatePicker(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK", color = AttendoColors.Green) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
    ) { DatePicker(state = state) }
}

/** Material time picker in the app's colours; 12- or 24-hour as the phone is set. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendoTimePicker(
    initial: LocalTime,
    onDismiss: () -> Unit,
    title: String? = null,
    confirmLabel: String = "OK",
    onPick: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.let { { Text(it) } },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = AttendoColors.GreenFill,
                    timeSelectorSelectedContentColor = AttendoColors.OnAccent,
                    periodSelectorSelectedContainerColor = AttendoColors.GreenFill,
                    periodSelectorSelectedContentColor = AttendoColors.OnAccent,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text(confirmLabel, color = AttendoColors.Green) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AttendoColors.Muted) } },
        containerColor = AttendoColors.CreamCard,
    )
}
