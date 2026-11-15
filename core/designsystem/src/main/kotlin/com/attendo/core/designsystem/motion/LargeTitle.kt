package com.attendo.core.designsystem.motion

import androidx.compose.foundation.layout.RowScope
import com.attendo.core.designsystem.theme.Space
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
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
 * fades in once the [LargeTitle] scrolls underneath. Instead of a hard divider, content fades out
 * as it scrolls under the bar (Apple's scroll edge effect), only once something is under it.
 * [onBack] null hides the back button (top-level tabs). Drawn above the content that follows it.
 */
@Composable
fun CollapsingTopBar(
    title: String,
    collapseFraction: () -> Float,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backLabel: String = "Back",
) {
    Box(
        modifier
            .zIndex(1f)
            .fillMaxWidth()
            .height(48.dp)
            .drawWithContent {
                drawContent()
                val f = collapseFraction()
                if (f > 0f) {
                    val fade = EdgeFade.toPx()
                    drawRect(
                        Brush.verticalGradient(
                            listOf(AttendoColors.Cream.copy(alpha = 0.92f * f), AttendoColors.Cream.copy(alpha = 0f)),
                            startY = size.height,
                            endY = size.height + fade,
                        ),
                        topLeft = Offset(0f, size.height),
                        size = Size(size.width, fade),
                    )
                }
            }
            .background(AttendoColors.Cream),
    ) {
        if (onBack != null) Row(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = Space.XXS)
                .iosPress(PressStyle.Dim, onClickLabel = backLabel, onClick = onBack)
                .padding(end = Space.S, top = Space.XS, bottom = Space.XS),
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
                .padding(horizontal = Space.XXL * 2)
                .graphicsLayer {
                    val f = collapseFraction()
                    alpha = ((f - 0.5f) * 2f).coerceIn(0f, 1f)
                },
        )
    }
}

/** How far below the bar the scroll edge fade reaches. */
private val EdgeFade = 18.dp

/** Height of the title row on every tab, so all five titles sit at exactly the same place. */
val PageHeaderHeight = 48.dp

/**
 * The title row at the top of each tab (Home, Classes, Tasks, Timer, Stats): the large bold title,
 * vertically centred in a [PageHeaderHeight] row that starts [Space.XS] below the status bar, with
 * optional buttons on the right. Every tab uses it, so titles line up when switching tabs.
 */
@Composable
fun PageHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    PageHeaderRow(modifier, trailing) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** [PageHeader] with a custom first line (Home's greeting). */
@Composable
fun PageHeaderRow(
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
    title: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = Space.XS)
            .height(PageHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { title() }
        trailing()
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
