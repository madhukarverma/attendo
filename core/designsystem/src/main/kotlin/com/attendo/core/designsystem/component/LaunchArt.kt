package com.attendo.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

/** Default launch-screen background (used until the user picks a launch image). */
val LaunchBackground = Color(0xFF1A1D1A)

/** Batch label shown at the top of the launch screen. */
const val BATCH_LABEL = "MCA 2026-29"

/** Greyed-out "developed by" line with the author's GitHub. */
@Composable
fun DeveloperCredit(modifier: Modifier = Modifier, color: Color = Color(0xFF8C8E82)) {
    Text(
        "Developed by @madhukarverma · github.com/madhukarverma",
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
