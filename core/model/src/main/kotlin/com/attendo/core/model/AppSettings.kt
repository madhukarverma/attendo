package com.attendo.core.model

data class AppSettings(
    val userName: String = "",
    val onboarded: Boolean = false,
    /** iOS-style transitions, bounce scrolling and press feedback. */
    val iosMotion: Boolean = true,
    /** Home shows swipeable subject cards instead of the list. */
    val tinderMode: Boolean = false,
)
