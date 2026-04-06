package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMapCornerZoneTest {

    @Test
    fun `wrap around zone contains samples on both sides of start finish`() {
        val zone = TrackMapCornerZone(
            cornerNumber = 9,
            startTrackPosition = 0.96f,
            endTrackPosition = 0.04f,
            apexTrackPosition = 0.99f,
            peakCurvature = 0.03f,
        )

        assertTrue(zone.wrapsAroundStartFinish)
        assertTrue(zone.contains(0.98f))
        assertTrue(zone.contains(0.02f))
        assertEquals(0.08f, zone.spanFraction(), 0.0001f)
    }
}
