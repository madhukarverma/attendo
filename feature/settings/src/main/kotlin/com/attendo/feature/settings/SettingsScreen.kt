package com.attendo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.attendo.core.designsystem.component.DeveloperCredit
import com.attendo.core.designsystem.component.PrimaryButton
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AnimationMode
import com.attendo.core.model.AppSettings
import com.attendo.core.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<AppSettings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { settings.setUserName(name) }
    }

    fun setAnimationMode(mode: AnimationMode) {
        viewModelScope.launch { settings.setAnimationMode(mode) }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setTheme(mode) }
    }

    fun setCustomColors(colors: Map<String, String>) {
        viewModelScope.launch { settings.setCustomColors(colors) }
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
    val state by vm.state.collectAsStateWithLifecycle()
    SettingsScreen(
        savedName = state?.userName,
        animationMode = state?.animationMode ?: AnimationMode.IOS,
        appVersion = appVersion,
        onSaveName = vm::saveName,
        onAnimationModeChange = vm::setAnimationMode,
        onExportCsv = onExportCsv,
        onClose = onClose,
        theme = state?.theme ?: ThemeMode.LIGHT,
        customColors = state?.customColors.orEmpty(),
        onThemeChange = vm::setTheme,
        onCustomColorsChange = vm::setCustomColors,
    )
}

@Composable
fun SettingsScreen(
    savedName: String?,
    animationMode: AnimationMode,
    appVersion: String,
    onSaveName: (String) -> Unit,
    onAnimationModeChange: (AnimationMode) -> Unit,
    onExportCsv: () -> Unit,
    onClose: () -> Unit,
    theme: ThemeMode = ThemeMode.LIGHT,
    customColors: Map<String, String> = emptyMap(),
    onThemeChange: (ThemeMode) -> Unit = {},
    onCustomColorsChange: (Map<String, String>) -> Unit = {},
) {
    var name by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(savedName) { if (savedName != null && name.isEmpty()) name = savedName }
    val scroll = rememberScrollState()
    val collapse by rememberCollapseFraction(scroll)

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        CollapsingTopBar(title = "Settings", collapseFraction = { collapse }, onBack = onClose, backLabel = "Home")
        BounceContainer(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 20.dp),
            ) {
                LargeTitle("Settings")

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

                SectionTitle("Theme")
                ThemeSection(theme, customColors, onThemeChange, onCustomColorsChange)

                SectionTitle("Animations")
                AnimationModeSlider(animationMode, onAnimationModeChange)

                SectionTitle("Your data")
                AttendoCard {
                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Everything is stored on this phone as CSV files: subjects.csv, attendance.csv, " +
                            "pomodoro.csv and settings.csv. Export them to open in Excel or Google Sheets.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Export CSV files", onClick = onExportCsv)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(Modifier.height(4.dp))
                DeveloperCredit()
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
}
