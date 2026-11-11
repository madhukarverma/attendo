package com.attendo.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors

/** One destination in [AttendoTabBar]; [icon] draws itself in the tint it's given. */
@Immutable
data class TabItem(
    val route: String,
    val label: String,
    val icon: @Composable (tint: Color) -> Unit,
)

/** Space to leave under scrolling content so its end can scroll clear of the tab bar. */
val TabBarClearance = 104.dp

/**
 * The app's main navigation: the dark pill bar from the designs. The current tab is the green pill
 * with its label (the spot the "+ New subject" button used to have); the others are icons.
 */
@Composable
fun AttendoTabBar(
    tabs: List<TabItem>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(AttendoColors.Hero)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            TabButton(tab, selected = tab.route == selectedRoute) { onSelect(tab.route) }
        }
    }
}

@Composable
private fun TabButton(tab: TabItem, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) AttendoColors.GreenFill else Color.Transparent, Motion.settle(), label = "tabBg")
    val content by animateColorAsState(if (selected) AttendoColors.OnAccent else AttendoColors.OnHero, Motion.settle(), label = "tabFg")
    Row(
        Modifier
            .height(44.dp)
            .iosPress(PressStyle.Scale, onClickLabel = tab.label, onClick = onClick)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tab.icon(content)
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(Motion.settle()) + fadeIn(Motion.settle()),
            exit = shrinkHorizontally(Motion.settle()) + fadeOut(Motion.settle()),
        ) {
            Text(
                tab.label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
