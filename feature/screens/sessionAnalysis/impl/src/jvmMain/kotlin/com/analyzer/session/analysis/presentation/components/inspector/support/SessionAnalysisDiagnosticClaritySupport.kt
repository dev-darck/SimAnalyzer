package com.analyzer.session.analysis.presentation.components.inspector.support

import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory

internal fun DiagnosticIssueUi.lineLookAtLabel(): String = category.toDrivingLookAtLabel(cornerNumber)

internal fun DiagnosticIssueUi.setupLookAtLabel(): String = category.toSetupLookAtLabel(
    cornerNumber = cornerNumber,
    system = setupSystem(),
)

internal fun CornerScoreUi.lookAtLabel(): String = category.toDrivingLookAtLabel(cornerNumber)

internal fun SessionAnalysisDiagnosisSource.fixInLabel(): String = when (this) {
    SessionAnalysisDiagnosisSource.DrivingStyle -> "Driver"
    SessionAnalysisDiagnosisSource.CarSetup -> "Car"
    SessionAnalysisDiagnosisSource.Mixed -> "Driver first. If it repeats, then car"
}

internal fun SessionAnalysisHighlightCategory?.toSetupLookAtLabel(
    cornerNumber: Int?,
    system: SessionAnalysisSetupSystem,
): String = when (this) {
    SessionAnalysisHighlightCategory.TyrePressureImbalance ->
        "all four hot pressures after one clean push lap"

    SessionAnalysisHighlightCategory.TyreTempImbalance,
    SessionAnalysisHighlightCategory.TyreOverheat,
    SessionAnalysisHighlightCategory.TyreCold,
    -> "inside, middle and outside tyre temps after the loaded corners"

    SessionAnalysisHighlightCategory.BrakeBalance ->
        "heavy braking zones with near-straight steering"

    SessionAnalysisHighlightCategory.AeroBalance,
    SessionAnalysisHighlightCategory.SetupUndersteer,
    SessionAnalysisHighlightCategory.SetupOversteer,
    -> cornerNumber?.let { corner -> "Turn $corner balance from entry to apex and minimum speed" }
        ?: "the repeated corners where the car keeps pushing wide or rotating too much"

    SessionAnalysisHighlightCategory.DamperIssue ->
        "kerbs, bumps and the first car movement after load transfer"

    else -> when (system) {
        SessionAnalysisSetupSystem.Tyres -> "tyre pressures and temperatures across the car"
        SessionAnalysisSetupSystem.Aero -> "fast corners where balance changes as speed rises"
        SessionAnalysisSetupSystem.Brakes -> "brake trace, front lock tendency and rear stability"
        SessionAnalysisSetupSystem.Suspension -> "bumps, kerbs and how the platform settles"
        SessionAnalysisSetupSystem.Balance -> "the same corners where balance repeats lap after lap"
    }
}

internal fun SessionAnalysisHighlightCategory?.toDrivingLookAtLabel(cornerNumber: Int?): String {
    val zonePrefix = cornerNumber?.let { corner -> "Turn $corner: " }.orEmpty()
    return when (this) {
        SessionAnalysisHighlightCategory.BrakePoint,
        SessionAnalysisHighlightCategory.TrailBrakingMissing,
        SessionAnalysisHighlightCategory.WheelLockup,
        -> zonePrefix + "entry brake trace, release timing and minimum speed"

        SessionAnalysisHighlightCategory.EarlyApexEntry,
        SessionAnalysisHighlightCategory.LateApexEntry,
        SessionAnalysisHighlightCategory.InconsistentLine,
        -> zonePrefix + "turn-in point, apex location and steering trace"

        SessionAnalysisHighlightCategory.CoastingZone,
        SessionAnalysisHighlightCategory.WheelSpin,
        SessionAnalysisHighlightCategory.ThrottleCommitment,
        -> zonePrefix + "first throttle pickup, steering unwind and exit speed"

        SessionAnalysisHighlightCategory.Understeer,
        SessionAnalysisHighlightCategory.Oversteer,
        -> zonePrefix + "steering angle versus car rotation through the loaded phase"

        else -> if (cornerNumber != null) {
            "Turn $cornerNumber: reference line, speed trace and input delta"
        } else {
            "reference line, speed trace and input delta"
        }
    }
}
