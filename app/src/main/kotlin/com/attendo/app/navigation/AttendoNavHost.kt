package com.attendo.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.attendo.app.AppContainer
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.feature.home.HomeRoute
import com.attendo.feature.onboarding.OnboardingRoute
import com.attendo.feature.stats.StatsRoute
import com.attendo.feature.settings.SettingsRoute
import com.attendo.app.BuildConfig
import com.attendo.app.CsvExporter
import androidx.compose.ui.platform.LocalContext
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

    NavHost(navController = navController, startDestination = start) {
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
                onAddSubject = { navController.navigate(Routes.ADD_SUBJECT) },
                onOpenSubject = { id -> navController.navigate(Routes.subjectDetail(id)) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.ADD_SUBJECT) {
            AddSubjectRoute(subjects = data.subjects, onClose = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsRoute(subjects = data.subjects, attendance = data.attendance, onClose = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            val context = LocalContext.current
            SettingsRoute(
                settings = data.settings,
                appVersion = BuildConfig.VERSION_NAME,
                onExportCsv = { CsvExporter.share(context, data) },
                onClose = { navController.popBackStack() },
            )
        }
        composable(Routes.SUBJECT_DETAIL) { entry ->
            SubjectDetailRoute(
                subjectId = entry.arguments?.getString("id").orEmpty(),
                subjects = data.subjects,
                attendance = data.attendance,
                onClose = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
    }
}
