package com.attendo.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.attendo.core.model.Todo
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Keeps one AlarmManager alarm per pending reminder. [sync] is called with the full to-do list
 * whenever todos.csv changes (and after boot/time changes), so alarms always mirror the file.
 */
class ReminderScheduler(private val context: Context) {

    private val alarms: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /** Ids this process has an alarm set for, so deleted to-dos get their alarm cancelled. */
    private val scheduled = HashSet<String>()

    @Synchronized
    fun sync(todos: List<Todo>, now: LocalDateTime = LocalDateTime.now()) {
        val pending = todos.filter { it.hasPendingReminder(now) }
        val pendingIds = pending.mapTo(HashSet()) { it.id }
        // Deleted since the last sync, or done / in the past (also covers alarms from an earlier run).
        (scheduled - pendingIds).forEach(::cancel)
        todos.filter { it.remindAt != null && it.id !in pendingIds }.forEach { cancel(it.id) }
        pending.forEach(::schedule)
        scheduled.clear()
        scheduled += pendingIds
    }

    private fun schedule(todo: Todo) {
        val at = todo.reminderDateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: return
        val intent = alarmIntent(todo.id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            // Exact alarms not allowed: still remind, possibly a few minutes late.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    private fun cancel(id: String) = alarms.cancel(alarmIntent(id))

    private fun alarmIntent(id: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        id.hashCode(),
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMIND)
            // A per-to-do data URI keeps PendingIntents distinct even if two ids share a hash code.
            .setData(Uri.parse("attendo-reminder://$id"))
            .putExtra(ReminderReceiver.EXTRA_ID, id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
