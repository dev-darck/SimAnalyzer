package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.analyzer.session.details.presentation.model.LapStatus
import com.project.analyzer.feature.screens.sessionDetails.Res.Res
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_status_best_lap
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_status_clean
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_status_dirty
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_status_invalid
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_status_out_lap
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_status_pit_in
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StatusChip(status: LapStatus, modifier: Modifier = Modifier) {
    val accent = when (status) {
        LapStatus.Clean -> SimAnalyzerTheme.extended.teal
        LapStatus.OutLap -> SimAnalyzerTheme.extended.yellow
        LapStatus.Dirty -> SimAnalyzerTheme.extended.amber
        LapStatus.BestLap -> SimAnalyzerTheme.extended.purple
        LapStatus.Invalid -> SimAnalyzerTheme.extended.red
        LapStatus.PitIn -> SimAnalyzerTheme.extended.cyan
    }

    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.corners.pill)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.6f), SimAnalyzerTheme.corners.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = statusLabel(status),
            color = accent,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun statusLabel(status: LapStatus): String = when (status) {
    LapStatus.Clean -> stringResource(Res.string.session_details_status_clean)
    LapStatus.OutLap -> stringResource(Res.string.session_details_status_out_lap)
    LapStatus.Dirty -> stringResource(Res.string.session_details_status_dirty)
    LapStatus.BestLap -> stringResource(Res.string.session_details_status_best_lap)
    LapStatus.Invalid -> stringResource(Res.string.session_details_status_invalid)
    LapStatus.PitIn -> stringResource(Res.string.session_details_status_pit_in)
}
