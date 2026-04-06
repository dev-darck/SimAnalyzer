package com.analyzer.session.analysis.presentation.preview

import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewCanvasState
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHeaderUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisState
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSummaryUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisGraphState
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisInteractionState
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisStudioState

internal fun sessionAnalysisPreviewHeader(): SessionAnalysisHeaderUi = SessionAnalysisHeaderUi(
    sessionTypeLabel = "Practice",
    carLabel = "Porsche 992 GT3 R",
    trackLabel = "Spa-Francorchamps",
    startedAtLabel = "2026-04-05 19:42",
    vehicleClassLabel = "GT3",
    compoundLabel = "Dry",
    airTempLabel = "18 C",
    trackTempLabel = "24 C",
    bestLapLabel = "2:16.482",
    selectedLapLabel = "Lap 7",
    lapTimeLabel = "2:16.841",
    validLapsLabel = "8 / 9",
    topSpeedLabel = "262 km/h",
    peakBrakeLabel = "612 C",
    peakCoreLabel = "91 C",
    surfaceWindowLabel = "73-79 C",
    coreWindowLabel = "82-94 C",
    brakeWindowLabel = "540-620 C",
)

internal fun sessionAnalysisPreviewSummary(): SessionAnalysisSummaryUi = SessionAnalysisSummaryUi(
    lapDeltaLabel = "-0.178s",
    biggestLossLabel = "Turn 7",
    biggestLossValueLabel = "+0.092s",
    consistencyLabel = "98.1%",
    fuelLabel = "34.6 L",
    topSpeedLabel = "262 km/h",
)

internal fun sessionAnalysisPreviewState(): SessionAnalysisState {
    val activePoint = sessionAnalysisTrackMapPreviewActivePoint()
    val activeSample = sessionAnalysisTrackMapPreviewActiveSample()

    return SessionAnalysisState(
        isLoading = false,
        header = sessionAnalysisPreviewHeader(),
        summary = sessionAnalysisPreviewSummary(),
        selectedSample = activeSample,
        selectedComparisonPoint = activePoint,
        studio = SessionAnalysisStudioState(
            trackCanvas = sessionAnalysisTrackMapPreviewCanvasState(),
            graph = SessionAnalysisGraphState(
                comparisonPoints = kotlinx.collections.immutable.persistentListOf(activePoint),
            ),
            interaction = SessionAnalysisInteractionState(
                visibleSamples = kotlinx.collections.immutable.persistentListOf(activeSample),
            ),
        ),
        selectedLapNumber = 7,
        referenceLapNumber = 3,
    )
}
