package com.attendo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicsTest {

    @Test
    fun projectionMatchesApplesFormula() {
        // (1000 px/s / 1000) · 0.998 / 0.002 = 499 px.
        assertEquals(499f, Physics.project(1000f), 0.01f)
        assertEquals(-99f, Physics.project(-1000f, Physics.FAST_DECELERATION), 0.01f)
        assertEquals(0f, Physics.project(0f), 0f)
    }

    @Test
    fun rubberBandResistsMoreTheFurtherItGoes() {
        val d = 1000f
        val small = Physics.rubberband(100f, d)
        val large = Physics.rubberband(1000f, d)
        assertEquals(100f * d * 0.55f / (d + 55f), small, 0.01f)
        assertTrue("follows less than the finger", small < 100f)
        assertTrue("each extra px moves it less", large - small < small * 9)
        assertTrue("never reaches the size of the area", Physics.rubberband(1_000_000f, d) < d)
        assertEquals(-small, Physics.rubberband(-100f, d), 0.01f)
        assertEquals(0f, Physics.rubberband(50f, 0f), 0f)
    }

    @Test
    fun rubberBandInverseUndoesIt() {
        val d = 800f
        for (overshoot in listOf(-600f, -40f, 0f, 3f, 120f, 900f)) {
            val offset = Physics.rubberband(overshoot, d)
            assertEquals(overshoot, Physics.rubberbandInverse(offset, d), 0.05f)
        }
    }
}
