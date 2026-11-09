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

/** Settings › Theme. */
enum class ThemeMode { LIGHT, DARK, CUSTOM }

data class AppSettings(
    val userName: String = "",
    val onboarded: Boolean = false,
    val animationMode: AnimationMode = AnimationMode.IOS,
    /** Home shows swipeable subject cards instead of the list. */
    val tinderMode: Boolean = false,
    val theme: ThemeMode = ThemeMode.LIGHT,
    /** Custom theme colours, palette key -> "#RRGGBB" (stored as `color.<key>` in settings.csv). */
    val customColors: Map<String, String> = emptyMap(),
    /** Whether the user picked a launch-screen image (stored in app files). */
    val launchImage: Boolean = false,
    /** Launch-screen background "#RRGGBB", sampled from the picked image's edges. */
    val launchBackground: String? = null,
)
