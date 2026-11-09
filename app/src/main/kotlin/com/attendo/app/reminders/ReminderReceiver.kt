package com.attendo.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.attendo.app.AttendoApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles the reminder alarm (shows the notification), the notification's "Done" action, and
 * boot / app-update / clock changes (re-arms every pending reminder, since alarms don't survive a
 * reboot and are wall-clock based).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as AttendoApplication).container
        val todos = container.data.todos
        val result = goAsync()
        container.appScope.launch {
            try {
                val id = intent.getStringExtra(EXTRA_ID)
                when (intent.action) {
                    ACTION_REMIND -> {
                        // Re-check the file: the to-do may have been ticked off or deleted meanwhile.
                        val todo = todos.todos.first().firstOrNull { it.id == id }
                        if (todo != null && !todo.done) ReminderNotifications.show(context, todo)
                    }
                    ACTION_DONE -> if (id != null) {
                        todos.setDone(id, true)
                        ReminderNotifications.dismiss(context, id)
                    }
                    else -> container.reminders.sync(todos.todos.first())
                }
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.attendo.app.action.REMIND"
        const val ACTION_DONE = "com.attendo.app.action.REMINDER_DONE"
        const val EXTRA_ID = "todo_id"
    }
}
