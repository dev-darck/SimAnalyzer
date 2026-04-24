package com.analyzer.session.analysis.presentation.components.hero

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosisSourceUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightCategoryUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_corner_chip
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_cues
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_fix_prefix
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_focus_driving
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_focus_setup
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_gain_chip
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_loading_short
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_no_major_issue
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_overview_empty
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_overview_loading
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_overview_pending
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_overview_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_score_band_attention
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_score_band_consistent
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_score_band_work
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_score_driving
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_score_overall
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_score_setup
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnostic_turns
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

/**
 * Shows a compact diagnostic snapshot that stays readable during loading and then collapses the
 * strongest driving and setup cues into a single hero-side card.
 */
@Composable
internal fun SessionAnalysisDiagnosticOverview(
    summary: SessionAnalysisDiagnosticSummaryUi?,
    isLoading: Boolean,
    highlightsCount: Int,
    turnCount: Int?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = SimAnalyzerTheme.corners.panel,
        color = SimAnalyzerTheme.material.surface,
        border = BorderStroke(1.dp, SimAnalyzerTheme.chrome.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SessionAnalysisDiagnosticHeader(
                summary = summary,
                isLoading = isLoading,
                highlightsCount = highlightsCount,
                turnCount = turnCount,
            )

            SessionAnalysisDiagnosticScoreRow(summary = summary)
            summary ?: return@Surface

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SessionAnalysisDiagnosticCueLine(
                    title = stringResource(Res.string.session_analysis_diagnostic_focus_driving),
                    issue = summary.topDrivingIssues.firstOrNull(),
                    accent = SimAnalyzerTheme.extended.amber,
                )
                SessionAnalysisDiagnosticCueLine(
                    title = stringResource(Res.string.session_analysis_diagnostic_focus_setup),
                    issue = summary.topSetupIssues.firstOrNull(),
                    accent = SimAnalyzerTheme.extended.red,
                )
            }
        }
    }
}

@Composable
private fun SessionAnalysisDiagnosticScoreRow(summary: SessionAnalysisDiagnosticSummaryUi?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SessionAnalysisDiagnosticScoreChip(
            label = stringResource(Res.string.session_analysis_diagnostic_score_overall),
            score = summary?.overallScore?.toString() ?: "--",
            accent = summary?.let { scoreAccent(it.overallScore) } ?: SimAnalyzerTheme.material.onSurface,
            modifier = Modifier.weight(1f),
        )
        SessionAnalysisDiagnosticScoreChip(
            label = stringResource(Res.string.session_analysis_diagnostic_score_driving),
            score = summary?.drivingScore?.toString() ?: "--",
            accent = summary?.let { scoreAccent(it.drivingScore) } ?: SimAnalyzerTheme.material.onSurface,
            modifier = Modifier.weight(1f),
        )
        SessionAnalysisDiagnosticScoreChip(
            label = stringResource(Res.string.session_analysis_diagnostic_score_setup),
            score = summary?.setupScore?.toString() ?: "--",
            accent = summary?.let { scoreAccent(it.setupScore) } ?: SimAnalyzerTheme.material.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SessionAnalysisDiagnosticHeader(
    summary: SessionAnalysisDiagnosticSummaryUi?,
    isLoading: Boolean,
    highlightsCount: Int,
    turnCount: Int?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = stringResource(Res.string.session_analysis_diagnostic_overview_title),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary == null) {
                Text(
                    text = when {
                        isLoading -> stringResource(Res.string.session_analysis_diagnostic_overview_loading)
                        highlightsCount > 0 -> stringResource(Res.string.session_analysis_diagnostic_overview_pending)
                        else -> stringResource(Res.string.session_analysis_diagnostic_overview_empty)
                    },
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (summary != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SessionAnalysisDiagnosticMetaPill(
                    label = stringResource(
                        Res.string.session_analysis_diagnostic_turns,
                        turnCount ?: summary.cornerScores.size,
                    ),
                    accent = SimAnalyzerTheme.material.onSurfaceVariant,
                )
                SessionAnalysisDiagnosticMetaPill(
                    label = scoreBandLabel(summary.overallScore),
                    accent = scoreAccent(summary.overallScore),
                )
            }
        } else if (isLoading || highlightsCount > 0) {
            SessionAnalysisDiagnosticMetaPill(
                label = if (isLoading) {
                    stringResource(Res.string.session_analysis_diagnostic_loading_short)
                } else {
                    stringResource(Res.string.session_analysis_diagnostic_cues, highlightsCount)
                },
                accent = if (isLoading) {
                    SimAnalyzerTheme.material.onSurfaceVariant
                } else {
                    SimAnalyzerTheme.extended.cyan
                },
            )
        }
    }
}

@Composable
private fun SessionAnalysisDiagnosticScoreChip(
    label: String,
    score: String,
    accent: Color = SimAnalyzerTheme.material.onSurface,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 34.dp),
        shape = SimAnalyzerTheme.corners.item,
        color = SimAnalyzerTheme.chrome.fillMuted,
        border = BorderStroke(1.dp, SimAnalyzerTheme.chrome.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = score,
                color = accent,
                style = SimAnalyzerTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SessionAnalysisDiagnosticCueLine(title: String, issue: DiagnosticIssueUi?, accent: Color) {
    val recommendation = issue?.recommendation?.takeIf(String::isNotBlank)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SimAnalyzerTheme.corners.item,
        color = SimAnalyzerTheme.chrome.fillMuted,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = accent,
                style = SimAnalyzerTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            issue?.cornerNumber?.let { cornerNumber ->
                SessionAnalysisDiagnosticMetaPill(
                    label = stringResource(Res.string.session_analysis_diagnostic_corner_chip, cornerNumber),
                    accent = accent,
                )
            }
            issue?.potentialTimeGainMs?.takeIf { gain -> gain > 0 }?.let { gain ->
                SessionAnalysisDiagnosticMetaPill(
                    label = stringResource(Res.string.session_analysis_diagnostic_gain_chip, gain),
                    accent = accent,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = issue?.title ?: stringResource(Res.string.session_analysis_diagnostic_no_major_issue),
                    color = SimAnalyzerTheme.material.onSurface,
                    style = SimAnalyzerTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                recommendation?.let { action ->
                    Text(
                        text = stringResource(Res.string.session_analysis_diagnostic_fix_prefix, action),
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        style = SimAnalyzerTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionAnalysisDiagnosticMetaPill(label: String, accent: Color) {
    Surface(
        shape = SimAnalyzerTheme.corners.pill,
        color = SimAnalyzerTheme.chrome.fillMuted,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = accent,
            style = SimAnalyzerTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun scoreAccent(score: Int): Color = when {
    score >= 80 -> SimAnalyzerTheme.extended.teal
    score >= 60 -> SimAnalyzerTheme.extended.amber
    else -> SimAnalyzerTheme.extended.red
}

@Composable
private fun scoreBandLabel(score: Int): String = when {
    score >= 80 -> stringResource(Res.string.session_analysis_diagnostic_score_band_consistent)
    score >= 60 -> stringResource(Res.string.session_analysis_diagnostic_score_band_attention)
    else -> stringResource(Res.string.session_analysis_diagnostic_score_band_work)
}

@Preview
@Composable
internal fun SessionAnalysisDiagnosticOverviewPreview() {
    SimAnalyzerTheme {
        SessionAnalysisDiagnosticOverview(
            summary = SessionAnalysisDiagnosticSummaryUi(
                overallScore = 78,
                drivingScore = 82,
                setupScore = 69,
                topDrivingIssues = persistentListOf(
                    DiagnosticIssueUi(
                        title = "Brake release is late",
                        description = "You keep pressure too long into the apex.",
                        recommendation = "Bleed pressure earlier before rotation.",
                        priority = 1,
                        source = SessionAnalysisDiagnosisSourceUi.DrivingStyle,
                        potentialTimeGainMs = 182,
                        category = SessionAnalysisHighlightCategoryUi.TimeLoss,
                        cornerNumber = 7,
                        trackPosition = 0.42f,
                    ),
                ),
                topSetupIssues = persistentListOf(
                    DiagnosticIssueUi(
                        title = "Rear rotates on exit",
                        description = "The rear axle unloads too quickly on throttle.",
                        recommendation = "Stabilize rear damping or diff preload.",
                        priority = 2,
                        source = SessionAnalysisDiagnosisSourceUi.CarSetup,
                        potentialTimeGainMs = 96,
                        category = SessionAnalysisHighlightCategoryUi.Oversteer,
                        cornerNumber = 9,
                        trackPosition = 0.61f,
                    ),
                ),
            ),
            isLoading = false,
            highlightsCount = 3,
            turnCount = 14,
            modifier = Modifier.width(340.dp),
        )
    }
}
