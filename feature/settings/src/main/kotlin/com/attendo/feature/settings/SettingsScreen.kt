package com.attendo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.SettingsRepository
import com.attendo.core.designsystem.component.AttendoCard
import com.attendo.core.designsystem.component.PrimaryButton
import com.attendo.core.designsystem.theme.AttendoColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val userName: StateFlow<String?> = settings.settings
        .map { it?.userName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { settings.setUserName(name) }
    }
}

/**
 * @param onExportCsv implemented by the app module (needs Android sharing APIs), so this feature stays
 * free of platform plumbing.
 */
@Composable
fun SettingsRoute(
    settings: SettingsRepository,
    appVersion: String,
    onExportCsv: () -> Unit,
    onClose: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel { SettingsViewModel(settings) }
    val savedName by vm.userName.collectAsStateWithLifecycle()
    SettingsScreen(savedName, appVersion, vm::saveName, onExportCsv, onClose)
}

@Composable
fun SettingsScreen(
    savedName: String?,
    appVersion: String,
    onSaveName: (String) -> Unit,
    onExportCsv: () -> Unit,
    onClose: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(savedName) { if (savedName != null && name.isEmpty()) name = savedName }

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        SectionTitle("Profile")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Your name") },
            shape = RoundedCornerShape(20.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AttendoColors.CreamCard,
                unfocusedContainerColor = AttendoColors.CreamCard,
                focusedBorderColor = AttendoColors.Green,
                unfocusedBorderColor = AttendoColors.CreamCard,
                cursorColor = AttendoColors.Green,
            ),
        )
        TextButton(
            onClick = { onSaveName(name) },
            enabled = name.isNotBlank() && name.trim() != savedName,
            modifier = Modifier.align(Alignment.End),
        ) { Text("Save name") }

        SectionTitle("Your data")
        AttendoCard {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                "Everything is stored on this phone as CSV files: subjects.csv, attendance.csv and settings.csv. " +
                    "Export them to open in Excel or Google Sheets.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Export CSV files", onClick = onExportCsv)
        Spacer(Modifier.height(8.dp))
        Row {
            Icon(Icons.Filled.Share, contentDescription = null, tint = AttendoColors.Muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Opens the share sheet: save to Drive, email, or Files.",
                style = MaterialTheme.typography.labelSmall,
                color = AttendoColors.Muted,
            )
        }

        SectionTitle("About")
        Text("Attendo $appVersion", style = MaterialTheme.typography.bodyMedium, color = AttendoColors.Muted)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
}

@Composable
private fun Row(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, content = content)
}
