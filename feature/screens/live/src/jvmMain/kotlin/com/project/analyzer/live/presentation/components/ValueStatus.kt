package com.project.analyzer.live.presentation.components

import androidx.compose.runtime.Composable
import com.project.analyzer.theme.SimAnalyzerTheme

internal enum class ValueStatus {
    NORMAL,
    BEST,
    COMPLETED,
    INVALID
}

@Composable
internal fun ValueStatus.statusColor() = when (this) {
    ValueStatus.BEST -> SimAnalyzerTheme.extended.purple
    ValueStatus.COMPLETED -> SimAnalyzerTheme.extended.teal
    ValueStatus.INVALID -> SimAnalyzerTheme.extended.red
    ValueStatus.NORMAL -> SimAnalyzerTheme.material.onSurface
}
