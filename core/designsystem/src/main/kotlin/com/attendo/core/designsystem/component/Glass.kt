package com.attendo.core.designsystem.component

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Frosted glass, after Apple's materials: a floating surface blurs whatever scrolls underneath it,
 * so it reads as a layer above the content rather than a solid strip that hides it.
 *
 * The content marked with [backdropSource] is recorded once per frame into a graphics layer;
 * each [glass] surface draws that same layer again, offset to its own position and blurred.
 * Blur needs Android 12 (API 31); older phones get a near-solid tint instead.
 */
@Stable
class Backdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberBackdrop(): Backdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { Backdrop(layer) }
}

/** The content that glass surfaces show through. A glass surface must not be inside it. */
fun Modifier.backdropSource(backdrop: Backdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInRoot() }
    .drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/**
 * Fills the surface with the blurred [backdrop] under a [tint]. Clip to the surface's shape first.
 * Keep the tint translucent (alpha around 0.6–0.8): heavier for big or structural surfaces.
 */
fun Modifier.glass(backdrop: Backdrop, tint: Color, blur: Dp = 24.dp): Modifier = composed {
    val blurred = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .drawBehind {
            if (Build.VERSION.SDK_INT >= 31) {
                val radius = blur.toPx()
                blurred.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                blurred.record {
                    translate(backdrop.origin.x - origin.x, backdrop.origin.y - origin.y) {
                        drawLayer(backdrop.layer)
                    }
                }
                drawLayer(blurred)
                drawRect(tint)
            } else {
                drawRect(tint.copy(alpha = maxOf(tint.alpha, 0.94f)))
            }
        }
}
