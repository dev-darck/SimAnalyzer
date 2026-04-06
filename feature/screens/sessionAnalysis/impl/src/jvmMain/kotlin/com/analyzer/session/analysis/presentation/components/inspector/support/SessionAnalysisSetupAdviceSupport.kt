package com.analyzer.session.analysis.presentation.components.inspector.support

import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory

/**
 * Groups setup highlights into the advice shapes expected by the inspector cards.
 */
internal fun SessionAnalysisHighlightUi.setupSystem(): SessionAnalysisSetupSystem = when (category) {
    SessionAnalysisHighlightCategory.TyrePressureImbalance,
    SessionAnalysisHighlightCategory.TyreTempImbalance,
    SessionAnalysisHighlightCategory.TyreOverheat,
    SessionAnalysisHighlightCategory.TyreCold,
        -> SessionAnalysisSetupSystem.Tyres

    SessionAnalysisHighlightCategory.AeroBalance -> SessionAnalysisSetupSystem.Aero

    SessionAnalysisHighlightCategory.BrakeBalance,
    SessionAnalysisHighlightCategory.WheelLockup,
        -> SessionAnalysisSetupSystem.Brakes

    SessionAnalysisHighlightCategory.DamperIssue,
        -> SessionAnalysisSetupSystem.Suspension

    SessionAnalysisHighlightCategory.SetupUndersteer,
    SessionAnalysisHighlightCategory.SetupOversteer,
        -> SessionAnalysisSetupSystem.Balance

    else -> SessionAnalysisSetupSystem.Balance
}

internal fun DiagnosticIssueUi.setupSystem(): SessionAnalysisSetupSystem = when (category) {
    SessionAnalysisHighlightCategory.TyrePressureImbalance,
    SessionAnalysisHighlightCategory.TyreTempImbalance,
    SessionAnalysisHighlightCategory.TyreOverheat,
    SessionAnalysisHighlightCategory.TyreCold,
        -> SessionAnalysisSetupSystem.Tyres

    SessionAnalysisHighlightCategory.AeroBalance -> SessionAnalysisSetupSystem.Aero

    SessionAnalysisHighlightCategory.BrakeBalance,
    SessionAnalysisHighlightCategory.WheelLockup,
        -> SessionAnalysisSetupSystem.Brakes

    SessionAnalysisHighlightCategory.DamperIssue,
        -> SessionAnalysisSetupSystem.Suspension

    SessionAnalysisHighlightCategory.SetupUndersteer,
    SessionAnalysisHighlightCategory.SetupOversteer,
        -> SessionAnalysisSetupSystem.Balance

    else -> SessionAnalysisSetupSystem.Balance
}
