package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.builder.lap.resolveLapFractions
import com.analyzer.session.analysis.presentation.builder.track.SessionAnalysisTrackCanvasStateFactory
import com.analyzer.session.analysis.presentation.builder.track.SessionAnalysisTrackCanvasStateInput
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackMapUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisInspectorState
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisInteractionState
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisStudioState
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Assembles the full studio state by joining lap selection, charts, track canvas, and inspector data.
 */
internal data class SessionAnalysisStudioStateInput(
    val trackMap: SessionAnalysisTrackMapUi?,
    val authoredTrackMap: SessionAnalysisTrackMap?,
    val sourceTrackMap: SessionAnalysisTrackMap?,
    val displayTrackMap: SessionAnalysisTrackMap?,
    val visibleSamplesCore: List<SessionAnalysisSample>,
    val referenceSamplesCore: List<SessionAnalysisSample>,
    val visibleSamples: ImmutableList<SessionAnalysisSampleUi>,
    val referenceSamples: ImmutableList<SessionAnalysisSampleUi>,
    val comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    val sectors: ImmutableList<SessionAnalysisSectorUi>,
    val diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?,
    val highlights: ImmutableList<SessionAnalysisHighlightUi>,
    val cornerZones: List<SessionAnalysisCornerZone>,
    val selectedLapNumber: Int?,
    val referenceLapNumber: Int?,
)

internal suspend fun buildStudioState(input: SessionAnalysisStudioStateInput): SessionAnalysisStudioState =
    SessionAnalysisStudioState(
        trackCanvas = SessionAnalysisTrackCanvasStateFactory.build(
            SessionAnalysisTrackCanvasStateInput(
                trackMap = input.trackMap,
                authoredTrackMap = input.authoredTrackMap,
                sourceTrackMap = input.sourceTrackMap,
                displayTrackMap = input.displayTrackMap,
                visibleSamplesCore = input.visibleSamplesCore,
                referenceSamplesCore = input.referenceSamplesCore,
                sectors = input.sectors,
                highlights = input.highlights,
                diagnosticSummary = input.diagnosticSummary,
                cornerZones = input.cornerZones,
            ),
        ),
        graph = buildGraphState(
            comparisonPoints = input.comparisonPoints,
            selectedLapNumber = input.selectedLapNumber,
            referenceLapNumber = input.referenceLapNumber,
        ),
        interaction = buildInteractionState(
            visibleSamplesCore = input.visibleSamplesCore,
            visibleSamples = input.visibleSamples,
            comparisonPoints = input.comparisonPoints,
            trackMap = input.sourceTrackMap ?: input.displayTrackMap,
        ),
        inspector = buildInspectorState(
            visibleSamples = input.visibleSamples,
            referenceSamples = input.referenceSamples,
        ),
    )

private fun buildInteractionState(
    visibleSamplesCore: List<SessionAnalysisSample>,
    visibleSamples: ImmutableList<SessionAnalysisSampleUi>,
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    trackMap: SessionAnalysisTrackMap?,
): SessionAnalysisInteractionState {
    val sampleIndexByFrameId = persistentHashMapOf<Long, Int>().builder()
    visibleSamples.forEachIndexed { index, sample ->
        sampleIndexByFrameId[sample.frameId] = index
    }
    val resolvedFractionsByFrameId = visibleSamplesCore
        .sortedBy(SessionAnalysisSample::sampleIndexInLap)
        .zip(resolveLapFractions(visibleSamplesCore, trackMap))
        .associate { (sample, fraction) -> sample.frameId to fraction }
    return SessionAnalysisInteractionState(
        visibleSamples = visibleSamples,
        comparisonFractions = comparisonPoints.map(SessionAnalysisComparisonPointUi::fraction).toImmutableList(),
        resolvedSamplePositions = visibleSamples.map { sample ->
            resolvedFractionsByFrameId[sample.frameId] ?: 0f
        }.toImmutableList(),
        sampleIndexByFrameId = sampleIndexByFrameId.build(),
    )
}

private fun buildInspectorState(
    visibleSamples: ImmutableList<SessionAnalysisSampleUi>,
    referenceSamples: ImmutableList<SessionAnalysisSampleUi>,
): SessionAnalysisInspectorState = SessionAnalysisInspectorState(
    selectedTyreAnalytics = buildTyreAnalyticsUi(visibleSamples),
    referenceTyreAnalytics = buildTyreAnalyticsUi(referenceSamples),
)
