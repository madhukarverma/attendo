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

/** All destinations in one place. Feature modules never know about each other. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
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
                onAddSubject = { /* feature/subjects */ },
                onOpenStats = { /* feature/stats */ },
                onOpenSettings = { /* feature/settings */ },
            )
        }
    }
}
