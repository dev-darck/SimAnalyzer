package com.project.analyzer.ui.geometry

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackPathSafetyTest {

    @Test
    fun `detects self intersection on figure eight polyline`() {
        val points = listOf(
            Offset(0f, 0f),
            Offset(6f, 6f),
            Offset(0f, 6f),
            Offset(6f, 0f),
        )

        assertTrue(points.hasTrackPathSelfIntersection())
    }

    @Test
    fun `does not flag simple closed loop as self intersecting`() {
        val points = listOf(
            Offset(0f, 0f),
            Offset(10f, 0f),
            Offset(10f, 6f),
            Offset(0f, 6f),
        )

        assertFalse(points.hasTrackPathSelfIntersection(closed = true))
    }
}
