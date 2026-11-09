package com.attendo.core.designsystem.motion

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.theme.AttendoColors

private val LargeTitleHeight = 52.dp

/** 0 = large title fully visible, 1 = scrolled under the bar (small title shown). */
@Composable
fun rememberCollapseFraction(state: LazyListState): State<Float> {
    val heightPx = with(LocalDensity.current) { LargeTitleHeight.toPx() }
    return remember(state) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / heightPx).coerceIn(0f, 1f)
        }
    }
}

@Composable
fun rememberCollapseFraction(state: ScrollState): State<Float> {
    val heightPx = with(LocalDensity.current) { LargeTitleHeight.toPx() }
    return remember(state) { derivedStateOf { (state.value / heightPx).coerceIn(0f, 1f) } }
}

/**
 * Navigation bar in the style of iOS Settings: "‹ Back" on the left, a small centred title that
 * fades in once the [LargeTitle] scrolls underneath, and a hairline that appears with it.
 * [onBack] null hides the back button (top-level tabs).
 */
@Composable
fun CollapsingTopBar(
    title: String,
    collapseFraction: () -> Float,
    onBack: (() -> Unit)?,
    backLabel: String = "Back",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(AttendoColors.Cream),
    ) {
        if (onBack != null) Row(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .iosPress(PressStyle.Dim, onClickLabel = backLabel, onClick = onBack)
                .padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = AttendoColors.Green, modifier = Modifier.size(30.dp))
            Text(backLabel, color = AttendoColors.Green, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 96.dp)
                .graphicsLayer {
                    val f = collapseFraction()
                    alpha = ((f - 0.5f) * 2f).coerceIn(0f, 1f)
                },
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(0.5.dp)
                .graphicsLayer { alpha = collapseFraction() }
                .background(AttendoColors.CreamDeep),
        )
    }
}

/** The big bold page title that sits at the top of the scrolling content. */
@Composable
fun LargeTitle(title: String, modifier: Modifier = Modifier) {
    Box(modifier.height(LargeTitleHeight), contentAlignment = Alignment.CenterStart) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
