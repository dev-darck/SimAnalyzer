@file:Suppress("LongParameterList")

package com.analyzer.session.analysis.presentation.components.map.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorMarker
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarker
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarkerResolverInput
import com.analyzer.session.analysis.presentation.components.map.model.TrackDiagnosticPalette
import com.analyzer.session.analysis.presentation.components.map.model.TrackIssueMarker
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewCanvasState
import com.analyzer.session.analysis.presentation.components.map.resolver.overlaySignature
import com.analyzer.session.analysis.presentation.components.map.resolver.resolveCornerMarkerPlacement
import com.analyzer.session.analysis.presentation.components.map.resolver.resolveCornerMarkers
import com.analyzer.session.analysis.presentation.components.map.resolver.resolveFocusedCorner
import com.analyzer.session.analysis.presentation.components.map.resolver.resolveFocusedIssues
import com.analyzer.session.analysis.presentation.components.map.resolver.resolveIssueMarkerPlacement
import com.analyzer.session.analysis.presentation.components.map.resolver.resolveIssueMarkers
import com.analyzer.session.analysis.presentation.components.map.resolver.selectOverlayCorners
import com.analyzer.session.analysis.presentation.components.map.resolver.shortLabel
import com.analyzer.session.analysis.presentation.components.map.resolver.stableOverlayKey
import com.analyzer.session.analysis.presentation.components.map.state.SessionAnalysisTrackMapViewportState
import com.analyzer.session.analysis.presentation.components.map.state.rememberSessionAnalysisTrackMapViewportState
import com.analyzer.session.analysis.presentation.components.map.support.averageOffset
import com.analyzer.session.analysis.presentation.components.map.support.focusOverlaySettleDelayMs
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapCornerMarkerBorderColor
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapCornerMarkerFillColor
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapIssueMarkerBorderColor
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapIssueMarkerFillColor
import com.analyzer.session.analysis.presentation.components.map.support.trackCornerMarkerEdgeGapDp
import com.analyzer.session.analysis.presentation.components.map.support.trackCornerMarkerOutwardRetryDp
import com.analyzer.session.analysis.presentation.components.map.support.trackCornerMarkerSizeDp
import com.analyzer.session.analysis.presentation.components.map.support.trackCornerMarkerSpacingDp
import com.analyzer.session.analysis.presentation.components.map.support.trackCornerMarkerTangentRetryDp
import com.analyzer.session.analysis.presentation.components.map.support.trackCornerMarkerViewportMarginDp
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTrackCanvasState
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val trackCornerMarkerLimit: Int = 12

@Composable
internal fun SessionAnalysisTrackMapDiagnosticOverlay(
    trackCanvasState: SessionAnalysisTrackCanvasState,
    viewportState: SessionAnalysisTrackMapViewportState,
    activeTrackPosition: Float?,
    selectionLocked: Boolean,
) {
    val density = LocalDensity.current
    val canvasSize = viewportState.cameraState.canvasSize
    val goodColor = SimAnalyzerTheme.extended.teal
    val warningColor = SimAnalyzerTheme.extended.amber
    val criticalColor = SimAnalyzerTheme.extended.red
    val lockupColor = SimAnalyzerTheme.material.primary
    val wheelSpinColor = SimAnalyzerTheme.extended.cyan
    val neutralColor = SimAnalyzerTheme.material.onSurfaceVariant
    val leftTurnColor = SimAnalyzerTheme.extended.cyan
    val rightTurnColor = SimAnalyzerTheme.extended.amber
    val straightTurnColor = SimAnalyzerTheme.material.primary
    val palette = remember(
        goodColor,
        warningColor,
        criticalColor,
        lockupColor,
        wheelSpinColor,
        neutralColor,
        leftTurnColor,
        rightTurnColor,
        straightTurnColor,
    ) {
        TrackDiagnosticPalette(
            good = goodColor,
            warning = warningColor,
            critical = criticalColor,
            oversteer = criticalColor,
            lockup = lockupColor,
            wheelSpin = wheelSpinColor,
            neutral = neutralColor,
            leftTurn = leftTurnColor,
            rightTurn = rightTurnColor,
            straightTurn = straightTurnColor,
        )
    }
    val anchorLine = remember(
        viewportState.centerLineInView,
        viewportState.selectedTraceInView,
        viewportState.referenceTraceInView,
    ) {
        viewportState.centerLineInView
            .ifEmpty { viewportState.selectedTraceInView }
            .ifEmpty { viewportState.referenceTraceInView }
    }
    if (anchorLine.isEmpty()) return

    val trackCenter = remember(anchorLine) { anchorLine.averageOffset() }
    val candidateFocusCorner = remember(trackCanvasState.cornerMarkers, activeTrackPosition) {
        resolveFocusedCorner(
            corners = trackCanvasState.cornerMarkers,
            activeTrackPosition = activeTrackPosition,
        )
    }
    val candidateFocusIssues = remember(trackCanvasState.issueMarkers, candidateFocusCorner, activeTrackPosition) {
        resolveFocusedIssues(
            issues = trackCanvasState.issueMarkers,
            focusCorner = candidateFocusCorner,
            activeTrackPosition = activeTrackPosition,
        )
    }
    val focus by produceState(
        initialValue = TrackMapDiagnosticOverlayFocus(
            corner = candidateFocusCorner,
            issues = candidateFocusIssues,
        ),
        selectionLocked,
        candidateFocusCorner?.cornerNumber,
        candidateFocusIssues.overlaySignature(),
    ) {
        val nextFocus = TrackMapDiagnosticOverlayFocus(
            corner = candidateFocusCorner,
            issues = candidateFocusIssues,
        )
        if (selectionLocked || activeTrackPosition == null) {
            value = nextFocus
            return@produceState
        }
        delay(focusOverlaySettleDelayMs.milliseconds)
        value = nextFocus
    }
    val focusCornerNumberForOverlay = if (selectionLocked) focus.corner?.cornerNumber else null
    val visibleCornerScores = remember(trackCanvasState.cornerMarkers, focusCornerNumberForOverlay, selectionLocked) {
        trackCanvasState.cornerMarkers.selectOverlayCorners(
            focusCorner = if (selectionLocked) focus.corner else null,
            limit = trackCornerMarkerLimit,
        )
    }
    val sectorMarkerOccupiedPositions = remember(viewportState.sectorMarkersInView) {
        viewportState.sectorMarkersInView.map(SessionAnalysisTrackSectorMarker::point)
    }
    val overlayMarkerScale = viewportState.overlayMarkerScale
    val cornerMarkerSizeDp = trackCornerMarkerSizeDp * overlayMarkerScale
    val cornerMarkerRadiusPx = with(density) { cornerMarkerSizeDp.toPx() * 0.5f }
    val cornerMarkerEdgeGapPx = with(density) { trackCornerMarkerEdgeGapDp.toPx() } * overlayMarkerScale
    val cornerMarkerSpacingPx = with(density) { trackCornerMarkerSpacingDp.toPx() } * overlayMarkerScale
    val cornerMarkerTangentRetryPx = with(density) { trackCornerMarkerTangentRetryDp.toPx() } * overlayMarkerScale
    val cornerMarkerOutwardRetryPx = with(density) { trackCornerMarkerOutwardRetryDp.toPx() } * overlayMarkerScale
    val cornerMarkerViewportMarginPx = with(density) { trackCornerMarkerViewportMarginDp.toPx() } * overlayMarkerScale
    val cornerPlacement = remember(
        viewportState.surfaceStrokePx,
        viewportState.cameraState.zoom,
        cornerMarkerRadiusPx,
        cornerMarkerEdgeGapPx,
        cornerMarkerSpacingPx,
        cornerMarkerTangentRetryPx,
        cornerMarkerOutwardRetryPx,
        cornerMarkerViewportMarginPx,
    ) {
        resolveCornerMarkerPlacement(
            surfaceStrokePx = viewportState.surfaceStrokePx * viewportState.cameraState.zoom,
            markerRadiusPx = cornerMarkerRadiusPx,
            edgeGapPx = cornerMarkerEdgeGapPx,
            spacingPx = cornerMarkerSpacingPx,
            tangentRetryPx = cornerMarkerTangentRetryPx,
            outwardRetryPx = cornerMarkerOutwardRetryPx,
            viewportMarginPx = cornerMarkerViewportMarginPx,
        )
    }
    val cornerMarkers = remember(
        visibleCornerScores,
        anchorLine,
        trackCenter,
        canvasSize,
        palette,
        cornerPlacement,
        sectorMarkerOccupiedPositions,
    ) {
        resolveCornerMarkers(
            corners = visibleCornerScores,
            anchorLine = anchorLine,
            input = TrackCornerMarkerResolverInput(
                trackCenter = trackCenter,
                canvasSize = canvasSize,
                palette = palette,
                placement = cornerPlacement,
                trackLeftEdge = viewportState.leftEdgeInView,
                trackRightEdge = viewportState.rightEdgeInView,
            ),
            occupiedPositions = sectorMarkerOccupiedPositions,
        )
    }
    val issuePlacement = remember(viewportState.surfaceStrokePx, viewportState.cameraState.zoom) {
        resolveIssueMarkerPlacement(surfaceStrokePx = viewportState.surfaceStrokePx * viewportState.cameraState.zoom)
    }
    val visibleIssuesForMarkers = remember(trackCanvasState.issueMarkers, focus.issues, selectionLocked) {
        if (selectionLocked) focus.issues else emptyList()
    }
    val emphasizedIssueKeys = remember(focus.issues) {
        focus.issues.mapTo(
            linkedSetOf(),
            com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi::stableOverlayKey,
        )
    }
    val occupiedPositions = remember(sectorMarkerOccupiedPositions, cornerMarkers) {
        buildList {
            addAll(sectorMarkerOccupiedPositions)
            addAll(cornerMarkers.map(TrackCornerMarker::position))
        }
    }
    val issueMarkers = remember(
        visibleIssuesForMarkers,
        anchorLine,
        focus.corner?.cornerNumber,
        occupiedPositions,
        canvasSize,
        palette,
    ) {
        resolveIssueMarkers(
            anchorLine = anchorLine,
            issues = visibleIssuesForMarkers,
            focusCorner = focus.corner,
            canvasSize = canvasSize,
            palette = palette,
            occupiedPositions = occupiedPositions,
            placement = issuePlacement,
        )
    }
    cornerMarkers.forEach { marker ->
        SessionAnalysisTrackCornerMarker(
            marker = marker,
            isFocused = focus.corner?.cornerNumber == marker.corner.cornerNumber,
            markerSizeDp = cornerMarkerSizeDp,
        )
    }
    issueMarkers.forEach { marker ->
        SessionAnalysisTrackIssueMarker(
            marker = marker,
            emphasized = marker.issue.stableOverlayKey() in emphasizedIssueKeys,
        )
    }
}

@Composable
private fun SessionAnalysisTrackCornerMarker(
    marker: TrackCornerMarker,
    isFocused: Boolean,
    markerSizeDp: androidx.compose.ui.unit.Dp,
) {
    val density = LocalDensity.current
    val markerSizePx = with(density) { markerSizeDp.toPx() }
    val badgeOffset = IntOffset(
        x = (marker.position.x - markerSizePx * 0.5f).roundToInt(),
        y = (marker.position.y - markerSizePx * 0.5f).roundToInt(),
    )

    Surface(
        modifier = Modifier
            .offset { badgeOffset }
            .size(markerSizeDp),
        shape = SimAnalyzerTheme.corners.pill,
        color = resolveTrackMapCornerMarkerFillColor(
            accent = marker.accent,
            focused = isFocused,
        ),
        border = BorderStroke(
            width = if (isFocused) 1.4.dp else 1.dp,
            color = resolveTrackMapCornerMarkerBorderColor(
                accent = marker.accent,
                focused = isFocused,
            ),
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = marker.corner.cornerNumber.toString(),
                color = marker.accent,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SessionAnalysisTrackIssueMarker(marker: TrackIssueMarker, emphasized: Boolean) {
    val badgeOffset = IntOffset(
        x = (marker.position.x - 11f).roundToInt(),
        y = (marker.position.y - 9f).roundToInt(),
    )

    Surface(
        modifier = Modifier.offset { badgeOffset },
        shape = SimAnalyzerTheme.corners.control,
        color = resolveTrackMapIssueMarkerFillColor(
            accent = marker.accent,
            emphasized = emphasized,
        ),
        border = BorderStroke(
            1.dp,
            resolveTrackMapIssueMarkerBorderColor(
                accent = marker.accent,
                emphasized = emphasized,
            ),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = marker.issue.category.shortLabel(),
                color = marker.accent,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
        }
    }
}

@Preview
@Composable
internal fun SessionAnalysisTrackMapDiagnosticOverlayPreview() {
    val trackCanvasState = sessionAnalysisTrackMapPreviewCanvasState()
    val activePoint = sessionAnalysisTrackMapPreviewActivePoint()
    val viewportState = rememberSessionAnalysisTrackMapViewportState(
        trackCanvasState = trackCanvasState,
        cursorFraction = activePoint.fraction,
        cursorFrameId = activePoint.selectedFrameId,
        activePoint = activePoint,
        activeSample = sessionAnalysisTrackMapPreviewActiveSample(),
        selectionLocked = true,
        focusMode = false,
        canvasSize = IntSize(width = 260, height = 220),
    )

    SimAnalyzerTheme {
        Box(modifier = Modifier.size(width = 260.dp, height = 220.dp)) {
            SessionAnalysisTrackMapDiagnosticOverlay(
                trackCanvasState = trackCanvasState,
                viewportState = viewportState,
                activeTrackPosition = activePoint.trackPosition,
                selectionLocked = true,
            )
        }
    }
}
