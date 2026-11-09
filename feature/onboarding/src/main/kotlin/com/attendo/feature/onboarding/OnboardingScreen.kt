package com.attendo.feature.onboarding

import com.attendo.core.backup.GoogleDriveBackup
import com.attendo.core.backup.rememberGoogleSignIn
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.SettingsRepository
import com.attendo.core.data.SubjectRepository
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.model.CourseCatalog
import com.attendo.core.model.Nickname
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import com.attendo.core.designsystem.component.BlobMascot
import com.attendo.core.designsystem.component.PrimaryButton
import com.attendo.core.designsystem.component.SpeechBubble
import com.attendo.core.designsystem.component.Squiggle
import com.attendo.core.designsystem.component.TailSide
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.AttendoTheme
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val settings: SettingsRepository,
    private val subjects: SubjectRepository,
) : ViewModel() {
    /** Becomes true when "Let's go" is tapped, or when a restored backup brings the flag back. */
    val onboarded: StateFlow<Boolean> = settings.settings.map { it?.onboarded == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun finish(name: String, addCourseSubjects: Boolean) {
        viewModelScope.launch {
            settings.setUserName(name)
            if (addCourseSubjects) subjects.addFromCatalog(CourseCatalog.CURRENT_SEMESTER)
            settings.setOnboarded(true)
        }
    }
}

/**
 * Route entry point: wires the ViewModel and exposes navigation callbacks only.
 *
 * @param googleBackup when given, offers "Restore from Google" so a reinstall can skip set-up.
 */
@Composable
fun OnboardingRoute(
    settings: SettingsRepository,
    subjects: SubjectRepository,
    onFinished: () -> Unit,
    googleBackup: GoogleDriveBackup? = null,
) {
    val vm: OnboardingViewModel = viewModel { OnboardingViewModel(settings, subjects) }
    val onboarded by vm.onboarded.collectAsStateWithLifecycle()
    LaunchedEffect(onboarded) { if (onboarded) onFinished() }
    OnboardingScreen(
        onContinue = { name, addCourse -> vm.finish(name, addCourse) },
        restoreSection = { if (googleBackup != null) RestoreFromGoogle(googleBackup) },
    )
}

@Composable
private fun RestoreFromGoogle(backup: GoogleDriveBackup) {
    val state by backup.state.collectAsStateWithLifecycle()
    val signIn = rememberGoogleSignIn(backup)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = signIn, enabled = !state.busy && state.loaded) {
            Text(
                if (state.busy) "Connecting…" else "Used Attendo before? Restore from Google",
                color = AttendoColors.OnHero,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        state.message?.let {
            Text(
                it,
                color = if (state.isError) AttendoColors.Pink else AttendoColors.OnHero.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    onContinue: (name: String, addCourseSubjects: Boolean) -> Unit,
    restoreSection: @Composable () -> Unit = {},
) {
    var name by rememberSaveable { mutableStateOf("") }
    var addCourse by rememberSaveable { mutableStateOf(true) }
    Box(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Hero),
    ) {
        BlobMascot(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = (-70).dp)
                .size(340.dp),
        )
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            val headline = MaterialTheme.typography.displayLarge
            Text("NEVER", style = headline, color = AttendoColors.OnHero)
            Text("MISS", style = headline, color = AttendoColors.Green)
            Text("A CLASS", style = headline, color = AttendoColors.Green)
            Text("WITH", style = headline, color = AttendoColors.OnHero)
            Text("ATTENDO", style = headline, color = AttendoColors.OnHero)
            Squiggle(Modifier.width(150.dp).padding(top = 4.dp))

            Spacer(Modifier.weight(1f))
            SpeechBubble(
                text = "It's more fun when you show up!",
                tail = TailSide.TopCenter,
                color = AttendoColors.Cream,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = CircleShape,
                placeholder = { Text("What should we call you?") },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onContinue(name, addCourse) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AttendoColors.Cream,
                    unfocusedContainerColor = AttendoColors.Cream,
                    focusedBorderColor = AttendoColors.Green,
                    unfocusedBorderColor = AttendoColors.Cream,
                    focusedTextColor = AttendoColors.Ink,
                    unfocusedTextColor = AttendoColors.Ink,
                    cursorColor = AttendoColors.Green,
                ),
            )
            if (Nickname.isShortened(name)) {
                Text(
                    "We'll call you “${Nickname.of(name)}” for short.",
                    color = AttendoColors.OnHero.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 20.dp, top = 6.dp),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .iosPress(PressStyle.Dim) { addCourse = !addCourse },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = addCourse,
                    onCheckedChange = { addCourse = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AttendoColors.Green,
                        uncheckedColor = AttendoColors.OnHero,
                        checkmarkColor = AttendoColors.OnHero,
                    ),
                )
                Text(
                    "Add my ${CourseCatalog.CURRENT_SEMESTER.size} semester subjects (CA711 – CA703)",
                    color = AttendoColors.OnHero,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(6.dp))
            PrimaryButton(
                text = "Let's go",
                onClick = { onContinue(name, addCourse) },
                enabled = name.isNotBlank(),
            )
            restoreSection()
        }
    }
}

@Preview
@Composable
private fun OnboardingPreview() {
    AttendoTheme { OnboardingScreen(onContinue = { _, _ -> }) }
}
