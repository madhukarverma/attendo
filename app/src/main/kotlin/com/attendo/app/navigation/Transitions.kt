package com.attendo.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.model.AnimationMode

/**
 * Screen transitions per Animations mode:
 * - IOS: UIKit push (new screen from the right, old one drifts 30% left and dims over a black
 *   backdrop), sheets slide up while the screen behind shrinks back; swipe-back via predictive back.
 * - SMOOTH: long cross-fades with a short, eased slide (1/8 of the width); sheets rise gently.
 *   Nothing moves far or fast, so there are no abrupt cuts.
 * - ANDROID: Navigation Compose's stock 700 ms cross-fade.
 * - NONE: instant swaps.
 *
 * Switching between bottom-bar tabs is not a push: iOS swaps instantly (like UITabBarController),
 * Smooth cross-fades, Android uses Material's fade-through, None is instant.
 */
internal class NavTransitions(
    private val mode: AnimationMode,
    private val modalRoutes: Set<String>,
    private val tabRoutes: Set<String> = emptySet(),
) {

    private val NavBackStackEntry.isModal get() = destination.route in modalRoutes

    private val AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch: Boolean
        get() = initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes

    private val androidFade = tween<Float>(700)

    private fun tabEnter(): EnterTransition = when (mode) {
        AnimationMode.NONE, AnimationMode.IOS -> EnterTransition.None
        AnimationMode.SMOOTH -> fadeIn(tween(320, easing = Motion.Easing))
        AnimationMode.ANDROID ->
            fadeIn(tween(210, delayMillis = 90)) + scaleIn(tween(210, delayMillis = 90), initialScale = 0.92f)
    }

    private fun tabExit(): ExitTransition = when (mode) {
        AnimationMode.NONE, AnimationMode.IOS -> ExitTransition.None
        AnimationMode.SMOOTH -> fadeOut(tween(320, easing = Motion.Easing))
        AnimationMode.ANDROID -> fadeOut(tween(90))
    }

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isTabSwitch) tabEnter() else when (mode) {
            AnimationMode.NONE -> EnterTransition.None
            AnimationMode.ANDROID -> fadeIn(androidFade)
            AnimationMode.SMOOTH ->
                if (targetState.isModal) fadeIn(Motion.sheet()) + slideInVertically(Motion.sheet()) { it / 6 }
                else fadeIn(Motion.push()) + slideInHorizontally(Motion.push()) { it / 8 }
            AnimationMode.IOS -> when {
                initialState.destination.route == Routes.ONBOARDING ->
                    fadeIn(Motion.push()) + scaleIn(Motion.push(), initialScale = 0.92f)
                targetState.isModal -> slideInVertically(Motion.sheet()) { it }
                else -> slideInHorizontally(Motion.push()) { it }
            }
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isTabSwitch) tabExit() else when (mode) {
            AnimationMode.NONE -> ExitTransition.None
            AnimationMode.ANDROID -> fadeOut(androidFade)
            AnimationMode.SMOOTH -> fadeOut(Motion.push())
            AnimationMode.IOS -> when {
                initialState.destination.route == Routes.ONBOARDING -> fadeOut(Motion.push())
                targetState.isModal ->
                    scaleOut(Motion.sheet(), targetScale = Motion.SHEET_BACKGROUND_SCALE) +
                        fadeOut(Motion.sheet(), targetAlpha = Motion.COVERED_ALPHA)
                else ->
                    slideOutHorizontally(Motion.push()) { -(it * Motion.PARALLAX_FRACTION).toInt() } +
                        fadeOut(Motion.push(), targetAlpha = Motion.COVERED_ALPHA)
            }
        }
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        if (isTabSwitch) tabEnter() else when (mode) {
            AnimationMode.NONE -> EnterTransition.None
            AnimationMode.ANDROID -> fadeIn(androidFade)
            AnimationMode.SMOOTH -> fadeIn(Motion.push()) + slideInHorizontally(Motion.push()) { -it / 8 }
            AnimationMode.IOS -> when {
                initialState.isModal ->
                    scaleIn(Motion.sheet(), initialScale = Motion.SHEET_BACKGROUND_SCALE) +
                        fadeIn(Motion.sheet(), initialAlpha = Motion.COVERED_ALPHA)
                else ->
                    slideInHorizontally(Motion.push()) { -(it * Motion.PARALLAX_FRACTION).toInt() } +
                        fadeIn(Motion.push(), initialAlpha = Motion.COVERED_ALPHA)
            }
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        if (isTabSwitch) tabExit() else when (mode) {
            AnimationMode.NONE -> ExitTransition.None
            AnimationMode.ANDROID -> fadeOut(androidFade)
            AnimationMode.SMOOTH ->
                if (initialState.isModal) fadeOut(Motion.sheet()) + slideOutVertically(Motion.sheet()) { it / 6 }
                else fadeOut(Motion.push()) + slideOutHorizontally(Motion.push()) { it / 8 }
            AnimationMode.IOS -> when {
                initialState.isModal -> slideOutVertically(Motion.sheet()) { it }
                else -> slideOutHorizontally(Motion.push()) { it }
            }
        }
    }
}
