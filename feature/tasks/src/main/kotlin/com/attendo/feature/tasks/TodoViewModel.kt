package com.attendo.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendo.core.data.TodoRepository
import com.attendo.core.model.Todo
import com.attendo.core.model.TodoAgenda
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class TodoUiState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val now: LocalDateTime = LocalDateTime.now(),
    val groups: TodoAgenda.Groups = TodoAgenda.group(emptyList(), LocalDate.now()),
) {
    val isEmpty: Boolean
        get() = groups.overdue.isEmpty() && groups.today.isEmpty() && groups.upcoming.isEmpty() && groups.done.isEmpty()
}

class TodoViewModel(private val todos: TodoRepository) : ViewModel() {

    /** Ticks every minute so "today", overdue and passed reminders stay current while open. */
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }

    val uiState: StateFlow<TodoUiState> = combine(todos.todos, clock) { list, now ->
        TodoUiState(loaded = true, today = now.toLocalDate(), now = now, groups = TodoAgenda.group(list, now.toLocalDate()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoUiState())

    fun add(title: String, date: LocalDate, remindAt: LocalTime?) {
        if (title.isBlank()) return
        viewModelScope.launch { todos.add(title, date, remindAt) }
    }

    fun toggle(todo: Todo) {
        viewModelScope.launch { todos.setDone(todo.id, !todo.done) }
    }

    fun delete(todo: Todo) {
        viewModelScope.launch { todos.delete(todo.id) }
    }

    fun clearDone() {
        viewModelScope.launch { todos.clearDone() }
    }
}
