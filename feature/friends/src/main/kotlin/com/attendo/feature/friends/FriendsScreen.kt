package com.attendo.feature.friends

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.PressStyle
import com.attendo.core.designsystem.motion.iosPress
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.social.FriendRequest
import com.attendo.core.social.SocialService
import com.attendo.core.social.SocialState
import com.attendo.core.social.StudentProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Friends, LeetCode-style: sign in with Google, find classmates by name or friend code, send a
 * request they approve, and see each other's friends and shared attendance. What you share is set
 * here (attendance on/off; grades off by default).
 */
@Composable
fun FriendsScreen(social: SocialService, onOpenStudent: (uid: String) -> Unit, onClose: () -> Unit) {
    val state by social.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val collapse by rememberCollapseFraction(listState)
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<StudentProfile>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    // Search as you type (after a short pause), once signed in.
    LaunchedEffect(query, state.me?.uid) {
        if (state.me == null || query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        searching = true
        results = social.search(query)
        searching = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        CollapsingTopBar(title = "Friends", collapseFraction = { collapse }, onBack = onClose, backLabel = "Back")
        BounceContainer(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { LargeTitle("Friends") }
                val me = state.me
                when {
                    !state.configured -> item { NotSetUp() }
                    !state.loaded -> item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AttendoColors.Green) } }
                    me == null -> item {
                        SignInCard(state) { activity?.let { a -> scope.launch { social.signIn(a) } } }
                    }
                    else -> {
                        item { ProfileCard(me, state, onSignOut = { activity?.let(social::signOut) }) }
                        item { SharingCard(state, social::setShareAttendance, social::setShareGrades) }
                        item {
                            SearchField(query, searching) { query = it }
                        }
                        if (query.trim().length >= 2 && !searching && results.isEmpty()) {
                            item { Muted("No students found. Try their first name or their 6-character friend code.") }
                        }
                        items(results, key = { "result-${it.uid}" }) { student ->
                            StudentRow(student, onClick = { onOpenStudent(student.uid) }) {
                                RelationButton(student, state, social)
                            }
                        }
                        if (state.incoming.isNotEmpty()) {
                            item { Section("Requests (${state.incoming.size})") }
                            items(state.incoming, key = { "in-${it.from}" }) { req ->
                                StudentRow(StudentProfile(req.from, req.fromName, req.fromCode)) {
                                    TextButton(onClick = { social.decline(req) }, enabled = !state.busy) { Text("Decline", color = AttendoColors.Muted) }
                                    SmallPill("Accept", primary = true, enabled = !state.busy) { social.accept(req) }
                                }
                            }
                        }
                        item { Section(if (state.friends.isEmpty()) "Friends" else "Friends (${state.friends.size})") }
                        if (state.friends.isEmpty()) {
                            item { Muted("No friends yet. Search for classmates above and send them a request.") }
                        }
                        items(state.friends, key = { "friend-${it.uid}" }) { friend ->
                            StudentRow(friend, onClick = { onOpenStudent(friend.uid) }) {
                                Text("View", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Green)
                            }
                        }
                        if (state.outgoing.isNotEmpty()) {
                            item { Section("Sent requests") }
                            items(state.outgoing, key = { "out-${it.to}" }) { req: FriendRequest ->
                                StudentRow(StudentProfile(req.to, req.toName, com.attendo.core.social.friendCode(req.to))) {
                                    TextButton(onClick = { social.cancel(req) }, enabled = !state.busy) { Text("Cancel", color = AttendoColors.Pink) }
                                }
                            }
                        }
                    }
                }
                state.message?.let { msg ->
                    item {
                        Text(
                            msg,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isError) AttendoColors.Pink else AttendoColors.Muted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun NotSetUp() {
    Card {
        Text("Not set up yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Friends needs a free Firebase project for this app (sign-in and the shared friend lists). " +
                "Once it's created and its settings are added to the build, this screen lets you sign in. " +
                "Steps: docs/FRIENDS_SETUP.md in the Attendo repository.",
            style = MaterialTheme.typography.bodyMedium,
            color = AttendoColors.Muted,
        )
    }
}

@Composable
private fun SignInCard(state: SocialState, onSignIn: () -> Unit) {
    Card {
        Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = AttendoColors.Ink, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp))
        Text("Study with friends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Sign in with Google to find classmates, send friend requests and compare attendance. " +
                "Only your name and friend code are visible to other students; your attendance is shown " +
                "to friends only while sharing is on, and grades stay private unless you choose to share them.",
            style = MaterialTheme.typography.bodyMedium,
            color = AttendoColors.Muted,
        )
        Spacer(Modifier.height(12.dp))
        SmallPill(if (state.busy) "Signing in…" else "Sign in with Google", primary = true, enabled = !state.busy, onClick = onSignIn)
    }
}

@Composable
private fun ProfileCard(me: StudentProfile, state: SocialState, onSignOut: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AttendoColors.Hero)
            .padding(18.dp),
    ) {
        Text(me.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AttendoColors.OnHero)
        Spacer(Modifier.height(2.dp))
        Text(me.programme, style = MaterialTheme.typography.labelMedium, color = AttendoColors.Muted)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("YOUR FRIEND CODE", style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
                Text(me.code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AttendoColors.Green)
            }
            Text("${state.friends.size} friend${if (state.friends.size == 1) "" else "s"}", style = MaterialTheme.typography.labelLarge, color = AttendoColors.OnHero)
        }
        TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.End)) { Text("Sign out", color = AttendoColors.Pink) }
    }
}

@Composable
private fun SharingCard(state: SocialState, onAttendance: (Boolean) -> Unit, onGrades: (Boolean) -> Unit) {
    Card {
        Text("What friends can see", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ToggleRow("Attendance", "Overall % and each subject", state.shareAttendance, onAttendance)
        ToggleRow("Grades", "Private by default", state.shareGrades, onGrades)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
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
}

@Composable
private fun SearchField(query: String, searching: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = { onChange(it.take(40)) },
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        singleLine = true,
        placeholder = { Text("Find classmates by name or friend code") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AttendoColors.Muted) },
        trailingIcon = { if (searching) CircularProgressIndicator(Modifier.size(18.dp), color = AttendoColors.Green, strokeWidth = 2.dp) },
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AttendoColors.CreamCard,
            unfocusedContainerColor = AttendoColors.CreamCard,
            focusedBorderColor = AttendoColors.Green,
            unfocusedBorderColor = AttendoColors.CreamCard,
            cursorColor = AttendoColors.Green,
        ),
    )
}

/** The action for a search result: Friends, Requested, Accept, or Add. */
@Composable
private fun RelationButton(student: StudentProfile, state: SocialState, social: SocialService) {
    val incoming = state.requestFrom(student.uid)
    when {
        state.isFriend(student.uid) -> Text("Friends", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Green)
        state.requestedTo(student.uid) -> Text("Requested", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Muted)
        incoming != null -> SmallPill("Accept", primary = true, enabled = !state.busy) { social.accept(incoming) }
        else -> SmallPill("Add", primary = true, enabled = !state.busy) { social.sendRequest(student) }
    }
}

@Composable
internal fun StudentRow(student: StudentProfile, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AttendoColors.CreamCard)
            .then(if (onClick != null) Modifier.iosPress(PressStyle.Highlight, onClickLabel = "Open ${student.name}", onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(AttendoColors.CreamDeep),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                student.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(student.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(student.code, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}

@Composable
internal fun SmallPill(text: String, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = when {
            !enabled -> AttendoColors.Muted
            primary -> AttendoColors.OnAccent
            else -> AttendoColors.Ink
        },
        modifier = Modifier
            .clip(CircleShape)
            .iosPress(PressStyle.Scale, enabled = enabled, onClickLabel = text, onClick = onClick)
            .background(if (!enabled) AttendoColors.CreamDeep else if (primary) AttendoColors.GreenFill else AttendoColors.CreamCard)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
internal fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AttendoColors.CreamCard)
            .padding(16.dp),
    ) { content() }
}

@Composable
internal fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
}

@Composable
internal fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = AttendoColors.Muted)
}
