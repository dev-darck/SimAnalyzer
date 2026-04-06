package com.analyzer.session.analysis.presentation.components.inspector.preview

import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisCoachInsightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisCoachMetricUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapCoachUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTyreUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisInspectorState
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTyreAnalyticsUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreTemperatureBand
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal fun sessionAnalysisInspectorPreviewSummary(): SessionAnalysisSummaryUi = SessionAnalysisSummaryUi(
    lapDeltaLabel = "-0.178s",
    biggestLossLabel = "Turn 7",
    biggestLossValueLabel = "+0.092s",
    consistencyLabel = "98.1%",
    fuelLabel = "34.6 L",
    topSpeedLabel = "262 km/h",
)

internal fun sessionAnalysisInspectorPreviewSectors(): ImmutableList<SessionAnalysisSectorUi> = persistentListOf(
    SessionAnalysisSectorUi("Sector 1", 0f, 0.33f, 0.16f, 45_281, 45_190, 91, "Entry rotation costs time."),
    SessionAnalysisSectorUi("Sector 2", 0.33f, 0.66f, 0.49f, 51_944, 51_801, 143, "Exit traction is unstable."),
    SessionAnalysisSectorUi(
        "Sector 3",
        0.66f,
        1f,
        0.83f,
        39_616,
        39_491,
        125,
        "Final chicane needs cleaner release.",
    ),
)

internal fun sessionAnalysisInspectorPreviewDiagnosticSummary(): SessionAnalysisDiagnosticSummaryUi =
    SessionAnalysisDiagnosticSummaryUi(
        overallScore = 78,
        drivingScore = 82,
        setupScore = 69,
        topDrivingIssues = persistentListOf(
            DiagnosticIssueUi(
                title = "Brake release is late",
                description = "The car stays loaded too deep into the corner.",
                recommendation = "Release pressure earlier before turn-in.",
                priority = 1,
                source = SessionAnalysisDiagnosisSource.DrivingStyle,
                potentialTimeGainMs = 182,
                category = SessionAnalysisHighlightCategory.TimeLoss,
                cornerNumber = 7,
                trackPosition = 0.42f,
            ),
        ),
        topSetupIssues = persistentListOf(
            DiagnosticIssueUi(
                title = "Rear rotates on exit",
                description = "Power application unloads the rear axle too abruptly.",
                recommendation = "Rebalance rear damping and diff preload.",
                priority = 2,
                source = SessionAnalysisDiagnosisSource.CarSetup,
                potentialTimeGainMs = 96,
                category = SessionAnalysisHighlightCategory.Oversteer,
                cornerNumber = 9,
                trackPosition = 0.61f,
            ),
        ),
        cornerScores = persistentListOf(
            CornerScoreUi(
                cornerNumber = 7,
                score = 72,
                trackPosition = 0.42f,
                mainIssue = "Late release into apex",
                timeVsReferenceMs = 92,
                recommendation = "Release brake earlier and rotate sooner.",
                source = SessionAnalysisDiagnosisSource.DrivingStyle,
                category = SessionAnalysisHighlightCategory.TrailBrakingMissing,
            ),
            CornerScoreUi(
                cornerNumber = 9,
                score = 66,
                trackPosition = 0.61f,
                mainIssue = "Rear instability on throttle",
                timeVsReferenceMs = 57,
                recommendation = "Soften throttle ramp or calm rear damping.",
                source = SessionAnalysisDiagnosisSource.Mixed,
                category = SessionAnalysisHighlightCategory.WheelSpin,
            ),
        ),
    )

internal fun sessionAnalysisInspectorPreviewHighlights(): ImmutableList<SessionAnalysisHighlightUi> = persistentListOf(
    SessionAnalysisHighlightUi(
        id = "setup-1",
        category = SessionAnalysisHighlightCategory.DamperIssue,
        severity = SessionAnalysisHighlightSeverity.Warning,
        lapNumber = 7,
        title = "Rear damper rebound is too aggressive",
        description = "The rear unloads too quickly at exit over curb compression.",
        trackPosition = 0.61f,
        deltaMs = 57,
        diagnosisSource = SessionAnalysisDiagnosisSource.CarSetup,
        recommendation = "Reduce rear rebound or soften throttle pickup.",
        cornerNumber = 9,
        score = 68,
        priority = 2,
    ),
    SessionAnalysisHighlightUi(
        id = "drive-1",
        category = SessionAnalysisHighlightCategory.TimeLoss,
        severity = SessionAnalysisHighlightSeverity.Warning,
        lapNumber = 7,
        title = "Brake release is late",
        description = "The car is held too long on the brake before apex.",
        trackPosition = 0.42f,
        deltaMs = 92,
        diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
        recommendation = "Release pressure earlier and rotate sooner.",
        cornerNumber = 7,
        score = 72,
        priority = 1,
    ),
)

internal fun sessionAnalysisInspectorPreviewCoach(): SessionAnalysisLapCoachUi = SessionAnalysisLapCoachUi(
    referenceLapLabel = "Lap 6",
    summaryTitle = "Reference lap gains most on exit phase",
    summaryDescription = "You match entry speed well but bleed time from late rotation and unstable power phase.",
    metrics = persistentListOf(
        SessionAnalysisCoachMetricUi("Entry", "Good"),
        SessionAnalysisCoachMetricUi("Apex", "Needs work"),
        SessionAnalysisCoachMetricUi("Exit", "Main loss"),
    ),
    insights = persistentListOf(
        SessionAnalysisCoachInsightUi(
            title = "Turn 7",
            description = "Release brake earlier to rotate before the apex.",
        ),
        SessionAnalysisCoachInsightUi(
            title = "Turn 9",
            description = "Stabilize rear on throttle pickup and open steering sooner.",
        ),
    ),
)

internal fun sessionAnalysisInspectorPreviewActivePoint(): SessionAnalysisComparisonPointUi =
    SessionAnalysisComparisonPointUi(
        fraction = 0.42f,
        trackPosition = 0.42f,
        selectedFrameId = 1201L,
        selectedElapsedMs = 52_134,
        referenceElapsedMs = 52_312,
        deltaMs = -178,
        selectedSpeedKmh = 146f,
        referenceSpeedKmh = 142f,
        selectedThrottle = 0.84f,
        referenceThrottle = 0.79f,
        selectedBrake = 0.06f,
        referenceBrake = 0.12f,
        selectedSteeringAngleRad = 0.19f,
        referenceSteeringAngleRad = 0.16f,
        selectedGear = 4,
        referenceGear = 4,
        selectedRpm = 7_120f,
        referenceRpm = 6_980f,
        selectedFuelLiters = 34.6f,
        referenceFuelLiters = 34.8f,
    )

internal fun sessionAnalysisInspectorPreviewActiveSample(): SessionAnalysisSampleUi = SessionAnalysisSampleUi(
    frameId = 1201L,
    lapNumber = 7,
    sectorIndex = 2,
    sampleIndexInLap = 418,
    trackPosition = 0.42f,
    speedKmh = 146f,
    gear = 4,
    rpm = 7_120f,
    throttle = 0.84f,
    brake = 0.06f,
    fuelLiters = 34.6f,
    tyreFl = tyre(26.7f, 87.5f, 592f),
    tyreFr = tyre(26.4f, 88.2f, 601f),
    tyreRl = tyre(25.8f, 91.3f, 544f),
    tyreRr = tyre(25.9f, 90.8f, 548f),
)

internal fun sessionAnalysisInspectorPreviewInspectorState(): SessionAnalysisInspectorState =
    SessionAnalysisInspectorState(
        selectedTyreAnalytics = SessionAnalysisTyreAnalyticsUi(
            avgCoreTempC = 89.4f,
            peakCoreTempC = 96.1f,
            peakBrakeTempC = 612f,
            pressureSpreadPsi = 0.42f,
            hottestTyreLabel = "RR",
            peakSlip = 0.12f,
        ),
        referenceTyreAnalytics = SessionAnalysisTyreAnalyticsUi(
            avgCoreTempC = 87.9f,
            peakCoreTempC = 93.4f,
            peakBrakeTempC = 606f,
            pressureSpreadPsi = 0.36f,
            hottestTyreLabel = "RR",
            peakSlip = 0.09f,
        ),
    )

private fun tyre(pressurePsi: Float, coreTempC: Float, brakeTempC: Float): SessionAnalysisTyreUi =
    SessionAnalysisTyreUi(
        pressurePsi = pressurePsi,
        coreTempC = coreTempC,
        avgTempC = coreTempC - 2f,
        brakeTempC = brakeTempC,
        slip = 0.08f,
        tempBand = SessionAnalysisTyreTemperatureBand.Optimal,
    )
