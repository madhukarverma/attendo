package com.attendo.app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.attendo.app.MainActivity
import com.attendo.app.R
import com.attendo.core.model.Todo
import java.time.format.DateTimeFormatter

/** The "Reminders" notification channel and the notification shown when a reminder is due. */
object ReminderNotifications {
    private const val CHANNEL_ID = "reminders"
    private const val NOTIFICATION_ID = 1

    /** Opens the To-do screen (handled by MainActivity / the nav host). */
    val OPEN_TODO_URI: Uri = Uri.parse("attendo://todo")

    private val TimeFormat = DateTimeFormatter.ofPattern("h:mm a")

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "To-do reminders you set in Attendo"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked by canNotify()
    fun show(context: Context, todo: Todo) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context,
            todo.id.hashCode(),
            Intent(Intent.ACTION_VIEW, OPEN_TODO_URI, context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val done = PendingIntent.getBroadcast(
            context,
            todo.id.hashCode(),
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_DONE)
                .setData(Uri.parse("attendo-done://${todo.id}"))
                .putExtra(ReminderReceiver.EXTRA_ID, todo.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // On a locked screen only "Attendo reminder" shows; the title appears after unlocking.
        val public = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setColor(0xFF50A65C.toInt())
            .setContentTitle("Attendo reminder")
            .setContentText("Unlock to see it")
            .build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setColor(0xFF50A65C.toInt())
            .setContentTitle(todo.title)
            .setContentText("Reminder · ${todo.remindAt?.format(TimeFormat).orEmpty()}")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Mark done", done)
            .build()
        NotificationManagerCompat.from(context).notify(todo.id, NOTIFICATION_ID, notification)
    }

    fun dismiss(context: Context, id: String) = NotificationManagerCompat.from(context).cancel(id, NOTIFICATION_ID)
}
