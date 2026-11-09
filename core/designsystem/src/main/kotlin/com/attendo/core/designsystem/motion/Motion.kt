package com.attendo.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf
import com.attendo.core.model.AnimationMode
import kotlin.math.PI

/**
 * App motion tokens. Every curve depends on the user's [AnimationMode] (Settings › Animations):
 *
 * - SMOOTH: long, critically damped springs and gentle ease-in-out tweens; nothing overshoots.
 * - IOS: Apple's perceptual springs (duration + bounce, WWDC23 "Animate with springs"):
 *   smooth = (0.5 s, 0), snappy = (≈0.35 s, 0.15) for UI feedback, push/pop as a 0.5 s smooth spring.
 * - ANDROID: Material defaults (FastOutSlowIn tweens, default springs).
 * - NONE: snaps; [AppMotionScale] additionally zeroes every animation's duration.
 *
 * [mode] is set by the app root whenever the setting changes; the functions read it when a spec is
 * created, so they can be used outside composition (e.g. navigation transition lambdas).
 */
object Motion {

    @Volatile
    var mode: AnimationMode = AnimationMode.IOS

    /**
     * Apple's spring model expressed in Compose terms (mass = 1):
     * stiffness = (2π / duration)², dampingRatio = 1 − bounce.
     */
    fun <T> appleSpring(duration: Float, bounce: Float = 0f): FiniteAnimationSpec<T> {
        val omega = 2 * PI.toFloat() / duration
        return spring(dampingRatio = 1f - bounce, stiffness = omega * omega)
    }

    private val IosEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    private val SmoothEasing = CubicBezierEasing(0.45f, 0f, 0.15f, 1f)

    /** Easing for the few explicit tweens (card fly-out, launch fade). */
    val Easing: Easing
        get() = when (mode) {
            AnimationMode.SMOOTH -> SmoothEasing
            AnimationMode.IOS -> IosEasing
            AnimationMode.ANDROID -> FastOutSlowInEasing
            AnimationMode.NONE -> LinearEasing
        }

    /** How far the previous screen drifts left while a new one is pushed (UIKit uses ~30%). */
    const val PARALLAX_FRACTION = 0.3f

    /** Alpha the covered screen fades to over the black backdrop, producing the iOS dimming. */
    const val COVERED_ALPHA = 0.75f

    /** Scale of the screen behind a sheet ("card stack" effect). */
    const val SHEET_BACKGROUND_SCALE = 0.93f

    /** Screen push / pop. */
    fun <T> push(): FiniteAnimationSpec<T> = when (mode) {
        AnimationMode.SMOOTH -> tween(560, easing = SmoothEasing)
        AnimationMode.IOS -> appleSpring(0.5f)
        AnimationMode.ANDROID -> tween(300, easing = FastOutSlowInEasing)
        AnimationMode.NONE -> snap()
    }

    /** Sheet presentation. */
    fun <T> sheet(): FiniteAnimationSpec<T> = when (mode) {
        AnimationMode.SMOOTH -> tween(620, easing = SmoothEasing)
        AnimationMode.IOS -> appleSpring(0.55f)
        AnimationMode.ANDROID -> tween(300, easing = FastOutSlowInEasing)
        AnimationMode.NONE -> snap()
    }

    /** Settling into place: rubber-band release, progress bars, counters. Never overshoots. */
    fun <T> settle(): FiniteAnimationSpec<T> = when (mode) {
        AnimationMode.SMOOTH -> appleSpring(0.8f)
        AnimationMode.IOS -> appleSpring(0.5f)
        AnimationMode.ANDROID -> spring(stiffness = Spring.StiffnessMediumLow)
        AnimationMode.NONE -> snap()
    }

    /** Selection / toggle / press feedback. A little bounce in iOS mode only. */
    fun <T> bouncy(): FiniteAnimationSpec<T> = when (mode) {
        AnimationMode.SMOOTH -> appleSpring(0.6f)
        AnimationMode.IOS -> appleSpring(0.35f, bounce = 0.15f)
        AnimationMode.ANDROID -> spring(stiffness = Spring.StiffnessMedium)
        AnimationMode.NONE -> snap()
    }

    /** Multiplier for idle loops such as the mascot's bounce (smooth mode slows them down). */
    val idleSlowdown: Float get() = if (mode == AnimationMode.SMOOTH) 1.6f else 1f
}

/** Current animation mode for composables that change structure (not just timing) per mode. */
val LocalAnimationMode = staticCompositionLocalOf { AnimationMode.IOS }
