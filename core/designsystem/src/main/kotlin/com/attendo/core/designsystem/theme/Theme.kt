package com.attendo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand palette taken from the reference designs (#232323 / #50A65C / #F8FAED) plus accents. */
object AttendoColors {
    val Ink = Color(0xFF232323)
    val Green = Color(0xFF50A65C)
    val GreenDark = Color(0xFF3E8A4A)
    val Cream = Color(0xFFF8FAED)
    val CreamCard = Color(0xFFF0F2E1)
    val CreamDeep = Color(0xFFE7EAD0)
    val Muted = Color(0xFF8C8E82)
    val Pink = Color(0xFFE0399A)
    val Blue = Color(0xFF2B8FE0)
    val Yellow = Color(0xFFE2D646)
    val Water = Color(0xFFA9DCF5)
}

private val AttendoColorScheme = lightColorScheme(
    primary = AttendoColors.Green,
    onPrimary = AttendoColors.Cream,
    secondary = AttendoColors.Ink,
    onSecondary = AttendoColors.Cream,
    background = AttendoColors.Cream,
    onBackground = AttendoColors.Ink,
    surface = AttendoColors.Cream,
    onSurface = AttendoColors.Ink,
    surfaceVariant = AttendoColors.CreamCard,
    onSurfaceVariant = AttendoColors.Muted,
    outline = AttendoColors.CreamDeep,
    error = AttendoColors.Pink,
)

private val Grotesk = FontFamily.SansSerif

/** Handwritten style used in speech bubbles. */
val HandFont = FontFamily.Cursive

private val AttendoTypography = Typography(
    displayLarge = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 54.sp, lineHeight = 54.sp, letterSpacing = (-1.5).sp),
    headlineLarge = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Grotesk, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Grotesk, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Grotesk, fontSize = 11.sp),
)

/** Bubble/handwritten text style. */
val HandTextStyle = TextStyle(
    fontFamily = HandFont,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 19.sp,
    color = AttendoColors.Ink,
)

private val AttendoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun AttendoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AttendoColorScheme,
        typography = AttendoTypography,
        shapes = AttendoShapes,
        content = content,
    )
}
