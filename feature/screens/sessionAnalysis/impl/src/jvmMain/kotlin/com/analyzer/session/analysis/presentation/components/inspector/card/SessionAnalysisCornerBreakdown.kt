package com.analyzer.session.analysis.presentation.components.inspector.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewDiagnosticSummary
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorCard
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorDetailRow
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorMetric
import com.analyzer.session.analysis.presentation.components.inspector.support.fixInLabel
import com.analyzer.session.analysis.presentation.components.inspector.support.lookAtLabel
import com.analyzer.session.analysis.presentation.formatter.formatDelta
import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnosis_driving
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnosis_mixed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnosis_setup
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_card_corner_breakdown
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_corner_empty
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_corner_hint
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_change_first
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_fix_in
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_watch
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_why
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_score
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_turn
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisCornerBreakdown(
    summary: SessionAnalysisDiagnosticSummaryUi,
    modifier: Modifier = Modifier,
    onTrackPositionSelected: (Float) -> Unit = {},
) {
    SessionAnalysisInspectorCard(title = stringResource(Res.string.session_analysis_inspector_card_corner_breakdown)) {
        if (summary.cornerScores.isEmpty()) {
            Text(
                text = stringResource(Res.string.session_analysis_inspector_corner_empty),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            return@SessionAnalysisInspectorCard
        }

        Text(
            text = stringResource(Res.string.session_analysis_inspector_corner_hint),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            summary.cornerScores.forEach { corner ->
                SessionAnalysisCornerBreakdownRow(
                    corner = corner,
                    onClick = { onTrackPositionSelected(corner.trackPosition) },
                )
            }
        }
    }
}

@Composable
private fun SessionAnalysisCornerBreakdownRow(corner: CornerScoreUi, onClick: () -> Unit) {
    val accent = when {
        corner.score >= 80 -> SimAnalyzerTheme.extended.teal
        corner.score >= 60 -> SimAnalyzerTheme.extended.amber
        else -> SimAnalyzerTheme.extended.red
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(accent.copy(alpha = 0.1f))
            .onClick(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.session_analysis_inspector_turn, corner.cornerNumber),
                modifier = Modifier.weight(1f),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SessionAnalysisInspectorMetric(
                label = stringResource(Res.string.session_analysis_inspector_score),
                value = corner.score.toString(),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionAnalysisCornerBadge(
                text = when (corner.source) {
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
            SessionAnalysisCornerBadge(
                text = formatDelta(corner.timeVsReferenceMs),
                accent = SimAnalyzerTheme.material.onSurfaceVariant,
            )
        }
        SessionAnalysisInspectorDetailRow(
            label = stringResource(Res.string.session_analysis_inspector_label_watch),
            value = corner.lookAtLabel(),
        )
        if (corner.recommendation.isNotBlank()) {
            SessionAnalysisInspectorDetailRow(
                label = stringResource(Res.string.session_analysis_inspector_label_change_first),
                value = corner.recommendation,
            )
        }
        SessionAnalysisInspectorDetailRow(
            label = stringResource(Res.string.session_analysis_inspector_label_fix_in),
            value = corner.source.fixInLabel(),
        )
        val whyText = listOfNotNull(
            corner.mainIssue?.takeIf(String::isNotBlank),
            corner.detail.takeIf(String::isNotBlank),
        ).joinToString(separator = " ")
        if (whyText.isNotBlank()) {
            Text(
                text = stringResource(Res.string.session_analysis_inspector_label_why),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
            Text(
                text = whyText,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun SessionAnalysisCornerBadge(text: String, accent: Color) {
    Text(
        text = text,
        modifier = Modifier
            .clip(SimAnalyzerTheme.shapes.small)
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        color = accent,
        style = SimAnalyzerTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview
@Composable
internal fun SessionAnalysisCornerBreakdownPreview() {
    SimAnalyzerTheme {
        SessionAnalysisCornerBreakdown(
            summary = sessionAnalysisInspectorPreviewDiagnosticSummary(),
            onTrackPositionSelected = {},
        )
    }
}
