package com.attendo.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion tokens approximating UIKit's navigation and list behaviour.
 *
 * All Compose animations also honour the system "Animator duration scale"/"Remove animations"
 * accessibility setting automatically.
 */
object IosMotion {
    /** Close match to the UINavigationController push curve (fast start, long soft settle). */
    val Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    const val PUSH_MILLIS = 420
    const val SHEET_MILLIS = 460

    /** How far the previous screen drifts left while a new one is pushed (UIKit uses ~30%). */
    const val PARALLAX_FRACTION = 0.3f

    /** Alpha the covered screen fades to over the black backdrop, producing the iOS dimming. */
    const val COVERED_ALPHA = 0.75f

    /** Scale of the screen behind a sheet ("card stack" effect). */
    const val SHEET_BACKGROUND_SCALE = 0.93f

    fun <T> push(): FiniteAnimationSpec<T> = tween(PUSH_MILLIS, easing = Easing)
    fun <T> sheet(): FiniteAnimationSpec<T> = tween(SHEET_MILLIS, easing = Easing)

    /** Critically damped spring used for settling (rubber band release, progress bars). */
    fun <T> settle(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)

    /** Slightly bouncy spring for selection/toggle changes. */
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.72f, stiffness = 500f)
}

/** Whether iOS-style motion is enabled (user setting). `false` falls back to Material defaults. */
val LocalIosMotion = staticCompositionLocalOf { true }
