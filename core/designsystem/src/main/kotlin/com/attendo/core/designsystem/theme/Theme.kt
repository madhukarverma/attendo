package com.attendo.core.designsystem.theme

import androidx.compose.ui.unit.dp
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.attendo.core.model.ThemeMode

/**
 * The typefaces a theme uses. Themes use fonts that ship with Android (the platform font first, as
 * the apple-design skill asks); Settings › Font can swap the body font for a bundled one.
 */
@Immutable
data class AttendoFonts(
    val body: FontFamily = FontFamily.SansSerif,
    /** Speech bubbles. */
    val hand: FontFamily = FontFamily.Cursive,
    val handItalic: Boolean = true,
    /** Text size multiplier (monospace faces run wide). */
    val scale: Float = 1f,
) {
    companion object {
        /** System sans with handwritten bubbles (Light, Dark, Custom). */
        val Default = AttendoFonts()

        /** System sans everywhere. */
        val Sans = AttendoFonts(hand = FontFamily.SansSerif, handItalic = false)

        /** The system's code font. */
        val Mono = AttendoFonts(body = FontFamily.Monospace, hand = FontFamily.Monospace, handItalic = false, scale = 0.92f)

        /** Android's typewriter-style slab monospace ("serif-monospace"). */
        val Typewriter: AttendoFonts by lazy {
            val face = AppFonts.Typewriter
            AttendoFonts(body = face, hand = face, handItalic = false, scale = 0.94f)
        }
    }
}

/**
 * Every colour the app uses, plus its fonts. Light is the original brand palette
 * (#232323 / #50A65C / #F8FAED).
 *
 * [accent], [pink], [blue] and [yellow] must be readable as text on [background]. Themes with
 * soft, pale accents (Pastel) also set the `*Fill` colours, used for large filled shapes such as
 * buttons and the selected tab; text on a fill comes from [AttendoColors.on].
 */
@Immutable
data class AttendoPalette(
    val background: Color,
    val card: Color,
    val cardDeep: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    /** Big feature cards and the bottom bar. */
    val hero: Color,
    val onHero: Color,
    val pink: Color,
    val blue: Color,
    val yellow: Color,
    val water: Color,
    val accentFill: Color = accent,
    val pinkFill: Color = pink,
    val blueFill: Color = blue,
    val fonts: AttendoFonts = AttendoFonts.Default,
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
            hero = Color(0xFF1C1C1E),
            onHero = Color(0xFFF2F2F2),
            pink = Color(0xFFFF5FAE),
            blue = Color(0xFF4DA3FF),
            yellow = Color(0xFFF2D94B),
            water = Color(0xFF7FC8EE),
        )

        /** Paper, black ink and magenta in a typewriter face (after the "reading ledger" reference). */
        val Ledger: AttendoPalette by lazy {
            AttendoPalette(
                background = Color(0xFFF4F1E8),
                card = Color(0xFFECE8DC),
                cardDeep = Color(0xFFDAD5C6),
                text = Color(0xFF141414),
                muted = Color(0xFF7B776C),
                accent = Color(0xFFD6247F),
                hero = Color(0xFF1B1B1B),
                onHero = Color(0xFFF4F1E8),
                pink = Color(0xFFC2511F), // "absent": rust, so it stays apart from the magenta accent
                blue = Color(0xFF3E5C76),
                yellow = Color(0xFFB98E12),
                water = Color(0xFFBFD3D9),
                fonts = AttendoFonts.Typewriter,
            )
        }

        /** Near-black with lime and orange (after the dark fitness-app reference). */
        val Neon = AttendoPalette(
            background = Color(0xFF0A0A0A),
            card = Color(0xFF151515),
            cardDeep = Color(0xFF262626),
            text = Color(0xFFF4F4F4),
            muted = Color(0xFF8B8B8B),
            accent = Color(0xFFC6F432),
            hero = Color(0xFF1A1A1A),
            onHero = Color(0xFFF4F4F4),
            pink = Color(0xFFEA5A2F),
            blue = Color(0xFF58B4F0),
            yellow = Color(0xFFF2C14E),
            water = Color(0xFFEA5A2F),
            fonts = AttendoFonts.Sans,
        )

        /** White with sky blue, mint and butter yellow (after the pastel reference). */
        val Pastel = AttendoPalette(
            background = Color(0xFFFFFFFF),
            card = Color(0xFFF5F6F6),
            cardDeep = Color(0xFFE2F0D6),
            text = Color(0xFF101214),
            muted = Color(0xFF7C7F84),
            accent = Color(0xFF2E8B57),
            hero = Color(0xFFBFE6EE),
            onHero = Color(0xFF101214),
            pink = Color(0xFFD9534A),
            blue = Color(0xFF2F87A8),
            yellow = Color(0xFFB8960C),
            water = Color(0xFFBFE6EE),
            accentFill = Color(0xFFFBEF8A),
            pinkFill = Color(0xFFF9C2BB),
            blueFill = Color(0xFFBFE6EE),
            fonts = AttendoFonts.Sans,
        )

        /** Colours of VS Code's "Dark 2026" theme (1.145), in the system code font. */
        val VsCode = AttendoPalette(
            background = Color(0xFF121314), // editor.background
            card = Color(0xFF191A1B), // sideBar / panel background
            cardDeep = Color(0xFF2A2B2C), // borders
            text = Color(0xFFBFBFBF), // foreground
            muted = Color(0xFF8C8C8C), // descriptionForeground
            accent = Color(0xFF48A0C7), // textLink.foreground
            hero = Color(0xFF202122), // editorWidget.background
            onHero = Color(0xFFBFBFBF),
            pink = Color(0xFFF48771), // errorForeground
            blue = Color(0xFFD2A8FF), // function token
            yellow = Color(0xFFE5BA7D), // modified
            water = Color(0xFFA5D6FF), // string token
            fonts = AttendoFonts.Mono,
        )

        /** Built-in themes in menu order, with the name and one-line description shown in Settings. */
        val PRESETS: List<Triple<ThemeMode, String, String>> = listOf(
            Triple(ThemeMode.LIGHT, "Light", "The original cream, ink and green."),
            Triple(ThemeMode.DARK, "Dark", "Pitch-black base with dark-grey cards."),
            Triple(ThemeMode.LEDGER, "Ledger", "Paper, black ink and magenta in a typewriter font."),
            Triple(ThemeMode.NEON, "Neon", "Near-black with lime and orange accents."),
            Triple(ThemeMode.PASTEL, "Pastel", "White with sky blue, mint and butter yellow."),
            Triple(ThemeMode.VSCODE, "VS Code", "VS Code's Dark 2026 colours in a code font."),
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
            val pink = pick("pink", base.pink)
            val blue = pick("blue", base.blue)
            return base.copy(
                background = pick("background", base.background),
                card = pick("card", base.card),
                cardDeep = pick("card_deep", base.cardDeep),
                text = pick("text", base.text),
                muted = pick("muted", base.muted),
                accent = accent,
                hero = pick("hero", base.hero),
                onHero = pick("on_hero", base.onHero),
                pink = pink,
                blue = blue,
                yellow = pick("yellow", base.yellow),
                accentFill = accent,
                pinkFill = pink,
                blueFill = blue,
            )
        }

        fun forMode(mode: ThemeMode, customHex: Map<String, String>) = when (mode) {
            ThemeMode.LIGHT -> Light
            ThemeMode.DARK -> Dark
            ThemeMode.LEDGER -> Ledger
            ThemeMode.NEON -> Neon
            ThemeMode.PASTEL -> Pastel
            ThemeMode.VSCODE -> VsCode
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

/** WCAG contrast ratio between two opaque colours (1 = none, 21 = black on white). */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/**
 * App colours. Backed by snapshot state, so switching theme recolours every screen (composition
 * and Canvas drawing alike) without each call site having to change.
 */
object AttendoColors {
    var palette: AttendoPalette by mutableStateOf(AttendoPalette.Light)

    val Ink: Color get() = palette.text
    val Green: Color get() = palette.accent
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

    /** Fill for primary buttons, the selected tab and "present" chips (may be paler than [Green]). */
    val GreenFill: Color get() = palette.accentFill
    val PinkFill: Color get() = palette.pinkFill
    val BlueFill: Color get() = palette.blueFill

    /** Text/icon colour on top of [GreenFill]. */
    val OnAccent: Color get() = on(palette.accentFill)

    /**
     * Text/icon colour for content drawn on [fill]: the background colour when that is readable
     * (the look every theme had so far), otherwise the text colour (pale fills, as in Pastel).
     */
    fun on(fill: Color): Color {
        val onBackground = contrastRatio(palette.background, fill)
        return if (onBackground >= 2f || onBackground >= contrastRatio(palette.text, fill)) palette.background else palette.text
    }
}

/**
 * Type scale after Apple's UI typography rules (WWDC20 "The Details of UI Typography"): tracking is
 * size-specific (large text tightened, body at 0, small text opened up a little) and leading runs
 * the other way (tight on big titles, roomier on body copy). Sizes are in sp, so the phone's
 * font-size setting scales everything, spacing included.
 */
private fun typographyFor(fonts: AttendoFonts): Typography {
    val f = fonts.body
    val s = fonts.scale
    fun style(size: Float, leading: Float, trackingEm: Float, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = f,
        fontWeight = weight,
        fontSize = (size * s).sp,
        lineHeight = (leading * s).sp,
        letterSpacing = trackingEm.em,
    )
    return Typography(
        displayLarge = style(54f, 56f, -0.028f, FontWeight.Bold),
        headlineLarge = style(30f, 34f, -0.018f),
        headlineSmall = style(22f, 27f, -0.012f, FontWeight.SemiBold),
        titleLarge = style(20f, 25f, -0.008f, FontWeight.SemiBold),
        titleMedium = style(17f, 22f, -0.004f, FontWeight.Medium),
        titleSmall = style(14f, 19f, 0f, FontWeight.Medium),
        bodyLarge = style(16f, 23f, 0f),
        bodyMedium = style(14f, 20f, 0f),
        bodySmall = style(12f, 17f, 0.004f),
        labelLarge = style(15f, 20f, 0f, FontWeight.Medium),
        labelMedium = style(12f, 16f, 0.008f, FontWeight.Medium),
        labelSmall = style(11f, 15f, 0.012f),
    )
}

/** Bubble text style (re-read each time so it follows the theme). */
val HandTextStyle: TextStyle
    get() {
        val fonts = AttendoColors.palette.fonts
        return TextStyle(
            fontFamily = fonts.hand,
            fontStyle = if (fonts.handItalic) FontStyle.Italic else FontStyle.Normal,
            fontWeight = FontWeight.Medium,
            fontSize = (if (fonts.handItalic) 16f else 13.5f * fonts.scale).sp,
            lineHeight = 19.sp,
            color = AttendoColors.Ink,
        )
    }

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
    val typography = remember(p.fonts) { typographyFor(p.fonts) }
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = AttendoShapes) {
        // Material 3 only sets the content colour inside a Surface; without this, every Text and
        // Icon that doesn't pass a colour falls back to black, which vanished on the dark theme.
        CompositionLocalProvider(LocalContentColor provides p.text, content = content)
    }
}
