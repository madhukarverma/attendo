package com.attendo.app

import android.content.Intent
import com.attendo.app.reminders.ReminderNotifications
import kotlinx.coroutines.flow.MutableStateFlow
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
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.AttendoPalette
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.attendo.core.designsystem.theme.parseHexColor
import com.attendo.feature.splash.LogoSplash
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var recomposer: Recomposer

    /** Screen requested by the launching intent (a reminder notification opens "todo"). */
    private val openRequest = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openRequest.value = intent.openTarget()
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
                val settings by container.data.settings.settings.collectAsStateWithLifecycle(initialValue = null)
                // Theme: Light / Dark / Custom palette from settings.csv, applied app-wide.
                val palette = settings?.let { AttendoPalette.forMode(it.theme, it.customColors) } ?: AttendoPalette.Light
                SideEffect { AttendoColors.palette = palette }
                LaunchedEffect(palette.isDark) {
                    val bars = if (palette.isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                }
                AttendoTheme {
                    val mode = settings?.animationMode
                    // Saveable so the launch screen only shows once per launch.
                    var showSplash by rememberSaveable { mutableStateOf(true) }
                    // "No animations" means no launch screen either: straight into the app.
                    if (mode == AnimationMode.NONE) showSplash = false
                    // The user's launch image (picked in Settings), decoded off the main thread.
                    val wantsImage = settings?.launchImage == true
                    val image by produceState<ImageBitmap?>(null, wantsImage) {
                        value = if (wantsImage) LaunchImageStore.load(this@MainActivity) else null
                    }
                    val ready = settings != null && (!wantsImage || image != null)
                    val launchBg = settings?.launchBackground?.let(::parseHexColor) ?: LaunchBackground
                    Box(Modifier.fillMaxSize()) {
                        val open by openRequest.collectAsStateWithLifecycle()
                        AttendoNavHost(container, openRequest = open, onOpenHandled = { openRequest.value = null })
                        AnimatedVisibility(
                            visible = showSplash,
                            enter = EnterTransition.None,
                            exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = 1.08f),
                        ) {
                            if (!ready) {
                                // Settings/image still loading (a few ms): plain launch colour, no flash.
                                Box(Modifier.fillMaxSize().background(launchBg))
                            } else {
                                LogoSplash(
                                    image = image,
                                    background = launchBg,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.openTarget()?.let { openRequest.value = it }
    }

    private fun Intent?.openTarget(): String? =
        this?.data?.takeIf { it.scheme == ReminderNotifications.OPEN_TODO_URI.scheme }?.host

    override fun onDestroy() {
        recomposer.cancel()
        super.onDestroy()
    }
}
