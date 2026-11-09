package com.attendo.core.designsystem.motion

import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import com.attendo.core.model.AnimationMode
import kotlin.math.abs

/**
 * iOS "rubber band" overscroll: when a scrollable child reaches its edge, further drags move the
 * content with increasing resistance, and it springs back on release. Flings that hit an edge
 * bounce. Works for LazyColumn and verticalScroll children alike (via nested scroll), and even when
 * the content is too short to scroll — just like UIScrollView's alwaysBounceVertical.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BounceContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // Rubber band only in the iOS and Smooth modes (Smooth settles more slowly via Motion.settle()).
    val mode = LocalAnimationMode.current
    if (mode == AnimationMode.ANDROID || mode == AnimationMode.NONE) {
        Box(modifier) { content() }
        return
    }
    val offset = remember { mutableFloatStateOf(0f) }
    val connection = remember { RubberBandConnection(offset) }
    Box(modifier.clipToBounds().nestedScroll(connection)) {
        // Disable Android's stretch effect; the rubber band replaces it.
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            Box(Modifier.graphicsLayer { translationY = offset.floatValue }) { content() }
        }
    }
}

private class RubberBandConnection(private val offset: MutableFloatState) : NestedScrollConnection {

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val current = offset.floatValue
        if (current == 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        // Dragging back towards rest first undoes the stretch before the list scrolls again.
        val movingBack = (current > 0f && available.y < 0f) || (current < 0f && available.y > 0f)
        if (!movingBack) return Offset.Zero
        val next = current + available.y
        val clamped = if (current > 0f) next.coerceAtLeast(0f) else next.coerceAtMost(0f)
        offset.floatValue = clamped
        return Offset(0f, clamped - current)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
        val current = offset.floatValue
        // Resistance grows with distance, like UIScrollView.
        val resistance = 0.55f / (1f + abs(current) / RESISTANCE_DISTANCE)
        offset.floatValue = current + available.y * resistance
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (offset.floatValue == 0f) return Velocity.Zero
        settle(available.y)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (abs(available.y) < 1f && offset.floatValue == 0f) return Velocity.Zero
        // The list hit an edge while flinging: carry the leftover velocity into a bounce.
        settle(available.y * FLING_TRANSFER)
        return available
    }

    private suspend fun settle(velocity: Float) {
        animate(
            initialValue = offset.floatValue,
            targetValue = 0f,
            initialVelocity = velocity,
            animationSpec = Motion.settle(),
        ) { value, _ -> offset.floatValue = value }
    }

    private companion object {
        const val RESISTANCE_DISTANCE = 320f
        const val FLING_TRANSFER = 0.35f
    }
}
