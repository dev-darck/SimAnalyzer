package com.analyzer.session.analysis.presentation.components.inspector.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewCoach
import com.analyzer.session.analysis.presentation.components.inspector.preview.sessionAnalysisInspectorPreviewHighlights
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorCard
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorDetailRow
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorMetric
import com.analyzer.session.analysis.presentation.components.inspector.section.SessionAnalysisInspectorValueRow
import com.analyzer.session.analysis.presentation.components.inspector.support.fixInLabel
import com.analyzer.session.analysis.presentation.formatter.formatDegrees
import com.analyzer.session.analysis.presentation.formatter.formatDelta
import com.analyzer.session.analysis.presentation.formatter.formatFuel
import com.analyzer.session.analysis.presentation.formatter.formatGear
import com.analyzer.session.analysis.presentation.formatter.formatPercent
import com.analyzer.session.analysis.presentation.formatter.formatRpm
import com.analyzer.session.analysis.presentation.formatter.formatSpeed
import com.analyzer.session.analysis.presentation.formatter.formatTrackPosition
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosisSourceUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapCoachUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_fuel
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_gear_rpm
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_card_current_point
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_card_live_cursor
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_change_first
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_fix_in
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_watch
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_label_why
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_pos
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_reference_brake
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_reference_speed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_reference_steer
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_reference_throttle
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_brake
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_delta
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_rpm
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_speed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_steer
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_throttle
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_track_position
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisInspectorCoachSummaryCard(coach: SessionAnalysisLapCoachUi) {
    SessionAnalysisInspectorCard(title = coach.summaryTitle) {
        Text(
            text = coach.summaryDescription,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            coach.metrics.forEach { metric ->
                SessionAnalysisInspectorValueRow(metric.label, metric.value)
            }
        }
    }
}

@Composable
internal fun SessionAnalysisInspectorNarrativeCard(
    title: String,
    description: String,
    recommendation: String = "",
    lookAt: String = "",
    source: SessionAnalysisDiagnosisSourceUi = SessionAnalysisDiagnosisSourceUi.DrivingStyle,
) {
    SessionAnalysisInspectorCard(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionAnalysisInspectorDetailRow(
                label = stringResource(Res.string.session_analysis_inspector_label_watch),
                value = lookAt,
            )
            if (recommendation.isNotBlank()) {
                SessionAnalysisInspectorDetailRow(
                    label = stringResource(Res.string.session_analysis_inspector_label_change_first),
                    value = recommendation,
                )
            }
            SessionAnalysisInspectorDetailRow(
                label = stringResource(Res.string.session_analysis_inspector_label_fix_in),
                value = source.fixInLabel(),
            )
            if (description.isNotBlank()) {
                Text(
                    text = stringResource(Res.string.session_analysis_inspector_label_why),
                    color = SimAnalyzerTheme.material.onSurface,
                    style = SimAnalyzerTheme.typography.labelSmall,
                )
                Text(
                    text = description,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
internal fun SessionAnalysisInspectorInputsCard(
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
) {
    SessionAnalysisInspectorCard(title = stringResource(Res.string.session_analysis_inspector_card_current_point)) {
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_metric_track_position),
            formatTrackPosition(activePoint?.trackPosition),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_metric_delta),
            formatDelta(activePoint?.deltaMs),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_metric_speed),
            formatSpeed(activePoint?.selectedSpeedKmh),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_reference_speed),
            formatSpeed(activePoint?.referenceSpeedKmh),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_metric_throttle),
            formatPercent(activePoint?.selectedThrottle),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_reference_throttle),
            formatPercent(activePoint?.referenceThrottle),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_metric_brake),
            formatPercent(activePoint?.selectedBrake),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_reference_brake),
            formatPercent(activePoint?.referenceBrake),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_metric_steer),
            formatDegrees(activePoint?.selectedSteeringAngleRad),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_inspector_reference_steer),
            formatDegrees(activePoint?.referenceSteeringAngleRad),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_hero_gear_rpm),
            formatGear(activeSample?.gear),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_metric_rpm),
            formatRpm(activeSample?.rpm),
        )
        SessionAnalysisInspectorValueRow(
            stringResource(Res.string.session_analysis_hero_fuel),
            formatFuel(activeSample?.fuelLiters),
        )
    }
}

@Composable
internal fun SessionAnalysisInspectorLiveBlock(
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
) {
    SessionAnalysisInspectorCard(title = stringResource(Res.string.session_analysis_inspector_card_live_cursor)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionAnalysisInspectorMetric(
                stringResource(Res.string.session_analysis_inspector_pos),
                formatTrackPosition(activePoint?.trackPosition),
            )
            SessionAnalysisInspectorMetric(
                stringResource(Res.string.session_analysis_metric_delta),
                formatDelta(activePoint?.deltaMs),
            )
            SessionAnalysisInspectorMetric(
                stringResource(Res.string.session_analysis_metric_speed),
                formatSpeed(activeSample?.speedKmh),
            )
            SessionAnalysisInspectorMetric(
                stringResource(Res.string.session_analysis_metric_brake),
                formatPercent(activeSample?.brake),
            )
            SessionAnalysisInspectorMetric(
                stringResource(Res.string.session_analysis_metric_throttle),
                formatPercent(activeSample?.throttle),
            )
            SessionAnalysisInspectorMetric(
                stringResource(Res.string.session_analysis_hero_gear_rpm),
                formatGear(activeSample?.gear),
            )
        }
    }
}

@Preview
@Composable
internal fun SessionAnalysisInspectorNarrativeCardsPreview() {
    val coach = sessionAnalysisInspectorPreviewCoach()
    val highlight = sessionAnalysisInspectorPreviewHighlights().first()

    SimAnalyzerTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SessionAnalysisInspectorCoachSummaryCard(coach = coach)
            SessionAnalysisInspectorNarrativeCard(
                title = highlight.title,
                description = highlight.description,
                recommendation = highlight.recommendation,
                lookAt = "Turn 7, brake release and entry speed",
            )
            SessionAnalysisInspectorInputsCard(
                activePoint = sessionAnalysisInspectorPreviewActivePoint(),
                activeSample = sessionAnalysisInspectorPreviewActiveSample(),
            )
            SessionAnalysisInspectorLiveBlock(
                activePoint = sessionAnalysisInspectorPreviewActivePoint(),
                activeSample = sessionAnalysisInspectorPreviewActiveSample(),
            )
        }
    }
}
