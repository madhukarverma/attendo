package com.attendo.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Line icons drawn for Attendo on a 24-unit grid with a 2-unit stroke, for the marks that used to
 * be text symbols (✓ ✕ ↺ ↓ ↷). Tint them with `Icon(…, tint = …)` like any Material icon; use
 * Material's `Icons.Filled` set for everything it already has.
 */
object AttendoIcons {
    /** Present. */
    val Check: ImageVector by lazy { line("Check") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) } }

    /** Absent. */
    val Cross: ImageVector by lazy {
        line("Cross") {
            moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f)
            moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f)
        }
    }

    /** No class (cancelled): a circle with a slash. */
    val NoClass: ImageVector by lazy {
        line("NoClass") {
            circle(12f, 12f, 8.5f)
            moveTo(6f, 6f); lineTo(18f, 18f)
        }
    }

    /** Not marked yet: an empty circle. */
    val NotMarked: ImageVector by lazy { line("NotMarked") { circle(12f, 12f, 7f) } }

    /** Undo: an arrow curving back to the left. */
    val Undo: ImageVector by lazy {
        line("Undo") {
            moveTo(9f, 14f); lineTo(4f, 9f); lineTo(9f, 4f)
            moveTo(4f, 9f); lineTo(14.5f, 9f)
            arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 14.5f, y1 = 20f)
            lineTo(11f, 20f)
        }
    }

    /** Down arrow (swipe down = no class). */
    val ArrowDown: ImageVector by lazy {
        line("ArrowDown") {
            moveTo(12f, 4f); lineTo(12f, 20f)
            moveTo(5f, 13f); lineTo(12f, 20f); lineTo(19f, 13f)
        }
    }

    /** A speech bubble, for feedback. */
    val Feedback: ImageVector by lazy {
        line("Feedback") {
            moveTo(7f, 4f); lineTo(17f, 4f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 20f, y1 = 7f)
            lineTo(20f, 14f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 17f, y1 = 17f)
            lineTo(10f, 17f); lineTo(5f, 21f); lineTo(5.5f, 17f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 4f, y1 = 14f)
            lineTo(4f, 7f)
            arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 7f, y1 = 4f)
            close()
            moveTo(8.5f, 10.5f); lineTo(15.5f, 10.5f)
        }
    }

    /** Warning triangle with an exclamation mark, for error messages. */
    val Alert: ImageVector by lazy {
        line("Alert") {
            moveTo(12f, 3.5f); lineTo(21f, 19.5f); lineTo(3f, 19.5f); close()
            moveTo(12f, 9.5f); lineTo(12f, 13.5f)
            moveTo(12f, 16.5f); lineTo(12f, 16.6f)
        }
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        close()
    }

    private fun line(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
            .build()
}
