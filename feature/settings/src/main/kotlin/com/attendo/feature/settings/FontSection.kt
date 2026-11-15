package com.attendo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.Corner
import com.attendo.core.designsystem.theme.Space
import com.attendo.core.designsystem.theme.fontPreview
import com.attendo.core.model.FontChoice

/**
 * Font picker, above the theme picker and built the same way: one dropdown, each font's name
 * written in that font, with a line about it underneath. Changes apply instantly.
 */
@Composable
internal fun FontSection(font: FontChoice, onFontChange: (FontChoice) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val palette = AttendoColors.palette
    Column(
        Modifier
            .fillMaxWidth()
            .clip(Corner.Shape)
            .background(AttendoColors.CreamCard)
            .padding(Space.M),
    ) {
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(Corner.Shape)
                    .iosPress(PressStyle.Highlight, onClickLabel = "Choose font") { open = true }
                    .background(AttendoColors.Cream)
                    .border(1.dp, AttendoColors.CreamDeep, Corner.Shape)
                    .padding(horizontal = Space.M, vertical = Space.S),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Aa",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontPreview(font, palette)),
                    color = AttendoColors.Muted,
                )
                Spacer(Modifier.size(Space.S))
                Text(
                    font.label,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontPreview(font, palette)),
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = AttendoColors.Muted)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = AttendoColors.CreamCard) {
                FontChoice.entries.forEach { choice ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                choice.label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = fontPreview(choice, palette)),
                            )
                        },
                        trailingIcon = {
                            if (choice == font) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = AttendoColors.Green)
                        },
                        onClick = {
                            open = false
                            onFontChange(choice)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.XS))
        Text(font.note, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
    }
}
