package com.attendo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.AttendoPalette
import com.attendo.core.designsystem.theme.parseHexColor
import com.attendo.core.designsystem.theme.toHex
import com.attendo.core.model.ThemeMode

/**
 * Theme picker: Light (default), Dark (pitch black with dark-grey layers) or Custom, where every
 * colour of the app can be set with a hex code.
 */
@Composable
internal fun ThemeSection(
    theme: ThemeMode,
    customColors: Map<String, String>,
    onThemeChange: (ThemeMode) -> Unit,
    onCustomColorsChange: (Map<String, String>) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip("Light", AttendoPalette.Light, theme == ThemeMode.LIGHT, Modifier.weight(1f)) { onThemeChange(ThemeMode.LIGHT) }
            ThemeChip("Dark", AttendoPalette.Dark, theme == ThemeMode.DARK, Modifier.weight(1f)) { onThemeChange(ThemeMode.DARK) }
            ThemeChip("Custom", AttendoPalette.custom(customColors), theme == ThemeMode.CUSTOM, Modifier.weight(1f)) {
                onThemeChange(ThemeMode.CUSTOM)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (theme) {
                ThemeMode.LIGHT -> "Light: the original cream, ink and green."
                ThemeMode.DARK -> "Dark: pitch-black base with dark-grey cards for texture."
                ThemeMode.CUSTOM -> "Custom: set any colour with a hex code (#RRGGBB). Changes apply instantly."
            },
            style = MaterialTheme.typography.labelSmall,
            color = AttendoColors.Muted,
        )

        if (theme == ThemeMode.CUSTOM) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Start from:", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
                TextButton(onClick = { onCustomColorsChange(AttendoPalette.Light.toHex()) }) { Text("Light") }
                TextButton(onClick = { onCustomColorsChange(AttendoPalette.Dark.toHex()) }) { Text("Dark") }
            }
            val effective = AttendoPalette.custom(customColors).toHex()
            AttendoPalette.CUSTOM_KEYS.forEach { (key, label) ->
                HexColorRow(label = label, hex = effective.getValue(key)) { newHex ->
                    onCustomColorsChange(mapOf(key to newHex))
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(label: String, preview: AttendoPalette, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .iosPress(PressStyle.Scale, onClickLabel = "$label theme", onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(preview.background)
            .border(if (selected) 2.dp else 1.dp, if (selected) AttendoColors.Green else AttendoColors.CreamDeep, RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(preview.card, preview.accent, preview.hero).forEach {
                Box(Modifier.size(12.dp).clip(CircleShape).background(it))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = preview.text,
        )
    }
}

@Composable
private fun HexColorRow(label: String, hex: String, onValid: (String) -> Unit) {
    var text by remember { mutableStateOf(hex) }
    // Follow external changes (presets) unless the user is mid-edit on an invalid value.
    LaunchedEffect(hex) { if (parseHexColor(text)?.toHex() != hex) text = hex }
    val parsed = parseHexColor(text)
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(parsed ?: AttendoColors.CreamDeep)
                .border(1.dp, AttendoColors.CreamDeep, CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = text,
            onValueChange = { v ->
                text = v.take(9).uppercase()
                parseHexColor(text)?.let { onValid(it.toHex()) }
            },
            singleLine = true,
            isError = parsed == null,
            textStyle = MaterialTheme.typography.labelLarge,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AttendoColors.Green,
                unfocusedBorderColor = AttendoColors.CreamDeep,
                cursorColor = AttendoColors.Green,
            ),
            modifier = Modifier.width(118.dp),
        )
    }
}
