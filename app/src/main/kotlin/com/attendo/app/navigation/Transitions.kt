package com.attendo.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.attendo.core.designsystem.motion.IosMotion

/**
 * UIKit-style navigation:
 * - push: new screen slides in from the right; the old one drifts 30% left and dims
 * - pop / swipe back: the reverse (driven by the predictive-back gesture on Android 14+)
 * - modal (sheet) routes slide up from the bottom while the screen behind shrinks back
 *
 * The NavHost sits on a black backdrop, so fading the covered screen produces the iOS dimming.
 */
internal class NavTransitions(private val ios: Boolean, private val modalRoutes: Set<String>) {

    private val NavBackStackEntry.isModal get() = destination.route in modalRoutes

    // Animations off: screens swap instantly.
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when {
            !ios -> EnterTransition.None
            initialState.destination.route == Routes.ONBOARDING ->
                fadeIn(IosMotion.push()) + scaleIn(IosMotion.push(), initialScale = 0.92f)
            targetState.isModal -> slideInVertically(IosMotion.sheet()) { it }
            else -> slideInHorizontally(IosMotion.push()) { it }
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when {
            !ios -> ExitTransition.None
            initialState.destination.route == Routes.ONBOARDING -> fadeOut(IosMotion.push())
            targetState.isModal ->
                scaleOut(IosMotion.sheet(), targetScale = IosMotion.SHEET_BACKGROUND_SCALE) +
                    fadeOut(IosMotion.sheet(), targetAlpha = IosMotion.COVERED_ALPHA)
            else ->
                slideOutHorizontally(IosMotion.push()) { -(it * IosMotion.PARALLAX_FRACTION).toInt() } +
                    fadeOut(IosMotion.push(), targetAlpha = IosMotion.COVERED_ALPHA)
        }
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when {
            !ios -> EnterTransition.None
            initialState.isModal ->
                scaleIn(IosMotion.sheet(), initialScale = IosMotion.SHEET_BACKGROUND_SCALE) +
                    fadeIn(IosMotion.sheet(), initialAlpha = IosMotion.COVERED_ALPHA)
            else ->
                slideInHorizontally(IosMotion.push()) { -(it * IosMotion.PARALLAX_FRACTION).toInt() } +
                    fadeIn(IosMotion.push(), initialAlpha = IosMotion.COVERED_ALPHA)
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when {
            !ios -> ExitTransition.None
            initialState.isModal -> slideOutVertically(IosMotion.sheet()) { it }
            else -> slideOutHorizontally(IosMotion.push()) { it }
        }
    }
}
