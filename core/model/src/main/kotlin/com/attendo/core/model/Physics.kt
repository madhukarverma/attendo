package com.attendo.core.model

import kotlin.math.abs
import kotlin.math.sign

/**
 * Gesture physics from Apple's "Designing Fluid Interfaces" (WWDC 2018), in pixels and seconds.
 * Plain maths so it can be unit-tested; the animation side lives in the design system's `Motion`.
 */
object Physics {

    /** Scroll-like deceleration. Apple uses 0.998 for scrolling; 0.99 feels snappier for flicks. */
    const val NORMAL_DECELERATION = 0.998f
    const val FAST_DECELERATION = 0.99f

    /**
     * How far a gesture released at [velocity] (px/s) would travel if it decelerated like a
     * scroll view. Add it to the release position and pick the target nearest that point, so a
     * quick flick counts even when the finger moved only a little.
     */
    fun project(velocity: Float, decelerationRate: Float = NORMAL_DECELERATION): Float =
        velocity / 1000f * decelerationRate / (1f - decelerationRate)

    /** Default stiffness of the rubber band (UIScrollView's constant). */
    const val RUBBER_BAND = 0.55f

    /**
     * Where content dragged [overshoot] px past its edge is drawn: it follows the finger less
     * and less, approaching but never reaching [dimension] (the size of the scrolling area).
     */
    fun rubberband(overshoot: Float, dimension: Float, constant: Float = RUBBER_BAND): Float {
        if (dimension <= 0f) return 0f
        return overshoot * dimension * constant / (dimension + constant * abs(overshoot))
    }

    /** The finger overshoot that [rubberband] turns into [offset]: undoes it exactly. */
    fun rubberbandInverse(offset: Float, dimension: Float, constant: Float = RUBBER_BAND): Float {
        if (dimension <= 0f || offset == 0f) return 0f
        val y = abs(offset).coerceAtMost(dimension * 0.999f)
        return sign(offset) * y * dimension / (constant * (dimension - y))
    }
}
