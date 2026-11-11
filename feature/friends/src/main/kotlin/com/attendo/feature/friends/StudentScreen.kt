package com.attendo.feature.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.attendo.core.designsystem.motion.BounceContainer
import com.attendo.core.designsystem.motion.CollapsingTopBar
import com.attendo.core.designsystem.motion.LargeTitle
import com.attendo.core.designsystem.motion.rememberCollapseFraction
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.social.FriendView
import com.attendo.core.social.SharedAttendance
import com.attendo.core.social.SharedGrades
import com.attendo.core.social.SocialService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val Updated = DateTimeFormatter.ofPattern("d MMM, H:mm")

/**
 * Another student's page. Friends see each other's friend list and whatever the other has chosen
 * to share; for anyone else it shows the name, code and the friend-request action.
 */
@Composable
fun StudentScreen(social: SocialService, uid: String, onOpenStudent: (String) -> Unit, onClose: () -> Unit) {
    val state by social.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val collapse by rememberCollapseFraction(listState)
    var view by remember { mutableStateOf<FriendView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmUnfriend by remember { mutableStateOf(false) }
    val isFriend = state.isFriend(uid)

    // Reload when friendship changes (e.g. a request was just accepted).
    LaunchedEffect(uid, isFriend) {
        social.loadStudent(uid).onSuccess { view = it; error = null }.onFailure { error = it.message }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        CollapsingTopBar(title = view?.profile?.name ?: "Student", collapseFraction = { collapse }, onBack = onClose, backLabel = "Back")
        BounceContainer(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val v = view
                if (v == null) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            if (error != null) Muted(error!!) else CircularProgressIndicator(color = AttendoColors.Green)
                        }
                    }
                } else {
                    item { LargeTitle(v.profile.name) }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Friend code ${v.profile.code}", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Muted)
                                if (v.profile.programme.isNotBlank()) {
                                    Text(v.profile.programme, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Muted)
                                }
                            }
                            val incoming = state.requestFrom(uid)
                            when {
                                uid == state.me?.uid -> Text("You", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Green)
                                isFriend -> TextButton(onClick = { if (confirmUnfriend) social.unfriend(v.profile) else confirmUnfriend = true }) {
                                    Text(if (confirmUnfriend) "Tap again to unfriend" else "Unfriend", color = AttendoColors.Pink)
                                }
                                state.requestedTo(uid) -> Text("Requested", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Muted)
                                incoming != null -> SmallPill("Accept", primary = true, enabled = !state.busy) { social.accept(incoming) }
                                else -> SmallPill("Add friend", primary = true, enabled = !state.busy) { social.sendRequest(v.profile) }
                            }
                        }
                    }
                    if (!v.isFriend) {
                        item {
                            Card {
                                Muted("Become friends to see ${v.profile.name.substringBefore(' ')}'s friends and attendance.")
                            }
                        }
                    } else {
                        item { Section("Attendance") }
                        item { AttendanceCard(v.attendance, v.profile.name) }
                        v.grades?.let { grades ->
                            item { Section("Grades") }
                            item { GradesCard(grades) }
                        }
                        item { Section("Friends (${v.friends.size})") }
                        if (v.friends.isEmpty()) item { Muted("No friends yet.") }
                        items(v.friends, key = { it.uid }) { friend ->
                            StudentRow(friend, onClick = { onOpenStudent(friend.uid) }) {
                                if (state.isFriend(friend.uid)) {
                                    Text("Friends", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Green)
                                } else if (friend.uid == state.me?.uid) {
                                    Text("You", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Green)
                                }
                            }
                        }
                    }
                }
                state.message?.takeIf { state.isError }?.let { msg ->
                    item { Text(msg, style = MaterialTheme.typography.labelSmall, color = AttendoColors.Pink) }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun AttendanceCard(attendance: SharedAttendance?, name: String) {
    Card {
        if (attendance == null) {
            Muted("${name.substringBefore(' ')} isn't sharing attendance.")
            return@Card
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                attendance.overall?.let { "${it.roundToInt()}%" } ?: "–",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text("overall", style = MaterialTheme.typography.labelLarge, color = AttendoColors.Muted, modifier = Modifier.padding(bottom = 4.dp))
        }
        attendance.subjects.forEach { s ->
            val onTrack = s.held == 0 || s.percent >= s.target
            Column(Modifier.padding(top = 10.dp)) {
                Row {
                    Text(s.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text(
                        if (s.held == 0) "–" else "${s.percent.roundToInt()}% · ${s.present}/${s.held}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (onTrack) AttendoColors.Ink else AttendoColors.Pink,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Bar((s.percent / 100).toFloat(), if (onTrack) AttendoColors.Green else AttendoColors.Pink)
            }
        }
        UpdatedAt(attendance.updatedAt)
    }
}

@Composable
private fun GradesCard(grades: SharedGrades) {
    Card {
        grades.overall?.let {
            Text("${it.roundToInt()}% overall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        grades.subjects.forEach { s ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(s.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text("${s.percent.roundToInt()}%  ${s.grade}", style = MaterialTheme.typography.labelLarge)
            }
        }
        UpdatedAt(grades.updatedAt)
    }
}

@Composable
private fun UpdatedAt(millis: Long) {
    if (millis <= 0) return
    Text(
        "Updated ${Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(Updated)}",
        style = MaterialTheme.typography.labelSmall,
        color = AttendoColors.Muted,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun Bar(fraction: Float, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(AttendoColors.CreamDeep),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}
