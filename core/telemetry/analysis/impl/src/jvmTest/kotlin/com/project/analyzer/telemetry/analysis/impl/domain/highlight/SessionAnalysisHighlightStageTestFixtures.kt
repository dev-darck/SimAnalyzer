package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreTemperatureBand
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisReport

internal fun highlightContext(
    samples: List<SessionAnalysisSample>,
    bestLapBySegmentId: Map<Long, Int?> = mapOf(1L to 1),
    cornerReport: SessionAnalysisCornerAnalysisReport? = null,
): SessionAnalysisHighlightContext = SessionAnalysisHighlightContext(
    samples = samples,
    bestLapBySegmentId = bestLapBySegmentId,
    tyreProfile = null,
    cornerReport = cornerReport,
)

internal fun highlightSample(
    segmentId: Long = 1L,
    lapNumber: Int = 1,
    sampleIndexInLap: Int = 0,
    trackPosition: Float = 0f,
    speedKmh: Float? = null,
    throttle: Float? = null,
    brake: Float? = null,
    deltaToBestMs: Int? = null,
    tyreBand: SessionAnalysisTyreTemperatureBand? = null,
    coreTempC: Float? = null,
): SessionAnalysisSample = SessionAnalysisSample(
    segmentId = segmentId,
    lapNumber = lapNumber,
    sampleIndexInLap = sampleIndexInLap,
    trackPosition = trackPosition,
    speedKmh = speedKmh,
    throttle = throttle,
    brake = brake,
    deltaToBestMs = deltaToBestMs,
    tyreFl = tyreState(
        tempBand = tyreBand,
        coreTempC = coreTempC,
    ),
)

private fun tyreState(
    tempBand: SessionAnalysisTyreTemperatureBand?,
    coreTempC: Float?,
): SessionAnalysisTyreState? {
    if (tempBand == null && coreTempC == null) return null
    return SessionAnalysisTyreState(
        tempBand = tempBand,
        coreTempC = coreTempC,
        avgTempC = coreTempC,
    )
}
