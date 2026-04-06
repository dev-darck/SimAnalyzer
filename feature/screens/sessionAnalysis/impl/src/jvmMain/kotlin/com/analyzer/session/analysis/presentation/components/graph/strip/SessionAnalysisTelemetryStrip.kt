package com.analyzer.session.analysis.presentation.components.graph.strip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.graph.preview.sessionAnalysisGraphPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.graph.preview.sessionAnalysisGraphPreviewComparisonPoints
import com.analyzer.session.analysis.presentation.components.graph.preview.sessionAnalysisGraphPreviewSpeedChart
import com.analyzer.session.analysis.presentation.components.graph.support.chartDisplayTitle
import com.analyzer.session.analysis.presentation.components.graph.support.chartValueLabel
import com.analyzer.session.analysis.presentation.components.graph.support.rememberSessionAnalysisTelemetryTonePalette
import com.analyzer.session.analysis.presentation.components.graph.support.resolveFractionDomain
import com.analyzer.session.analysis.presentation.components.graph.support.toneColor
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetrySeriesTone
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_legend_lap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_legend_ref
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_selection_pinned
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisTelemetryStrip(
    chart: SessionAnalysisTelemetryChartUi,
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    activePoint: SessionAnalysisComparisonPointUi,
    selectionLocked: Boolean,
    modifier: Modifier = Modifier,
    onHoverFraction: (Float?) -> Unit = {},
    onPressFraction: (Float?) -> Unit = {},
) {
    val (chartDomainStart, chartDomainEnd) = remember(chart.series) {
        chart.resolveFractionDomain()
    }
    val tonePalette = rememberSessionAnalysisTelemetryTonePalette()
    val lapToneColor = toneColor(
        tone = chart.series.firstOrNull { series -> !series.dashed }?.tone
            ?: SessionAnalysisTelemetrySeriesTone.Primary,
        palette = tonePalette,
    )

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionAnalysisTelemetryStripMeta(
            chart = chart,
            activePoint = activePoint,
            selectionLocked = selectionLocked,
            lapToneColor = lapToneColor,
            referenceToneColor = tonePalette.referenceToneColor,
        )
        SessionAnalysisTelemetryStripChart(
            chart = chart,
            comparisonPoints = comparisonPoints,
            activePoint = activePoint,
            selectionLocked = selectionLocked,
            chartDomainStart = chartDomainStart,
            chartDomainEnd = chartDomainEnd,
            onHoverFraction = onHoverFraction,
            onPressFraction = onPressFraction,
        )
    }
}

@Composable
private fun SessionAnalysisTelemetryStripMeta(
    chart: SessionAnalysisTelemetryChartUi,
    activePoint: SessionAnalysisComparisonPointUi,
    selectionLocked: Boolean,
    lapToneColor: Color,
    referenceToneColor: Color,
) {
    Column(
        modifier = Modifier.width(108.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = chartDisplayTitle(chart.kind),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.labelLarge,
        )
        Text(
            text = chartValueLabel(chart.kind, activePoint),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        if (chart.series.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionAnalysisStripLegendItem(
                    label = stringResource(Res.string.session_analysis_graph_legend_lap),
                    color = lapToneColor,
                )
                SessionAnalysisStripLegendItem(
                    label = stringResource(Res.string.session_analysis_graph_legend_ref),
                    color = referenceToneColor,
                )
            }
        }
        if (selectionLocked) {
            Text(
                text = stringResource(Res.string.session_analysis_graph_selection_pinned),
                color = SimAnalyzerTheme.extended.amber,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SessionAnalysisStripLegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = SimAnalyzerTheme.corners.indicator),
        )
        Text(
            text = label,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
    }
}

@Preview
@Composable
internal fun SessionAnalysisTelemetryStripPreview() {
    SimAnalyzerTheme {
        SessionAnalysisTelemetryStrip(
            chart = sessionAnalysisGraphPreviewSpeedChart(),
            comparisonPoints = sessionAnalysisGraphPreviewComparisonPoints(),
            activePoint = sessionAnalysisGraphPreviewActivePoint(),
            selectionLocked = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
        )
    }
}
