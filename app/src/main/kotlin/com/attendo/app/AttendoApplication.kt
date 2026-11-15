package com.attendo.app

import android.os.Build
import com.attendo.core.model.FeedbackDiagnostics
import com.attendo.core.social.FirestoreFeedbackSender
import com.attendo.core.social.FirebaseProject
import com.attendo.core.social.FirebaseConfig
import com.attendo.core.social.SocialService
import com.attendo.core.data.curriculum.CurriculumLoader
import com.attendo.core.backup.GoogleDriveBackup
import com.attendo.app.reminders.ReminderNotifications
import com.attendo.app.reminders.ReminderScheduler
import kotlinx.coroutines.launch
import android.app.Application
import android.widget.Toast
import com.attendo.core.data.AttendoData
import com.attendo.core.data.pomodoro.PomodoroTimer
import com.attendo.core.data.calendar.AcademicCalendarLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Manual dependency container. Kept deliberately small; it can be replaced by Hilt/Koin later
 * without changing feature modules, which only receive repository interfaces.
 */
class AppContainer(application: Application) {
    val appScope = CoroutineScope(SupervisorJob())
    val data = AttendoData(File(application.filesDir, "data"), appScope)

    /** Semester calendar bundled as calendar/academic_calendar.csv (from the official PDF). */
    val academicCalendar by lazy { AcademicCalendarLoader.bundled() }

    /** MCA curriculum bundled as curriculum/mca_semester1.csv (from the official PDF). */
    val curriculum by lazy { CurriculumLoader.bundled() }

    /** Settings › Backup › Google account: all data in the account's hidden Drive app folder. */
    val googleBackup = GoogleDriveBackup(
        context = application,
        data = data,
        scope = appScope,
        appVersion = BuildConfig.VERSION_NAME,
        extraFiles = mapOf(LaunchImageStore.FILE_NAME to LaunchImageStore.file(application)),
    )

    /** The Firebase project (Friends and feedback); none until firebase.properties is filled in at build time. */
    private val firebase = FirebaseProject(
        context = application,
        config = FirebaseConfig(
            projectId = BuildConfig.FIREBASE_PROJECT_ID,
            appId = BuildConfig.FIREBASE_APP_ID,
            apiKey = BuildConfig.FIREBASE_API_KEY,
            webClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID,
        ),
    )

    /** Friends; shows "not set up" when the build has no Firebase project. */
    val social = SocialService(
        context = application,
        project = firebase,
        scope = appScope,
        data = data,
        programme = "MCA 2026-29",
    )

    /** Settings › Feedback, read by the developer in the Firebase console (docs/FEEDBACK.md). */
    val feedback = FirestoreFeedbackSender(application, firebase, appScope)

    /** What "Include phone details" attaches to feedback. */
    val feedbackDiagnostics = FeedbackDiagnostics(
        appVersion = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE,
        androidSdk = Build.VERSION.SDK_INT,
        device = listOf(Build.MANUFACTURER.replaceFirstChar(Char::uppercaseChar), Build.MODEL)
            .distinct().joinToString(" ").take(60),
    )

    /** One alarm per pending reminder in todos.csv. */
    val reminders = ReminderScheduler(application)

    /** App-scoped so the countdown keeps going when the user leaves the timer screen. */
    val pomodoroTimer = PomodoroTimer(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), data.pomodoro)
}

class AttendoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderNotifications.createChannel(this)
        // v0.25: plain to-dos (no reminder time) become board cards, once.
        container.appScope.launch { runCatching { container.data.migratePlainTodosToBoard() } }
        // Official lecture hours for catalog subjects that don't have any yet.
        container.appScope.launch { runCatching { container.data.fillCatalogLectureHours() } }
        // A data file that couldn't be read or saved: say so instead of failing silently.
        container.appScope.launch(Dispatchers.Main) {
            container.data.storageProblems.collect { message ->
                Toast.makeText(this@AttendoApplication, message, Toast.LENGTH_LONG).show()
            }
        }
        // Re-arm alarms whenever the to-do list changes (and once at start-up).
        container.appScope.launch {
            container.data.todos.todos.collect { container.reminders.sync(it) }
        }
    }
}
