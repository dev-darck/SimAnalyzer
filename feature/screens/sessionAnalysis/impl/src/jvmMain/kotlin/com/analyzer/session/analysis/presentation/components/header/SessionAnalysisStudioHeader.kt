@file:OptIn(ExperimentalLayoutApi::class)

package com.analyzer.session.analysis.presentation.components.header

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.SessionAnalysisUiTokens
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisStudioPanel
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisStudioTile
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHeaderUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisScreenMode
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSessionOptionUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSummaryUi
import com.analyzer.session.analysis.presentation.preview.sessionAnalysisPreviewHeader
import com.analyzer.session.analysis.presentation.preview.sessionAnalysisPreviewSummary
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_biggest_loss
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_compare
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_compare_value
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_cursor
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_cursor_live
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_cursor_pinned
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_meta
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_refresh
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_sessions
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_header_started
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_lap_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_delta
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_no_selection_placeholder
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_session_filter
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_temps_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_temps_value
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_title
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_top_speed
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisStudioHeader(
    header: SessionAnalysisHeaderUi,
    screenMode: SessionAnalysisScreenMode,
    summary: SessionAnalysisSummaryUi?,
    sessionOptions: ImmutableList<SessionAnalysisSessionOptionUi>,
    selectedSessionId: Long?,
    selectedLapNumber: Int?,
    referenceLapNumber: Int?,
    selectionLocked: Boolean,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit = {},
    onSessionSelected: (Long?) -> Unit = {},
) {
    val noSelectionText = stringResource(Res.string.session_analysis_no_selection_placeholder)
    val selectedLapText = selectedLapNumber
        ?.let { lapNumber -> stringResource(Res.string.session_analysis_lap_label, lapNumber) }
        ?: noSelectionText
    val referenceLapText = referenceLapNumber
        ?.let { lapNumber -> stringResource(Res.string.session_analysis_lap_label, lapNumber) }
        ?: noSelectionText

    SessionAnalysisStudioPanel(
        modifier = modifier,
        containerColor = SimAnalyzerTheme.material.surface,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.sectionGap),
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
                        text = stringResource(Res.string.session_analysis_title),
                        color = SimAnalyzerTheme.material.onSurface,
                        style = SimAnalyzerTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            Res.string.session_analysis_header_meta,
                            header.trackLabel,
                            header.carLabel,
                        ),
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        style = SimAnalyzerTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                FilledTonalButton(
                    onClick = onRefresh,
                ) {
                    Text(text = stringResource(Res.string.session_analysis_header_refresh))
                }
            }

            if (screenMode == SessionAnalysisScreenMode.Analysis && sessionOptions.size > 1) {
                SessionAnalysisHeaderSessionSwitcher(
                    sessionOptions = sessionOptions,
                    selectedSessionId = selectedSessionId,
                    onSessionSelected = onSessionSelected,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.itemGap),
                verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.itemGap),
            ) {
                SessionAnalysisHeaderPill(
                    label = stringResource(Res.string.session_analysis_session_filter),
                    value = header.sessionTypeLabel,
                )
                SessionAnalysisHeaderPill(
                    label = stringResource(Res.string.session_analysis_header_started),
                    value = header.startedAtLabel,
                )
                SessionAnalysisHeaderPill(
                    label = stringResource(Res.string.session_analysis_temps_label),
                    value = stringResource(
                        Res.string.session_analysis_temps_value,
                        header.airTempLabel,
                        header.trackTempLabel,
                    ),
                )
                SessionAnalysisHeaderPill(
                    label = stringResource(Res.string.session_analysis_header_compare),
                    value = stringResource(
                        Res.string.session_analysis_header_compare_value,
                        selectedLapText,
                        referenceLapText,
                    ),
                )
                SessionAnalysisHeaderPill(
                    label = stringResource(Res.string.session_analysis_header_cursor),
                    value = if (selectionLocked) {
                        stringResource(Res.string.session_analysis_header_cursor_pinned)
                    } else {
                        stringResource(Res.string.session_analysis_header_cursor_live)
                    },
                    accent = if (selectionLocked) SimAnalyzerTheme.extended.amber else SimAnalyzerTheme.extended.teal,
                )
                summary?.let { resolvedSummary ->
                    SessionAnalysisHeaderPill(
                        label = stringResource(Res.string.session_analysis_metric_delta),
                        value = resolvedSummary.lapDeltaLabel,
                    )
                    if (screenMode == SessionAnalysisScreenMode.Analysis) {
                        SessionAnalysisHeaderPill(
                            label = stringResource(Res.string.session_analysis_header_biggest_loss),
                            value = resolvedSummary.biggestLossValueLabel,
                            supporting = resolvedSummary.biggestLossLabel,
                        )
                        SessionAnalysisHeaderPill(
                            label = stringResource(Res.string.session_analysis_top_speed),
                            value = resolvedSummary.topSpeedLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionAnalysisHeaderPill(
    label: String,
    value: String,
    supporting: String? = null,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = SimAnalyzerTheme.material.primary,
) {
    SessionAnalysisStudioTile(
        modifier = modifier.widthIn(min = 112.dp, max = 168.dp),
        contentPadding = PaddingValues(
            horizontal = SessionAnalysisUiTokens.tileHorizontalPadding,
            vertical = 11.dp,
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                color = accent,
                style = SimAnalyzerTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            supporting
                ?.takeIf(String::isNotBlank)
                ?.let { supportingText ->
                    Text(
                        text = supportingText,
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        style = SimAnalyzerTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
        }
    }
}

@Composable
private fun SessionAnalysisHeaderSessionSwitcher(
    sessionOptions: ImmutableList<SessionAnalysisSessionOptionUi>,
    selectedSessionId: Long?,
    onSessionSelected: (Long?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.chipGap),
    ) {
        Text(
            text = stringResource(Res.string.session_analysis_header_sessions),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.labelMedium,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.chipGap),
        ) {
            sessionOptions.forEach { option ->
                val selected = option.segmentId == selectedSessionId
                SessionAnalysisStudioTile(
                    modifier = Modifier.widthIn(min = 110.dp, max = 170.dp),
                    selected = selected,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    onClick = { onSessionSelected(option.segmentId) },
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = option.primaryLabel,
                            color = if (selected) {
                                SimAnalyzerTheme.material.primary
                            } else {
                                SimAnalyzerTheme.material.onSurface
                            },
                            style = if (selected) {
                                SimAnalyzerTheme.typography.labelLarge
                            } else {
                                SimAnalyzerTheme.typography.labelMedium
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = option.supportingLabel,
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
}

@Preview
@Composable
internal fun SessionAnalysisStudioHeaderPreview() {
    SimAnalyzerTheme {
        SessionAnalysisStudioHeader(
            header = sessionAnalysisPreviewHeader(),
            screenMode = SessionAnalysisScreenMode.Analysis,
            summary = sessionAnalysisPreviewSummary(),
            sessionOptions = persistentListOf(
                SessionAnalysisSessionOptionUi(
                    segmentId = 12L,
                    primaryLabel = "Race sim",
                    supportingLabel = "19:42, GT3",
                ),
                SessionAnalysisSessionOptionUi(
                    segmentId = 18L,
                    primaryLabel = "Quali prep",
                    supportingLabel = "18:10, GT3",
                ),
            ),
            selectedSessionId = 12L,
            selectedLapNumber = 7,
            referenceLapNumber = 3,
            selectionLocked = false,
        )
    }
}
