package com.attendo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.Corner
import com.attendo.core.designsystem.theme.Space

/**
 * What a screen or list shows when there's nothing in it yet: an icon, a one-line title that says
 * what's missing, a sentence on how to fill it, and optionally the button that does it.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Space.L, vertical = Space.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(AttendoColors.CreamCard, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(Space.M))
        Text(title, style = MaterialTheme.typography.titleMedium, color = AttendoColors.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.XS))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AttendoColors.Muted, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.L))
            PillButton(actionLabel, null, AttendoColors.Ink, AttendoColors.Cream, onClick = onAction)
        }
    }
}

/**
 * An error shown in place (not a toast): an alert icon and the message, which ends with its code
 * from `ErrorCode` ("… (FB-505)"). Screen readers announce it when it appears.
 */
@Composable
fun ErrorNote(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(AttendoColors.PinkFill.copy(alpha = 0.16f), Corner.Shape)
            .padding(Space.S)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Icon(AttendoIcons.Alert, contentDescription = "Error", tint = AttendoColors.Pink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Space.XS))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = AttendoColors.Ink)
    }
}
