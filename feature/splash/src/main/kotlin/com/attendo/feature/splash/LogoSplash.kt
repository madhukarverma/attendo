package com.attendo.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendo.core.designsystem.component.BATCH_LABEL
import com.attendo.core.designsystem.component.DeveloperCredit
import com.attendo.core.designsystem.component.LaunchBackground
import com.attendo.core.designsystem.motion.Motion
import com.attendo.core.designsystem.theme.AttendoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the launch screen stays up. */
const val SPLASH_MILLIS = 1_400L

private val LaunchText = Color(0xFFF8FAED)
private val LaunchAccent = Color(0xFF50A65C)

/**
 * Launch screen: "MCA 2026-29" at the top, then the user's launch image (picked in Settings) on a
 * background sampled from the image's edges so the two blend, the wordmark, and a greyed-out
 * developer credit. Without an image it shows just the text on the default dark background.
 */
@Composable
fun LogoSplash(
    image: ImageBitmap?,
    background: Color,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reveal = remember { Animatable(0f) }
    val wordmark = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { reveal.animateTo(1f, tween(500, easing = Motion.Easing)) }
        launch {
            delay(250)
            wordmark.animateTo(1f, tween(450, easing = Motion.Easing))
        }
        delay(SPLASH_MILLIS)
        onFinished()
    }

    Box(modifier.fillMaxSize().background(background)) {
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                BATCH_LABEL,
                color = LaunchText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                modifier = Modifier.graphicsLayer { alpha = reveal.value },
            )
            Spacer(Modifier.height(16.dp))
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = reveal.value
                            translationY = (1f - reveal.value) * 16.dp.toPx()
                        },
                )
                Spacer(Modifier.height(16.dp))
            }
            Text(
                "ATTENDO",
                color = LaunchText,
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
                color = LaunchAccent,
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
    AttendoTheme { LogoSplash(image = null, background = LaunchBackground, onFinished = {}) }
}
