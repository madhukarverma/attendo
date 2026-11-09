package com.attendo.feature.onboarding

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
import com.attendo.core.designsystem.component.BlobMascot
import com.attendo.core.designsystem.component.PrimaryButton
import com.attendo.core.designsystem.component.SpeechBubble
import com.attendo.core.designsystem.component.Squiggle
import com.attendo.core.designsystem.component.TailSide
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.AttendoTheme
import kotlinx.coroutines.launch

class OnboardingViewModel(private val settings: SettingsRepository) : ViewModel() {
    fun finish(name: String, onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setUserName(name)
            settings.setOnboarded(true)
            onDone()
        }
    }
}

/** Route entry point: wires the ViewModel and exposes navigation callbacks only. */
@Composable
fun OnboardingRoute(settings: SettingsRepository, onFinished: () -> Unit) {
    val vm: OnboardingViewModel = viewModel { OnboardingViewModel(settings) }
    OnboardingScreen(onContinue = { name -> vm.finish(name, onFinished) })
}

@Composable
fun OnboardingScreen(onContinue: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Box(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Ink),
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
            Text("NEVER", style = headline, color = AttendoColors.Cream)
            Text("MISS", style = headline, color = AttendoColors.Green)
            Text("A CLASS", style = headline, color = AttendoColors.Green)
            Text("WITH", style = headline, color = AttendoColors.Cream)
            Text("ATTENDO", style = headline, color = AttendoColors.Cream)
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
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onContinue(name) }),
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
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = "Let's go",
                onClick = { onContinue(name) },
                enabled = name.isNotBlank(),
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingPreview() {
    AttendoTheme { OnboardingScreen(onContinue = {}) }
}
