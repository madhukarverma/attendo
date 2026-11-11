package com.attendo.feature.tasks

import com.attendo.core.data.HabitRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.attendo.core.data.KanbanRepository
import com.attendo.core.data.TodoRepository
import com.attendo.core.designsystem.component.SegmentedControl
import com.attendo.core.designsystem.theme.AttendoColors
import java.time.LocalDate

/** Pages of the Tasks tab, in the order of its segmented control. */
enum class TasksPage(val label: String) {
    /** Weekday checklist (habit tracker). */
    CHECKLIST("Checklist"),
    BOARD("Board"),

    /** Dated reminders with notifications (what's left of the old to-do list). */
    REMINDERS("Reminders"),
}

/**
 * The Tasks tab: the weekday checklist, the Kanban board and reminders.
 *
 * @param bottomPadding room left under each page for the app's tab bar.
 * @param requestedPage a page to show now (a reminder notification opens Reminders);
 * [onRequestedPageHandled] clears it.
 */
@Composable
fun TasksRoute(
    todos: TodoRepository,
    kanban: KanbanRepository,
    habits: HabitRepository,
    bottomPadding: Dp = 24.dp,
    requestedPage: TasksPage? = null,
    onRequestedPageHandled: () -> Unit = {},
) {
    val boardVm: BoardViewModel = viewModel { BoardViewModel(kanban) }
    val todoVm: TodoViewModel = viewModel { TodoViewModel(todos) }
    val habitsVm: HabitsViewModel = viewModel { HabitsViewModel(habits) }
    val board by boardVm.board.collectAsStateWithLifecycle()
    val checklist by habitsVm.uiState.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(TasksPage.CHECKLIST) }
    LaunchedEffect(requestedPage) {
        if (requestedPage != null) {
            page = requestedPage
            onRequestedPageHandled()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AttendoColors.Cream)
            .safeDrawingPadding(),
    ) {
        Text(
            "Tasks",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
        )
        SegmentedControl(
            items = TasksPage.entries.map { it.label },
            selected = page.ordinal,
            onSelect = { page = TasksPage.entries[it] },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
        Box(Modifier.weight(1f)) {
            when (page) {
                TasksPage.CHECKLIST -> HabitsPage(
                    state = checklist,
                    onSelect = habitsVm::select,
                    onShiftWeek = habitsVm::shiftWeek,
                    onToggle = habitsVm::toggle,
                    onAdd = habitsVm::add,
                    onUpdate = habitsVm::update,
                    onDelete = habitsVm::delete,
                    bottomPadding = bottomPadding,
                )
                TasksPage.BOARD -> BoardPage(board, LocalDate.now(), boardVm, bottomPadding)
                TasksPage.REMINDERS -> TodoPageHost(todoVm, bottomPadding)
            }
        }
    }
}
