package com.attendo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AnimationMode
import kotlin.math.roundToInt

private val Modes = listOf(AnimationMode.SMOOTH, AnimationMode.IOS, AnimationMode.ANDROID, AnimationMode.NONE)

private fun AnimationMode.title() = when (this) {
    AnimationMode.SMOOTH -> "Smooth"
    AnimationMode.IOS -> "iOS"
    AnimationMode.ANDROID -> "Android"
    AnimationMode.NONE -> "None"
}

private fun AnimationMode.description() = when (this) {
    AnimationMode.SMOOTH ->
        "Smooth & relaxed: long, fully damped motion with no overshoot or abrupt cuts. Runs at your " +
            "screen's highest refresh rate."
    AnimationMode.IOS ->
        "iOS style: Apple-style spring physics, push/pop with parallax, swipe back, sheets and " +
            "rubber-band scrolling. High refresh rate."
    AnimationMode.ANDROID ->
        "Android: the stock Material animations: cross-fades, ripples and stretch overscroll."
    AnimationMode.NONE ->
        "No animations: no transitions, motion or launch screen. Everything is instant for the fastest app."
}

/** Four-stop slider: Smooth · iOS · Android · None. Saves when the thumb is released. */
@Composable
internal fun AnimationModeSlider(mode: AnimationMode, onChange: (AnimationMode) -> Unit) {
    var position by remember { mutableFloatStateOf(Modes.indexOf(mode).toFloat()) }
    LaunchedEffect(mode) { position = Modes.indexOf(mode).toFloat() }
    val shown = Modes[position.roundToInt().coerceIn(0, Modes.lastIndex)]

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(shown.title(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Slider(
            value = position,
            onValueChange = { position = it },
            onValueChangeFinished = {
                position = position.roundToInt().toFloat()
                onChange(Modes[position.toInt()])
            },
            valueRange = 0f..Modes.lastIndex.toFloat(),
            steps = Modes.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = AttendoColors.Green,
                activeTrackColor = AttendoColors.Green,
                inactiveTrackColor = AttendoColors.CreamDeep,
                activeTickColor = AttendoColors.Cream,
                inactiveTickColor = AttendoColors.Muted,
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            Modes.forEachIndexed { i, m ->
                Text(
                    m.title(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (m == shown) FontWeight.Bold else FontWeight.Normal,
                    color = if (m == shown) AttendoColors.Ink else AttendoColors.Muted,
                    textAlign = when (i) {
                        0 -> TextAlign.Start
                        Modes.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(shown.description(), style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
    }
}
