package com.analyzer.session.analysis.presentation.model

import com.project.analyzer.telemetry.analysis.api.model.handling.SessionAnalysisHandlingState
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreTemperatureBand

internal enum class SessionAnalysisDiagnosisSourceUi {
    DrivingStyle,
    CarSetup,
    Mixed,
}

internal enum class SessionAnalysisHighlightCategoryUi {
    TopSpeed,
    ThrottleCommitment,
    BrakePoint,
    TimeLoss,
    Understeer,
    Oversteer,
    TyreOverheat,
    TyreCold,
    TrailBrakingMissing,
    EarlyApexEntry,
    LateApexEntry,
    CoastingZone,
    WheelLockup,
    WheelSpin,
    InconsistentLine,
    SetupUndersteer,
    SetupOversteer,
    TyrePressureImbalance,
    TyreTempImbalance,
    BrakeBalance,
    AeroBalance,
    DamperIssue,
}

internal enum class SessionAnalysisHighlightSeverityUi {
    Positive,
    Warning,
    Critical,
}

internal enum class SessionAnalysisHandlingStateUi {
    Neutral,
    Understeer,
    Oversteer,
}

internal enum class SessionAnalysisTyreTemperatureBandUi {
    Cold,
    Warming,
    Optimal,
    Warm,
    Hot,
    Overheated,
    Critical,
}

internal fun SessionAnalysisDiagnosisSource.toUi(): SessionAnalysisDiagnosisSourceUi = when (this) {
    SessionAnalysisDiagnosisSource.DrivingStyle -> SessionAnalysisDiagnosisSourceUi.DrivingStyle
    SessionAnalysisDiagnosisSource.CarSetup -> SessionAnalysisDiagnosisSourceUi.CarSetup
    SessionAnalysisDiagnosisSource.Mixed -> SessionAnalysisDiagnosisSourceUi.Mixed
}

internal fun SessionAnalysisHighlightCategory.toUi(): SessionAnalysisHighlightCategoryUi = when (this) {
    SessionAnalysisHighlightCategory.TopSpeed -> SessionAnalysisHighlightCategoryUi.TopSpeed
    SessionAnalysisHighlightCategory.ThrottleCommitment -> SessionAnalysisHighlightCategoryUi.ThrottleCommitment
    SessionAnalysisHighlightCategory.BrakePoint -> SessionAnalysisHighlightCategoryUi.BrakePoint
    SessionAnalysisHighlightCategory.TimeLoss -> SessionAnalysisHighlightCategoryUi.TimeLoss
    SessionAnalysisHighlightCategory.Understeer -> SessionAnalysisHighlightCategoryUi.Understeer
    SessionAnalysisHighlightCategory.Oversteer -> SessionAnalysisHighlightCategoryUi.Oversteer
    SessionAnalysisHighlightCategory.TyreOverheat -> SessionAnalysisHighlightCategoryUi.TyreOverheat
    SessionAnalysisHighlightCategory.TyreCold -> SessionAnalysisHighlightCategoryUi.TyreCold
    SessionAnalysisHighlightCategory.TrailBrakingMissing -> SessionAnalysisHighlightCategoryUi.TrailBrakingMissing
    SessionAnalysisHighlightCategory.EarlyApexEntry -> SessionAnalysisHighlightCategoryUi.EarlyApexEntry
    SessionAnalysisHighlightCategory.LateApexEntry -> SessionAnalysisHighlightCategoryUi.LateApexEntry
    SessionAnalysisHighlightCategory.CoastingZone -> SessionAnalysisHighlightCategoryUi.CoastingZone
    SessionAnalysisHighlightCategory.WheelLockup -> SessionAnalysisHighlightCategoryUi.WheelLockup
    SessionAnalysisHighlightCategory.WheelSpin -> SessionAnalysisHighlightCategoryUi.WheelSpin
    SessionAnalysisHighlightCategory.InconsistentLine -> SessionAnalysisHighlightCategoryUi.InconsistentLine
    SessionAnalysisHighlightCategory.SetupUndersteer -> SessionAnalysisHighlightCategoryUi.SetupUndersteer
    SessionAnalysisHighlightCategory.SetupOversteer -> SessionAnalysisHighlightCategoryUi.SetupOversteer
    SessionAnalysisHighlightCategory.TyrePressureImbalance -> SessionAnalysisHighlightCategoryUi.TyrePressureImbalance
    SessionAnalysisHighlightCategory.TyreTempImbalance -> SessionAnalysisHighlightCategoryUi.TyreTempImbalance
    SessionAnalysisHighlightCategory.BrakeBalance -> SessionAnalysisHighlightCategoryUi.BrakeBalance
    SessionAnalysisHighlightCategory.AeroBalance -> SessionAnalysisHighlightCategoryUi.AeroBalance
    SessionAnalysisHighlightCategory.DamperIssue -> SessionAnalysisHighlightCategoryUi.DamperIssue
}

internal fun SessionAnalysisHighlightSeverity.toUi(): SessionAnalysisHighlightSeverityUi = when (this) {
    SessionAnalysisHighlightSeverity.Positive -> SessionAnalysisHighlightSeverityUi.Positive
    SessionAnalysisHighlightSeverity.Warning -> SessionAnalysisHighlightSeverityUi.Warning
    SessionAnalysisHighlightSeverity.Critical -> SessionAnalysisHighlightSeverityUi.Critical
}

internal fun SessionAnalysisHandlingState.toUi(): SessionAnalysisHandlingStateUi = when (this) {
    SessionAnalysisHandlingState.Neutral -> SessionAnalysisHandlingStateUi.Neutral
    SessionAnalysisHandlingState.Understeer -> SessionAnalysisHandlingStateUi.Understeer
    SessionAnalysisHandlingState.Oversteer -> SessionAnalysisHandlingStateUi.Oversteer
}

internal fun SessionAnalysisTyreTemperatureBand.toUi(): SessionAnalysisTyreTemperatureBandUi = when (this) {
    SessionAnalysisTyreTemperatureBand.Cold -> SessionAnalysisTyreTemperatureBandUi.Cold
    SessionAnalysisTyreTemperatureBand.Warming -> SessionAnalysisTyreTemperatureBandUi.Warming
    SessionAnalysisTyreTemperatureBand.Optimal -> SessionAnalysisTyreTemperatureBandUi.Optimal
    SessionAnalysisTyreTemperatureBand.Warm -> SessionAnalysisTyreTemperatureBandUi.Warm
    SessionAnalysisTyreTemperatureBand.Hot -> SessionAnalysisTyreTemperatureBandUi.Hot
    SessionAnalysisTyreTemperatureBand.Overheated -> SessionAnalysisTyreTemperatureBandUi.Overheated
    SessionAnalysisTyreTemperatureBand.Critical -> SessionAnalysisTyreTemperatureBandUi.Critical
}
