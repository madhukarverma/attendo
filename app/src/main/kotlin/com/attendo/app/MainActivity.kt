package com.attendo.app

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.PausableMonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.attendo.app.navigation.AttendoNavHost
import com.attendo.core.designsystem.motion.AppMotionScale
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendo.core.designsystem.component.LaunchBackground
import com.attendo.core.model.AnimationMode
import com.attendo.core.designsystem.theme.AttendoTheme
import com.attendo.feature.splash.LogoSplash
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var recomposer: Recomposer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        AppMotionScale.systemScale =
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)

        // Own Recomposer so AppMotionScale (the in-app "Animations" switch) reaches every animation.
        // The frame clock is paused while the activity is stopped, like the default window recomposer.
        val uiContext = AndroidUiDispatcher.CurrentThread
        val frameClock = PausableMonotonicFrameClock(uiContext[MonotonicFrameClock]!!)
        val context = uiContext + frameClock + AppMotionScale
        recomposer = Recomposer(context)
        lifecycleScope.launch(context, start = CoroutineStart.UNDISPATCHED) { recomposer.runRecomposeAndApplyChanges() }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = frameClock.resume()
            override fun onStop(owner: LifecycleOwner) = frameClock.pause()
        })

        val container = (application as AttendoApplication).container
        val view = ComposeView(this).apply {
            setParentCompositionContext(recomposer)
            setContent {
                AttendoTheme {
                    val settings by container.data.settings.settings.collectAsStateWithLifecycle(initialValue = null)
                    val mode = settings?.animationMode
                    // Saveable so the launch screen only shows once per launch.
                    var showSplash by rememberSaveable { mutableStateOf(true) }
                    // "No animations" means no launch screen either: straight into the app.
                    if (mode == AnimationMode.NONE) showSplash = false
                    Box(Modifier.fillMaxSize()) {
                        AttendoNavHost(container)
                        AnimatedVisibility(
                            visible = showSplash,
                            enter = EnterTransition.None,
                            exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = 1.08f),
                        ) {
                            if (mode == null) {
                                // Settings still loading (a few ms): plain launch colour, no flash.
                                Box(Modifier.fillMaxSize().background(LaunchBackground))
                            } else {
                                LogoSplash(
                                    animated = true,
                                    onFinished = { showSplash = false },
                                )
                            }
                        }
                    }
                }
            }
        }
        setContentView(view)
    }

    override fun onDestroy() {
        recomposer.cancel()
        super.onDestroy()
    }
}
