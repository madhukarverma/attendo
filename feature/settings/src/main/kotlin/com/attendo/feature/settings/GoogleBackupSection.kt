package com.attendo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendo.core.backup.GoogleDriveBackup
import com.attendo.core.backup.rememberGoogleSignIn
import com.attendo.core.designsystem.theme.AttendoColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFormat = DateTimeFormatter.ofPattern("h:mm a")
private val DayTimeFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a")

internal fun backupTime(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return when (time.toLocalDate()) {
        LocalDate.now() -> "today, " + time.format(TimeFormat)
        LocalDate.now().minusDays(1) -> "yesterday, " + time.format(TimeFormat)
        else -> time.format(DayTimeFormat)
    }
}

/**
 * Backup › Google account: sign in, automatic + manual backup to the account's hidden Drive app
 * folder, restore, and sign out.
 */
@Composable
internal fun GoogleBackupSection(backup: GoogleDriveBackup) {
    val state by backup.state.collectAsStateWithLifecycle()
    val signIn = rememberGoogleSignIn(backup)
    var confirmRestore by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AttendoColors.CreamCard)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = AttendoColors.Ink, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Google account", style = MaterialTheme.typography.titleMedium)
                Text(
                    state.account?.let { email -> state.displayName?.let { "$it · $email" } ?: email } ?: "Not signed in",
                    style = MaterialTheme.typography.labelSmall,
                    color = AttendoColors.Muted,
                )
            }
            if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), color = AttendoColors.Green, strokeWidth = 2.dp)
        }
        Spacer(Modifier.height(10.dp))

        if (state.account == null) {
            Text(
                "Sign in to keep a private copy of all your data in your Google Drive, in a hidden folder only " +
                    "Attendo can see. After reinstalling, sign in again and everything comes back.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            PillButton(
                "Sign in with Google",
                Icons.Filled.AccountCircle,
                AttendoColors.Hero,
                AttendoColors.OnHero,
                Modifier.fillMaxWidth(),
                enabled = !state.busy && state.loaded,
                onClick = signIn,
            )
        } else {
            Text(
                state.lastBackupAt?.let { "Last backup: ${backupTime(it)}" } ?: "Not backed up yet",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Back up automatically", style = MaterialTheme.typography.labelLarge)
                    Text("A few seconds after every change", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
                }
                Switch(
                    checked = state.autoBackup,
                    onCheckedChange = backup::setAutoBackup,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = AttendoColors.Green,
                        checkedThumbColor = AttendoColors.Cream,
                        checkedBorderColor = AttendoColors.Green,
                        uncheckedTrackColor = AttendoColors.CreamDeep,
                        uncheckedThumbColor = AttendoColors.Cream,
                        uncheckedBorderColor = AttendoColors.CreamDeep,
                    ),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    "Back up now",
                    Icons.Filled.Share,
                    AttendoColors.Green,
                    AttendoColors.Cream,
                    Modifier.weight(1f),
                    enabled = !state.busy,
                    onClick = backup::backUpNow,
                )
                PillButton(
                    "Restore",
                    Icons.Filled.Refresh,
                    AttendoColors.CreamDeep,
                    AttendoColors.Ink,
                    Modifier.weight(1f),
                    enabled = !state.busy,
                ) { confirmRestore = true }
            }
            TextButton(onClick = backup::signOut, enabled = !state.busy, modifier = Modifier.align(Alignment.End)) {
                Text("Sign out", color = AttendoColors.Pink)
            }
        }

        state.message?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = if (state.isError) AttendoColors.Pink else AttendoColors.Muted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Restore from Google?") },
            text = {
                Text(
                    "This replaces everything on this phone (subjects, attendance, pomodoro, to-dos and settings) " +
                        "with your latest backup in ${state.account}.",
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmRestore = false; backup.restoreNow() }) {
                    Text("Restore", color = AttendoColors.Pink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancel") } },
            containerColor = AttendoColors.Cream,
        )
    }

    state.pendingRestoreFrom?.let { at ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Backup found") },
            text = {
                Text(
                    "${state.account} already has a backup" + (if (at > 0) " from ${backupTime(at)}" else "") + ". " +
                        "Restore it to replace the data on this phone, or keep this phone's data and replace the backup.",
                )
            },
            confirmButton = {
                TextButton(onClick = { backup.resolvePendingRestore(restore = true) }) {
                    Text("Restore backup", color = AttendoColors.Green, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backup.resolvePendingRestore(restore = false) }) { Text("Keep this phone's data") }
            },
            containerColor = AttendoColors.Cream,
        )
    }
}
