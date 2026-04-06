package com.analyzer.session.analysis.presentation.components.navigator.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.navigator.preview.sessionAnalysisNavigatorPreviewLaps
import com.analyzer.session.analysis.presentation.formatter.formatDelta
import com.analyzer.session.analysis.presentation.formatter.formatLapTime
import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapSummaryUi
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_badge_reference_short
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_badge_selected_ref_short
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_badge_selected_short
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_navigator_ref_action_short
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_no_selection_placeholder
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionAnalysisLapRailRow(
    lap: SessionAnalysisLapSummaryUi,
    isSelected: Boolean,
    isReference: Boolean,
    referenceLapIsCustom: Boolean,
    onClick: () -> Unit,
    onReferenceClick: () -> Unit,
    onResetReferenceClick: () -> Unit,
) {
    val rowBackground = when {
        isSelected || isReference -> SimAnalyzerTheme.chrome.fillMuted
        else -> SimAnalyzerTheme.material.surface
    }
    val deltaColor = when {
        (lap.deltaToBestMs ?: 0) <= 0 -> SimAnalyzerTheme.extended.teal
        else -> SimAnalyzerTheme.extended.amber
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .onClick(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionAnalysisLapRailCell(
            text = lap.lapNumber.toString().padStart(2, '0'),
            width = 34.dp,
            align = TextAlign.Start,
            mono = true,
        )
        SessionAnalysisLapRailCell(
            text = lap.durationMs?.let(::formatLapTime)
                ?: stringResource(Res.string.session_analysis_no_selection_placeholder),
            width = 94.dp,
            align = TextAlign.Start,
            mono = true,
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionAnalysisLapRailCell(
                text = lap.deltaToBestMs?.let(::formatDelta).orEmpty(),
                width = 58.dp,
                align = TextAlign.End,
                color = deltaColor,
                mono = true,
            )
            SessionAnalysisLapRailCell(
                text = when {
                    isSelected && isReference -> stringResource(
                        Res.string.session_analysis_navigator_badge_selected_ref_short,
                    )

                    isReference && referenceLapIsCustom -> stringResource(
                        Res.string.session_analysis_navigator_badge_reference_short,
                    )

                    isReference -> stringResource(Res.string.session_analysis_navigator_badge_reference_short)

                    isSelected -> stringResource(Res.string.session_analysis_navigator_badge_selected_short)

                    else -> stringResource(Res.string.session_analysis_navigator_ref_action_short)
                },
                width = 40.dp,
                align = TextAlign.Center,
                color = when {
                    isReference -> SimAnalyzerTheme.material.onSurfaceVariant
                    isSelected -> SimAnalyzerTheme.material.onSurfaceVariant
                    else -> SimAnalyzerTheme.extended.teal
                },
                style = SimAnalyzerTheme.typography.labelSmall,
                onClick = when {
                    isReference && referenceLapIsCustom -> onResetReferenceClick
                    !isReference -> onReferenceClick
                    else -> null
                },
            )
        }
    }
}

@Composable
private fun SessionAnalysisLapRailCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    align: TextAlign,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = SimAnalyzerTheme.material.onSurface,
    mono: Boolean = false,
    style: androidx.compose.ui.text.TextStyle = SimAnalyzerTheme.typography.labelMedium,
    onClick: (() -> Unit)? = null,
) {
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier
            .width(width)
            .then(if (onClick != null) Modifier.onClick(onClick = onClick) else Modifier),
        color = color,
        style = style.copy(
            fontFamily = if (mono) {
                SimAnalyzerTheme.fonts.mono
            } else {
                FontFamily.Default
            },
        ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
    )
}

@Preview
@Composable
internal fun SessionAnalysisLapRailRowPreview() {
    SimAnalyzerTheme {
        SessionAnalysisLapRailRow(
            lap = sessionAnalysisNavigatorPreviewLaps().first(),
            isSelected = true,
            isReference = false,
            referenceLapIsCustom = true,
            onClick = {},
            onReferenceClick = {},
            onResetReferenceClick = {},
        )
    }
}
