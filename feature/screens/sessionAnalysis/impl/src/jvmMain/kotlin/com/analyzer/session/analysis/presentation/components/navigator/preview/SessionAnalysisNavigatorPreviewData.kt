package com.analyzer.session.analysis.presentation.components.navigator.preview

import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosisSourceUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHeaderUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightCategoryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightSeverityUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSessionOptionUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun sessionAnalysisNavigatorPreviewHeader(): SessionAnalysisHeaderUi = SessionAnalysisHeaderUi(
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

internal fun sessionAnalysisNavigatorPreviewSessions(): ImmutableList<SessionAnalysisSessionOptionUi> =
    persistentListOf(
        SessionAnalysisSessionOptionUi(12L, "Race sim", "19:42, GT3"),
        SessionAnalysisSessionOptionUi(18L, "Quali prep", "18:10, GT3"),
    )

internal fun sessionAnalysisNavigatorPreviewLaps(): ImmutableList<SessionAnalysisLapSummaryUi> = persistentListOf(
    SessionAnalysisLapSummaryUi(
        lapNumber = 7,
        isValid = true,
        isPitLap = false,
        isComplete = true,
        durationMs = 136_841,
        sampleCount = 512,
        avgSpeedKmh = 174f,
        maxSpeedKmh = 262f,
        deltaToBestMs = 359,
        endFuelLiters = 34.6f,
        diagnosticScore = 78,
    ),
    SessionAnalysisLapSummaryUi(
        lapNumber = 6,
        isValid = true,
        isPitLap = false,
        isComplete = true,
        durationMs = 136_482,
        sampleCount = 508,
        avgSpeedKmh = 175f,
        maxSpeedKmh = 261f,
        deltaToBestMs = 0,
        endFuelLiters = 35.1f,
        diagnosticScore = 82,
    ),
    SessionAnalysisLapSummaryUi(
        lapNumber = 5,
        isValid = false,
        isPitLap = false,
        isComplete = true,
        durationMs = 137_944,
        sampleCount = 501,
        avgSpeedKmh = 171f,
        maxSpeedKmh = 258f,
        deltaToBestMs = 1_462,
        endFuelLiters = 35.7f,
        diagnosticScore = 61,
    ),
)

internal fun sessionAnalysisNavigatorPreviewHighlights(): ImmutableList<SessionAnalysisHighlightUi> = persistentListOf(
    SessionAnalysisHighlightUi(
        id = "hl-1",
        category = SessionAnalysisHighlightCategoryUi.TimeLoss,
        severity = SessionAnalysisHighlightSeverityUi.Warning,
        lapNumber = 7,
        title = "Late brake release into Turn 7",
        description = "The cursor drifts deep into entry and costs rotation speed.",
        trackPosition = 0.42f,
        deltaMs = 92,
        diagnosisSource = SessionAnalysisDiagnosisSourceUi.DrivingStyle,
        recommendation = "Release pressure earlier before the apex.",
        cornerNumber = 7,
        score = 72,
        priority = 2,
    ),
    SessionAnalysisHighlightUi(
        id = "hl-2",
        category = SessionAnalysisHighlightCategoryUi.Oversteer,
        severity = SessionAnalysisHighlightSeverityUi.Warning,
        lapNumber = 7,
        title = "Rear instability on exit",
        description = "Throttle pickup unsettles the rear axle at Turn 9.",
        trackPosition = 0.61f,
        deltaMs = 57,
        diagnosisSource = SessionAnalysisDiagnosisSourceUi.Mixed,
        recommendation = "Smooth the throttle ramp or calm rear damping.",
        cornerNumber = 9,
        score = 68,
        priority = 1,
    ),
)
