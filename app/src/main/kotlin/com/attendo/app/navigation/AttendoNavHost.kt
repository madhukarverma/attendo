package com.attendo.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attendo.app.AppContainer
import com.attendo.app.BuildConfig
import com.attendo.app.CsvExporter
import android.app.Activity
import androidx.compose.runtime.LaunchedEffect
import com.attendo.app.preferHighRefreshRate
import com.attendo.core.designsystem.motion.AppMotionScale
import com.attendo.core.designsystem.motion.LocalAnimationMode
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.model.AnimationMode
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.SideEffect
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.feature.home.HomeRoute
import com.attendo.feature.onboarding.OnboardingRoute
import com.attendo.feature.pomodoro.PomodoroRoute
import com.attendo.feature.calendar.CalendarScreen
import java.time.LocalDate
import com.attendo.core.data.AttendoData
import com.attendo.feature.settings.SettingsRoute
import com.attendo.feature.stats.StatsRoute
import com.attendo.feature.subjects.AddSubjectRoute
import com.attendo.feature.subjects.SubjectDetailRoute

/** All destinations in one place. Feature modules never know about each other. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val ADD_SUBJECT = "subject/add"
    const val SUBJECT_DETAIL = "subject/{id}"
    fun subjectDetail(id: String) = "subject/$id"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val POMODORO = "pomodoro"
    const val CALENDAR = "calendar/{date}"
    fun calendar(date: LocalDate) = "calendar/$date"

    /** Presented as bottom sheets rather than pushed screens. */
    val MODAL = setOf(ADD_SUBJECT)
}

/** savedStateHandle key used by the calendar screen to hand a picked date back to Home. */
private const val KEY_PICKED_DATE = "picked_date"

/** Ignores taps on a screen that is already animating away (prevents double pushes). */
private fun NavHostController.push(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route)
}

private fun NavHostController.back() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) popBackStack()
}

@Composable
fun AttendoNavHost(container: AppContainer) {
    val data = container.data
    val settings by data.settings.settings.collectAsStateWithLifecycle(initialValue = null)

    val loaded = settings ?: run {
        // Splash: CSV files are still loading (a few milliseconds).
        Box(Modifier.fillMaxSize().background(AttendoColors.Cream))
        return
    }

    val navController = rememberNavController()
    val start = remember { if (loaded.onboarded) Routes.HOME else Routes.ONBOARDING }
    val mode = loaded.animationMode
    val transitions = remember(mode) { NavTransitions(mode, Routes.MODAL) }
    // Apply the Animations setting app-wide: motion tokens, the global duration scale (NONE = 0)
    // and the display refresh rate (SMOOTH/IOS ask for the panel's highest rate).
    val activity = LocalContext.current as? Activity
    SideEffect {
        Motion.mode = mode
        AppMotionScale.enabled = mode != AnimationMode.NONE
    }
    LaunchedEffect(mode) {
        activity?.preferHighRefreshRate(mode == AnimationMode.SMOOTH || mode == AnimationMode.IOS)
    }

    @OptIn(ExperimentalFoundationApi::class)
    CompositionLocalProvider(
        LocalAnimationMode provides mode,
        // Stretch overscroll only in Android mode; iOS/Smooth use the rubber band, None nothing.
        LocalOverscrollConfiguration provides
            if (mode == AnimationMode.ANDROID) LocalOverscrollConfiguration.current else null,
    ) {
        NavHost(
            navController = navController,
            startDestination = start,
            // iOS: black backdrop so covered screens dim as they fade. Other modes: the app
            // background, so cross-fades never flash dark.
            modifier = Modifier
                .fillMaxSize()
                .background(if (mode == AnimationMode.IOS) Color.Black else AttendoColors.Cream),
            enterTransition = transitions.enter,
            exitTransition = transitions.exit,
            popEnterTransition = transitions.popEnter,
            popExitTransition = transitions.popExit,
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingRoute(
                    settings = data.settings,
                    subjects = data.subjects,
                    onFinished = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) { entry ->
                val requested by entry.savedStateHandle
                    .getStateFlow<String?>(KEY_PICKED_DATE, null)
                    .collectAsStateWithLifecycle()
                HomeRoute(
                    settings = data.settings,
                    subjects = data.subjects,
                    attendance = data.attendance,
                    onAddSubject = { navController.push(Routes.ADD_SUBJECT) },
                    onOpenSubject = { id -> navController.push(Routes.subjectDetail(id)) },
                    onOpenStats = { navController.push(Routes.STATS) },
                    onOpenSettings = { navController.push(Routes.SETTINGS) },
                    onOpenPomodoro = { navController.push(Routes.POMODORO) },
                    calendar = container.academicCalendar,
                    onOpenCalendar = { date -> navController.push(Routes.calendar(date)) },
                    requestedDate = requested?.let(LocalDate::parse),
                    onRequestedDateHandled = { entry.savedStateHandle[KEY_PICKED_DATE] = null },
                )
            }
            composable(Routes.CALENDAR) { entry ->
                val selected = entry.arguments?.getString("date")?.let(LocalDate::parse) ?: LocalDate.now()
                CalendarScreen(
                    calendar = container.academicCalendar,
                    selected = selected,
                    onPickDate = { date ->
                        // Hand the date back to Home, then return there.
                        navController.previousBackStackEntry?.savedStateHandle?.set(KEY_PICKED_DATE, date.toString())
                        navController.back()
                    },
                    onClose = { navController.back() },
                )
            }
            composable(Routes.POMODORO) {
                val context = LocalContext.current
                PomodoroRoute(
                    timer = container.pomodoroTimer,
                    pomodoro = data.pomodoro,
                    subjects = data.subjects,
                    onExportCsv = { CsvExporter.share(context, data, only = listOf(AttendoData.POMODORO_FILE)) },
                    onClose = { navController.back() },
                )
            }
            composable(Routes.ADD_SUBJECT) {
                AddSubjectRoute(subjects = data.subjects, onClose = { navController.back() })
            }
            composable(Routes.STATS) {
                StatsRoute(subjects = data.subjects, attendance = data.attendance, onClose = { navController.back() })
            }
            composable(Routes.SETTINGS) {
                val context = LocalContext.current
                SettingsRoute(
                    settings = data.settings,
                    appVersion = BuildConfig.VERSION_NAME,
                    onExportCsv = { CsvExporter.share(context, data) },
                    onClose = { navController.back() },
                )
            }
            composable(Routes.SUBJECT_DETAIL) { entry ->
                SubjectDetailRoute(
                    subjectId = entry.arguments?.getString("id").orEmpty(),
                    subjects = data.subjects,
                    attendance = data.attendance,
                    onClose = { navController.back() },
                )
            }
        }
    }
}
