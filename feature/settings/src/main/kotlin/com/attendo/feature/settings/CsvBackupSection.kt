package com.attendo.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendo.core.data.backup.CsvImportPlan
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.theme.AttendoColors
import kotlinx.coroutines.launch

/**
 * Backup › CSV files: export (share sheet) and restore. Restore lets the user pick one or more
 * exported CSV files, shows which parts of the data each one replaces, and only then applies it.
 */
@Composable
internal fun CsvBackupSection(
    onExport: () -> Unit,
    onPlanImport: suspend (List<Uri>) -> CsvImportPlan,
    onApplyImport: suspend (CsvImportPlan) -> Map<String, Int>,
) {
    val scope = rememberCoroutineScope()
    var plan by remember { mutableStateOf<CsvImportPlan?>(null) }
    var result by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // message to isError
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val planned = runCatching { onPlanImport(uris) }.getOrNull()
            busy = false
            when {
                planned == null -> result = "Couldn't read those files." to true
                planned.isEmpty -> result = "Those files don't look like Attendo CSV exports." to true
                else -> plan = planned
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = AttendoColors.Ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Everything is stored on this phone as CSV files: subjects, attendance, pomodoro, todos " +
                    "and settings. Export them to open in Excel or Google Sheets, and restore them here.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Export", Icons.Filled.Share, AttendoColors.GreenFill, AttendoColors.OnAccent, Modifier.weight(1f), onClick = onExport)
            PillButton(
                if (busy) "Reading…" else "Restore",
                Icons.Filled.Refresh,
                AttendoColors.CreamDeep,
                AttendoColors.Ink,
                Modifier.weight(1f),
            ) {
                result = null
                picker.launch(CSV_TYPES)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            result?.first ?: "Restore: pick one or more exported files. Each file replaces that part of your data.",
            style = MaterialTheme.typography.labelSmall,
            color = when (result?.second) {
                true -> AttendoColors.Pink
                false -> AttendoColors.Green
                null -> AttendoColors.Muted
            },
        )
    }

    plan?.let { p ->
        ConfirmRestoreDialog(
            plan = p,
            onDismiss = { plan = null },
            onConfirm = {
                plan = null
                scope.launch {
                    result = runCatching { onApplyImport(p) }.fold(
                        onSuccess = { kept ->
                            "Restored " + p.tables.joinToString(", ") { "${kept[it.file] ?: 0} ${it.label.lowercase()}" } + "." to false
                        },
                        onFailure = { "Restore failed: ${it.message ?: "unknown error"}" to true },
                    )
                }
            },
        )
    }
}

/** Exports are text/csv, but file managers label CSV files in several ways. */
private val CSV_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/csv",
    "application/vnd.ms-excel",
    "application/octet-stream",
)

@Composable
private fun ConfirmRestoreDialog(plan: CsvImportPlan, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore from CSV?") },
        text = {
            Column {
                Text("This replaces these parts of your data on this phone:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                plan.tables.forEach { t ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(AttendoColors.Green))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("${t.label}: ${t.rowCount} row${if (t.rowCount == 1) "" else "s"}", style = MaterialTheme.typography.labelLarge)
                            Text("from ${t.source}", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
                        }
                    }
                }
                if (plan.unrecognised.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Skipped (not an Attendo file, or a second copy): " + plan.unrecognised.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = AttendoColors.Pink,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Anything not listed stays as it is. Export first if you want a copy of the current data.",
                    style = MaterialTheme.typography.labelSmall,
                    color = AttendoColors.Muted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Replace", color = AttendoColors.Pink, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = AttendoColors.Cream,
    )
}

@Composable
internal fun PillButton(
    text: String,
    icon: ImageVector?,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(48.dp)
            .iosPress(PressStyle.Dim, enabled = enabled, onClickLabel = text, onClick = onClick)
            .clip(CircleShape)
            .background(if (enabled) container else AttendoColors.CreamDeep)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled) content else AttendoColors.Muted
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}
