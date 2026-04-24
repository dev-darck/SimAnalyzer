package com.analyzer.session.analysis.presentation.components.inspector.support

import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightCategoryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi

/**
 * Groups setup highlights into the advice shapes expected by the inspector cards.
 */
internal fun SessionAnalysisHighlightUi.setupSystem(): SessionAnalysisSetupSystem = when (category) {
    SessionAnalysisHighlightCategoryUi.TyrePressureImbalance,
    SessionAnalysisHighlightCategoryUi.TyreTempImbalance,
    SessionAnalysisHighlightCategoryUi.TyreOverheat,
    SessionAnalysisHighlightCategoryUi.TyreCold,
    -> SessionAnalysisSetupSystem.Tyres

    SessionAnalysisHighlightCategoryUi.AeroBalance -> SessionAnalysisSetupSystem.Aero

    SessionAnalysisHighlightCategoryUi.BrakeBalance,
    SessionAnalysisHighlightCategoryUi.WheelLockup,
    -> SessionAnalysisSetupSystem.Brakes

    SessionAnalysisHighlightCategoryUi.DamperIssue,
    -> SessionAnalysisSetupSystem.Suspension

    SessionAnalysisHighlightCategoryUi.SetupUndersteer,
    SessionAnalysisHighlightCategoryUi.SetupOversteer,
    -> SessionAnalysisSetupSystem.Balance

    else -> SessionAnalysisSetupSystem.Balance
}

internal fun DiagnosticIssueUi.setupSystem(): SessionAnalysisSetupSystem = when (category) {
    SessionAnalysisHighlightCategoryUi.TyrePressureImbalance,
    SessionAnalysisHighlightCategoryUi.TyreTempImbalance,
    SessionAnalysisHighlightCategoryUi.TyreOverheat,
    SessionAnalysisHighlightCategoryUi.TyreCold,
    -> SessionAnalysisSetupSystem.Tyres

    SessionAnalysisHighlightCategoryUi.AeroBalance -> SessionAnalysisSetupSystem.Aero

    SessionAnalysisHighlightCategoryUi.BrakeBalance,
    SessionAnalysisHighlightCategoryUi.WheelLockup,
    -> SessionAnalysisSetupSystem.Brakes

    SessionAnalysisHighlightCategoryUi.DamperIssue,
    -> SessionAnalysisSetupSystem.Suspension

    SessionAnalysisHighlightCategoryUi.SetupUndersteer,
    SessionAnalysisHighlightCategoryUi.SetupOversteer,
    -> SessionAnalysisSetupSystem.Balance

    else -> SessionAnalysisSetupSystem.Balance
}
