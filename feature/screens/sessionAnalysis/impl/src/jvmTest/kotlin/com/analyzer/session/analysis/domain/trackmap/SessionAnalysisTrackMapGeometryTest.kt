package com.analyzer.session.analysis.domain.trackmap

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionAnalysisTrackMapGeometryTest {

    private val geometry = SessionAnalysisTrackMapGeometry()

    @Test
    fun `projectOverlay keeps ideal line on unevenly sampled authored center line`() {
        val sourceCenterLine = listOf(
            SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 25f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 75f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
        )
        val authoredCenterLine = buildList {
            repeat(80) { index ->
                val fraction = index.toFloat() / 79f
                add(
                    SessionAnalysisTrackMapPoint(
                        x = fraction * 20f,
                        y = 0f,
                        leftWidthMeters = 6f,
                        rightWidthMeters = 6f,
                    ),
                )
            }
            repeat(20) { index ->
                val fraction = index.toFloat() / 19f
                add(
                    SessionAnalysisTrackMapPoint(
                        x = 20f + fraction * 80f,
                        y = 0f,
                        leftWidthMeters = 6f,
                        rightWidthMeters = 6f,
                    ),
                )
            }
        }
        val overlayPoints = listOf(
            SessionAnalysisTrackMapPoint(x = 0f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 20f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 40f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 60f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 80f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 100f, y = 2f),
        )

        val projected = geometry.projectOverlay(
            overlayPoints = overlayPoints,
            sourceCenterLine = sourceCenterLine,
            authoredCenterLine = authoredCenterLine,
        )

        assertEquals(overlayPoints.size, projected.size)
        assertTrue(projected.first().x <= 2f)
        assertTrue(projected.last().x >= 98f)
        assertTrue(projected.zipWithNext().all { (previous, current) -> current.x > previous.x })
    }

    @Test
    fun `transformOverlay maps authored ideal line through similarity transform fallback`() {
        val sourceCenterLine = listOf(
            SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
        )
        val targetCenterLine = listOf(
            SessionAnalysisTrackMapPoint(x = 10f, y = 20f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 10f, y = 70f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 10f, y = 120f, leftWidthMeters = 6f, rightWidthMeters = 6f),
        )
        val overlayPoints = listOf(
            SessionAnalysisTrackMapPoint(x = 20f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 80f, y = 2f),
        )

        val transformed = geometry.transformOverlay(
            overlayPoints = overlayPoints,
            sourceCenterLine = sourceCenterLine,
            targetCenterLine = targetCenterLine,
        )

        assertEquals(2, transformed.size)
        assertTrue(transformed.all { point -> point.x in 4f..16f })
        assertTrue(kotlin.math.abs(transformed.first().x - transformed.last().x) <= 0.001f)
        assertTrue(transformed.first().y in 38f..42f)
        assertTrue(transformed.last().y in 98f..102f)
    }

    @Test
    fun `projectOverlay keeps shorter ideal line that covers only part of the lap`() {
        val sourceCenterLine = listOf(
            SessionAnalysisTrackMapPoint(x = 0f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 50f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
            SessionAnalysisTrackMapPoint(x = 100f, y = 0f, leftWidthMeters = 6f, rightWidthMeters = 6f),
        )
        val authoredCenterLine = sourceCenterLine
        val overlayPoints = listOf(
            SessionAnalysisTrackMapPoint(x = 18f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 32f, y = 2f),
            SessionAnalysisTrackMapPoint(x = 48f, y = 2f),
        )

        val projected = geometry.projectOverlay(
            overlayPoints = overlayPoints,
            sourceCenterLine = sourceCenterLine,
            authoredCenterLine = authoredCenterLine,
        )

        assertEquals(overlayPoints.size, projected.size)
        assertTrue(projected.first().x in 16f..20f)
        assertTrue(projected.last().x in 46f..50f)
        assertTrue(projected.all { point -> point.y in 1.5f..2.5f })
    }
}

