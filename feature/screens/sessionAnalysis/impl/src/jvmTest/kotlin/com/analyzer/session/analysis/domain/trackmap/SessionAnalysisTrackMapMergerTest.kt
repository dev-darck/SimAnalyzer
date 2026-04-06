package com.analyzer.session.analysis.domain.trackmap

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SessionAnalysisTrackMapMergerTest {

    private val merger = SessionAnalysisTrackMapMerger(
        geometry = SessionAnalysisTrackMapGeometry(),
    )

    @Test
    fun `merge keeps authored ideal line when transformed overlay is the only available ideal`() {
        val telemetryTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 1f,
        )
        val authoredTrackMap = SessionAnalysisTrackMap(
            points = telemetryTrackMap.points,
            idealPoints = listOf(
                SessionAnalysisTrackMapPoint(x = 20f, y = 2f),
                SessionAnalysisTrackMapPoint(x = 80f, y = 2f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 2f,
        )

        val merged = merger.merge(
            authoredTrackMap = authoredTrackMap,
            telemetryTrackMap = telemetryTrackMap,
        )

        assertNotNull(merged)
        assertEquals(2, merged?.idealPoints?.size)
        assertEquals(20f, merged?.idealPoints?.first()?.x ?: -1f, 0.001f)
        assertEquals(80f, merged?.idealPoints?.last()?.x ?: -1f, 0.001f)
    }

    @Test
    fun `merge keeps shorter authored ideal line when it covers only part of the lap`() {
        val telemetryTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 1f,
        )
        val authoredTrackMap = SessionAnalysisTrackMap(
            points = telemetryTrackMap.points,
            idealPoints = listOf(
                SessionAnalysisTrackMapPoint(x = 18f, y = 2f),
                SessionAnalysisTrackMapPoint(x = 32f, y = 2f),
                SessionAnalysisTrackMapPoint(x = 48f, y = 2f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 100f,
            maxY = 2f,
        )

        val merged = merger.merge(
            authoredTrackMap = authoredTrackMap,
            telemetryTrackMap = telemetryTrackMap,
        )

        assertNotNull(merged)
        assertEquals(3, merged?.idealPoints?.size)
        assertEquals(18f, merged?.idealPoints?.first()?.x ?: -1f, 0.001f)
        assertEquals(48f, merged?.idealPoints?.last()?.x ?: -1f, 0.001f)
    }

    @Test
    fun `merge keeps telemetry center line order when authored map start point is rotated`() {
        val telemetryTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 10f,
            maxY = 10f,
        )
        val authoredTrackMap = telemetryTrackMap.copy(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 10f, y = 10f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 10f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 0f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 10f, leftWidthMeters = 7f, rightWidthMeters = 7f),
            ),
        )

        val merged = merger.merge(
            authoredTrackMap = authoredTrackMap,
            telemetryTrackMap = telemetryTrackMap,
        )

        assertNotNull(merged)
        assertEquals(
            telemetryTrackMap.points.map(SessionAnalysisTrackMapPoint::x),
            merged?.points?.map(SessionAnalysisTrackMapPoint::x),
        )
        assertEquals(
            telemetryTrackMap.points.map(SessionAnalysisTrackMapPoint::y),
            merged?.points?.map(SessionAnalysisTrackMapPoint::y),
        )
    }

    @Test
    fun `merge prefers aligned authored geometry over sparse telemetry polyline`() {
        val telemetryTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 10f, leftWidthMeters = 6f, rightWidthMeters = 6f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 10f,
            maxY = 10f,
        )
        val authoredTrackMap = SessionAnalysisTrackMap(
            points = listOf(
                SessionAnalysisTrackMapPoint(x = 10f, y = 10f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 5f, y = 10f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 10f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 5f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 5f, y = 0f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 0f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 5f, leftWidthMeters = 7f, rightWidthMeters = 7f),
                SessionAnalysisTrackMapPoint(x = 10f, y = 10f, leftWidthMeters = 7f, rightWidthMeters = 7f),
            ),
            minX = 0f,
            minY = 0f,
            maxX = 10f,
            maxY = 10f,
        )

        val merged = merger.merge(
            authoredTrackMap = authoredTrackMap,
            telemetryTrackMap = telemetryTrackMap,
        )

        assertNotNull(merged)
        assertEquals(9, merged?.points?.size)
        assertEquals(0f, merged?.points?.first()?.x ?: -1f, 0.001f)
        assertEquals(0f, merged?.points?.first()?.y ?: -1f, 0.001f)
        assertEquals(5f, merged?.points?.get(1)?.x ?: -1f, 0.001f)
        assertEquals(0f, merged?.points?.get(1)?.y ?: -1f, 0.001f)
    }
}

