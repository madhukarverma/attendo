package com.attendo.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendo.core.designsystem.component.DeveloperCredit
import com.attendo.core.designsystem.component.LaunchArt
import com.attendo.core.designsystem.component.LaunchBackground
import com.attendo.core.designsystem.motion.IosMotion
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.AttendoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the launch screen stays up (with animations / without). */
const val SPLASH_MILLIS = 1_400L
const val SPLASH_MILLIS_INSTANT = 700L

/**
 * Launch screen on the artwork's own background colour so the two blend seamlessly: the portrait
 * fades up, the wordmark slides in, and a greyed-out developer credit sits at the bottom.
 */
@Composable
fun LogoSplash(animated: Boolean, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val art = remember { Animatable(0f) }
    val wordmark = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { art.animateTo(1f, tween(500, easing = IosMotion.Easing)) }
        launch {
            delay(if (animated) 250 else 0)
            wordmark.animateTo(1f, tween(450, easing = IosMotion.Easing))
        }
        delay(if (animated) SPLASH_MILLIS else SPLASH_MILLIS_INSTANT)
        onFinished()
    }

    Box(modifier.fillMaxSize().background(LaunchBackground)) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            LaunchArt(
                Modifier
                    .size(width = 260.dp, height = 325.dp)
                    .graphicsLayer {
                        alpha = art.value
                        translationY = (1f - art.value) * 16.dp.toPx()
                    },
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "ATTENDO",
                color = AttendoColors.Cream,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = wordmark.value
                    translationY = (1f - wordmark.value) * 20.dp.toPx()
                },
            )
            Text(
                "show up. every class.",
                color = AttendoColors.Green,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.graphicsLayer { alpha = wordmark.value },
            )
        }
        DeveloperCredit(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .graphicsLayer { alpha = wordmark.value },
        )
    }
}

@Preview
@Composable
private fun LogoSplashPreview() {
    AttendoTheme { LogoSplash(animated = true, onFinished = {}) }
}
