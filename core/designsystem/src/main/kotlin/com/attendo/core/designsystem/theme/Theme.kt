package com.attendo.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendo.core.model.ThemeMode

/** Every colour the app uses. Light is the original brand palette (#232323 / #50A65C / #F8FAED). */
@Immutable
data class AttendoPalette(
    val background: Color,
    val card: Color,
    val cardDeep: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accentDark: Color,
    /** Big feature cards and the bottom bar. */
    val hero: Color,
    val onHero: Color,
    val pink: Color,
    val blue: Color,
    val yellow: Color,
    val water: Color,
) {
    val isDark: Boolean get() = background.luminance() < 0.4f

    companion object {
        val Light = AttendoPalette(
            background = Color(0xFFF8FAED),
            card = Color(0xFFF0F2E1),
            cardDeep = Color(0xFFE7EAD0),
            text = Color(0xFF232323),
            muted = Color(0xFF8C8E82),
            accent = Color(0xFF50A65C),
            accentDark = Color(0xFF3E8A4A),
            hero = Color(0xFF232323),
            onHero = Color(0xFFF8FAED),
            pink = Color(0xFFE0399A),
            blue = Color(0xFF2B8FE0),
            yellow = Color(0xFFE2D646),
            water = Color(0xFFA9DCF5),
        )

        /** Pitch-black base with dark-grey layers for texture. */
        val Dark = AttendoPalette(
            background = Color(0xFF000000),
            card = Color(0xFF141414),
            cardDeep = Color(0xFF262626),
            text = Color(0xFFF2F2F2),
            muted = Color(0xFF8E8E93),
            accent = Color(0xFF5DBB6A),
            accentDark = Color(0xFF4A9A55),
            hero = Color(0xFF1C1C1E),
            onHero = Color(0xFFF2F2F2),
            pink = Color(0xFFFF5FAE),
            blue = Color(0xFF4DA3FF),
            yellow = Color(0xFFF2D94B),
            water = Color(0xFF7FC8EE),
        )

        /** Colours a user can override in the Custom theme, in display order. */
        val CUSTOM_KEYS: List<Pair<String, String>> = listOf(
            "background" to "Background",
            "card" to "Cards",
            "card_deep" to "Card accents & tracks",
            "text" to "Text",
            "muted" to "Secondary text",
            "accent" to "Accent (green)",
            "hero" to "Hero cards & bottom bar",
            "on_hero" to "Text on hero cards",
            "pink" to "Pink (absent, holidays)",
            "blue" to "Blue (no class, exams)",
            "yellow" to "Yellow (deadlines)",
        )

        /** Builds a custom palette from `key -> #RRGGBB`; anything missing or invalid comes from [base]. */
        fun custom(hex: Map<String, String>, base: AttendoPalette = Light): AttendoPalette {
            fun pick(key: String, fallback: Color) = hex[key]?.let(::parseHexColor) ?: fallback
            val accent = pick("accent", base.accent)
            return base.copy(
                background = pick("background", base.background),
                card = pick("card", base.card),
                cardDeep = pick("card_deep", base.cardDeep),
                text = pick("text", base.text),
                muted = pick("muted", base.muted),
                accent = accent,
                accentDark = accent.copy(red = accent.red * 0.8f, green = accent.green * 0.8f, blue = accent.blue * 0.8f),
                hero = pick("hero", base.hero),
                onHero = pick("on_hero", base.onHero),
                pink = pick("pink", base.pink),
                blue = pick("blue", base.blue),
                yellow = pick("yellow", base.yellow),
            )
        }

        fun forMode(mode: ThemeMode, customHex: Map<String, String>) = when (mode) {
            ThemeMode.LIGHT -> Light
            ThemeMode.DARK -> Dark
            ThemeMode.CUSTOM -> custom(customHex)
        }
    }

    /** `key -> #RRGGBB` for the custom-theme editor. */
    fun toHex(): Map<String, String> = mapOf(
        "background" to background, "card" to card, "card_deep" to cardDeep, "text" to text,
        "muted" to muted, "accent" to accent, "hero" to hero, "on_hero" to onHero,
        "pink" to pink, "blue" to blue, "yellow" to yellow,
    ).mapValues { (_, c) -> c.toHex() }
}

/** Parses `#RRGGBB`, `RRGGBB` or `#AARRGGBB`; null when invalid. */
fun parseHexColor(text: String): Color? {
    val h = text.trim().removePrefix("#")
    if (!(h.length == 6 || h.length == 8) || h.any { it.lowercaseChar() !in "0123456789abcdef" }) return null
    val v = h.toLong(16)
    return if (h.length == 6) Color(0xFF000000 or v) else Color(v)
}

fun Color.toHex(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}

/**
 * App colours. Backed by snapshot state, so switching theme recolours every screen (composition
 * and Canvas drawing alike) without each call site having to change.
 */
object AttendoColors {
    var palette: AttendoPalette by mutableStateOf(AttendoPalette.Light)

    val Ink: Color get() = palette.text
    val Green: Color get() = palette.accent
    val GreenDark: Color get() = palette.accentDark
    val Cream: Color get() = palette.background
    val CreamCard: Color get() = palette.card
    val CreamDeep: Color get() = palette.cardDeep
    val Muted: Color get() = palette.muted
    val Pink: Color get() = palette.pink
    val Blue: Color get() = palette.blue
    val Yellow: Color get() = palette.yellow
    val Water: Color get() = palette.water
    val Hero: Color get() = palette.hero
    val OnHero: Color get() = palette.onHero
}

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

/** Bubble/handwritten text style (re-read each time so it follows the theme). */
val HandTextStyle: TextStyle
    get() = TextStyle(
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
    val p = AttendoColors.palette
    val base = if (p.isDark) darkColorScheme() else lightColorScheme()
    // Every Material slot comes from the palette, so dialogs, pickers and menus never fall back to
    // Material's default purple-tinted surfaces.
    val scheme = base.copy(
        primary = p.accent, onPrimary = p.background,
        primaryContainer = p.cardDeep, onPrimaryContainer = p.text,
        secondary = p.text, onSecondary = p.background,
        secondaryContainer = p.cardDeep, onSecondaryContainer = p.text,
        tertiary = p.accent, onTertiary = p.background,
        tertiaryContainer = p.cardDeep, onTertiaryContainer = p.text,
        background = p.background, onBackground = p.text,
        surface = p.background, onSurface = p.text,
        surfaceVariant = p.card, onSurfaceVariant = p.muted,
        surfaceTint = Color.Transparent,
        surfaceBright = p.card, surfaceDim = p.background,
        surfaceContainerLowest = p.background, surfaceContainerLow = p.card,
        surfaceContainer = p.card, surfaceContainerHigh = p.card, surfaceContainerHighest = p.cardDeep,
        inverseSurface = p.hero, inverseOnSurface = p.onHero, inversePrimary = p.accent,
        outline = p.cardDeep, outlineVariant = p.cardDeep,
        error = p.pink, onError = p.background,
    )
    MaterialTheme(colorScheme = scheme, typography = AttendoTypography, shapes = AttendoShapes) {
        // Material 3 only sets the content colour inside a Surface; without this, every Text and
        // Icon that doesn't pass a colour falls back to black, which vanished on the dark theme.
        CompositionLocalProvider(LocalContentColor provides p.text, content = content)
    }
}
