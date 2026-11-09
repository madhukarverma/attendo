package com.attendo.app

import android.app.Application
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

    /** App-scoped so the countdown keeps going when the user leaves the timer screen. */
    val pomodoroTimer = PomodoroTimer(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), data.pomodoro)
}

class AttendoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
