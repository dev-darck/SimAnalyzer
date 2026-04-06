package com.analyzer.session.analysis.presentation.components.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorBoundary
import com.analyzer.session.analysis.presentation.components.map.support.buildSectorBoundaries
import com.analyzer.session.analysis.presentation.components.map.support.buildSectorMarkers
import com.analyzer.session.analysis.presentation.components.map.support.limitSectorBoundaryLength
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapOverlayMarkerScale
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisTrackMapSectorsTest {

    @Test
    fun `buildSectorBoundaries uses local normal instead of skewed edge chord`() {
        val centerLine = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
        )
        val leftEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = -20f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = -20f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = -20f),
        )
        val rightEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 10f, y = 20f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 60f, y = 20f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 110f, y = 20f),
        )
        val sectors = persistentListOf(
            SessionAnalysisSectorUi(
                label = "S1",
                startTrackPosition = 0.5f,
                endTrackPosition = 1f,
                markerTrackPosition = 0.75f,
                gateNormalX = 0f,
                gateNormalY = 1f,
            ),
        )

        val boundaries = buildSectorBoundaries(
            sectors = sectors,
            centerLine = centerLine,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            fallbackHalfWidthPx = 18f,
            bounds = Rect(left = 0f, top = -20f, right = 110f, bottom = 20f),
            canvasSize = IntSize(width = 220, height = 80),
            padding = 12f,
        )

        assertEquals(1, boundaries.size)
        assertEquals(boundaries.first().start.x, boundaries.first().end.x, 0.001f)
    }

    @Test
    fun `buildSectorBoundaries prefers rendered track normal before skewed gate normal`() {
        val centerLine = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
        )
        val leftEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = -16f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = -16f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = -16f),
        )
        val rightEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 16f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 16f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 16f),
        )
        val sectors = persistentListOf(
            SessionAnalysisSectorUi(
                label = "S1",
                startTrackPosition = 0.5f,
                endTrackPosition = 1f,
                markerTrackPosition = 0.75f,
                gateNormalX = 1f,
                gateNormalY = 1f,
            ),
        )

        val boundaries = buildSectorBoundaries(
            sectors = sectors,
            centerLine = centerLine,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            fallbackHalfWidthPx = 12f,
            bounds = Rect(left = 0f, top = -16f, right = 100f, bottom = 16f),
            canvasSize = IntSize(width = 200, height = 100),
            padding = 12f,
        )

        assertEquals(1, boundaries.size)
        assertEquals(boundaries.first().start.x, boundaries.first().end.x, 0.001f)
    }

    @Test
    fun `buildSectorBoundaries anchors calibrated sector to gate center instead of normalized fraction`() {
        val centerLine = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
        )
        val sectors = persistentListOf(
            SessionAnalysisSectorUi(
                label = "S1",
                startTrackPosition = 0.2f,
                endTrackPosition = 0.7f,
                markerTrackPosition = 0.45f,
                gateCenterX = 80f,
                gateCenterY = 0f,
                gateNormalX = 1f,
                gateNormalY = 1f,
                gateHalfWidthMeters = 8f,
            ),
        )

        val boundaries = buildSectorBoundaries(
            sectors = sectors,
            centerLine = centerLine,
            leftEdge = persistentListOf(),
            rightEdge = persistentListOf(),
            fallbackHalfWidthPx = 10f,
            bounds = Rect(left = 0f, top = 0f, right = 100f, bottom = 100f),
            canvasSize = IntSize(width = 100, height = 100),
            padding = 0f,
        )

        assertEquals(1, boundaries.size)
        val midpoint = (boundaries.first().start + boundaries.first().end) * 0.5f
        assertEquals(80f, midpoint.x, 0.001f)
        assertEquals(boundaries.first().start.x, boundaries.first().end.x, 0.001f)
    }

    @Test
    fun `buildSectorBoundaries snaps nearby calibrated gate center to rendered centerline`() {
        val centerLine = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
        )
        val sectors = persistentListOf(
            SessionAnalysisSectorUi(
                label = "S1",
                startTrackPosition = 0.2f,
                endTrackPosition = 0.7f,
                markerTrackPosition = 0.45f,
                gateCenterX = 80f,
                gateCenterY = 12f,
                gateHalfWidthMeters = 8f,
            ),
        )

        val boundaries = buildSectorBoundaries(
            sectors = sectors,
            centerLine = centerLine,
            leftEdge = persistentListOf(),
            rightEdge = persistentListOf(),
            fallbackHalfWidthPx = 10f,
            bounds = Rect(left = 0f, top = 0f, right = 100f, bottom = 100f),
            canvasSize = IntSize(width = 100, height = 100),
            padding = 0f,
        )

        assertEquals(1, boundaries.size)
        val midpoint = (boundaries.first().start + boundaries.first().end) * 0.5f
        assertEquals(80f, midpoint.x, 0.001f)
        assertEquals(0f, midpoint.y, 0.001f)
    }

    @Test
    fun `buildSectorBoundaries ignores far calibrated gate center outside rendered track`() {
        val centerLine = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
        )
        val sectors = persistentListOf(
            SessionAnalysisSectorUi(
                label = "S1",
                startTrackPosition = 0.2f,
                endTrackPosition = 0.7f,
                markerTrackPosition = 0.45f,
                gateCenterX = 80f,
                gateCenterY = 80f,
                gateHalfWidthMeters = 8f,
            ),
        )

        val boundaries = buildSectorBoundaries(
            sectors = sectors,
            centerLine = centerLine,
            leftEdge = persistentListOf(),
            rightEdge = persistentListOf(),
            fallbackHalfWidthPx = 10f,
            bounds = Rect(left = 0f, top = -20f, right = 100f, bottom = 20f),
            canvasSize = IntSize(width = 100, height = 40),
            padding = 0f,
        )

        assertEquals(1, boundaries.size)
        val midpoint = (boundaries.first().start + boundaries.first().end) * 0.5f
        assertEquals(20f, midpoint.x, 0.001f)
        assertEquals(0f, midpoint.y, 0.001f)
    }

    @Test
    fun `buildSectorBoundaries uses rendered track normal at start finish seam`() {
        val centerLine = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 25f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
        )
        val leftEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = -8f, y = -4f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 42f, y = 21f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 92f, y = -4f),
        )
        val rightEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 8f, y = 4f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 58f, y = 29f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 108f, y = 4f),
        )
        val sectors = persistentListOf(
            SessionAnalysisSectorUi(
                label = "S1",
                startTrackPosition = 0f,
                endTrackPosition = 0.5f,
                markerTrackPosition = 0.25f,
                gateCenterX = 10f,
                gateCenterY = 0f,
                gateNormalX = 0f,
                gateNormalY = 1f,
                gateHalfWidthMeters = 10f,
            ),
        )

        val boundaries = buildSectorBoundaries(
            sectors = sectors,
            centerLine = centerLine,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            fallbackHalfWidthPx = 18f,
            bounds = Rect(left = 0f, top = 0f, right = 100f, bottom = 50f),
            canvasSize = IntSize(width = 100, height = 50),
            padding = 0f,
        )

        assertEquals(1, boundaries.size)
        val boundaryVector = boundaries.first().end - boundaries.first().start
        val localTangent = Offset(
            x = centerLine[1].x - centerLine[0].x,
            y = centerLine[1].y - centerLine[0].y,
        )
        assertEquals(0f, boundaryVector.x * localTangent.x + boundaryVector.y * localTangent.y, 0.001f)
        val boundaryMidpoint = (boundaries.first().start + boundaries.first().end) * 0.5f
        assertEquals(boundaryMidpoint.x * 0.5f, boundaryMidpoint.y, 0.001f)
    }

    @Test
    fun `buildSectorMarkers uses boundary normal with constant offset`() {
        val boundaries = persistentListOf(
            SessionAnalysisTrackSectorBoundary(
                label = "S1",
                start = Offset(x = 20f, y = 80f),
                end = Offset(x = 40f, y = 80f),
            ),
        )

        val markers = buildSectorMarkers(
            boundaries = boundaries,
            canvasSize = IntSize(width = 200, height = 200),
            trackCenter = Offset(x = 10f, y = 80f),
            markerDistancePx = 21f,
            markerSpacingPx = 34f,
            markerNudgePx = 10f,
        )

        assertEquals(1, markers.size)
        assertEquals("SF", markers.first().label)
        assertEquals(61f, markers.first().point.x, 0.001f)
        assertEquals(80f, markers.first().point.y, 0.001f)
    }

    @Test
    fun `resolveTrackMapOverlayMarkerScale shrinks labels in collapsed viewport`() {
        val normalScale = resolveTrackMapOverlayMarkerScale(IntSize(width = 280, height = 220))
        val collapsedScale = resolveTrackMapOverlayMarkerScale(IntSize(width = 280, height = 120))

        assertEquals(1f, normalScale, 0.001f)
        assertTrue(collapsedScale < normalScale)
        assertTrue(collapsedScale >= 0.62f)
    }

    @Test
    fun `buildSectorMarkers flips marker to the outside of the track`() {
        val boundaries = persistentListOf(
            SessionAnalysisTrackSectorBoundary(
                label = "S1",
                start = Offset(x = 20f, y = 80f),
                end = Offset(x = 40f, y = 80f),
            ),
        )

        val markers = buildSectorMarkers(
            boundaries = boundaries,
            canvasSize = IntSize(width = 200, height = 200),
            trackCenter = Offset(x = 60f, y = 80f),
            markerDistancePx = 21f,
            markerSpacingPx = 34f,
            markerNudgePx = 10f,
        )

        assertEquals(1, markers.size)
        assertEquals(14f, markers.first().point.x, 0.001f)
        assertEquals(80f, markers.first().point.y, 0.001f)
    }

    @Test
    fun `buildSectorBoundaries extends to sampled track edges for non calibrated sector`() {
        val centerLine = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
        )
        val leftEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = -18f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = -18f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = -18f),
        )
        val rightEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 18f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 18f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 18f),
        )
        val sectors = persistentListOf(
            SessionAnalysisSectorUi(
                label = "S1",
                startTrackPosition = 0.5f,
                endTrackPosition = 1f,
                markerTrackPosition = 0.75f,
                gateNormalX = 0f,
                gateNormalY = 1f,
            ),
        )

        val boundaries = buildSectorBoundaries(
            sectors = sectors,
            centerLine = centerLine,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            fallbackHalfWidthPx = 12f,
            bounds = Rect(left = 0f, top = -18f, right = 100f, bottom = 18f),
            canvasSize = IntSize(width = 200, height = 120),
            padding = 12f,
        )

        assertEquals(1, boundaries.size)
        assertTrue(boundaries.first().start.y <= -20f)
        assertTrue(boundaries.first().end.y >= 20f)
        assertTrue(boundaries.first().end.y - boundaries.first().start.y >= 40f)
    }

    @Test
    fun `buildSectorBoundaries prefers rendered edge width over wide calibration gate`() {
        val centerLine = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
        )
        val leftEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = -6f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = -6f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = -6f),
        )
        val rightEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 6f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 6f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 6f),
        )
        val sectors = persistentListOf(
            SessionAnalysisSectorUi(
                label = "S1",
                startTrackPosition = 0.5f,
                endTrackPosition = 1f,
                markerTrackPosition = 0.75f,
                gateCenterX = 50f,
                gateCenterY = 0f,
                gateHalfWidthMeters = 30f,
            ),
        )

        val boundaries = buildSectorBoundaries(
            sectors = sectors,
            centerLine = centerLine,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            fallbackHalfWidthPx = 8f,
            bounds = Rect(left = 0f, top = -40f, right = 100f, bottom = 40f),
            canvasSize = IntSize(width = 100, height = 80),
            padding = 0f,
        )

        val boundaryLength = (boundaries.first().end - boundaries.first().start).getDistance()
        assertTrue(boundaryLength < 18f)
    }

    @Test
    fun `buildSectorBoundaries clamps impossible sampled edge distance in collapsed map`() {
        val centerLine = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 0f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
        )
        val leftEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = -220f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = -220f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = -220f),
        )
        val rightEdge = persistentListOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 220f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = 220f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 220f),
        )
        val sectors = persistentListOf(
            SessionAnalysisSectorUi(
                label = "S1",
                startTrackPosition = 0.5f,
                endTrackPosition = 1f,
                markerTrackPosition = 0.75f,
                gateNormalX = 0f,
                gateNormalY = 1f,
            ),
        )

        val boundaries = buildSectorBoundaries(
            sectors = sectors,
            centerLine = centerLine,
            leftEdge = leftEdge,
            rightEdge = rightEdge,
            fallbackHalfWidthPx = 8f,
            bounds = Rect(left = 0f, top = -220f, right = 100f, bottom = 220f),
            canvasSize = IntSize(width = 220, height = 120),
            padding = 12f,
        )

        val boundaryLength = (boundaries.first().end - boundaries.first().start).getDistance()
        assertTrue(boundaryLength < 42f)
    }

    @Test
    fun `limitSectorBoundaryLength keeps focused collapsed sector line inside viewport budget`() {
        val boundaries = persistentListOf(
            SessionAnalysisTrackSectorBoundary(
                label = "S1",
                start = Offset(x = 80f, y = 0f),
                end = Offset(x = 80f, y = 120f),
            ),
        )

        val limited = boundaries.limitSectorBoundaryLength(
            canvasSize = IntSize(width = 280, height = 120),
            surfaceStrokePx = 14f,
            cameraZoom = 3.2f,
        )
        val boundaryLength = (limited.first().end - limited.first().start).getDistance()

        assertTrue(boundaryLength <= 29f)
        assertEquals(60f, (limited.first().start.y + limited.first().end.y) * 0.5f, 0.001f)
    }
}
