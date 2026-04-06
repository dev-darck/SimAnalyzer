package com.analyzer.session.analysis.presentation.components.inspector.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewSectors
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewSummary
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorCard
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorValueRow
import com.analyzer.session.analysis.presentation.formatter.formatDelta
import com.analyzer.session.analysis.presentation.formatter.formatLapTime
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSummaryUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_fuel
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_card_lap_summary
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_card_sectors
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_consistency
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_lap_delta
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_reference
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_selected
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_delta
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_no_selection_placeholder
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_top_speed
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisInspectorSummaryCard(summary: SessionAnalysisSummaryUi) {
    SessionAnalysisInspectorCard(title = stringResource(Res.string.session_analysis_inspector_card_lap_summary)) {
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_lap_delta),
            summary.lapDeltaLabel,
        )
        SessionAnalysisInspectorValueRow(summary.biggestLossLabel, summary.biggestLossValueLabel)
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_consistency),
            summary.consistencyLabel,
        )
        SessionAnalysisInspectorValueRow(stringResource(Res.string.session_analysis_hero_fuel), summary.fuelLabel)
        SessionAnalysisInspectorValueRow(stringResource(Res.string.session_analysis_top_speed), summary.topSpeedLabel)
    }
}

@Composable
internal fun SessionAnalysisInspectorSectorsCard(sectors: ImmutableList<SessionAnalysisSectorUi>) {
    SessionAnalysisInspectorCard(title = stringResource(Res.string.session_analysis_inspector_card_sectors)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sectors.forEach { sector ->
                SessionAnalysisInspectorSectorRow(sector)
            }
        }
    }
}

@Composable
private fun SessionAnalysisInspectorSectorRow(sector: SessionAnalysisSectorUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        androidx.compose.material3.Text(
            text = sector.label,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.labelLarge,
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_selected),
            sector.selectedTimeMs?.let(::formatLapTime)
                ?: stringResource(Res.string.session_analysis_no_selection_placeholder),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_reference),
            sector.referenceTimeMs?.let(::formatLapTime)
                ?: stringResource(Res.string.session_analysis_no_selection_placeholder),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_metric_delta),
            formatDelta(sector.deltaMs),
        )
        androidx.compose.material3.Text(
            text = sector.note,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
    }
}

@Preview
@Composable
internal fun SessionAnalysisInspectorTimingCardsPreview() {
    SimAnalyzerTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SessionAnalysisInspectorSummaryCard(summary = sessionAnalysisInspectorPreviewSummary())
            SessionAnalysisInspectorSectorsCard(sectors = sessionAnalysisInspectorPreviewSectors())
        }
    }
}
