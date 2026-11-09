package com.attendo.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendo.core.designsystem.component.AttendoLogo
import com.attendo.core.designsystem.motion.IosMotion
import com.attendo.core.designsystem.theme.AttendoColors
import com.attendo.core.designsystem.theme.AttendoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Total time the logo is on screen before [onFinished] fires (the caller then fades it out). */
const val SPLASH_MILLIS = 1_300L

/**
 * Brief brand flash on cold start: the logo pops in with a spring, the wordmark slides up, holds,
 * and then [onFinished] is called so the host can fade the splash away (~1.5 s in total).
 */
@Composable
fun LogoSplash(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val logoScale = remember { Animatable(0.7f) }
    val logoAlpha = remember { Animatable(0f) }
    val wordmark = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(250)) }
        launch { logoScale.animateTo(1f, IosMotion.bouncy()) }
        launch {
            delay(200)
            wordmark.animateTo(1f, tween(400, easing = IosMotion.Easing))
        }
        delay(SPLASH_MILLIS)
        onFinished()
    }

    Column(
        modifier
            .fillMaxSize()
            .background(AttendoColors.Ink),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AttendoLogo(
            Modifier
                .size(168.dp)
                .graphicsLayer {
                    alpha = logoAlpha.value
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                }
                .clip(RoundedCornerShape(40.dp)),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "ATTENDO",
            color = AttendoColors.Cream,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            modifier = Modifier.graphicsLayer {
                alpha = wordmark.value
                translationY = (1f - wordmark.value) * 24.dp.toPx()
            },
        )
        Text(
            "show up. every class.",
            color = AttendoColors.Green,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.graphicsLayer { alpha = wordmark.value },
        )
    }
}

@Preview
@Composable
private fun LogoSplashPreview() {
    AttendoTheme { LogoSplash(onFinished = {}) }
}
