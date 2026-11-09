package com.attendo.core.designsystem.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import com.attendo.core.designsystem.theme.AttendoColors

enum class PressStyle {
    /** Table-row grey flash that fades out after release (UITableViewCell). */
    Highlight,

    /** Content dims while held (UIButton). */
    Dim,

    /** Shrinks slightly with a spring (iOS cards / app icons). */
    Scale,
}

/**
 * Click handling with iOS press feedback instead of the Material ripple. Falls back to the normal
 * ripple `clickable` when iOS motion is turned off.
 */
fun Modifier.iosPress(
    style: PressStyle = PressStyle.Highlight,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    if (!LocalIosMotion.current) {
        return@composed clickable(enabled = enabled, onClickLabel = onClickLabel, onClick = onClick)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val base = clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )
    when (style) {
        PressStyle.Highlight -> {
            val amount by animateFloatAsState(
                targetValue = if (pressed) 1f else 0f,
                animationSpec = if (pressed) snap() else tween(320),
                label = "highlight",
            )
            drawWithContent {
                drawContent()
                drawRect(AttendoColors.Ink.copy(alpha = 0.08f * amount))
            }.then(base)
        }
        PressStyle.Dim -> {
            val alpha by animateFloatAsState(
                targetValue = if (pressed) 0.45f else 1f,
                animationSpec = if (pressed) snap() else tween(220),
                label = "dim",
            )
            graphicsLayer { this.alpha = alpha }.then(base)
        }
        PressStyle.Scale -> {
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.95f else 1f,
                animationSpec = IosMotion.bouncy(),
                label = "scale",
            )
            graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.then(base)
        }
    }
}
