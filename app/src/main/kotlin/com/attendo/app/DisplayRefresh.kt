package com.attendo.app

import android.app.Activity
import android.os.Build
import android.view.Display

/**
 * Asks the system to run this window at the panel's highest refresh rate (e.g. 90/120/144 Hz) at the
 * current resolution, or hands the choice back to the system when [enabled] is false. Devices
 * without multiple modes simply keep their only one.
 */
fun Activity.preferHighRefreshRate(enabled: Boolean) {
    @Suppress("DEPRECATION")
    val display: Display = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay)
        ?: return
    val current = display.mode
    val best = display.supportedModes
        .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
        .maxByOrNull { it.refreshRate }
        ?: return
    val wanted = if (enabled) best.modeId else 0
    val params = window.attributes
    if (params.preferredDisplayModeId != wanted) {
        params.preferredDisplayModeId = wanted
        window.attributes = params
    }
}
