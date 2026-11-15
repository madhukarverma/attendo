package com.attendo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Apple's continuous corner (the iOS "squircle"): instead of a circular arc that meets the straight
 * edge abruptly, the curve starts about 1.5 × [radius] along each edge and eases into the corner, so
 * boxes look like iOS cards and buttons. Use it like `RoundedCornerShape(radius)`; each box keeps
 * the radius it was designed with (DEC-027). When a box is too small for the full curve (a radius
 * meant to make a capsule, say), it falls back to an ordinary round corner of the same radius, so
 * capsules stay capsules.
 *
 * The curve is the one iOS 7+ uses for rounded rectangles (control points as reverse-engineered by
 * PaintCode), drawn per corner as three cubic Béziers joined by two short lines.
 */
@Immutable
class ContinuousShape(private val radius: Dp) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }
        val half = size.minDimension / 2f
        return when {
            r <= 0f -> Outline.Rectangle(size.toRect())
            r * EDGE <= half -> Outline.Generic(continuousRect(size.width, size.height, r))
            else -> Outline.Rounded(RoundRect(size.toRect(), CornerRadius(minOf(r, half))))
        }
    }

    override fun equals(other: Any?): Boolean = other is ContinuousShape && other.radius == radius

    override fun hashCode(): Int = radius.hashCode()

    override fun toString(): String = "ContinuousShape(radius=$radius)"
}

/** How far along each edge the corner curve reaches, as a multiple of the radius. */
private const val EDGE = 1.52866483f

/** Clockwise from the top edge; each corner goes from the incoming edge round to the outgoing one. */
private fun continuousRect(w: Float, h: Float, r: Float): Path = Path().apply {
    moveTo(EDGE * r, 0f)
    lineTo(w - EDGE * r, 0f)
    corner(w, 0f, -1f, 1f, swap = false, r = r) // top right
    lineTo(w, h - EDGE * r)
    corner(w, h, -1f, -1f, swap = true, r = r) // bottom right
    lineTo(EDGE * r, h)
    corner(0f, h, 1f, -1f, swap = false, r = r) // bottom left
    lineTo(0f, EDGE * r)
    corner(0f, 0f, 1f, 1f, swap = true, r = r) // top left
    close()
}

/**
 * One corner at ([cx], [cy]). In corner space, a runs along the incoming edge towards the corner and
 * b along the outgoing edge; [dx]/[dy] point into the shape, and [swap] says the incoming edge is
 * vertical.
 */
private fun Path.corner(cx: Float, cy: Float, dx: Float, dy: Float, swap: Boolean, r: Float) {
    fun x(a: Float, b: Float) = cx + (if (swap) b else a) * r * dx
    fun y(a: Float, b: Float) = cy + (if (swap) a else b) * r * dy
    cubicTo(x(1.08849323f, 0f), y(1.08849323f, 0f), x(0.86840689f, 0f), y(0.86840689f, 0f), x(0.66993427f, 0.06549600f), y(0.66993427f, 0.06549600f))
    lineTo(x(0.63149399f, 0.07491100f), y(0.63149399f, 0.07491100f))
    cubicTo(
        x(0.37282392f, 0.16905899f), y(0.37282392f, 0.16905899f),
        x(0.16906013f, 0.37282401f), y(0.16906013f, 0.37282401f),
        x(0.07491176f, 0.63149399f), y(0.07491176f, 0.63149399f),
    )
    lineTo(x(0.06549569f, 0.66993423f), y(0.06549569f, 0.66993423f))
    cubicTo(x(0f, 0.86840701f), y(0f, 0.86840701f), x(0f, 1.08849299f), y(0f, 1.08849299f), x(0f, EDGE), y(0f, EDGE))
}
