package com.attendo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.R

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
 * The one corner radius (DEC-024): cards, buttons, fields, chips, sheets and dialogs all use it.
 * Only things that are circles (dots, avatars, round icon buttons) use `CircleShape`.
 */
object Corner {
    val Radius: Dp = 16.dp
    val Shape: RoundedCornerShape = RoundedCornerShape(Radius)
}

/**
 * Plus Jakarta Sans, the app's typeface (SIL Open Font License, `core/designsystem/fonts/`).
 * One variable font file; each weight picks its point on the font's weight axis.
 */
val AttendoSans: FontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold),
)
