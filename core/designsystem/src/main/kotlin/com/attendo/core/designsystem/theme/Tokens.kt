package com.attendo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale (DEC-024). Every padding and gap in the app is one of these steps; values in
 * between aren't used. Component sizes (a 48 dp button, a 24 dp icon) aren't spacing.
 */
object Space {
    val XXS: Dp = 4.dp
    val XS: Dp = 8.dp
    val S: Dp = 12.dp
    val M: Dp = 16.dp
    val L: Dp = 24.dp
    val XL: Dp = 32.dp
    val XXL: Dp = 48.dp

    /** Side margin of every screen. */
    val Screen: Dp = L
}

/**
 * The corner radius for every rounded surface: cards, fields, sheets and dialogs (DEC-024).
 * Buttons, chips and the tab bar are capsules and round things are circles (`CircleShape`), as in
 * Apple's design (DEC-026).
 */
object Corner {
    val Radius: Dp = 16.dp
    val Shape: RoundedCornerShape = RoundedCornerShape(Radius)
}
