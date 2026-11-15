package com.attendo.feature.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.designsystem.component.AttendoIcons
import com.attendo.core.designsystem.component.ErrorNote
import com.attendo.core.designsystem.component.PillButton
import com.attendo.core.designsystem.component.PrimaryButton
import com.attendo.core.designsystem.component.attendoFilledFieldColors
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import com.attendo.core.designsystem.motion.rememberHaptics
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.Corner
import com.attendo.core.designsystem.theme.Space
import com.attendo.core.model.Feedback
import com.attendo.core.model.FeedbackCategory
import com.attendo.core.model.FeedbackDiagnostics
import com.attendo.core.model.FeedbackResult
import com.attendo.core.model.FeedbackSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val rating: Int = 0,
    val category: FeedbackCategory = FeedbackCategory.OTHER,
    val message: String = "",
    val contact: String = "",
    val includeDetails: Boolean = true,
    val sending: Boolean = false,
    /** Sent or queued: the thank-you view. Failed: the error under the form. */
    val result: FeedbackResult? = null,
)

class FeedbackViewModel(
    private val sender: FeedbackSender,
    private val diagnostics: FeedbackDiagnostics,
) : ViewModel() {
    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    fun setRating(stars: Int) = edit { copy(rating = if (rating == stars) 0 else stars) }
    fun setCategory(c: FeedbackCategory) = edit { copy(category = c) }
    fun setMessage(s: String) = edit { copy(message = s.take(Feedback.MAX_MESSAGE)) }
    fun setContact(s: String) = edit { copy(contact = s.take(Feedback.MAX_CONTACT)) }
    fun setIncludeDetails(on: Boolean) = edit { copy(includeDetails = on) }

    fun send() {
        val s = _state.value
        if (s.sending) return
        val feedback = Feedback(s.rating, s.category, s.message, s.contact, diagnostics.takeIf { s.includeDetails })
        feedback.problem()?.let { problem ->
            _state.update { it.copy(result = FeedbackResult.Failed(problem)) }
            return
        }
        _state.update { it.copy(sending = true, result = null) }
        viewModelScope.launch {
            val result = sender.send(feedback)
            _state.update { it.copy(sending = false, result = result) }
        }
    }

    /** Back to an empty form after a thank-you. */
    fun reset() = _state.update { FeedbackUiState() }

    /** Any edit clears an old error, since it may no longer apply. */
    private fun edit(change: FeedbackUiState.() -> FeedbackUiState) = _state.update {
        val next = it.change()
        if (next.result is FeedbackResult.Failed) next.copy(result = null) else next
    }
}

/** Settings › Feedback. [diagnostics] are the phone details the student can choose to attach. */
@Composable
fun FeedbackRoute(sender: FeedbackSender, diagnostics: FeedbackDiagnostics, onClose: () -> Unit) {
    val vm: FeedbackViewModel = viewModel { FeedbackViewModel(sender, diagnostics) }
    val state by vm.state.collectAsStateWithLifecycle()
    FeedbackScreen(
        state = state,
        diagnostics = diagnostics,
        onRating = vm::setRating,
        onCategory = vm::setCategory,
        onMessage = vm::setMessage,
        onContact = vm::setContact,
        onIncludeDetails = vm::setIncludeDetails,
        onSend = vm::send,
        onSendAnother = vm::reset,
        onClose = onClose,
    )
}

@Composable
fun FeedbackScreen(
    state: FeedbackUiState,
    diagnostics: FeedbackDiagnostics,
    onRating: (Int) -> Unit,
    onCategory: (FeedbackCategory) -> Unit,
    onMessage: (String) -> Unit,
    onContact: (String) -> Unit,
    onIncludeDetails: (Boolean) -> Unit,
    onSend: () -> Unit,
    onSendAnother: () -> Unit,
    onClose: () -> Unit,
) {
    val scroll = rememberScrollState()
    val collapse by rememberCollapseFraction(scroll)
    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        CollapsingTopBar(title = "Feedback", collapseFraction = { collapse }, onBack = onClose, backLabel = "Settings")
        BounceContainer(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = Space.Screen),
            ) {
                LargeTitle("Feedback")
                when (val result = state.result) {
                    is FeedbackResult.Sent -> ThankYou(result.reference, queued = false, onClose, onSendAnother)
                    is FeedbackResult.Queued -> ThankYou(result.reference, queued = true, onClose, onSendAnother)
                    else -> Form(state, diagnostics, onRating, onCategory, onMessage, onContact, onIncludeDetails, onSend)
                }
                Spacer(Modifier.height(Space.XL))
            }
        }
    }
}

@Composable
private fun Form(
    state: FeedbackUiState,
    diagnostics: FeedbackDiagnostics,
    onRating: (Int) -> Unit,
    onCategory: (FeedbackCategory) -> Unit,
    onMessage: (String) -> Unit,
    onContact: (String) -> Unit,
    onIncludeDetails: (Boolean) -> Unit,
    onSend: () -> Unit,
) {
    Text(
        "Tell the developer what works and what doesn't. Every message is read.",
        style = MaterialTheme.typography.bodyLarge,
        color = AttendoColors.Muted,
    )

    Section("Rate Attendo")
    Stars(state.rating, onRating)
    Spacer(Modifier.height(Space.XS))
    Text(Feedback.ratingLabel(state.rating), style = MaterialTheme.typography.bodyMedium, color = AttendoColors.Muted)

    Section("What's it about?")
    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.XS)) {
        FeedbackCategory.entries.forEach { c ->
            CategoryChip(c.label, selected = state.category == c) { onCategory(c) }
        }
    }

    Section("Message")
    OutlinedTextField(
        value = state.message,
        onValueChange = onMessage,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(state.category.prompt) },
        minLines = 5,
        maxLines = 10,
        shape = Corner.Shape,
        colors = attendoFilledFieldColors(AttendoColors.CreamCard),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        supportingText = {
            Text("${state.message.length} / ${Feedback.MAX_MESSAGE}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
        },
    )

    Section("Email (optional)")
    val badEmail = state.contact.isNotBlank() && !Feedback.looksLikeEmail(state.contact.trim())
    OutlinedTextField(
        value = state.contact,
        onValueChange = onContact,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("you@example.com") },
        isError = badEmail && state.result != null,
        shape = Corner.Shape,
        colors = attendoFilledFieldColors(AttendoColors.CreamCard),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        supportingText = { Text("Only if you'd like a reply. It's never shown to anyone else.") },
    )

    Spacer(Modifier.height(Space.L))
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = state.includeDetails, role = Role.Switch, onValueChange = onIncludeDetails),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Include phone details", style = MaterialTheme.typography.titleMedium, color = AttendoColors.Ink)
            Spacer(Modifier.height(Space.XXS))
            Text(diagnostics.summary, style = MaterialTheme.typography.bodySmall, color = AttendoColors.Muted)
        }
        Spacer(Modifier.width(Space.M))
        Switch(
            checked = state.includeDetails,
            onCheckedChange = null,
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

    Spacer(Modifier.height(Space.L))
    Text(
        "Your attendance, marks and tasks are never sent. Feedback goes to the developer's Firebase project.",
        style = MaterialTheme.typography.bodySmall,
        color = AttendoColors.Muted,
    )

    (state.result as? FeedbackResult.Failed)?.let {
        Spacer(Modifier.height(Space.M))
        ErrorNote(it.message)
    }

    Spacer(Modifier.height(Space.L))
    PrimaryButton(if (state.sending) "Sending…" else "Send feedback", onClick = onSend, enabled = !state.sending)
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = AttendoColors.Ink,
        modifier = Modifier.padding(top = Space.XL, bottom = Space.S),
    )
}

@Composable
private fun Stars(rating: Int, onRating: (Int) -> Unit) {
    val haptics = rememberHaptics()
    Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(Space.XXS)) {
        (1..5).forEach { n ->
            Box(
                Modifier
                    .size(48.dp)
                    .selectable(selected = rating == n, role = Role.RadioButton) {
                        haptics.tick()
                        onRating(n)
                    }
                    .semantics { contentDescription = "$n star" + if (n > 1) "s" else "" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (n <= rating) AttendoColors.Yellow else AttendoColors.CreamDeep,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(40.dp)
            .background(if (selected) AttendoColors.Ink else AttendoColors.CreamCard, Corner.Shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Space.M),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) AttendoColors.Cream else AttendoColors.Ink)
    }
}

/** The designed "sent" state, in place of the form. */
@Composable
private fun ThankYou(reference: String, queued: Boolean, onDone: () -> Unit, onSendAnother: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = Space.XL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(if (queued) AttendoColors.CreamCard else AttendoColors.GreenFill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AttendoIcons.Check,
                contentDescription = null,
                tint = if (queued) AttendoColors.Muted else AttendoColors.OnAccent,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(Space.L))
        Text(
            if (queued) "Saved. It'll send when you're online." else "Thanks for your feedback",
            style = MaterialTheme.typography.headlineSmall,
            color = AttendoColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.XS))
        Text(
            if (queued) {
                "There's no connection right now. Attendo sends it automatically, even after a restart."
            } else {
                "It reached the developer. Thank you for helping make Attendo better."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = AttendoColors.Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Space.M))
        Text(
            "Reference $reference",
            style = MaterialTheme.typography.labelLarge,
            color = AttendoColors.Ink,
            modifier = Modifier
                .background(AttendoColors.CreamCard, Corner.Shape)
                .padding(horizontal = Space.S, vertical = Space.XS),
        )
        Spacer(Modifier.height(Space.XL))
        PrimaryButton("Done", onClick = onDone)
        Spacer(Modifier.height(Space.S))
        PillButton("Send more feedback", null, AttendoColors.CreamCard, AttendoColors.Ink, Modifier.fillMaxWidth(), onClick = onSendAnother)
    }
}
