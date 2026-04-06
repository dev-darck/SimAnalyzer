package com.analyzer.session.analysis.presentation.components.inspector.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewInspectorState
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorCard
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorValueRow
import com.analyzer.session.analysis.presentation.components.inspector.support.formatPressureDelta
import com.analyzer.session.analysis.presentation.components.inspector.support.formatTemperatureDelta
import com.analyzer.session.analysis.presentation.components.navigator.preview.sessionAnalysisNavigatorPreviewHeader
import com.analyzer.session.analysis.presentation.formatter.formatPercent
import com.analyzer.session.analysis.presentation.formatter.formatPressure
import com.analyzer.session.analysis.presentation.formatter.formatTemperature
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHeaderUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTyreAnalyticsUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_avg_core
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_avg_core_vs_ref
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_brake_window
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_card_live_tyre_state
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_card_tyre_analytics
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_card_tyre_windows
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_core_window
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_fl_core
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_fl_pressure
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_fr_core
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_fr_pressure
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_hottest_corner
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_peak_slip
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_pressure_spread
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_rl_core
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_rl_pressure
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_rr_core
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_rr_pressure
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_surface_window
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_peak_brake
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_peak_core
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisInspectorTyreWindowsCard(header: SessionAnalysisHeaderUi) {
    SessionAnalysisInspectorCard(title = stringResource(Res.string.session_analysis_inspector_card_tyre_windows)) {
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_surface_window),
            header.surfaceWindowLabel,
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_core_window),
            header.coreWindowLabel,
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_brake_window),
            header.brakeWindowLabel,
        )
    }
}

@Composable
internal fun SessionAnalysisInspectorTyreLiveCard(activeSample: SessionAnalysisSampleUi?) {
    SessionAnalysisInspectorCard(title = stringResource(Res.string.session_analysis_inspector_card_live_tyre_state)) {
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_fl_core),
            formatTemperature(activeSample?.tyreFl?.coreTempC),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_fr_core),
            formatTemperature(activeSample?.tyreFr?.coreTempC),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_rl_core),
            formatTemperature(activeSample?.tyreRl?.coreTempC),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_rr_core),
            formatTemperature(activeSample?.tyreRr?.coreTempC),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_fl_pressure),
            formatPressure(activeSample?.tyreFl?.pressurePsi),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_fr_pressure),
            formatPressure(activeSample?.tyreFr?.pressurePsi),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_rl_pressure),
            formatPressure(activeSample?.tyreRl?.pressurePsi),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_rr_pressure),
            formatPressure(activeSample?.tyreRr?.pressurePsi),
        )
    }
}

@Composable
internal fun SessionAnalysisInspectorTyreAnalyticsCard(
    selectedAnalytics: SessionAnalysisTyreAnalyticsUi,
    referenceAnalytics: SessionAnalysisTyreAnalyticsUi,
) {
    SessionAnalysisInspectorCard(title = stringResource(Res.string.session_analysis_inspector_card_tyre_analytics)) {
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_avg_core),
            formatTemperature(selectedAnalytics.avgCoreTempC),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_avg_core_vs_ref),
            formatTemperatureDelta(selectedAnalytics.avgCoreTempC, referenceAnalytics.avgCoreTempC),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_peak_core),
            formatTemperature(selectedAnalytics.peakCoreTempC),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_peak_brake),
            formatTemperature(selectedAnalytics.peakBrakeTempC),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_pressure_spread),
            formatPressureDelta(selectedAnalytics.pressureSpreadPsi),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_hottest_corner),
            selectedAnalytics.hottestTyreLabel,
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_peak_slip),
            formatPercent(selectedAnalytics.peakSlip),
        )
    }
}

@Preview
@Composable
internal fun SessionAnalysisInspectorTyreCardsPreview() {
    val inspectorState = sessionAnalysisInspectorPreviewInspectorState()

    SimAnalyzerTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SessionAnalysisInspectorTyreWindowsCard(header = sessionAnalysisNavigatorPreviewHeader())
            SessionAnalysisInspectorTyreLiveCard(activeSample = sessionAnalysisInspectorPreviewActiveSample())
            SessionAnalysisInspectorTyreAnalyticsCard(
                selectedAnalytics = inspectorState.selectedTyreAnalytics,
                referenceAnalytics = inspectorState.referenceTyreAnalytics,
            )
        }
    }
}
