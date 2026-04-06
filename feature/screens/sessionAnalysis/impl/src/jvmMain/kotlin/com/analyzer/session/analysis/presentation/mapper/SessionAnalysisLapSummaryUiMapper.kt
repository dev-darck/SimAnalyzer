package com.analyzer.session.analysis.presentation.mapper

import com.analyzer.session.analysis.presentation.model.SessionAnalysisLapSummaryUi
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap

/**
 * Converts analysed laps into compact cards used for lap selection and reference switching.
 */
internal fun SessionAnalysisLap.toUi(): SessionAnalysisLapSummaryUi = SessionAnalysisLapSummaryUi(
    lapNumber = lapNumber,
    isValid = isValid,
    isPitLap = isPitLap,
    isComplete = isComplete,
    durationMs = durationMs,
    sampleCount = sampleCount,
    avgSpeedKmh = avgSpeedKmh,
    maxSpeedKmh = maxSpeedKmh,
    deltaToBestMs = deltaToBestMs,
    maxRpm = maxRpm,
    endFuelLiters = endFuelLiters,
    fuelUsedLiters = fuelUsedLiters,
    peakBrakeTempC = peakBrakeTempC,
    peakCoreTempC = peakCoreTempC,
)
