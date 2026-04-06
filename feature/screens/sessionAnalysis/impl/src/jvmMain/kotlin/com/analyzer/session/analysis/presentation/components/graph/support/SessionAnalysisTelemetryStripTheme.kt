package com.analyzer.session.analysis.presentation.components.graph.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun rememberSessionAnalysisTelemetryTonePalette(): SessionAnalysisTelemetryTonePalette {
    val primaryToneColor = SimAnalyzerTheme.material.primary
    val referenceToneColor = SimAnalyzerTheme.material.onSurfaceVariant
    val throttleToneColor = SimAnalyzerTheme.extended.teal
    val brakeToneColor = SimAnalyzerTheme.extended.red
    val steeringToneColor = SimAnalyzerTheme.extended.amber
    val cyanToneColor = SimAnalyzerTheme.extended.cyan

    return remember(
        primaryToneColor,
        referenceToneColor,
        throttleToneColor,
        brakeToneColor,
        steeringToneColor,
        cyanToneColor,
    ) {
        SessionAnalysisTelemetryTonePalette(
            primaryToneColor = primaryToneColor,
            referenceToneColor = referenceToneColor,
            throttleToneColor = throttleToneColor,
            brakeToneColor = brakeToneColor,
            steeringToneColor = steeringToneColor,
            cyanToneColor = cyanToneColor,
        )
    }
}
