package com.attendo.core.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A to-do on a calendar day. With [remindAt] set it is also a reminder: the app notifies at that
 * time on [date].
 */
data class Todo(
    val id: String,
    val title: String,
    val date: LocalDate,
    val remindAt: LocalTime? = null,
    val done: Boolean = false,
    val createdAt: Long = 0L,
) {
    /** When the reminder fires, or `null` for a plain to-do. */
    val reminderDateTime: LocalDateTime? get() = remindAt?.let(date::atTime)

    /** Not done and its reminder time is still ahead, so a notification should be scheduled. */
    fun hasPendingReminder(now: LocalDateTime): Boolean =
        !done && reminderDateTime?.isAfter(now) == true
}

/** Groups to-dos the way the To-do screen and the Home summary show them. */
object TodoAgenda {

    data class Groups(
        /** Not done, dated before today. */
        val overdue: List<Todo>,
        val today: List<Todo>,
        /** Not done, after today, soonest first. */
        val upcoming: List<Todo>,
        /** Most recently dated first. */
        val done: List<Todo>,
    ) {
        val openCount: Int get() = overdue.size + today.count { !it.done } + upcoming.size
    }

    /** Sort key within a day: reminders by time first, then plain to-dos in the order added. */
    private val order = compareBy<Todo>({ it.date }, { it.remindAt ?: LocalTime.MAX }, { it.createdAt })

    fun group(todos: List<Todo>, today: LocalDate): Groups {
        val open = todos.filterNot { it.done }
        return Groups(
            overdue = open.filter { it.date < today }.sortedWith(order),
            // Today keeps its done items (ticked off) so the day's list doesn't jump around.
            today = todos.filter { it.date == today }.sortedWith(compareBy<Todo> { it.done }.then(order)),
            upcoming = open.filter { it.date > today }.sortedWith(order),
            done = todos.filter { it.done && it.date != today }.sortedWith(order).reversed(),
        )
    }

    /** Reminders still to fire, soonest first. */
    fun upcomingReminders(todos: List<Todo>, now: LocalDateTime): List<Todo> =
        todos.filter { it.hasPendingReminder(now) }.sortedBy { it.reminderDateTime }

    /** Days that have at least one open to-do (calendar markers). */
    fun openDays(todos: List<Todo>): Set<LocalDate> = todos.filterNot { it.done }.mapTo(HashSet()) { it.date }
}
