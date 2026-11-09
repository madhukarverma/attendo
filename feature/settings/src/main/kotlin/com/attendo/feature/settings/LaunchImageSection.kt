package com.attendo.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.attendo.core.designsystem.component.BATCH_LABEL
import com.attendo.core.designsystem.component.LaunchBackground
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.parseHexColor

/**
 * Lets the user choose their own launch-screen image from the system photo picker (no storage
 * permission needed). The app copies it into private storage and matches the launch background to
 * the image's edges.
 */
@Composable
internal fun LaunchImageSection(
    hasImage: Boolean,
    backgroundHex: String?,
    onPick: (Uri) -> Unit,
    onRemove: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPick(uri)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(backgroundHex?.let(::parseHexColor) ?: LaunchBackground)
                    .border(1.dp, AttendoColors.CreamDeep, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Launch image", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (hasImage) "Set. Shown under \"$BATCH_LABEL\"; the background matches its edges."
                    else "None. Pick any image from your gallery to show under \"$BATCH_LABEL\".",
                    style = MaterialTheme.typography.labelSmall,
                    color = AttendoColors.Muted,
                )
            }
        }
        Row {
            TextButton(onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text(if (hasImage) "Change" else "Choose from gallery") }
            if (hasImage) TextButton(onClick = onRemove) { Text("Remove", color = AttendoColors.Pink) }
        }
    }
}
