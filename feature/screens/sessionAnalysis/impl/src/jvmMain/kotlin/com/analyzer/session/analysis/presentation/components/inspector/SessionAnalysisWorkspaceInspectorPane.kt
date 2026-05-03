package com.analyzer.session.analysis.presentation.components.inspector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.SessionAnalysisUiTokens
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisAdaptiveVerticalPane
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisStudioPanel
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisStudioTile
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisCornerBreakdown
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisInspectorCoachSummaryCard
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisInspectorInputsCard
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisInspectorLiveBlock
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisInspectorNarrativeCard
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisInspectorSectorsCard
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisInspectorSummaryCard
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisInspectorTyreAnalyticsCard
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisInspectorTyreLiveCard
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisInspectorTyreWindowsCard
import com.analyzer.session.analysis.presentation.components.inspector.card.SessionAnalysisSetupAdvice
import com.analyzer.session.analysis.presentation.components.inspector.model.SessionAnalysisInspectorTab
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewCoach
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewDiagnosticSummary
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewHighlights
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewInspectorState
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewSectors
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewSummary
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorMetric
import com.analyzer.session.analysis.presentation.components.inspector.support.resolveLineNarratives
import com.analyzer.session.analysis.presentation.components.inspector.support.title
import com.analyzer.session.analysis.presentation.components.layout.model.SessionAnalysisPaneLayoutMode
import com.analyzer.session.analysis.presentation.components.navigator.preview.sessionAnalysisNavigatorPreviewHeader
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHeaderUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapCoachUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisScreenMode
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSummaryUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisInspectorState
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_metric_lap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_metric_reference
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_subtitle
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_lap_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_no_selection_placeholder
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

/**
 * Composes the inspector tabs and cards so detailed analysis stays separate from the main map workspace.
 */
@Composable
internal fun SessionAnalysisWorkspaceInspectorPane(
    header: SessionAnalysisHeaderUi,
    screenMode: SessionAnalysisScreenMode,
    summary: SessionAnalysisSummaryUi?,
    sectors: ImmutableList<SessionAnalysisSectorUi>,
    lapCoach: SessionAnalysisLapCoachUi?,
    diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?,
    highlights: ImmutableList<SessionAnalysisHighlightUi>,
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
    selectedLapNumber: Int?,
    referenceLapNumber: Int?,
    selectedTab: SessionAnalysisInspectorTab,
    inspectorState: SessionAnalysisInspectorState? = null,
    layoutMode: SessionAnalysisPaneLayoutMode = SessionAnalysisPaneLayoutMode.Bounded,
    modifier: Modifier = Modifier,
    onTabSelected: (SessionAnalysisInspectorTab) -> Unit = {},
    onTrackPositionSelected: (Float) -> Unit = {},
) {
    val resolvedInspectorState = inspectorState ?: SessionAnalysisInspectorState()
    val visibleTabs = remember(screenMode) { screenMode.visibleInspectorTabs() }
    val resolvedSelectedTab = if (selectedTab in visibleTabs) {
        selectedTab
    } else {
        visibleTabs.first()
    }

    SessionAnalysisStudioPanel(
        modifier = modifier,
        opaqueBackground = true,
    ) {
        SessionAnalysisAdaptiveVerticalPane(
            layoutMode = layoutMode,
            modifier = if (layoutMode == SessionAnalysisPaneLayoutMode.Bounded) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxWidth()
            },
            itemSpacing = SessionAnalysisUiTokens.sectionGap,
            contentPadding = PaddingValues(vertical = 2.dp),
            boundedScrollbarPadding = 2.dp,
        ) {
            SessionAnalysisInspectorHeader(
                selectedLapNumber = selectedLapNumber,
                referenceLapNumber = referenceLapNumber,
            )
            SessionAnalysisInspectorLiveBlock(
                activePoint = activePoint,
                activeSample = activeSample,
            )
            SessionAnalysisInspectorTabs(
                tabs = visibleTabs,
                selectedTab = resolvedSelectedTab,
                onTabSelected = onTabSelected,
            )
            SessionAnalysisInspectorContent(
                header = header,
                screenMode = screenMode,
                summary = summary,
                sectors = sectors,
                lapCoach = lapCoach,
                diagnosticSummary = diagnosticSummary,
                highlights = highlights,
                activePoint = activePoint,
                activeSample = activeSample,
                selectedTab = resolvedSelectedTab,
                inspectorState = resolvedInspectorState,
                onTrackPositionSelected = onTrackPositionSelected,
            )
        }
    }
}

@Composable
private fun SessionAnalysisInspectorHeader(selectedLapNumber: Int?, referenceLapNumber: Int?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.session_analysis_inspector_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.session_analysis_inspector_subtitle),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionAnalysisInspectorMetric(
                stringResource(Res.string.session_analysis_inspector_metric_lap),
                selectedLapNumber
                    ?.let { lapNumber -> stringResource(Res.string.session_analysis_lap_label, lapNumber) }
                    ?: stringResource(Res.string.session_analysis_no_selection_placeholder),
            )
            SessionAnalysisInspectorMetric(
                stringResource(Res.string.session_analysis_inspector_metric_reference),
                referenceLapNumber
                    ?.let { lapNumber -> stringResource(Res.string.session_analysis_lap_label, lapNumber) }
                    ?: stringResource(Res.string.session_analysis_no_selection_placeholder),
            )
        }
    }
}

@Composable
private fun SessionAnalysisInspectorTabs(
    tabs: List<SessionAnalysisInspectorTab>,
    selectedTab: SessionAnalysisInspectorTab,
    onTabSelected: (SessionAnalysisInspectorTab) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.chipGap),
        verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.chipGap),
    ) {
        tabs.forEach { tab ->
            SessionAnalysisStudioTile(
                selected = tab == selectedTab,
                contentPadding = PaddingValues(
                    horizontal = SessionAnalysisUiTokens.tileHorizontalPadding,
                    vertical = SessionAnalysisUiTokens.tileVerticalPadding,
                ),
                onClick = { onTabSelected(tab) },
            ) {
                Box {
                    Text(
                        text = tab.title,
                        color = if (tab == selectedTab) {
                            SimAnalyzerTheme.material.primary
                        } else {
                            SimAnalyzerTheme.material.onSurfaceVariant
                        },
                        style = if (tab == selectedTab) {
                            SimAnalyzerTheme.typography.labelLarge
                        } else {
                            SimAnalyzerTheme.typography.labelMedium
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionAnalysisInspectorContent(
    header: SessionAnalysisHeaderUi,
    screenMode: SessionAnalysisScreenMode,
    summary: SessionAnalysisSummaryUi?,
    sectors: ImmutableList<SessionAnalysisSectorUi>,
    lapCoach: SessionAnalysisLapCoachUi?,
    diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?,
    highlights: ImmutableList<SessionAnalysisHighlightUi>,
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
    selectedTab: SessionAnalysisInspectorTab,
    inspectorState: SessionAnalysisInspectorState,
    onTrackPositionSelected: (Float) -> Unit,
) {
    val lineNarratives = remember(diagnosticSummary, lapCoach, highlights) {
        resolveLineNarratives(
            diagnosticSummary = diagnosticSummary,
            lapCoach = lapCoach,
            highlights = highlights,
        )
    }

    when (selectedTab) {
        SessionAnalysisInspectorTab.Timing -> {
            summary?.let { SessionAnalysisInspectorSummaryCard(summary = it) }
            if (sectors.isNotEmpty()) {
                SessionAnalysisInspectorSectorsCard(sectors = sectors)
            }
        }

        SessionAnalysisInspectorTab.Corners -> {
            diagnosticSummary?.let {
                SessionAnalysisCornerBreakdown(
                    summary = it,
                    onTrackPositionSelected = onTrackPositionSelected,
                )
            }
        }

        SessionAnalysisInspectorTab.Line -> {
            lapCoach?.let { coach ->
                SessionAnalysisInspectorCoachSummaryCard(coach = coach)
            }
            lineNarratives.forEach { narrative ->
                SessionAnalysisInspectorNarrativeCard(
                    title = narrative.title,
                    description = narrative.description,
                    recommendation = narrative.recommendation,
                    lookAt = narrative.lookAt,
                    source = narrative.source,
                )
            }
        }

        SessionAnalysisInspectorTab.Setup -> {
            if (screenMode == SessionAnalysisScreenMode.Analysis) {
                SessionAnalysisSetupAdvice(
                    highlights = highlights,
                    summary = diagnosticSummary,
                    onTrackPositionSelected = onTrackPositionSelected,
                )
            }
        }

        SessionAnalysisInspectorTab.Inputs -> {
            SessionAnalysisInspectorInputsCard(
                activePoint = activePoint,
                activeSample = activeSample,
            )
        }

        SessionAnalysisInspectorTab.Tyres -> {
            SessionAnalysisInspectorTyreWindowsCard(header = header)
            SessionAnalysisInspectorTyreAnalyticsCard(
                selectedAnalytics = inspectorState.selectedTyreAnalytics,
                referenceAnalytics = inspectorState.referenceTyreAnalytics,
            )
            SessionAnalysisInspectorTyreLiveCard(activeSample = activeSample)
        }
    }
}

@Preview
@Composable
internal fun SessionAnalysisWorkspaceInspectorPanePreview() {
    SimAnalyzerTheme {
        SessionAnalysisWorkspaceInspectorPane(
            header = sessionAnalysisNavigatorPreviewHeader(),
            screenMode = SessionAnalysisScreenMode.Analysis,
            summary = sessionAnalysisInspectorPreviewSummary(),
            sectors = sessionAnalysisInspectorPreviewSectors(),
            lapCoach = sessionAnalysisInspectorPreviewCoach(),
            diagnosticSummary = sessionAnalysisInspectorPreviewDiagnosticSummary(),
            highlights = sessionAnalysisInspectorPreviewHighlights(),
            activePoint = sessionAnalysisInspectorPreviewActivePoint(),
            activeSample = sessionAnalysisInspectorPreviewActiveSample(),
            selectedLapNumber = 7,
            referenceLapNumber = 6,
            selectedTab = SessionAnalysisInspectorTab.Setup,
            inspectorState = sessionAnalysisInspectorPreviewInspectorState(),
            onTrackPositionSelected = {},
        )
    }
}

private fun SessionAnalysisScreenMode.visibleInspectorTabs(): List<SessionAnalysisInspectorTab> = when (this) {
    SessionAnalysisScreenMode.Analysis -> SessionAnalysisInspectorTab.entries

    SessionAnalysisScreenMode.Comparison -> listOf(
        SessionAnalysisInspectorTab.Timing,
        SessionAnalysisInspectorTab.Inputs,
        SessionAnalysisInspectorTab.Tyres,
    )
}
