package com.attendo.core.designsystem.theme

import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.attendo.core.designsystem.R
import com.attendo.core.model.FontChoice

/**
 * The fonts behind Settings › Font. Bundled fonts are separate files per weight (not variable
 * fonts, whose weight setting some phones ignore), cut down to Latin text; letters outside it fall
 * back to the phone's font. Licences: `core/designsystem/fonts/` (all SIL Open Font License).
 */
internal object AppFonts {
    private fun family(regular: Int, medium: Int, semiBold: Int, bold: Int) = FontFamily(
        Font(regular, FontWeight.Normal),
        Font(medium, FontWeight.Medium),
        Font(semiBold, FontWeight.SemiBold),
        Font(bold, FontWeight.Bold),
    )

    val Roboto by lazy { family(R.font.font_roboto_regular, R.font.font_roboto_medium, R.font.font_roboto_semibold, R.font.font_roboto_bold) }
    val OpenSans by lazy { family(R.font.font_open_sans_regular, R.font.font_open_sans_medium, R.font.font_open_sans_semibold, R.font.font_open_sans_bold) }
    val Montserrat by lazy { family(R.font.font_montserrat_regular, R.font.font_montserrat_medium, R.font.font_montserrat_semibold, R.font.font_montserrat_bold) }
    val Poppins by lazy { family(R.font.font_poppins_regular, R.font.font_poppins_medium, R.font.font_poppins_semibold, R.font.font_poppins_bold) }
    val Jakarta by lazy { family(R.font.font_jakarta_regular, R.font.font_jakarta_medium, R.font.font_jakarta_semibold, R.font.font_jakarta_bold) }

    /** Lato's licence doesn't allow trimming it, so it ships as its two full weights. */
    val Lato by lazy {
        FontFamily(Font(R.font.font_lato_regular, FontWeight.Normal), Font(R.font.font_lato_bold, FontWeight.Bold))
    }

    val Typewriter: FontFamily by lazy { FontFamily(Typeface.create("serif-monospace", Typeface.NORMAL)) }

    /** The body font and its size multiplier for [choice]; null keeps the theme's own font. */
    fun bodyFor(choice: FontChoice): Pair<FontFamily, Float>? = when (choice) {
        FontChoice.THEME -> null
        FontChoice.SYSTEM -> FontFamily.SansSerif to 1f
        FontChoice.ROBOTO -> Roboto to 1f
        FontChoice.OPEN_SANS -> OpenSans to 1f
        FontChoice.MONTSERRAT -> Montserrat to 0.96f
        FontChoice.LATO -> Lato to 1f
        FontChoice.POPPINS -> Poppins to 0.96f
        FontChoice.PLUS_JAKARTA_SANS -> Jakarta to 1f
        FontChoice.MONOSPACE -> FontFamily.Monospace to 0.92f
        FontChoice.TYPEWRITER -> Typewriter to 0.94f
        FontChoice.HANDWRITTEN -> FontFamily.Cursive to 1.05f
    }
}

/**
 * This palette with Settings › Font applied: the chosen font replaces the theme's body font
 * everywhere; speech bubbles keep the theme's handwriting.
 */
fun AttendoPalette.withFont(choice: FontChoice): AttendoPalette {
    val (body, scale) = AppFonts.bodyFor(choice) ?: return this
    return copy(fonts = fonts.copy(body = body, scale = scale))
}

/** How [choice] looks, for showing each option in its own font. */
fun fontPreview(choice: FontChoice, theme: AttendoPalette): FontFamily = AppFonts.bodyFor(choice)?.first ?: theme.fonts.body
