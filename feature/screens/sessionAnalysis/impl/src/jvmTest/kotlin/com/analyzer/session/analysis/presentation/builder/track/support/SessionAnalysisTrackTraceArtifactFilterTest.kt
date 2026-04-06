package com.analyzer.session.analysis.presentation.builder.track.support

import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionAnalysisTrackTraceArtifactFilterTest {

    private val artifactFilter = SessionAnalysisTrackTraceArtifactFilter()

    @Test
    fun `smoothMinorTraceJitter keeps genuine corner arc when guide path also turns`() {
        val guideLookup = TrackMapFractionLookup.create(
            listOf(
                trackPoint(0f, 0f),
                trackPoint(10f, 0.2f),
                trackPoint(20f, 0.8f),
                trackPoint(30f, 2.1f),
                trackPoint(40f, 4.7f),
                trackPoint(50f, 9.4f),
                trackPoint(60f, 16f),
                trackPoint(70f, 24.5f),
            ),
        ) ?: error("Guide lookup should exist")
        val trace = listOf(
            tracePoint(0f, 0f, 0f),
            tracePoint(1f / 7f, 10f, 0.2f),
            tracePoint(2f / 7f, 20f, 0.8f),
            tracePoint(3f / 7f, 30f, 2.0f),
            tracePoint(4f / 7f, 40f, 4.5f),
            tracePoint(5f / 7f, 50f, 9.0f),
            tracePoint(6f / 7f, 60f, 15.5f),
            tracePoint(1f, 70f, 24f),
        )

        val unguided = artifactFilter.smoothMinorTraceJitter(
            points = trace,
            maxLateralDrift = 1.44f,
        )
        val guided = artifactFilter.smoothMinorTraceJitter(
            points = trace,
            maxLateralDrift = 1.44f,
            guideLookup = guideLookup,
        )

        assertNotEquals(trace[3], unguided[3])
        assertEquals(trace[3], guided[3])
        assertEquals(trace[4], guided[4])
        assertTrue(guided.zipWithNext().all { (start, end) -> end.x >= start.x && end.y >= start.y })
    }
}

private fun trackPoint(x: Float, y: Float): SessionAnalysisTrackMapPoint = SessionAnalysisTrackMapPoint(x = x, y = y)

private fun tracePoint(fraction: Float, x: Float, y: Float): SessionAnalysisFractionPointUi =
    SessionAnalysisFractionPointUi(
        fraction = fraction,
        x = x,
        y = y,
    )

