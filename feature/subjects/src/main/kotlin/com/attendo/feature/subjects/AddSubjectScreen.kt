package com.attendo.feature.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.SubjectRepository
import com.attendo.core.designsystem.component.PrimaryButton
import com.attendo.core.designsystem.component.SpeechBubble
import com.attendo.core.designsystem.component.TailSide
import com.attendo.core.designsystem.component.TriangleMascot
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.AttendoTheme
import com.attendo.core.model.Subject
import com.attendo.core.model.CatalogSubject
import com.attendo.core.model.CourseCatalog
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AddSubjectViewModel(private val subjects: SubjectRepository) : ViewModel() {
    /** Course subjects not added yet, offered as one-tap suggestions. */
    val suggestions: StateFlow<List<CatalogSubject>> = subjects.subjects
        .map { CourseCatalog.missing(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(name: String, code: String, target: Int, onSaved: () -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            subjects.add(name, code, target)
            onSaved()
        }
    }
}

@Composable
fun AddSubjectRoute(subjects: SubjectRepository, onClose: () -> Unit) {
    val vm: AddSubjectViewModel = viewModel { AddSubjectViewModel(subjects) }
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    AddSubjectScreen(
        suggestions = suggestions,
        onClose = onClose,
        onSave = { name, code, target -> vm.save(name, code, target, onClose) },
    )
}

@Composable
fun AddSubjectScreen(
    onClose: () -> Unit,
    onSave: (name: String, code: String, target: Int) -> Unit,
    suggestions: List<CatalogSubject> = emptyList(),
) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableFloatStateOf(Subject.DEFAULT_TARGET_PERCENT.toFloat()) }

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
            Text("New subject", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Center))
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
            TriangleMascot(Modifier.size(150.dp))
            SpeechBubble("Which class are we tracking?", tail = TailSide.BottomStart, modifier = Modifier.padding(bottom = 80.dp))
        }

        if (suggestions.isNotEmpty()) {
            FieldLabel("From your course")
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestions.forEach { s ->
                    val selected = s.code == code
                    Column(
                        Modifier
                            .iosPress(PressStyle.Scale, onClickLabel = "Use ${s.name}") {
                                name = s.name
                                code = s.code
                            }
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) AttendoColors.Ink else AttendoColors.CreamCard)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .widthIn(max = 180.dp),
                    ) {
                        Text(
                            s.code + if (s.isLab) " · Lab" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) AttendoColors.Green else AttendoColors.Pink,
                        )
                        Text(
                            s.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) AttendoColors.Cream else AttendoColors.Ink,
                            maxLines = 2,
                        )
                    }
                }
            }
        }

        FieldLabel("Subject name")
        RoundedField(name, { name = it.take(40) }, "e.g. Data Structures", KeyboardCapitalization.Words)
        Spacer(Modifier.height(14.dp))
        FieldLabel("Code (optional)")
        RoundedField(code, { code = it.take(12) }, "e.g. CS201", KeyboardCapitalization.Characters)
        Spacer(Modifier.height(20.dp))
        FieldLabel("Minimum attendance: ${target.roundToInt()}%")
        Slider(
            value = target,
            onValueChange = { target = (it / 5).roundToInt() * 5f },
            valueRange = 50f..100f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = AttendoColors.Green,
                activeTrackColor = AttendoColors.Green,
                inactiveTrackColor = AttendoColors.CreamDeep,
            ),
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "Save subject",
            onClick = { onSave(name, code, target.roundToInt()) },
            enabled = name.isNotBlank(),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
}

@Composable
private fun RoundedField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AttendoColors.CreamCard,
            unfocusedContainerColor = AttendoColors.CreamCard,
            focusedBorderColor = AttendoColors.Green,
            unfocusedBorderColor = AttendoColors.CreamCard,
            cursorColor = AttendoColors.Green,
        ),
    )
}

@Preview
@Composable
private fun AddSubjectPreview() {
    AttendoTheme { AddSubjectScreen(onClose = {}, onSave = { _, _, _ -> }) }
}
