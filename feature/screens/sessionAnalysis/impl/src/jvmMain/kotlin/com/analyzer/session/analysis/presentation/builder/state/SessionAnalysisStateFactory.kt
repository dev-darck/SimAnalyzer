@file:Suppress("LongParameterList")

package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.builder.coach.buildLapCoach
import com.analyzer.session.analysis.presentation.builder.diagnostic.applyLapDiagnosticScores
import com.analyzer.session.analysis.presentation.mapper.toUi
import com.analyzer.session.analysis.presentation.mapper.toUiSamples
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapCoachUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisState
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackMapUi
import com.analyzer.session.analysis.presentation.pipeline.buildComparisonPoints
import com.analyzer.session.analysis.presentation.pipeline.buildHighlights
import com.analyzer.session.analysis.presentation.pipeline.buildSectors
import com.analyzer.session.analysis.presentation.pipeline.buildSummary
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts the domain report into a fully hydrated screen state, keeping shell data usable even
 * before the heavier analytics branches finish enriching the report.
 */
internal suspend fun SessionAnalysisReport.toState(
    calibration: TrackCalibration? = null,
    authoredTrackMap: SessionAnalysisTrackMap? = null,
    sourceTrackMap: SessionAnalysisTrackMap? = trackMap,
    displayTrackMap: SessionAnalysisTrackMap? = trackMap,
    selectedSegmentId: Long? = null,
    selectedLapNumber: Int? = null,
    selectedReferenceLapNumber: Int? = null,
    selectedFrameId: Long? = null,
): SessionAnalysisState {
    val selection = resolveSelectionContext(
        selectedSegmentId = selectedSegmentId,
        selectedLapNumber = selectedLapNumber,
        selectedReferenceLapNumber = selectedReferenceLapNumber,
    )
    val telemetryContext = buildTelemetryContext(
        selection = selection,
        selectedFrameId = selectedFrameId,
        sourceTrackMap = sourceTrackMap,
        displayTrackMap = displayTrackMap,
    )
    val highlightContext = buildHighlightContext(
        selection = selection,
        comparisonPoints = telemetryContext.comparisonPoints,
    )
    val lapContext = buildLapContext(
        selection = selection,
        telemetryContext = telemetryContext,
        highlightContext = highlightContext,
        calibration = calibration,
        authoredTrackMap = authoredTrackMap,
        sourceTrackMap = sourceTrackMap,
        displayTrackMap = displayTrackMap,
    )
    val trackMapUi = displayTrackMap?.toUi()
    val studioState = buildStudioState(
        SessionAnalysisStudioStateInput(
            trackMap = trackMapUi,
            authoredTrackMap = authoredTrackMap,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
            visibleSamplesCore = selection.selectedSamples,
            referenceSamplesCore = selection.referenceSamples,
            visibleSamples = telemetryContext.visibleSamples,
            referenceSamples = telemetryContext.referenceSamples,
            comparisonPoints = telemetryContext.comparisonPoints,
            sectors = lapContext.sectors,
            diagnosticSummary = highlightContext.diagnosticSummary,
            highlights = highlightContext.resolvedHighlights,
            cornerZones = selection.cornerZones,
            selectedLapNumber = selection.selectedLapNumber,
            referenceLapNumber = selection.referenceLapNumber,
        ),
    )

    return SessionAnalysisState(
        isLoading = false,
        error = null,
        header = buildStateHeader(selection),
        summary = buildSummary(
            laps = lapContext.lapsUi,
            selectedLap = lapContext.selectedLapUi,
            referenceLap = lapContext.referenceLapUi,
            highlights = highlightContext.resolvedHighlights,
            analytics = comprehensiveAnalysis,
        ),
        sessionOptions = buildSessionOptions(),
        lapOptions = buildLapOptions(selection),
        sectors = lapContext.sectors,
        selectedSample = telemetryContext.selectedSample,
        selectedComparisonPoint = telemetryContext.selectedComparisonPoint,
        lapCoach = lapContext.lapCoach,
        diagnosticSummary = highlightContext.diagnosticSummary,
        highlights = highlightContext.resolvedHighlights,
        laps = lapContext.lapsUi,
        studio = studioState,
        selectedSegmentId = selection.segmentId,
        selectedLapNumber = selection.selectedLapNumber,
        referenceLapNumber = selection.referenceLapNumber,
        referenceLapIsCustom = selection.referenceLapIsCustom,
        selectedFrameId = telemetryContext.selectedSample?.frameId,
    )
}

private fun SessionAnalysisReport.buildTelemetryContext(
    selection: SessionAnalysisSelectionContext,
    selectedFrameId: Long?,
    sourceTrackMap: SessionAnalysisTrackMap?,
    displayTrackMap: SessionAnalysisTrackMap?,
): SessionAnalysisTelemetryContext {
    val visibleSamples = selection.selectedSamples.toUiSamples().toImmutableList()
    val referenceSamples = selection.referenceSamples.toUiSamples().toImmutableList()
    val comparisonPoints = buildComparisonPoints(
        selectedSamples = selection.selectedSamples,
        referenceSamples = selection.referenceSamples,
        trackMap = sourceTrackMap ?: displayTrackMap,
    ).toImmutableList()
    val selectedSample = visibleSamples.firstOrNull { sample -> sample.frameId == selectedFrameId }
    val selectedComparisonPoint = comparisonPoints.firstOrNull { point ->
        point.selectedFrameId == selectedSample?.frameId
    } ?: selectedSample?.trackPosition?.let(comparisonPoints::nearestToTrackPosition)

    return SessionAnalysisTelemetryContext(
        visibleSamples = visibleSamples,
        referenceSamples = referenceSamples,
        comparisonPoints = comparisonPoints,
        selectedSample = selectedSample,
        selectedComparisonPoint = selectedComparisonPoint,
    )
}

private suspend fun SessionAnalysisReport.buildHighlightContext(
    selection: SessionAnalysisSelectionContext,
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
): SessionAnalysisHighlightContext {
    val segmentHighlights = highlights.filterHighlightsForSegment(selection.segmentId)
    val generatedHighlights = buildHighlights(
        comparisonPoints = comparisonPoints,
        selectedLapNumber = selection.selectedLapNumber,
    )
    val reportHighlights = segmentHighlights.map(SessionAnalysisHighlight::toUi)
    val mergedHighlights = mergeHighlights(
        reportHighlights = reportHighlights,
        generatedHighlights = generatedHighlights,
    )
    val visibleHighlights = resolveVisibleHighlights(
        highlights = mergedHighlights,
        selectedLapNumber = selection.selectedLapNumber,
    )

    return SessionAnalysisHighlightContext(
        segmentHighlights = segmentHighlights,
        mergedHighlights = mergedHighlights,
        resolvedHighlights = visibleHighlights.toImmutableList(),
        diagnosticSummary = resolveDiagnosticSummary(
            visibleHighlights = visibleHighlights,
            fallbackHighlights = mergedHighlights,
        ),
    )
}

private suspend fun SessionAnalysisReport.buildLapContext(
    selection: SessionAnalysisSelectionContext,
    telemetryContext: SessionAnalysisTelemetryContext,
    highlightContext: SessionAnalysisHighlightContext,
    calibration: TrackCalibration?,
    authoredTrackMap: SessionAnalysisTrackMap?,
    sourceTrackMap: SessionAnalysisTrackMap?,
    displayTrackMap: SessionAnalysisTrackMap?,
): SessionAnalysisLapContext {
    val lapsUi = applyLapDiagnosticScores(
        laps = selection.segmentLaps
            .sortedBy(SessionAnalysisLap::lapNumber)
            .map(SessionAnalysisLap::toUi),
        highlights = highlightContext.mergedHighlights,
    ).toImmutableList()
    val selectedLapUi = lapsUi.firstOrNull { lap -> lap.lapNumber == selection.selectedLapNumber }
    val referenceLapUi = lapsUi.firstOrNull { lap -> lap.lapNumber == selection.referenceLapNumber }
    val sectors = buildSectors(
        selectedSamples = selection.selectedSamples,
        referenceSamples = selection.referenceSamples,
        comparisonPoints = telemetryContext.comparisonPoints,
        trackMap = resolveSectorCalibrationTrackMap(
            calibration = calibration,
            authoredTrackMap = authoredTrackMap,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
            fallbackTrackMap = trackMap,
        ),
        displayTrackMap = displayTrackMap ?: sourceTrackMap ?: trackMap,
        calibration = calibration,
    ).toImmutableList()

    return SessionAnalysisLapContext(
        lapsUi = lapsUi,
        selectedLapUi = selectedLapUi,
        referenceLapUi = referenceLapUi,
        sectors = sectors,
        lapCoach = buildLapCoach(
            selectedLap = selection.selectedLap,
            selectedSamples = selection.selectedSamples,
            referenceLap = selection.referenceLap,
            referenceSamples = selection.referenceSamples,
            trackMap = trackMap,
            segmentSamples = selection.segmentSamples,
            highlights = highlightContext.segmentHighlights,
            diagnosticSummary = highlightContext.diagnosticSummary,
        ),
    )
}

private suspend fun SessionAnalysisReport.buildStateHeader(selection: SessionAnalysisSelectionContext) = buildHeaderUi(
    reportHeader = header,
    segment = selection.segment,
    segmentLaps = selection.segmentLaps,
    selectedLap = selection.selectedLap,
    tyreProfile = tyreProfile,
    vehicleClassLabel = vehicleClass.toDisplayLabel(),
)

private fun SessionAnalysisReport.buildSessionOptions() = segments
    .map(SessionAnalysisSegment::toOptionUi)
    .toImmutableList()

private suspend fun buildLapOptions(selection: SessionAnalysisSelectionContext) = selection.segmentLaps
    .sortedBy(SessionAnalysisLap::lapNumber)
    .toLapOptionUiList()
    .toImmutableList()

private fun resolveSectorCalibrationTrackMap(
    calibration: TrackCalibration?,
    authoredTrackMap: SessionAnalysisTrackMap?,
    sourceTrackMap: SessionAnalysisTrackMap?,
    displayTrackMap: SessionAnalysisTrackMap?,
    fallbackTrackMap: SessionAnalysisTrackMap?,
): SessionAnalysisTrackMap? = when (calibration?.source) {
    TrackCalibrationSource.GAME -> sourceTrackMap ?: authoredTrackMap ?: displayTrackMap ?: fallbackTrackMap
    TrackCalibrationSource.USER -> authoredTrackMap ?: sourceTrackMap ?: displayTrackMap ?: fallbackTrackMap
    null -> authoredTrackMap ?: sourceTrackMap ?: displayTrackMap ?: fallbackTrackMap
}

internal suspend fun SessionAnalysisReport.toShellState(
    authoredTrackMap: SessionAnalysisTrackMap? = null,
    sourceTrackMap: SessionAnalysisTrackMap? = trackMap,
    displayTrackMap: SessionAnalysisTrackMap? = trackMap,
    selectedSegmentId: Long? = null,
    selectedLapNumber: Int? = null,
    selectedReferenceLapNumber: Int? = null,
): SessionAnalysisState {
    val selection = resolveSelectionContext(
        selectedSegmentId = selectedSegmentId,
        selectedLapNumber = selectedLapNumber,
        selectedReferenceLapNumber = selectedReferenceLapNumber,
    )
    val lapsUi = selection.segmentLaps
        .sortedBy(SessionAnalysisLap::lapNumber)
        .map(SessionAnalysisLap::toUi)
        .toImmutableList()
    val trackMapUi = displayTrackMap?.toUi()

    return buildShellState(
        selection = selection,
        lapsUi = lapsUi,
        selectedSample = selection.selectedSamples.firstOrNull()?.toUi(
            elapsedMs = selection.selectedSamples.firstOrNull()?.let { 0 },
        ),
        trackMapUi = trackMapUi,
        authoredTrackMap = authoredTrackMap,
        sourceTrackMap = sourceTrackMap,
        displayTrackMap = displayTrackMap,
    )
}

private suspend fun SessionAnalysisReport.buildShellState(
    selection: SessionAnalysisSelectionContext,
    lapsUi: ImmutableList<SessionAnalysisLapSummaryUi>,
    selectedSample: SessionAnalysisSampleUi?,
    trackMapUi: SessionAnalysisTrackMapUi?,
    authoredTrackMap: SessionAnalysisTrackMap?,
    sourceTrackMap: SessionAnalysisTrackMap?,
    displayTrackMap: SessionAnalysisTrackMap?,
): SessionAnalysisState = SessionAnalysisState(
    isLoading = true,
    error = null,
    header = buildStateHeader(selection),
    sessionOptions = buildSessionOptions(),
    lapOptions = buildLapOptions(selection),
    laps = lapsUi,
    selectedSample = selectedSample,
    selectedSegmentId = selection.segmentId,
    selectedLapNumber = selection.selectedLapNumber,
    referenceLapNumber = selection.referenceLapNumber,
    referenceLapIsCustom = selection.referenceLapIsCustom,
    studio = buildStudioState(
        SessionAnalysisStudioStateInput(
            trackMap = trackMapUi,
            authoredTrackMap = authoredTrackMap,
            sourceTrackMap = sourceTrackMap,
            displayTrackMap = displayTrackMap,
            visibleSamplesCore = selection.selectedSamples,
            referenceSamplesCore = emptyList(),
            visibleSamples = persistentListOf(),
            referenceSamples = persistentListOf(),
            comparisonPoints = persistentListOf(),
            sectors = persistentListOf(),
            diagnosticSummary = null,
            highlights = persistentListOf(),
            cornerZones = selection.cornerZones,
            selectedLapNumber = selection.selectedLapNumber,
            referenceLapNumber = selection.referenceLapNumber,
        ),
    ),
)

private data class SessionAnalysisTelemetryContext(
    val visibleSamples: ImmutableList<SessionAnalysisSampleUi>,
    val referenceSamples: ImmutableList<SessionAnalysisSampleUi>,
    val comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    val selectedSample: SessionAnalysisSampleUi?,
    val selectedComparisonPoint: SessionAnalysisComparisonPointUi?,
)

private data class SessionAnalysisHighlightContext(
    val segmentHighlights: List<SessionAnalysisHighlight>,
    val mergedHighlights: List<SessionAnalysisHighlightUi>,
    val resolvedHighlights: ImmutableList<SessionAnalysisHighlightUi>,
    val diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?,
)

private data class SessionAnalysisLapContext(
    val lapsUi: ImmutableList<SessionAnalysisLapSummaryUi>,
    val selectedLapUi: SessionAnalysisLapSummaryUi?,
    val referenceLapUi: SessionAnalysisLapSummaryUi?,
    val sectors: ImmutableList<SessionAnalysisSectorUi>,
    val lapCoach: SessionAnalysisLapCoachUi?,
)
