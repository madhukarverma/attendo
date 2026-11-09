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
import com.attendo.core.designsystem.motion.LocalIosMotion
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.feature.home.HomeRoute
import com.attendo.feature.onboarding.OnboardingRoute
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

    /** Presented as bottom sheets rather than pushed screens. */
    val MODAL = setOf(ADD_SUBJECT)
}

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
    val transitions = remember(loaded.iosMotion) { NavTransitions(loaded.iosMotion, Routes.MODAL) }

    CompositionLocalProvider(LocalIosMotion provides loaded.iosMotion) {
        NavHost(
            navController = navController,
            startDestination = start,
            // Black backdrop: covered screens fade towards it, giving the iOS dimming effect.
            modifier = Modifier.fillMaxSize().background(Color.Black),
            enterTransition = transitions.enter,
            exitTransition = transitions.exit,
            popEnterTransition = transitions.popEnter,
            popExitTransition = transitions.popExit,
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingRoute(
                    settings = data.settings,
                    onFinished = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeRoute(
                    settings = data.settings,
                    subjects = data.subjects,
                    attendance = data.attendance,
                    onAddSubject = { navController.push(Routes.ADD_SUBJECT) },
                    onOpenSubject = { id -> navController.push(Routes.subjectDetail(id)) },
                    onOpenStats = { navController.push(Routes.STATS) },
                    onOpenSettings = { navController.push(Routes.SETTINGS) },
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
