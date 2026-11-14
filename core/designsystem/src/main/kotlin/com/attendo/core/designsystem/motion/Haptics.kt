package com.attendo.core.designsystem.motion

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * The app's three kinds of haptic feedback (WWDC "Designing Audio-Haptic Experiences"): fire them
 * on the frame the thing happens (harmony), only for moments that matter (utility), and so it's
 * obvious what caused them (causality). The phone's own haptics setting still applies.
 */
@Stable
class Haptics internal constructor(private val view: View) {

    /** A light tick: a swipe crossed its "let go to commit" point, a drop slot changed, a unit ticked. */
    fun tick() = perform(
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK else HapticFeedbackConstants.CLOCK_TICK,
    )

    /** Something was committed: attendance recorded, a card dropped, a focus session finished. */
    fun confirm() = perform(
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY,
    )

    /** Picked up to drag. */
    fun grab() = perform(HapticFeedbackConstants.LONG_PRESS)

    private fun perform(type: Int) {
        view.performHapticFeedback(type)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
