package com.attendo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.attendo.app.navigation.AttendoNavHost
import com.attendo.core.designsystem.theme.AttendoTheme
import com.attendo.feature.splash.LogoSplash
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val container = (application as AttendoApplication).container
        setContent {
            AttendoTheme {
                // Saveable so the logo only flashes on a real launch, not on rotation.
                var showSplash by rememberSaveable { mutableStateOf(true) }
                Box(Modifier.fillMaxSize()) {
                    AttendoNavHost(container)
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = 1.08f),
                    ) {
                        LogoSplash(onFinished = { showSplash = false })
                    }
                }
            }
        }
    }
}
