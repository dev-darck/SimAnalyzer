package com.project.analyzer.ui.geometry

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackSurfaceBandsTest {

    @Test
    fun `buildTrackSurfaceBandPaths keeps surface bands for figure eight track`() {
        val centerLine = listOf(
            Offset(0f, 0f),
            Offset(6f, 6f),
            Offset(0f, 6f),
            Offset(6f, 0f),
            Offset(80f, 0f),
        )
        val leftEdge = centerLine.map { point -> Offset(point.x - 0.7f, point.y - 0.7f) }
        val rightEdge = centerLine.map { point -> Offset(point.x + 0.7f, point.y + 0.7f) }

        val bandPaths = buildTrackSurfaceBandPaths(
            centerLine = centerLine,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            gapMultiplier = 8f,
            minimumGapPx = 4f,
        )

        assertTrue(centerLine.hasTrackPathSelfIntersection())
        assertFalse(centerLine.isTrackPathClosedLoop(minimumClosurePx = 4f, gapMultiplier = 8f))
        assertEquals(3, bandPaths.size)
    }

    @Test
    fun `buildTrackSurfaceBandPaths skips teleport gap segments`() {
        val centerLine = listOf(
            Offset(0f, 0f),
            Offset(4f, 0f),
            Offset(8f, 0f),
            Offset(80f, 60f),
            Offset(84f, 60f),
        )
        val leftEdge = centerLine.map { point -> Offset(point.x, point.y - 1f) }
        val rightEdge = centerLine.map { point -> Offset(point.x, point.y + 1f) }

        val bandPaths = buildTrackSurfaceBandPaths(
            centerLine = centerLine,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            gapMultiplier = 6f,
            minimumGapPx = 6f,
        )

        assertEquals(3, bandPaths.size)
        assertFalse(centerLine.isTrackPathClosedLoop())
    }
}
