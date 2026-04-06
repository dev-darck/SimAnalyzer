package com.analyzer.session.analysis.presentation.builder.track.line

import com.analyzer.session.analysis.presentation.builder.track.support.SessionAnalysisTrackPointSampler
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackPointUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisTrackLinePreparerTest {

    private val linePreparer = SessionAnalysisTrackLinePreparer()
    private val pointSampler = SessionAnalysisTrackPointSampler()

    @Test
    fun `buildTrackEdges keeps edge point count when center line has duplicate points`() {
        val points = listOf(
            SessionAnalysisTrackPointUi(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackPointUi(x = 20f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackPointUi(x = 20f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackPointUi(x = 35f, y = 12f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackPointUi(x = 48f, y = 24f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackPointUi(x = 60f, y = 24f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackPointUi(x = 60f, y = 24f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackPointUi(x = 72f, y = 8f, leftWidthMeters = 6f, rightWidthMeters = 6f),
        )

        val edges = linePreparer.buildTrackEdges(points)

        assertEquals(points.size, edges.first.size)
        assertEquals(points.size, edges.second.size)
    }

    @Test
    fun `sampleEvenlyByPathDistance preserves distant corner on unevenly sampled path`() {
        val points = listOf(
            SessionAnalysisTrackPointUi(x = 0f, y = 0f),
            SessionAnalysisTrackPointUi(x = 0.2f, y = 0f),
            SessionAnalysisTrackPointUi(x = 0.4f, y = 0f),
            SessionAnalysisTrackPointUi(x = 0.6f, y = 0f),
            SessionAnalysisTrackPointUi(x = 20f, y = 0f),
            SessionAnalysisTrackPointUi(x = 20f, y = 20f),
            SessionAnalysisTrackPointUi(x = 20f, y = 40f),
        )

        val sampled = pointSampler.sampleEvenlyByPathDistance(
            points = points,
            maxPoints = 4,
            xSelector = SessionAnalysisTrackPointUi::x,
            ySelector = SessionAnalysisTrackPointUi::y,
        )

        assertEquals(4, sampled.size)
        assertEquals(20f, sampled[1].x, 0.001f)
        assertEquals(0f, sampled[1].y, 0.001f)
        assertEquals(20f, sampled[2].x, 0.001f)
        assertEquals(20f, sampled[2].y, 0.001f)
        assertTrue(sampled.last().y >= 40f)
    }
}
