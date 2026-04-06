package com.analyzer.session.analysis.presentation.components.inspector.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewDiagnosticSummary
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewHighlights
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorCard
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorDetailRow
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorMetric
import com.analyzer.session.analysis.presentation.components.inspector.support.SessionAnalysisSetupSystem
import com.analyzer.session.analysis.presentation.components.inspector.support.fixInLabel
import com.analyzer.session.analysis.presentation.components.inspector.support.setupSystem
import com.analyzer.session.analysis.presentation.components.inspector.support.toSetupAdviceItem
import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnosis_driving
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnosis_mixed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnosis_setup
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_card_setup_advice
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_change_first
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_fix_in
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_watch
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_why
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_setup_empty
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_setup_issues
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_setup_score
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_setup_system_aero
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_setup_system_balance
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_setup_system_brakes
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_setup_system_suspension
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_setup_system_tyres
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisSetupAdvice(
    highlights: ImmutableList<SessionAnalysisHighlightUi>,
    summary: SessionAnalysisDiagnosticSummaryUi?,
    onTrackPositionSelected: (Float) -> Unit = {},
) {
    val groupedIssues: Map<SessionAnalysisSetupSystem, List<SessionAnalysisSetupAdviceItem>> = remember(
        summary,
        highlights,
    ) {
        val summaryIssues = summary?.topSetupIssues.orEmpty()
        if (summaryIssues.isNotEmpty()) {
            summaryIssues
                .sortedWith(
                    compareByDescending(DiagnosticIssueUi::potentialTimeGainMs)
                        .thenByDescending(DiagnosticIssueUi::priority),
                )
                .groupBy(DiagnosticIssueUi::setupSystem)
                .mapValues { (_, issues) ->
                    issues
                        .take(2)
                        .map(DiagnosticIssueUi::toSetupAdviceItem)
                }
                .toSortedMap()
        } else {
            highlights
                .filter { highlight ->
                    highlight.diagnosisSource != SessionAnalysisDiagnosisSource.DrivingStyle &&
                        highlight.recommendation.isNotBlank()
                }
                .sortedWith(
                    compareByDescending(SessionAnalysisHighlightUi::priority)
                        .thenByDescending { highlight -> highlight.deltaMs ?: 0 },
                )
                .distinctBy { highlight ->
                    highlight.id.ifBlank { "${highlight.category}-${highlight.cornerNumber}-${highlight.title}" }
                }
                .groupBy(SessionAnalysisHighlightUi::setupSystem)
                .mapValues { (_, issueHighlights) ->
                    issueHighlights
                        .take(2)
                        .map(SessionAnalysisHighlightUi::toSetupAdviceItem)
                }
                .toSortedMap()
        }
    }

    SessionAnalysisInspectorCard(title = stringResource(Res.string.session_analysis_inspector_card_setup_advice)) {
        summary?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionAnalysisInspectorMetric(
                    stringResource(Res.string.session_analysis_inspector_setup_score),
                    "${it.setupScore}/100",
                )
                SessionAnalysisInspectorMetric(
                    stringResource(Res.string.session_analysis_inspector_setup_issues),
                    groupedIssues.values.sumOf { issues -> issues.size }.toString(),
                )
            }
        }
        if (groupedIssues.isEmpty()) {
            Text(
                text = stringResource(Res.string.session_analysis_inspector_setup_empty),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            return@SessionAnalysisInspectorCard
        }
        groupedIssues.forEach { (system, issues) ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = when (system) {
                        SessionAnalysisSetupSystem.Tyres -> stringResource(
                            Res.string.session_analysis_setup_system_tyres,
                        )

                        SessionAnalysisSetupSystem.Aero -> stringResource(Res.string.session_analysis_setup_system_aero)

                        SessionAnalysisSetupSystem.Brakes -> stringResource(
                            Res.string.session_analysis_setup_system_brakes,
                        )

                        SessionAnalysisSetupSystem.Suspension -> stringResource(
                            Res.string.session_analysis_setup_system_suspension,
                        )

                        SessionAnalysisSetupSystem.Balance -> stringResource(
                            Res.string.session_analysis_setup_system_balance,
                        )
                    },
                    color = SimAnalyzerTheme.material.onSurface,
                    style = SimAnalyzerTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                issues.forEach { issue ->
                    SessionAnalysisSetupAdviceRow(
                        issue = issue,
                        onClick = issue.trackPosition?.let { trackPosition ->
                            {
                                onTrackPositionSelected(trackPosition)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionAnalysisSetupAdviceRow(issue: SessionAnalysisSetupAdviceItem, onClick: (() -> Unit)?) {
    val accent = when (issue.source) {
        SessionAnalysisDiagnosisSource.CarSetup -> SimAnalyzerTheme.extended.red
        SessionAnalysisDiagnosisSource.Mixed -> SimAnalyzerTheme.extended.amber
        SessionAnalysisDiagnosisSource.DrivingStyle -> SimAnalyzerTheme.extended.teal
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(accent.copy(alpha = 0.08f))
            .then(if (onClick != null) Modifier.onClick(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = issue.title,
                modifier = Modifier.weight(1f),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SessionAnalysisCornerBadge(
                text = when (issue.source) {
                    SessionAnalysisDiagnosisSource.CarSetup -> stringResource(
                        Res.string.session_analysis_diagnosis_setup,
                    )

                    SessionAnalysisDiagnosisSource.Mixed -> stringResource(Res.string.session_analysis_diagnosis_mixed)

                    SessionAnalysisDiagnosisSource.DrivingStyle -> stringResource(
                        Res.string.session_analysis_diagnosis_driving,
                    )
                },
                accent = accent,
            )
        }
        SessionAnalysisInspectorDetailRow(
            label = stringResource(Res.string.session_analysis_inspector_label_watch),
            value = issue.lookAt,
        )
        if (issue.recommendation.isNotBlank()) {
            SessionAnalysisInspectorDetailRow(
                label = stringResource(Res.string.session_analysis_inspector_label_change_first),
                value = issue.recommendation,
            )
        }
        SessionAnalysisInspectorDetailRow(
            label = stringResource(Res.string.session_analysis_inspector_label_fix_in),
            value = issue.source.fixInLabel(),
        )
        if (issue.description.isNotBlank()) {
            Text(
                text = stringResource(Res.string.session_analysis_inspector_label_why),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
            Text(
                text = issue.description,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}

@Preview
@Composable
internal fun SessionAnalysisSetupAdvicePreview() {
    SimAnalyzerTheme {
        SessionAnalysisSetupAdvice(
            highlights = sessionAnalysisInspectorPreviewHighlights(),
            summary = sessionAnalysisInspectorPreviewDiagnosticSummary(),
            onTrackPositionSelected = {},
        )
    }
}
