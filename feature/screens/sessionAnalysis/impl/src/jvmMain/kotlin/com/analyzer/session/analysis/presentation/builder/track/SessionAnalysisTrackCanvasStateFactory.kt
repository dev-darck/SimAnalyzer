@file:Suppress("MaximumLineLength")

package com.analyzer.session.analysis.presentation.builder.track

import com.analyzer.session.analysis.presentation.builder.lap.resolveLapFractionResolution
import com.analyzer.session.analysis.presentation.builder.track.line.SessionAnalysisTrackLinePreparer
import com.analyzer.session.analysis.presentation.builder.track.trace.SessionAnalysisTrackCanvasTraceBuilder
import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightCategoryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTrackCanvasBounds
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTrackCanvasState
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal object SessionAnalysisTrackCanvasStateFactory {

    private val linePreparer: SessionAnalysisTrackLinePreparer = SessionAnalysisTrackLinePreparer()
    private val traceBuilder: SessionAnalysisTrackCanvasTraceBuilder = SessionAnalysisTrackCanvasTraceBuilder()
    private val trackDiagnosticCategories = setOf(
        SessionAnalysisHighlightCategoryUi.TimeLoss,
        SessionAnalysisHighlightCategoryUi.TrailBrakingMissing,
        SessionAnalysisHighlightCategoryUi.EarlyApexEntry,
        SessionAnalysisHighlightCategoryUi.LateApexEntry,
        SessionAnalysisHighlightCategoryUi.CoastingZone,
        SessionAnalysisHighlightCategoryUi.Understeer,
        SessionAnalysisHighlightCategoryUi.Oversteer,
        SessionAnalysisHighlightCategoryUi.WheelLockup,
        SessionAnalysisHighlightCategoryUi.WheelSpin,
        SessionAnalysisHighlightCategoryUi.InconsistentLine,
    )

    /**
     * Produces one immutable canvas snapshot from the current selection so every rendered layer
     * shares the same geometry and camera bounds.
     */
    fun build(input: SessionAnalysisTrackCanvasStateInput): SessionAnalysisTrackCanvasState? {
        val lines = buildTrackCanvasLines(input)
        val trackSurfaceWidthMeters = linePreparer.resolveSurfaceWidth(input.trackMap?.points.orEmpty())
        val bounds = resolveBounds(
            lines = lines,
            extraMarginMeters = trackSurfaceWidthMeters * TRACK_SURFACE_MARGIN_RATIO,
        ) ?: return null
        val selectedTrailStartFraction = resolveLapFractionResolution(
            samples = input.visibleSamplesCore,
            trackMap = input.sourceTrackMap ?: input.displayTrackMap,
        ).trailStartFraction
        val cornerMarkers = resolveCornerMarkers(
            cornerZones = input.cornerZones,
            diagnosticCornerScores = input.diagnosticSummary?.cornerScores.orEmpty(),
        )

        return SessionAnalysisTrackCanvasState(
            centerLine = lines.centerLine,
            trackLeftEdge = lines.trackLeftEdge,
            trackRightEdge = lines.trackRightEdge,
            idealLine = lines.idealLine,
            pitLine = lines.pitLine,
            selectedTrace = lines.selectedTrace,
            referenceTrace = lines.referenceTrace,
            interactionTrace = lines.interactionTrace,
            sectors = input.sectors,
            cornerScores = input.diagnosticSummary?.cornerScores ?: persistentListOf(),
            cornerMarkers = cornerMarkers.toImmutableList(),
            issueMarkers = input.highlights
                .filter { highlight ->
                    highlight.trackPosition != null &&
                        highlight.category in trackDiagnosticCategories
                }
                .sortedWith(
                    compareByDescending(
                        com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi::priority,
                    ).thenByDescending { highlight -> highlight.deltaMs ?: 0 },
                )
                .toImmutableList(),
            selectedTrailStartFraction = selectedTrailStartFraction,
            minX = bounds.minX,
            minY = bounds.minY,
            maxX = bounds.maxX,
            maxY = bounds.maxY,
            trackSurfaceWidthMeters = trackSurfaceWidthMeters,
        )
    }

    /**
     * Reconciles diagnostic corner scores with authored corner zones to keep numbering and marker
     * placement stable even when telemetry highlights are sparse.
     */
    private fun resolveCornerMarkers(
        cornerZones: List<SessionAnalysisCornerZone>,
        diagnosticCornerScores: List<CornerScoreUi>,
    ): List<CornerScoreUi> {
        if (cornerZones.isEmpty()) {
            return diagnosticCornerScores
                .sortedBy(CornerScoreUi::trackPosition)
                .collapseNearbyCornerScores()
                .renumberSequentially()
        }
        val sortedZones = cornerZones
            .sortedBy(SessionAnalysisCornerZone::apexTrackPosition)
        val markerTrackPositions = sortedZones.resolveMarkerTrackPositions()
        val diagnosticsByZone = sortedZones.indices.associateWith { mutableListOf<CornerScoreUi>() }
        diagnosticCornerScores
            .sortedBy(CornerScoreUi::trackPosition)
            .forEach { diagnosticCorner ->
                val matchedZoneIndex = sortedZones
                    .withIndex()
                    .mapNotNull { (index, zone) ->
                        zone.matchDistance(diagnosticCorner)?.let { distance -> index to distance }
                    }
                    .minByOrNull(Pair<Int, Float>::second)
                    ?.first
                if (matchedZoneIndex != null) {
                    diagnosticsByZone.getValue(matchedZoneIndex) += diagnosticCorner
                }
            }
        return sortedZones.mapIndexed { index, zone ->
            zone.toCornerScore(
                diagnostics = diagnosticsByZone.getValue(index),
                markerTrackPosition = markerTrackPositions[index],
            )
        }
    }

    private fun buildTrackCanvasLines(input: SessionAnalysisTrackCanvasStateInput): SessionAnalysisTrackCanvasLines {
        val trackPoints = input.trackMap?.points.orEmpty()
        val centerLine = linePreparer.prepareTrackLine(
            points = trackPoints,
            maxPoints = TRACK_CENTER_LINE_MAX_POINTS,
        )
        val trackEdges = linePreparer.buildTrackEdges(trackPoints)
        val idealLine = resolveIdealLine(input)
        val pitLine = linePreparer.prepareTrackLine(
            points = input.trackMap?.pitPoints.orEmpty(),
            maxPoints = TRACK_PIT_LINE_MAX_POINTS,
        )
        val selectedTrace = traceBuilder.buildProjectedTrackTrace(
            samples = input.visibleSamplesCore,
            sourceTrackMap = input.sourceTrackMap,
            displayTrackMap = input.displayTrackMap,
            maxPoints = TRACK_SELECTED_TRACE_MAX_POINTS,
        )
        val referenceTrace = traceBuilder.buildProjectedTrackTrace(
            samples = input.referenceSamplesCore,
            sourceTrackMap = input.sourceTrackMap,
            displayTrackMap = input.displayTrackMap,
            maxPoints = TRACK_REFERENCE_TRACE_MAX_POINTS,
        )

        return SessionAnalysisTrackCanvasLines(
            centerLine = centerLine,
            trackLeftEdge = trackEdges.first,
            trackRightEdge = trackEdges.second,
            idealLine = idealLine,
            pitLine = pitLine,
            selectedTrace = selectedTrace,
            referenceTrace = referenceTrace,
            interactionTrace = resolveInteractionTrace(
                selectedTrace = selectedTrace,
                referenceTrace = referenceTrace,
                centerLine = centerLine,
            ),
        )
    }

    private fun resolveIdealLine(
        input: SessionAnalysisTrackCanvasStateInput,
    ): ImmutableList<SessionAnalysisFractionPointUi> {
        val idealOverlayTrackMap = when {
            (input.authoredTrackMap?.idealPoints?.size ?: 0) >= 2 -> input.authoredTrackMap
            (input.displayTrackMap?.idealPoints?.size ?: 0) >= 2 -> input.displayTrackMap
            (input.sourceTrackMap?.idealPoints?.size ?: 0) >= 2 -> input.sourceTrackMap
            else -> null
        }
        val guidedIdealLine = traceBuilder.buildProjectedOverlayTrace(
            overlayPoints = idealOverlayTrackMap?.idealPoints.orEmpty(),
            sourceTrackMap = idealOverlayTrackMap,
            displayTrackMap = input.displayTrackMap ?: idealOverlayTrackMap ?: input.sourceTrackMap,
            maxPoints = TRACK_IDEAL_LINE_MAX_POINTS,
        )
        val fallbackIdealPoints = idealOverlayTrackMap?.idealPoints
            ?.takeIf { it.size >= 2 }
            ?.map { point ->
                SessionAnalysisTrackPointUi(
                    x = point.x,
                    y = point.y,
                    leftWidthMeters = point.leftWidthMeters,
                    rightWidthMeters = point.rightWidthMeters,
                )
            } ?: input.trackMap?.idealPoints.orEmpty()
        return guidedIdealLine.takeIf { it.size >= 2 } ?: linePreparer.prepareTrackLine(
            points = fallbackIdealPoints,
            maxPoints = TRACK_IDEAL_LINE_MAX_POINTS,
        )
    }

    private fun resolveInteractionTrace(
        selectedTrace: ImmutableList<SessionAnalysisFractionPointUi>,
        referenceTrace: ImmutableList<SessionAnalysisFractionPointUi>,
        centerLine: ImmutableList<SessionAnalysisFractionPointUi>,
    ): ImmutableList<SessionAnalysisFractionPointUi> = when {
        selectedTrace.size >= 2 -> selectedTrace
        referenceTrace.size >= 2 -> referenceTrace
        centerLine.size >= 2 -> centerLine
        else -> persistentListOf()
    }

    /**
     * Computes camera bounds from every visible line layer so optional overlays never get clipped
     * when the track view auto-fits.
     */
    private fun resolveBounds(
        lines: SessionAnalysisTrackCanvasLines,
        extraMarginMeters: Float,
    ): SessionAnalysisTrackCanvasBounds? {
        val allPoints = buildList {
            addAll(lines.centerLine)
            addAll(lines.trackLeftEdge)
            addAll(lines.trackRightEdge)
            addAll(lines.idealLine)
            addAll(lines.pitLine)
            addAll(lines.selectedTrace)
            addAll(lines.referenceTrace)
            addAll(lines.interactionTrace)
        }
        if (allPoints.size < 2) return null

        val minX = allPoints.minOf(SessionAnalysisFractionPointUi::x)
        val minY = allPoints.minOf(SessionAnalysisFractionPointUi::y)
        val maxX = allPoints.maxOf(SessionAnalysisFractionPointUi::x)
        val maxY = allPoints.maxOf(SessionAnalysisFractionPointUi::y)
        val safeWidth = (maxX - minX).takeIf { width -> width > 0.001f } ?: 1f
        val safeHeight = (maxY - minY).takeIf { height -> height > 0.001f } ?: 1f
        val margin = extraMarginMeters.takeIf { value -> value.isFinite() && value > 0f }
            ?: TRACK_DEFAULT_HALF_WIDTH_METERS

        return SessionAnalysisTrackCanvasBounds(
            minX = minX - margin,
            minY = minY - margin,
            maxX = minX + safeWidth + margin,
            maxY = minY + safeHeight + margin,
        )
    }
}
