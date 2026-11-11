package com.attendo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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

private const val CUSTOM_LABEL = "Custom"
private const val CUSTOM_DESCRIPTION = "Set any colour with a hex code (#RRGGBB). Changes apply instantly."

/**
 * Theme picker: one dropdown listing every built-in theme (Light, Dark, Ledger, Neon, Pastel,
 * VS Code) plus Custom, where each colour of the app can be set with a hex code.
 */
@Composable
internal fun ThemeSection(
    theme: ThemeMode,
    customColors: Map<String, String>,
    onThemeChange: (ThemeMode) -> Unit,
    onCustomColorsChange: (Map<String, String>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val preset = AttendoPalette.PRESETS.firstOrNull { it.first == theme }
    val label = preset?.second ?: CUSTOM_LABEL

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .padding(14.dp),
    ) {
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .iosPress(PressStyle.Highlight, onClickLabel = "Choose theme") { open = true }
                    .background(AttendoColors.Cream)
                    .border(1.dp, AttendoColors.CreamDeep, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Swatches(AttendoPalette.forMode(theme, customColors))
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = AttendoColors.Muted)
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                containerColor = AttendoColors.CreamCard,
            ) {
                AttendoPalette.PRESETS.forEach { (mode, name, _) ->
                    ThemeMenuItem(name, AttendoPalette.forMode(mode, customColors), selected = mode == theme) {
                        open = false
                        onThemeChange(mode)
                    }
                }
                ThemeMenuItem(CUSTOM_LABEL, AttendoPalette.custom(customColors), selected = theme == ThemeMode.CUSTOM) {
                    open = false
                    onThemeChange(ThemeMode.CUSTOM)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            preset?.third ?: CUSTOM_DESCRIPTION,
            style = MaterialTheme.typography.labelSmall,
            color = AttendoColors.Muted,
        )

        if (theme == ThemeMode.CUSTOM) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Start from:", style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
                AttendoPalette.PRESETS.forEach { (mode, name, _) ->
                    TextButton(onClick = { onCustomColorsChange(AttendoPalette.forMode(mode, emptyMap()).toHex()) }) { Text(name) }
                }
            }
            val effective = AttendoPalette.custom(customColors).toHex()
            AttendoPalette.CUSTOM_KEYS.forEach { (key, keyLabel) ->
                HexColorRow(label = keyLabel, hex = effective.getValue(key)) { newHex ->
                    onCustomColorsChange(mapOf(key to newHex))
                }
            }
        }
    }
}

/** Background, accent fill, text-colour accent and hero colour of a theme, as overlapping dots. */
@Composable
private fun Swatches(palette: AttendoPalette) {
    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        listOf(palette.background, palette.accentFill, palette.accent, palette.hero).forEach {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(it)
                    .border(1.dp, AttendoColors.Muted.copy(alpha = 0.5f), CircleShape),
            )
        }
    }
}

@Composable
private fun ThemeMenuItem(name: String, palette: AttendoPalette, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = AttendoColors.Ink,
            )
        },
        leadingIcon = { Swatches(palette) },
        trailingIcon = {
            if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = AttendoColors.Green, modifier = Modifier.size(18.dp))
        },
        onClick = onClick,
    )
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
