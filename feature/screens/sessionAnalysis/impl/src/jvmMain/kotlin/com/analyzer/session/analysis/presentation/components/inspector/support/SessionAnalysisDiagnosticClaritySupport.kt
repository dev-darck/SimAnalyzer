package com.analyzer.session.analysis.presentation.components.inspector.support

import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosisSourceUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightCategoryUi

internal fun DiagnosticIssueUi.lineLookAtLabel(): String = category.toDrivingLookAtLabel(cornerNumber)

internal fun DiagnosticIssueUi.setupLookAtLabel(): String = category.toSetupLookAtLabel(
    cornerNumber = cornerNumber,
    system = setupSystem(),
)

internal fun CornerScoreUi.lookAtLabel(): String = category.toDrivingLookAtLabel(cornerNumber)

internal fun SessionAnalysisDiagnosisSourceUi.fixInLabel(): String = when (this) {
    SessionAnalysisDiagnosisSourceUi.DrivingStyle -> "Driver"
    SessionAnalysisDiagnosisSourceUi.CarSetup -> "Car"
    SessionAnalysisDiagnosisSourceUi.Mixed -> "Driver first. If it repeats, then car"
}

internal fun SessionAnalysisHighlightCategoryUi?.toSetupLookAtLabel(
    cornerNumber: Int?,
    system: SessionAnalysisSetupSystem,
): String = when (this) {
    SessionAnalysisHighlightCategoryUi.TyrePressureImbalance ->
        "all four hot pressures after one clean push lap"

    SessionAnalysisHighlightCategoryUi.TyreTempImbalance,
    SessionAnalysisHighlightCategoryUi.TyreOverheat,
    SessionAnalysisHighlightCategoryUi.TyreCold,
    -> "inside, middle and outside tyre temps after the loaded corners"

    SessionAnalysisHighlightCategoryUi.BrakeBalance ->
        "heavy braking zones with near-straight steering"

    SessionAnalysisHighlightCategoryUi.AeroBalance,
    SessionAnalysisHighlightCategoryUi.SetupUndersteer,
    SessionAnalysisHighlightCategoryUi.SetupOversteer,
    -> cornerNumber?.let { corner -> "Turn $corner balance from entry to apex and minimum speed" }
        ?: "the repeated corners where the car keeps pushing wide or rotating too much"

    SessionAnalysisHighlightCategoryUi.DamperIssue ->
        "kerbs, bumps and the first car movement after load transfer"

    else -> when (system) {
        SessionAnalysisSetupSystem.Tyres -> "tyre pressures and temperatures across the car"
        SessionAnalysisSetupSystem.Aero -> "fast corners where balance changes as speed rises"
        SessionAnalysisSetupSystem.Brakes -> "brake trace, front lock tendency and rear stability"
        SessionAnalysisSetupSystem.Suspension -> "bumps, kerbs and how the platform settles"
        SessionAnalysisSetupSystem.Balance -> "the same corners where balance repeats lap after lap"
    }
}

internal fun SessionAnalysisHighlightCategoryUi?.toDrivingLookAtLabel(cornerNumber: Int?): String {
    val zonePrefix = cornerNumber?.let { corner -> "Turn $corner: " }.orEmpty()
    return when (this) {
        SessionAnalysisHighlightCategoryUi.BrakePoint,
        SessionAnalysisHighlightCategoryUi.TrailBrakingMissing,
        SessionAnalysisHighlightCategoryUi.WheelLockup,
        -> zonePrefix + "entry brake trace, release timing and minimum speed"

        SessionAnalysisHighlightCategoryUi.EarlyApexEntry,
        SessionAnalysisHighlightCategoryUi.LateApexEntry,
        SessionAnalysisHighlightCategoryUi.InconsistentLine,
        -> zonePrefix + "turn-in point, apex location and steering trace"

        SessionAnalysisHighlightCategoryUi.CoastingZone,
        SessionAnalysisHighlightCategoryUi.WheelSpin,
        SessionAnalysisHighlightCategoryUi.ThrottleCommitment,
        -> zonePrefix + "first throttle pickup, steering unwind and exit speed"

        SessionAnalysisHighlightCategoryUi.Understeer,
        SessionAnalysisHighlightCategoryUi.Oversteer,
        -> zonePrefix + "steering angle versus car rotation through the loaded phase"

        else -> if (cornerNumber != null) {
            "Turn $cornerNumber: reference line, speed trace and input delta"
        } else {
            "reference line, speed trace and input delta"
        }
    }
}
