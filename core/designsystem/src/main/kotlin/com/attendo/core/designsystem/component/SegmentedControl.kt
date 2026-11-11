package com.attendo.core.designsystem.component

import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors

/** iOS-style segmented control with a sliding thumb, for switching pages inside a screen. */
@Composable
fun SegmentedControl(items: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AttendoColors.CreamDeep)
            .padding(3.dp),
    ) {
        val segment = maxWidth / items.size.coerceAtLeast(1)
        val x by animateDpAsState(segment * selected, Motion.bouncy(), label = "thumb")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segment)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(AttendoColors.Ink),
        )
        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { i, label ->
                val color by animateColorAsState(if (i == selected) AttendoColors.Cream else AttendoColors.Ink, label = "seg")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            role = Role.Tab
                            this.selected = i == selected
                        }
                        .iosPress(PressStyle.Dim, onClickLabel = label) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}
