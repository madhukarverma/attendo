package com.attendo.app

import android.app.Application
import com.attendo.core.data.AttendoData
import com.attendo.core.data.pomodoro.PomodoroTimer
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
