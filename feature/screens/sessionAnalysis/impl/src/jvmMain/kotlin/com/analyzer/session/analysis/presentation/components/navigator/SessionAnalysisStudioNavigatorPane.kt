package com.analyzer.session.analysis.presentation.components.navigator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisAdaptiveVerticalPane
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisStudioPanel
import com.analyzer.session.analysis.presentation.components.layout.model.SessionAnalysisPaneLayoutMode
import com.analyzer.session.analysis.presentation.components.navigator.card.SessionAnalysisLapRailRow
import com.analyzer.session.analysis.presentation.components.navigator.preview.sessionAnalysisNavigatorPreviewHeader
import com.analyzer.session.analysis.presentation.components.navigator.preview.sessionAnalysisNavigatorPreviewHighlights
import com.analyzer.session.analysis.presentation.components.navigator.preview.sessionAnalysisNavigatorPreviewLaps
import com.analyzer.session.analysis.presentation.components.navigator.preview.sessionAnalysisNavigatorPreviewSessions
import com.analyzer.session.analysis.presentation.formatter.formatDelta
import com.analyzer.session.analysis.presentation.formatter.formatLapTime
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosisSourceUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHeaderUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSessionOptionUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnosis_driving
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnosis_mixed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_diagnosis_setup
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_turn
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_lap_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_action_use_best_as_ref
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_focus_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_header_delta
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_header_issue
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_header_lap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_header_ref
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_header_time
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_header_turn
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_history_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_info_best
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_info_reference
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_info_selected
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_laps_hint
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_my_laps_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_subtitle
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_no_selection_placeholder
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisStudioNavigatorPane(
    header: SessionAnalysisHeaderUi,
    sessionOptions: ImmutableList<SessionAnalysisSessionOptionUi>,
    selectedSessionId: Long?,
    laps: ImmutableList<SessionAnalysisLapSummaryUi>,
    selectedLapNumber: Int?,
    referenceLapNumber: Int?,
    referenceLapIsCustom: Boolean,
    highlights: ImmutableList<SessionAnalysisHighlightUi>,
    layoutMode: SessionAnalysisPaneLayoutMode = SessionAnalysisPaneLayoutMode.Bounded,
    modifier: Modifier = Modifier,
    onSessionSelected: (Long?) -> Unit = {},
    onLapSelected: (Int?) -> Unit = {},
    onReferenceLapSelected: (Int?) -> Unit = {},
) {
    val selectedLap = remember(laps, selectedLapNumber) {
        laps.firstOrNull { lap -> lap.lapNumber == selectedLapNumber }
    }
    val referenceLap = remember(laps, referenceLapNumber) {
        laps.firstOrNull { lap -> lap.lapNumber == referenceLapNumber }
    }
    val bestLap = remember(laps) {
        laps
            .filter { lap -> lap.isValid && lap.isComplete && !lap.isPitLap && lap.durationMs != null }
            .minByOrNull { lap -> lap.durationMs ?: Int.MAX_VALUE }
    }
    val focusQueue = remember(highlights) {
        highlights
            .sortedWith(
                compareByDescending<SessionAnalysisHighlightUi>(SessionAnalysisHighlightUi::priority)
                    .thenByDescending { highlight -> highlight.deltaMs ?: 0 },
            )
            .take(3)
    }

    SessionAnalysisStudioPanel(
        modifier = modifier,
        opaqueBackground = true,
    ) {
        SessionAnalysisAdaptiveVerticalPane(
            layoutMode = layoutMode,
            modifier = if (layoutMode == SessionAnalysisPaneLayoutMode.Bounded) {
                Modifier.fillMaxHeight()
            } else {
                Modifier
            },
            itemSpacing = 0.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            SessionAnalysisNavigatorHeaderBand(header = header)
            SessionAnalysisNavigatorSectionDivider()
            SessionAnalysisNavigatorCompareShelf(
                selectedLap = selectedLap,
                referenceLap = referenceLap,
                bestLap = bestLap,
                bestLapLabel = header.bestLapLabel,
                referenceLapIsCustom = referenceLapIsCustom,
                onReferenceReset = { onReferenceLapSelected(null) },
            )
            SessionAnalysisNavigatorSectionDivider()
            SessionAnalysisNavigatorSectionHeading(
                title = stringResource(Res.string.session_analysis_navigator_history_label),
            )
            sessionOptions
                .sortedByDescending { option -> option.segmentId == selectedSessionId }
                .forEachIndexed { index, option ->
                    if (index > 0) {
                        SessionAnalysisNavigatorRowDivider()
                    }
                    SessionAnalysisNavigatorSessionRow(
                        title = option.primaryLabel,
                        subtitle = option.supportingLabel,
                        selected = option.segmentId == selectedSessionId,
                        onClick = { onSessionSelected(option.segmentId) },
                    )
                }
            SessionAnalysisNavigatorSectionDivider()
            SessionAnalysisNavigatorSectionHeading(
                title = stringResource(Res.string.session_analysis_navigator_my_laps_label),
                subtitle = stringResource(Res.string.session_analysis_navigator_laps_hint),
            )
            SessionAnalysisNavigatorLapTableHeader()
            SessionAnalysisNavigatorRowDivider()
            laps.forEachIndexed { index, lap ->
                SessionAnalysisLapRailRow(
                    lap = lap,
                    isSelected = lap.lapNumber == selectedLapNumber,
                    isReference = lap.lapNumber == referenceLapNumber,
                    referenceLapIsCustom = referenceLapIsCustom,
                    onClick = { onLapSelected(lap.lapNumber) },
                    onReferenceClick = { onReferenceLapSelected(lap.lapNumber) },
                    onResetReferenceClick = { onReferenceLapSelected(null) },
                )
                if (index < laps.lastIndex) {
                    SessionAnalysisNavigatorRowDivider()
                }
            }
            if (focusQueue.isNotEmpty()) {
                SessionAnalysisNavigatorSectionDivider()
                SessionAnalysisNavigatorSectionHeading(
                    title = stringResource(Res.string.session_analysis_navigator_focus_label),
                )
                SessionAnalysisNavigatorFocusTableHeader()
                SessionAnalysisNavigatorRowDivider()
                focusQueue.forEachIndexed { index, highlight ->
                    SessionAnalysisNavigatorFocusRow(highlight = highlight)
                    if (index < focusQueue.lastIndex) {
                        SessionAnalysisNavigatorRowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionAnalysisNavigatorHeaderBand(header: SessionAnalysisHeaderUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SimAnalyzerTheme.chrome.fillMuted)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.session_analysis_navigator_title),
                modifier = Modifier.weight(1f),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = header.trackLabel,
                modifier = Modifier.weight(1f),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall.copy(
                    fontFamily = SimAnalyzerTheme.fonts.mono,
                ),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.End,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(Res.string.session_analysis_navigator_subtitle),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SessionAnalysisNavigatorCompareShelf(
    selectedLap: SessionAnalysisLapSummaryUi?,
    referenceLap: SessionAnalysisLapSummaryUi?,
    bestLap: SessionAnalysisLapSummaryUi?,
    bestLapLabel: String,
    referenceLapIsCustom: Boolean,
    onReferenceReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionAnalysisNavigatorCompareSlot(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.session_analysis_navigator_info_selected),
                accent = SimAnalyzerTheme.material.primary,
                lap = selectedLap,
                footer = selectedLap?.deltaToBestMs?.let(::formatDelta)
                    ?: stringResource(Res.string.session_analysis_no_selection_placeholder),
            )
            SessionAnalysisNavigatorCompareSlot(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.session_analysis_navigator_info_reference),
                accent = SimAnalyzerTheme.extended.amber,
                lap = referenceLap,
                footer = if (referenceLapIsCustom) {
                    stringResource(Res.string.session_analysis_navigator_action_use_best_as_ref)
                } else {
                    referenceLap?.deltaToBestMs?.let(::formatDelta)
                        ?: stringResource(Res.string.session_analysis_no_selection_placeholder)
                },
                onFooterClick = if (referenceLapIsCustom) onReferenceReset else null,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.session_analysis_navigator_info_best),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
            Text(
                text = when {
                    bestLap != null && bestLap.durationMs != null -> {
                        "${
                            stringResource(
                                Res.string.session_analysis_lap_label,
                                bestLap.lapNumber,
                            )
                        } • ${formatLapTime(bestLap.durationMs)}"
                    }

                    bestLapLabel.isNotBlank() -> bestLapLabel

                    else -> stringResource(Res.string.session_analysis_no_selection_placeholder)
                },
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelMedium.copy(
                    fontFamily = SimAnalyzerTheme.fonts.mono,
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionAnalysisNavigatorCompareSlot(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    lap: SessionAnalysisLapSummaryUi?,
    footer: String,
    modifier: Modifier = Modifier,
    onFooterClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = SimAnalyzerTheme.corners.field,
        color = SimAnalyzerTheme.material.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SimAnalyzerTheme.chrome.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .clip(SimAnalyzerTheme.corners.pill)
                    .background(accent.copy(alpha = 0.82f))
                    .padding(vertical = 1.dp),
            )
            Text(
                text = label.uppercase(),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = lap?.let { resolvedLap ->
                    stringResource(Res.string.session_analysis_lap_label, resolvedLap.lapNumber)
                } ?: stringResource(Res.string.session_analysis_no_selection_placeholder),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleMedium.copy(
                    fontFamily = SimAnalyzerTheme.fonts.mono,
                ),
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = lap?.durationMs?.let(::formatLapTime)
                    ?: stringResource(Res.string.session_analysis_no_selection_placeholder),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelMedium.copy(
                    fontFamily = SimAnalyzerTheme.fonts.mono,
                ),
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = footer,
                modifier = if (onFooterClick != null) {
                    Modifier.onClick(onClick = onFooterClick)
                } else {
                    Modifier
                },
                color = if (onFooterClick != null) accent else SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionAnalysisNavigatorSectionHeading(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title.uppercase(),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SessionAnalysisNavigatorSessionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) {
                    SimAnalyzerTheme.chrome.fillMuted
                } else {
                    SimAnalyzerTheme.material.surface
                },
            )
            .onClick(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .clip(SimAnalyzerTheme.corners.pill)
                .background(
                    if (selected) {
                        SimAnalyzerTheme.material.primary
                    } else {
                        SimAnalyzerTheme.chrome.borderSubtle
                    },
                )
                .padding(vertical = 16.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionAnalysisNavigatorLapTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionAnalysisNavigatorTableHeaderCell(
            text = stringResource(Res.string.session_analysis_navigator_header_lap),
            width = 34.dp,
            align = TextAlign.Start,
        )
        SessionAnalysisNavigatorTableHeaderCell(
            text = stringResource(Res.string.session_analysis_navigator_header_time),
            width = 94.dp,
            align = TextAlign.Start,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionAnalysisNavigatorTableHeaderCell(
                text = stringResource(Res.string.session_analysis_navigator_header_delta),
                width = 58.dp,
                align = TextAlign.End,
            )
            SessionAnalysisNavigatorTableHeaderCell(
                text = stringResource(Res.string.session_analysis_navigator_header_ref),
                width = 40.dp,
                align = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SessionAnalysisNavigatorFocusTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionAnalysisNavigatorTableHeaderCell(
            text = stringResource(Res.string.session_analysis_navigator_header_turn),
            width = 52.dp,
            align = TextAlign.Start,
        )
        Text(
            text = stringResource(Res.string.session_analysis_navigator_header_issue),
            modifier = Modifier.weight(1f),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        SessionAnalysisNavigatorTableHeaderCell(
            text = stringResource(Res.string.session_analysis_navigator_header_delta),
            width = 72.dp,
            align = TextAlign.End,
        )
    }
}

@Composable
private fun SessionAnalysisNavigatorTableHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    align: TextAlign,
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        color = SimAnalyzerTheme.material.onSurfaceVariant,
        style = SimAnalyzerTheme.typography.labelSmall,
        maxLines = 1,
        softWrap = false,
        textAlign = align,
    )
}

@Composable
private fun SessionAnalysisNavigatorFocusRow(highlight: SessionAnalysisHighlightUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = highlight.cornerNumber?.let { corner ->
                stringResource(Res.string.session_analysis_inspector_turn, corner)
            } ?: stringResource(Res.string.session_analysis_no_selection_placeholder),
            modifier = Modifier.width(52.dp),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.labelMedium.copy(
                fontFamily = SimAnalyzerTheme.fonts.mono,
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = highlight.title,
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sessionAnalysisNavigatorFocusSourceLabel(highlight),
                color = sessionAnalysisNavigatorFocusAccent(highlight),
                style = SimAnalyzerTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
            )
        }
        Text(
            text = highlight.deltaMs?.let(::formatDelta).orEmpty(),
            modifier = Modifier.width(72.dp),
            color = sessionAnalysisNavigatorFocusAccent(highlight),
            style = SimAnalyzerTheme.typography.labelMedium.copy(
                fontFamily = SimAnalyzerTheme.fonts.mono,
            ),
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SessionAnalysisNavigatorSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 10.dp),
        thickness = 1.dp,
        color = SimAnalyzerTheme.chrome.borderSubtle,
    )
}

@Composable
private fun SessionAnalysisNavigatorRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 10.dp),
        thickness = 1.dp,
        color = SimAnalyzerTheme.chrome.borderSubtle,
    )
}

@Composable
private fun sessionAnalysisNavigatorFocusAccent(highlight: SessionAnalysisHighlightUi) = when (
    highlight.diagnosisSource
) {
    SessionAnalysisDiagnosisSourceUi.CarSetup -> SimAnalyzerTheme.extended.red
    SessionAnalysisDiagnosisSourceUi.Mixed -> SimAnalyzerTheme.extended.amber
    SessionAnalysisDiagnosisSourceUi.DrivingStyle -> SimAnalyzerTheme.extended.teal
}

@Composable
private fun sessionAnalysisNavigatorFocusSourceLabel(highlight: SessionAnalysisHighlightUi) = when (
    highlight.diagnosisSource
) {
    SessionAnalysisDiagnosisSourceUi.CarSetup -> stringResource(Res.string.session_analysis_diagnosis_setup)
    SessionAnalysisDiagnosisSourceUi.Mixed -> stringResource(Res.string.session_analysis_diagnosis_mixed)
    SessionAnalysisDiagnosisSourceUi.DrivingStyle -> stringResource(Res.string.session_analysis_diagnosis_driving)
}

@Preview
@Composable
internal fun SessionAnalysisStudioNavigatorPanePreview() {
    SimAnalyzerTheme {
        SessionAnalysisStudioNavigatorPane(
            header = sessionAnalysisNavigatorPreviewHeader(),
            sessionOptions = sessionAnalysisNavigatorPreviewSessions(),
            selectedSessionId = 12L,
            laps = sessionAnalysisNavigatorPreviewLaps(),
            selectedLapNumber = 7,
            referenceLapNumber = 6,
            referenceLapIsCustom = true,
            highlights = sessionAnalysisNavigatorPreviewHighlights(),
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp),
        )
    }
}
