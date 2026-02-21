package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analyzer.session.details.presentation.model.LapStatus
import com.project.analyzer.theme.SimAnalyzerTheme

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
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = statusLabel(status),
            color = accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun statusLabel(status: LapStatus): String = when (status) {
    LapStatus.Clean -> "Clean"
    LapStatus.OutLap -> "Out Lap"
    LapStatus.Dirty -> "Dirty"
    LapStatus.BestLap -> "Best Lap"
    LapStatus.Invalid -> "Invalid"
    LapStatus.PitIn -> "Pit In"
}
