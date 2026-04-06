@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
)

package com.analyzer.session.analysis.presentation.components.graph

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisAdaptiveVerticalPane
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisStudioPanel
import com.analyzer.session.analysis.presentation.components.graph.preview.sessionAnalysisGraphPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.graph.preview.sessionAnalysisGraphPreviewComparisonPoints
import com.analyzer.session.analysis.presentation.components.graph.preview.sessionAnalysisGraphPreviewState
import com.analyzer.session.analysis.presentation.components.graph.strip.SessionAnalysisTelemetryStrip
import com.analyzer.session.analysis.presentation.components.graph.support.toGraphLapLabel
import com.analyzer.session.analysis.presentation.components.layout.model.SessionAnalysisPaneLayoutMode
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisGraphState
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_cursor_live
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_cursor_pinned
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_empty_message
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_empty_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_subtitle
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_title
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Hosts the telemetry chart dock and keeps comparison graphs synchronized with the active workspace point.
 */
@Composable
internal fun SessionAnalysisWorkspaceGraphDock(
    graphState: SessionAnalysisGraphState,
    activePoint: SessionAnalysisComparisonPointUi?,
    selectionLocked: Boolean,
    selectedLapNumber: Int?,
    referenceLapNumber: Int?,
    boundedHeight: Boolean = false,
    modifier: Modifier = Modifier,
    onHoverFraction: (Float?) -> Unit = {},
    onPressFraction: (Float?) -> Unit = {},
) {
    val comparisonPoints = graphState.comparisonPoints
    if (graphState.charts.isEmpty() || comparisonPoints.isEmpty()) {
        SessionAnalysisWorkspaceGraphEmptyState(modifier = modifier)
        return
    }

    val resolvedActivePoint = activePoint ?: comparisonPoints.first()
    val selectedLapLabel = graphState.selectedLapLabel.takeUnless { it == "--" } ?: selectedLapNumber.toGraphLapLabel()
    val referenceLapLabel = graphState.referenceLapLabel.takeUnless { it == "--" }
        ?: referenceLapNumber.toGraphLapLabel()
    val layoutMode = if (boundedHeight) {
        SessionAnalysisPaneLayoutMode.Bounded
    } else {
        SessionAnalysisPaneLayoutMode.Embedded
    }

    SessionAnalysisStudioPanel(
        modifier = modifier,
        opaqueBackground = true,
    ) {
        Column(
            modifier = if (boundedHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SessionAnalysisWorkspaceGraphHeader(
                selectedLapLabel = selectedLapLabel,
                referenceLapLabel = referenceLapLabel,
                selectionLocked = selectionLocked,
            )

            Box(
                modifier = Modifier
                    .then(
                        if (boundedHeight) {
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        } else {
                            Modifier.fillMaxWidth()
                        },
                    )
                    .clip(SimAnalyzerTheme.corners.badge)
                    .background(SimAnalyzerTheme.chrome.fillMuted)
                    .onPointerEvent(PointerEventType.Exit) {
                        if (!selectionLocked) {
                            onHoverFraction(null)
                        }
                    },
            ) {
                SessionAnalysisAdaptiveVerticalPane(
                    layoutMode = layoutMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    itemSpacing = 0.dp,
                    boundedScrollbarPadding = 6.dp,
                ) {
                    graphState.charts.forEachIndexed { index, chart ->
                        SessionAnalysisTelemetryStrip(
                            chart = chart,
                            comparisonPoints = comparisonPoints,
                            activePoint = resolvedActivePoint,
                            selectionLocked = selectionLocked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(chart.height),
                            onHoverFraction = onHoverFraction,
                            onPressFraction = onPressFraction,
                        )
                        if (index != graphState.charts.lastIndex) {
                            HorizontalDivider(color = SimAnalyzerTheme.chrome.dividerSubtle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionAnalysisWorkspaceGraphEmptyState(modifier: Modifier = Modifier) {
    SessionAnalysisStudioPanel(
        modifier = modifier,
        opaqueBackground = true,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.session_analysis_graph_empty_title),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.session_analysis_graph_empty_message),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SessionAnalysisWorkspaceGraphHeader(
    selectedLapLabel: String,
    referenceLapLabel: String,
    selectionLocked: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.session_analysis_graph_title),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    Res.string.session_analysis_graph_subtitle,
                    selectedLapLabel,
                    referenceLapLabel,
                ),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
        Surface(
            shape = SimAnalyzerTheme.corners.pill,
            color = if (selectionLocked) {
                SimAnalyzerTheme.chrome.fillSelection
            } else {
                SimAnalyzerTheme.chrome.fillMuted
            },
            border = BorderStroke(1.dp, SimAnalyzerTheme.chrome.borderSubtle),
        ) {
            Text(
                text = if (selectionLocked) {
                    stringResource(Res.string.session_analysis_graph_cursor_pinned)
                } else {
                    stringResource(Res.string.session_analysis_graph_cursor_live)
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (selectionLocked) SimAnalyzerTheme.extended.amber else SimAnalyzerTheme.extended.teal,
                style = SimAnalyzerTheme.typography.labelMedium,
            )
        }
    }
}

@Preview
@Composable
internal fun SessionAnalysisWorkspaceGraphDockPreview() {
    val comparisonPoints = sessionAnalysisGraphPreviewComparisonPoints()

    SimAnalyzerTheme {
        SessionAnalysisWorkspaceGraphDock(
            graphState = sessionAnalysisGraphPreviewState(),
            activePoint = sessionAnalysisGraphPreviewActivePoint(),
            selectionLocked = false,
            selectedLapNumber = 7,
            referenceLapNumber = 3,
            boundedHeight = false,
            modifier = Modifier
                .width(1240.dp)
                .height(720.dp),
        )
    }
}
