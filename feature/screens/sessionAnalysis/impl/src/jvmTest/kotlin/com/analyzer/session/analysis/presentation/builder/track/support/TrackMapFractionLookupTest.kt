package com.analyzer.session.analysis.presentation.builder.track.support

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackMapFractionLookupTest {

    @Test
    fun `create preserves incomplete fraction override range`() {
        val lookup = TrackMapFractionLookup.create(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 30f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f),
                SessionAnalysisTrackMapPoint(x = 80f, y = 0f),
            ),
            fractionsOverride = listOf(0.3f, 0.5f, 0.8f),
        )

        assertEquals(30f, lookup?.samplePositionAt(0.3f)?.first ?: 0f, 0.001f)
        assertEquals(50f, lookup?.samplePositionAt(0.5f)?.first ?: 0f, 0.001f)
        assertEquals(80f, lookup?.samplePositionAt(0.8f)?.first ?: 0f, 0.001f)
    }
}

