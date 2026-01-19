package com.project.analyzer.live.presentation

internal sealed interface LiveIntent {
    data object Start : LiveIntent
}
