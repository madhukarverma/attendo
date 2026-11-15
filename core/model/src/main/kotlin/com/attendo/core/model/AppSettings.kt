package com.attendo.core.model

/** The four positions of the Settings › Animations slider. */
enum class AnimationMode {
    /** Long, fully damped, fluid motion; no overshoot or abrupt cuts; highest refresh rate. */
    SMOOTH,

    /** Apple-style springs (duration + bounce), push/pop parallax, rubber-band scrolling; high refresh rate. */
    IOS,

    /** Stock Android/Material behaviour: default crossfades, ripples, stretch overscroll. */
    ANDROID,

    /** No animations, transitions or launch screen at all. */
    NONE,
}

/** Settings › Theme. New values go at the end; the name is what's stored in settings.csv. */
enum class ThemeMode { LIGHT, DARK, CUSTOM, LEDGER, NEON, PASTEL, VSCODE }

/**
 * Settings › Font: the font used for all text. [THEME] keeps each theme's own font. The five most
 * used Google Fonts, Plus Jakarta Sans, and every font a theme uses are offered. New values go at
 * the end; the name is what's stored in settings.csv.
 */
enum class FontChoice(val label: String, val note: String) {
    THEME("Theme font", "Each theme's own font (the phone's font for most)"),
    SYSTEM("System", "Your phone's own font, in every theme"),
    ROBOTO("Roboto", "Google's classic Android font"),
    OPEN_SANS("Open Sans", "Open and very easy to read"),
    MONTSERRAT("Montserrat", "Geometric and wide"),
    LATO("Lato", "Warm and rounded"),
    POPPINS("Poppins", "Round and geometric"),
    PLUS_JAKARTA_SANS("Plus Jakarta Sans", "Modern and crisp"),
    MONOSPACE("Monospace", "The code font from the VS Code theme"),
    TYPEWRITER("Typewriter", "The slab font from the Ledger theme"),
    HANDWRITTEN("Handwritten", "The speech-bubble font"),
}

data class AppSettings(
    val userName: String = "",
    val onboarded: Boolean = false,
    val animationMode: AnimationMode = AnimationMode.IOS,
    /** Home shows swipeable subject cards instead of the list. */
    val tinderMode: Boolean = false,
    val theme: ThemeMode = ThemeMode.LIGHT,
    /** Settings › Font, applied on top of the theme. */
    val font: FontChoice = FontChoice.THEME,
    /** Custom theme colours, palette key -> "#RRGGBB" (stored as `color.<key>` in settings.csv). */
    val customColors: Map<String, String> = emptyMap(),
    /** Whether the user picked a launch-screen image (stored in app files). */
    val launchImage: Boolean = false,
    /** Launch-screen background "#RRGGBB", sampled from the picked image's edges. */
    val launchBackground: String? = null,
) {
    /** What the app calls the user: the name, or its first 4 letters when longer than 10 characters. */
    val nickname: String get() = Nickname.of(userName)
}
