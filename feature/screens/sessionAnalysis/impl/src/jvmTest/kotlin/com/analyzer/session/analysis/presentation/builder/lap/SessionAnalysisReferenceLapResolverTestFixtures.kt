package com.analyzer.session.analysis.presentation.builder.lap

import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample

internal fun buildLap(
    lapNumber: Int,
    durationMs: Int,
    sectors: List<Int>,
    pathLengthMeters: Float,
    sampleCount: Int = 160,
): Pair<SessionAnalysisLap, List<SessionAnalysisSample>> {
    val samples = List(sampleCount) { index ->
        val fraction = if (sampleCount <= 1) 0f else index.toFloat() / (sampleCount - 1).toFloat()
        val sectorSize = (sampleCount / sectors.size).coerceAtLeast(1)
        val sectorIndex = sectors[(index / sectorSize).coerceAtMost(sectors.lastIndex)]
        SessionAnalysisSample(
            lapNumber = lapNumber,
            sectorIndex = sectorIndex,
            sampleIndexInLap = index,
            trackPosition = fraction,
            trackX = fraction * pathLengthMeters,
            trackY = kotlin.math.sin(fraction * Math.PI).toFloat() * 4f,
        )
    }
    return SessionAnalysisLap(
        lapNumber = lapNumber,
        isValid = true,
        isPitLap = false,
        isComplete = true,
        durationMs = durationMs,
        sampleCount = samples.size,
    ) to samples
}

