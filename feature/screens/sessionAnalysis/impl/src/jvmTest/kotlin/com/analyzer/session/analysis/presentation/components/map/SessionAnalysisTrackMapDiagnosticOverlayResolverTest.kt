package com.analyzer.session.analysis.presentation.components.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarkerResolverInput
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarkerSide
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerTurnDirection
import com.analyzer.session.analysis.presentation.components.map.model.TrackDiagnosticPalette
import com.analyzer.session.analysis.presentation.components.map.resolver.resolveCornerMarkerPlacement
import com.analyzer.session.analysis.presentation.components.map.resolver.resolveCornerMarkers
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapOverlayMarkerScale
import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class SessionAnalysisTrackMapDiagnosticOverlayResolverTest {

    private val placement = resolveCornerMarkerPlacement(surfaceStrokePx = 20f)
    private val palette = TrackDiagnosticPalette(
        good = Color.Green,
        warning = Color.Yellow,
        critical = Color.Red,
        oversteer = Color.Red,
        lockup = Color.Blue,
        wheelSpin = Color.Cyan,
        neutral = Color.Gray,
        leftTurn = Color.Cyan,
        rightTurn = Color.Magenta,
        straightTurn = Color.Gray,
    )

    @Test
    fun `resolveCornerMarkerPlacement uses collapsed marker metrics`() {
        val collapsedScale = resolveTrackMapOverlayMarkerScale(IntSize(width = 280, height = 120))
        val collapsedPlacement = resolveCornerMarkerPlacement(
            surfaceStrokePx = 20f,
            markerRadiusPx = 13f * collapsedScale,
            edgeGapPx = 16f * collapsedScale,
            spacingPx = 32f * collapsedScale,
            tangentRetryPx = 18f * collapsedScale,
            outwardRetryPx = 10f * collapsedScale,
            viewportMarginPx = 24f * collapsedScale,
        )

        assertTrue(collapsedPlacement.edgeOffsetPx < placement.edgeOffsetPx)
        assertTrue(collapsedPlacement.spacingPx < placement.spacingPx)
        assertTrue(collapsedPlacement.viewportMarginPx < placement.viewportMarginPx)
    }

    @Test
    fun `resolveCornerMarkers keeps fixed visual gap from center line on straight`() {
        val centerLine = horizontalLine(y = 50f)

        val marker = resolveCornerMarkers(
            corners = listOf(
                CornerScoreUi(
                    cornerNumber = 1,
                    score = 92,
                    trackPosition = 0.5f,
                    markerTrackPosition = 0.5f,
                ),
            ),
            anchorLine = centerLine,
            input = TrackCornerMarkerResolverInput(
                trackCenter = Offset(x = 50f, y = 20f),
                canvasSize = IntSize(width = 200, height = 160),
                palette = palette,
                placement = placement,
            ),
        ).single()

        assertEquals(TrackCornerTurnDirection.Straight, marker.turnDirection)
        assertEquals(TrackCornerMarkerSide.Outer, marker.placementSide)
        assertEquals(palette.straightTurn, marker.accent)
        assertEquals(50f, marker.position.x, 0.001f)
        assertEquals(50f + placement.baseDistancePx, marker.position.y, 0.001f)
    }

    @Test
    fun `resolveCornerMarkers uses outer side for left turn`() {
        val marker = resolveCornerMarkers(
            corners = listOf(
                CornerScoreUi(
                    cornerNumber = 2,
                    score = 90,
                    trackPosition = 0.55f,
                    markerTrackPosition = 0.55f,
                ),
            ),
            anchorLine = leftTurnLine(),
            input = TrackCornerMarkerResolverInput(
                trackCenter = Offset(x = 30f, y = 40f),
                canvasSize = IntSize(width = 220, height = 220),
                palette = palette,
                placement = placement,
            ),
        ).single()

        assertEquals(TrackCornerTurnDirection.Left, marker.turnDirection)
        assertEquals(TrackCornerMarkerSide.Outer, marker.placementSide)
        assertEquals(palette.leftTurn, marker.accent)
        assertTrue(marker.position.y > 95f)
    }

    @Test
    fun `resolveCornerMarkers uses outer side for right turn`() {
        val marker = resolveCornerMarkers(
            corners = listOf(
                CornerScoreUi(
                    cornerNumber = 3,
                    score = 90,
                    trackPosition = 0.55f,
                    markerTrackPosition = 0.55f,
                ),
            ),
            anchorLine = rightTurnLine(),
            input = TrackCornerMarkerResolverInput(
                trackCenter = Offset(x = 30f, y = 140f),
                canvasSize = IntSize(width = 220, height = 220),
                palette = palette,
                placement = placement,
            ),
        ).single()

        assertEquals(TrackCornerTurnDirection.Right, marker.turnDirection)
        assertEquals(TrackCornerMarkerSide.Outer, marker.placementSide)
        assertEquals(palette.rightTurn, marker.accent)
        assertTrue(marker.position.y < 55f)
    }

    @Test
    fun `resolveCornerMarkers falls back when preferred side hits nearby parallel track`() {
        val centerLine = listOf(
            SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 80f),
            SessionAnalysisFractionPointUi(fraction = 0.25f, x = 100f, y = 80f),
            SessionAnalysisFractionPointUi(fraction = 0.5f, x = 100f, y = 25f),
            SessionAnalysisFractionPointUi(fraction = 0.75f, x = 0f, y = 25f),
            SessionAnalysisFractionPointUi(fraction = 1f, x = 0f, y = 80f),
        )

        val marker = resolveCornerMarkers(
            corners = listOf(
                CornerScoreUi(
                    cornerNumber = 4,
                    score = 88,
                    trackPosition = 0.125f,
                    markerTrackPosition = 0.125f,
                ),
            ),
            anchorLine = centerLine,
            input = TrackCornerMarkerResolverInput(
                trackCenter = Offset(x = 50f, y = 200f),
                canvasSize = IntSize(width = 220, height = 200),
                palette = palette,
                placement = placement,
            ),
        ).single()

        assertEquals(TrackCornerMarkerSide.Inner, marker.placementSide)
        assertTrue(marker.position.y > 80f)
    }

    @Test
    fun `resolveCornerMarkers avoids occupied sector label positions`() {
        val centerLine = horizontalLine(y = 100f)

        val marker = resolveCornerMarkers(
            corners = listOf(
                CornerScoreUi(
                    cornerNumber = 5,
                    score = 84,
                    trackPosition = 0.5f,
                    markerTrackPosition = 0.5f,
                ),
            ),
            anchorLine = centerLine,
            input = TrackCornerMarkerResolverInput(
                trackCenter = Offset(x = 50f, y = 70f),
                canvasSize = IntSize(width = 220, height = 240),
                palette = palette,
                placement = placement,
            ),
            occupiedPositions = listOf(Offset(x = 50f, y = 100f + placement.baseDistancePx)),
        ).single()

        assertEquals(placement.baseDistancePx, kotlin.math.abs(marker.position.y - 100f), 0.001f)
        assertTrue(
            marker.position.testDistanceTo(Offset(x = 50f, y = 100f + placement.baseDistancePx)) >= placement.spacingPx,
        )
    }

    @Test
    fun `resolveCornerMarkers anchors outer side to actual track edge when local width is larger than average`() {
        val centerLine = horizontalLine(y = 100f)
        val leftEdge = horizontalLine(y = 88f)
        val rightEdge = horizontalLine(y = 124f)

        val marker = resolveCornerMarkers(
            corners = listOf(
                CornerScoreUi(
                    cornerNumber = 6,
                    score = 86,
                    trackPosition = 0.5f,
                    markerTrackPosition = 0.5f,
                ),
            ),
            anchorLine = centerLine,
            input = TrackCornerMarkerResolverInput(
                trackCenter = Offset(x = 50f, y = 70f),
                canvasSize = IntSize(width = 220, height = 260),
                palette = palette,
                placement = placement,
                trackLeftEdge = leftEdge,
                trackRightEdge = rightEdge,
            ),
        ).single()

        assertEquals(TrackCornerMarkerSide.Outer, marker.placementSide)
        assertEquals(50f, marker.position.x, 0.001f)
        assertEquals(124f + placement.edgeOffsetPx, marker.position.y, 0.001f)
    }

    private fun horizontalLine(y: Float): List<SessionAnalysisFractionPointUi> = listOf(
        SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = y),
        SessionAnalysisFractionPointUi(fraction = 0.5f, x = 50f, y = y),
        SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = y),
    )

    private fun leftTurnLine(): List<SessionAnalysisFractionPointUi> = listOf(
        SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 100f),
        SessionAnalysisFractionPointUi(fraction = 0.3f, x = 30f, y = 100f),
        SessionAnalysisFractionPointUi(fraction = 0.45f, x = 60f, y = 100f),
        SessionAnalysisFractionPointUi(fraction = 0.55f, x = 85f, y = 95f),
        SessionAnalysisFractionPointUi(fraction = 0.65f, x = 100f, y = 70f),
        SessionAnalysisFractionPointUi(fraction = 0.8f, x = 100f, y = 40f),
        SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 0f),
    )

    private fun rightTurnLine(): List<SessionAnalysisFractionPointUi> = listOf(
        SessionAnalysisFractionPointUi(fraction = 0f, x = 0f, y = 50f),
        SessionAnalysisFractionPointUi(fraction = 0.3f, x = 30f, y = 50f),
        SessionAnalysisFractionPointUi(fraction = 0.45f, x = 60f, y = 50f),
        SessionAnalysisFractionPointUi(fraction = 0.55f, x = 85f, y = 55f),
        SessionAnalysisFractionPointUi(fraction = 0.65f, x = 100f, y = 80f),
        SessionAnalysisFractionPointUi(fraction = 0.8f, x = 100f, y = 110f),
        SessionAnalysisFractionPointUi(fraction = 1f, x = 100f, y = 140f),
    )

    private fun Offset.testDistanceTo(other: Offset): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt((dx * dx) + (dy * dy))
    }
}
