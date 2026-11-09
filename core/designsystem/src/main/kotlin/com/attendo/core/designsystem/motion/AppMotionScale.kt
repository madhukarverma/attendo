package com.attendo.core.designsystem.motion

import androidx.compose.ui.MotionDurationScale

/**
 * App-wide animation speed, installed in the root Recomposer's coroutine context. Every Compose
 * animation (animate*AsState, Animatable, AnimatedContent, list item animations, Material
 * components, navigation transitions) reads it, so setting [enabled] = false makes all of them
 * finish instantly. Scroll flings are unaffected: Compose runs them with its own scale.
 */
object AppMotionScale : MotionDurationScale {

    @Volatile
    var enabled: Boolean = true

    /** The system "Animator duration scale" developer/accessibility setting (0 = remove animations). */
    @Volatile
    var systemScale: Float = 1f

    override val scaleFactor: Float
        get() = if (enabled) systemScale else 0f
}
