package com.attendo.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.attendo.core.designsystem.theme.AttendoColors
import kotlin.random.Random

/** Scattered dots and triangles in brand accents, as in the "Good job!" screen. */
@Composable
fun Confetti(modifier: Modifier = Modifier, seed: Int = 7, count: Int = 22) {
    val pieces = remember(seed, count) {
        val r = Random(seed)
        val colors = listOf(AttendoColors.Pink, AttendoColors.Blue, AttendoColors.Yellow)
        List(count) {
            Triple(Offset(r.nextFloat(), r.nextFloat()), colors[r.nextInt(colors.size)], r.nextInt(3) to (0.012f + r.nextFloat() * 0.022f))
        }
    }
    Canvas(modifier) {
        val s = size.minDimension
        pieces.forEach { (pos, color, shape) ->
            val (kind, radius) = shape
            val c = Offset(pos.x * size.width, pos.y * size.height)
            val rr = radius * s
            if (kind == 0) {
                val p = Path().apply {
                    moveTo(c.x - rr, c.y - rr); lineTo(c.x + rr * 1.3f, c.y); lineTo(c.x - rr, c.y + rr); close()
                }
                drawPath(p, color)
            } else {
                drawCircle(color, rr, c)
            }
        }
    }
}
