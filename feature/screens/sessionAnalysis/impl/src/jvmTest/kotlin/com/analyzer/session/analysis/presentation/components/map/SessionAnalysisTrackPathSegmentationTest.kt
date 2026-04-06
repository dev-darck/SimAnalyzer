package com.analyzer.session.analysis.presentation.components.map

import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.components.map.path.segmentTrackPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisTrackPathSegmentationTest {

    @Test
    fun `segmentTrackPath splits impossible internal jump into separate segments`() {
        val path = segmentTrackPath(
            points = listOf(
                Offset(0f, 0f),
                Offset(10f, 0f),
                Offset(20f, 0f),
                Offset(30f, 0f),
                Offset(280f, 240f),
                Offset(40f, 0f),
                Offset(50f, 0f),
                Offset(60f, 0f),
            ),
            closed = false,
        )

        assertEquals(2, path.segments.size)
        assertFalse(path.closeLoop)
        assertEquals(4, path.segments.first().size)
        assertEquals(3, path.segments.last().size)
    }

    @Test
    fun `segmentTrackPath keeps clean closed loop as single segment`() {
        val path = segmentTrackPath(
            points = listOf(
                Offset(0f, 0f),
                Offset(10f, 0f),
                Offset(20f, 0f),
                Offset(20f, 10f),
                Offset(20f, 20f),
                Offset(10f, 20f),
                Offset(0f, 20f),
                Offset(0f, 10f),
            ),
            closed = true,
        )

        assertEquals(1, path.segments.size)
        assertTrue(path.closeLoop)
        assertEquals(8, path.segments.first().size)
    }

    @Test
    fun `segmentTrackPath keeps compressed corner samples in collapsed viewport`() {
        val points = listOf(
            Offset(0.00f, 0.00f),
            Offset(0.12f, 0.00f),
            Offset(0.24f, 0.00f),
            Offset(0.28f, 0.08f),
            Offset(0.30f, 0.18f),
            Offset(0.30f, 0.30f),
        )

        val path = segmentTrackPath(
            points = points,
            closed = false,
        )

        assertEquals(1, path.segments.size)
        assertEquals(points.size, path.segments.first().size)
    }

    @Test
    fun `segmentTrackPath still removes exact duplicate points`() {
        val path = segmentTrackPath(
            points = listOf(
                Offset(0f, 0f),
                Offset(0f, 0f),
                Offset(1f, 0f),
                Offset(1f, 0f),
                Offset(2f, 0f),
            ),
            closed = false,
        )

        assertEquals(1, path.segments.size)
        assertEquals(3, path.segments.first().size)
    }

    @Test
    fun `segmentTrackPath splits forced telemetry fraction gaps`() {
        val path = segmentTrackPath(
            points = listOf(
                Offset(0f, 0f),
                Offset(1f, 0f),
                Offset(2f, 0f),
                Offset(3f, 0f),
            ),
            closed = false,
            breakIndices = setOf(2),
        )

        assertEquals(2, path.segments.size)
        assertEquals(2, path.segments.first().size)
        assertEquals(2, path.segments.last().size)
    }
}
