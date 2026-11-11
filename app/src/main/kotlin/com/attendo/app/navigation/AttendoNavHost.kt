package com.attendo.app.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.attendo.app.CsvImporter
import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.attendo.app.AppContainer
import com.attendo.app.BuildConfig
import com.attendo.app.CsvExporter
import com.attendo.app.LaunchImageStore
import com.attendo.app.preferHighRefreshRate
import com.attendo.core.data.AttendoData
import com.attendo.core.designsystem.component.AttendoTabBar
import com.attendo.core.designsystem.component.StatsGlyph
import com.attendo.core.designsystem.component.TabBarClearance
import com.attendo.core.designsystem.component.TabItem
import com.attendo.core.designsystem.component.TimerGlyph
import com.attendo.core.designsystem.motion.AppMotionScale
import com.attendo.core.designsystem.motion.LocalAnimationMode
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.model.AnimationMode
import com.attendo.feature.attendance.AttendanceRoute
import com.attendo.feature.calendar.CalendarScreen
import com.attendo.feature.dashboard.DashboardRoute
import com.attendo.feature.onboarding.OnboardingRoute
import com.attendo.feature.pomodoro.PomodoroRoute
import com.attendo.feature.settings.SettingsRoute
import com.attendo.feature.stats.StatsRoute
import com.attendo.feature.subjects.AddSubjectRoute
import com.attendo.feature.subjects.SubjectDetailRoute
import com.attendo.feature.tasks.TasksPage
import com.attendo.feature.tasks.TasksRoute
import kotlinx.coroutines.launch
import java.time.LocalDate

/** All destinations in one place. Feature modules never know about each other. */
object Routes {
    const val ONBOARDING = "onboarding"

    // Tabs (bottom bar), in bar order.
    /** Summary of every section. */
    const val HOME = "home"
    /** Daily attendance marking (the main screen before v0.19). */
    const val ATTENDANCE = "attendance"
    const val TODO = "todo"
    const val POMODORO = "pomodoro"
    const val STATS = "stats"

    // Pushed screens.
    const val ADD_SUBJECT = "subject/add"
    const val SUBJECT_DETAIL = "subject/{id}"
    fun subjectDetail(id: String) = "subject/$id"
    const val SETTINGS = "settings"
    const val CALENDAR = "calendar/{date}"
    fun calendar(date: LocalDate) = "calendar/$date"

    val TABS = listOf(HOME, ATTENDANCE, TODO, POMODORO, STATS)

    /** Presented as bottom sheets rather than pushed screens. */
    val MODAL = setOf(ADD_SUBJECT)
}

private val TabIconSize = 22.dp

private val Tabs = listOf(
    TabItem(Routes.HOME, "Home") { Icon(Icons.Filled.Home, contentDescription = null, tint = it, modifier = Modifier.size(TabIconSize)) },
    TabItem(Routes.ATTENDANCE, "Classes") {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = it, modifier = Modifier.size(TabIconSize))
    },
    TabItem(Routes.TODO, "Tasks") {
        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = it, modifier = Modifier.size(TabIconSize))
    },
    TabItem(Routes.POMODORO, "Timer") { TimerGlyph(color = it, size = TabIconSize) },
    TabItem(Routes.STATS, "Stats") { StatsGlyph(color = it, size = TabIconSize) },
)

/** savedStateHandle key used by the calendar screen to hand a picked date to the Classes tab. */
private const val KEY_PICKED_DATE = "picked_date"

/** Ignores taps on a screen that is already animating away (prevents double pushes). */
private fun NavHostController.push(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navigate(route)
}

private fun NavHostController.back() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) popBackStack()
}

/**
 * Standard bottom-navigation behaviour: one copy of each tab on top of Home, each tab keeps its
 * state (scroll, selected day) when you come back, and Back from any tab returns to Home.
 */
private fun NavHostController.switchTab(route: String) {
    if (currentBackStackEntry?.destination?.route == route) return
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * @param openRequest a screen to open, e.g. "todo" when a reminder notification is tapped;
 * [onOpenHandled] clears it once navigated.
 */
@Composable
fun AttendoNavHost(container: AppContainer, openRequest: String? = null, onOpenHandled: () -> Unit = {}) {
    val data = container.data
    val settings by data.settings.settings.collectAsStateWithLifecycle(initialValue = null)

    val loaded = settings ?: run {
        // Splash: CSV files are still loading (a few milliseconds).
        Box(Modifier.fillMaxSize().background(AttendoColors.Cream))
        return
    }

    val navController = rememberNavController()
    var tasksPage by remember { mutableStateOf<TasksPage?>(null) }
    val start = remember { if (loaded.onboarded) Routes.HOME else Routes.ONBOARDING }
    val mode = loaded.animationMode
    val transitions = remember(mode) { NavTransitions(mode, Routes.MODAL, Routes.TABS.toSet()) }
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
    LaunchedEffect(openRequest) {
        if (openRequest == Routes.TODO && loaded.onboarded) {
            // A reminder notification: open the Tasks tab on Reminders.
            tasksPage = TasksPage.REMINDERS
            navController.switchTab(Routes.TODO)
        }
        if (openRequest != null) onOpenHandled()
    }
    val current by navController.currentBackStackEntryAsState()
    val currentRoute = current?.destination?.route

    @OptIn(ExperimentalFoundationApi::class)
    CompositionLocalProvider(
        LocalAnimationMode provides mode,
        // Stretch overscroll only in Android mode; iOS/Smooth use the rubber band, None nothing.
        LocalOverscrollConfiguration provides
            if (mode == AnimationMode.ANDROID) LocalOverscrollConfiguration.current else null,
    ) {
        Box(Modifier.fillMaxSize()) {
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
                        googleBackup = container.googleBackup,
                        onFinished = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        },
                    )
                }
                composable(Routes.HOME) {
                    DashboardRoute(
                        settings = data.settings,
                        subjects = data.subjects,
                        attendance = data.attendance,
                        todos = data.todos,
                        habits = data.habits,
                        kanban = data.kanban,
                        pomodoro = data.pomodoro,
                        timer = container.pomodoroTimer,
                        calendar = container.academicCalendar,
                        onOpenAttendance = { navController.switchTab(Routes.ATTENDANCE) },
                        onOpenChecklist = {
                            tasksPage = TasksPage.CHECKLIST
                            navController.switchTab(Routes.TODO)
                        },
                        onOpenBoard = {
                            tasksPage = TasksPage.BOARD
                            navController.switchTab(Routes.TODO)
                        },
                        onOpenReminders = {
                            tasksPage = TasksPage.REMINDERS
                            navController.switchTab(Routes.TODO)
                        },
                        onOpenPomodoro = { navController.switchTab(Routes.POMODORO) },
                        onOpenCalendar = { navController.push(Routes.calendar(LocalDate.now())) },
                        onOpenSettings = { navController.push(Routes.SETTINGS) },
                    )
                }
                composable(Routes.ATTENDANCE) { entry ->
                    val requested by entry.savedStateHandle
                        .getStateFlow<String?>(KEY_PICKED_DATE, null)
                        .collectAsStateWithLifecycle()
                    AttendanceRoute(
                        settings = data.settings,
                        subjects = data.subjects,
                        attendance = data.attendance,
                        onAddSubject = { navController.push(Routes.ADD_SUBJECT) },
                        onOpenSubject = { id -> navController.push(Routes.subjectDetail(id)) },
                        calendar = container.academicCalendar,
                        onOpenCalendar = { date -> navController.push(Routes.calendar(date)) },
                        requestedDate = requested?.let(LocalDate::parse),
                        onRequestedDateHandled = { entry.savedStateHandle[KEY_PICKED_DATE] = null },
                    )
                }
                composable(Routes.TODO) {
                    TasksRoute(
                        todos = data.todos,
                        kanban = data.kanban,
                        habits = data.habits,
                        bottomPadding = TabBarClearance,
                        requestedPage = tasksPage,
                        onRequestedPageHandled = { tasksPage = null },
                    )
                }
                composable(Routes.POMODORO) {
                    val context = LocalContext.current
                    PomodoroRoute(
                        timer = container.pomodoroTimer,
                        pomodoro = data.pomodoro,
                        subjects = data.subjects,
                        onExportCsv = { CsvExporter.share(context, data, only = listOf(AttendoData.POMODORO_FILE)) },
                        onClose = null,
                    )
                }
                composable(Routes.STATS) {
                    StatsRoute(subjects = data.subjects, attendance = data.attendance, onClose = null)
                }
                composable(Routes.CALENDAR) { entry ->
                    val selected = entry.arguments?.getString("date")?.let(LocalDate::parse) ?: LocalDate.now()
                    val todos by data.todos.todos.collectAsStateWithLifecycle(initialValue = emptyList())
                    CalendarScreen(
                        calendar = container.academicCalendar,
                        selected = selected,
                        todos = todos,
                        onPickDate = { date ->
                            val previous = navController.previousBackStackEntry
                            if (previous?.destination?.route == Routes.ATTENDANCE) {
                                // Opened from Classes: hand the date back and return there.
                                previous.savedStateHandle[KEY_PICKED_DATE] = date.toString()
                                navController.back()
                            } else {
                                // Opened from Home: go to the Classes tab on that day.
                                navController.back()
                                navController.switchTab(Routes.ATTENDANCE)
                                navController.currentBackStackEntry?.savedStateHandle?.set(KEY_PICKED_DATE, date.toString())
                            }
                        },
                        onClose = { navController.back() },
                    )
                }
                composable(Routes.ADD_SUBJECT) {
                    AddSubjectRoute(subjects = data.subjects, onClose = { navController.back() })
                }
                composable(Routes.SETTINGS) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    SettingsRoute(
                        settings = data.settings,
                        appVersion = BuildConfig.VERSION_NAME,
                        onExportCsv = { CsvExporter.share(context, data) },
                        onClose = { navController.back() },
                        onPickLaunchImage = { uri ->
                            scope.launch {
                                val edge = LaunchImageStore.save(context, uri)
                                data.settings.setLaunchImage(enabled = edge != null, backgroundHex = edge)
                            }
                        },
                        onRemoveLaunchImage = {
                            scope.launch {
                                LaunchImageStore.delete(context)
                                data.settings.setLaunchImage(enabled = false, backgroundHex = null)
                            }
                        },
                        googleBackup = container.googleBackup,
                        onPlanCsvImport = { uris -> data.planCsvImport(CsvImporter.read(context, uris)) },
                        onApplyCsvImport = { plan -> data.applyCsvImport(plan) },
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

            // One tab bar for the whole app, shown on tab screens; it slides away for pushed screens.
            AnimatedVisibility(
                visible = currentRoute in Routes.TABS,
                enter = slideInVertically(Motion.push()) { it } + fadeIn(Motion.push()),
                exit = slideOutVertically(Motion.push()) { it } + fadeOut(Motion.push()),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                AttendoTabBar(
                    tabs = Tabs,
                    selectedRoute = currentRoute,
                    onSelect = navController::switchTab,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}
