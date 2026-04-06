package com.analyzer.session.analysis.presentation

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceData
import com.project.analyzer.telemetry.analysis.api.model.header.SessionAnalysisHeader
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import kotlin.math.PI
import kotlin.math.sin

internal fun sessionAnalysisWorkspaceData(sessionId: Long = 77L): SessionAnalysisWorkspaceData {
    val trackMap = sessionAnalysisTrackMap()
    return SessionAnalysisWorkspaceData(
        report = SessionAnalysisReport(
            sessionId = sessionId,
            header = SessionAnalysisHeader(
                gameId = "acevo",
                sessionTypeLabel = "Qualifying",
                carLabel = "Porsche 911 GT3 R",
                trackLabel = "Spa",
                startedAtMs = 1_000L,
            ),
            vehicleClass = SessionAnalysisVehicleClass.GT3,
            trackMap = trackMap,
            segments = listOf(
                SessionAnalysisSegment(
                    segmentId = 101L,
                    sessionTypeLabel = "Practice",
                    sessionLabel = "Practice",
                    carLabel = "Porsche 911 GT3 R",
                    trackLabel = "Spa",
                    startedAtMs = 1_000L,
                ),
                SessionAnalysisSegment(
                    segmentId = 202L,
                    sessionTypeLabel = "Qualifying",
                    sessionLabel = "Qualifying",
                    carLabel = "Porsche 911 GT3 R",
                    trackLabel = "Spa",
                    startedAtMs = 2_000L,
                ),
            ),
            laps = listOf(
                sessionAnalysisLap(segmentId = 101L, lapNumber = 1, durationMs = 92_500, deltaToBestMs = 0),
                sessionAnalysisLap(segmentId = 101L, lapNumber = 2, durationMs = 93_100, deltaToBestMs = 600),
                sessionAnalysisLap(segmentId = 202L, lapNumber = 1, durationMs = 89_500, deltaToBestMs = 0),
                sessionAnalysisLap(segmentId = 202L, lapNumber = 2, durationMs = 90_200, deltaToBestMs = 700),
            ),
            samples = sessionAnalysisLapSamples(segmentId = 101L, lapNumber = 1, deltaToBestMs = 0) +
                sessionAnalysisLapSamples(segmentId = 101L, lapNumber = 2, deltaToBestMs = 600) +
                sessionAnalysisLapSamples(segmentId = 202L, lapNumber = 1, deltaToBestMs = 0) +
                sessionAnalysisLapSamples(segmentId = 202L, lapNumber = 2, deltaToBestMs = 700),
            bestLapNumber = 1,
        ),
        sourceTrackMap = trackMap,
        displayTrackMap = trackMap,
    )
}

private fun sessionAnalysisTrackMap(): SessionAnalysisTrackMap {
    val points = (0..64).map { index ->
        val fraction = index.toFloat() / 64f
        SessionAnalysisTrackMapPoint(
            x = fraction * 1_000f,
            y = sin(fraction * PI.toFloat() * 2f) * 42f,
        )
    }
    return SessionAnalysisTrackMap(
        points = points,
        minX = points.minOf(SessionAnalysisTrackMapPoint::x),
        minY = points.minOf(SessionAnalysisTrackMapPoint::y),
        maxX = points.maxOf(SessionAnalysisTrackMapPoint::x),
        maxY = points.maxOf(SessionAnalysisTrackMapPoint::y),
    )
}

private fun sessionAnalysisLap(
    segmentId: Long,
    lapNumber: Int,
    durationMs: Int,
    deltaToBestMs: Int,
    sampleCount: Int = ViewModelTestSampleCount,
): SessionAnalysisLap = SessionAnalysisLap(
    segmentId = segmentId,
    sessionTypeLabel = "Qualifying",
    lapNumber = lapNumber,
    isValid = true,
    isPitLap = false,
    isComplete = true,
    durationMs = durationMs,
    sampleCount = sampleCount,
    avgSpeedKmh = 158f - lapNumber,
    maxSpeedKmh = 261f,
    deltaToBestMs = deltaToBestMs,
    endFuelLiters = 38f - lapNumber,
    fuelUsedLiters = 2.6f,
    peakCoreTempC = 94f,
    peakBrakeTempC = 612f,
)

private fun sessionAnalysisLapSamples(
    segmentId: Long,
    lapNumber: Int,
    deltaToBestMs: Int,
    count: Int = ViewModelTestSampleCount,
): List<SessionAnalysisSample> = List(count) { index ->
    val fraction = index.toFloat() / (count - 1).coerceAtLeast(1)
    val sectorIndex = when {
        fraction < 0.33f -> 0
        fraction < 0.66f -> 1
        else -> 2
    }
    SessionAnalysisSample(
        segmentId = segmentId,
        frameId = segmentId * 10_000L + lapNumber * 1_000L + index,
        timestampNs = lapNumber * 1_000_000_000L + index * 16_000_000L,
        lapNumber = lapNumber,
        sectorIndex = sectorIndex,
        sampleIndexInLap = index,
        trackPosition = fraction,
        trackX = fraction * 1_000f,
        trackY = sin(fraction * PI.toFloat() * 2f) * 40f + segmentId.toFloat() / 10f,
        speedKmh = 140f + (1f - fraction) * 24f,
        throttle = if (fraction >= 0.55f) 0.88f else 0.18f,
        brake = if (fraction <= 0.28f) 0.64f else 0.02f,
        deltaToBestMs = (deltaToBestMs * fraction).toInt(),
        fuelLiters = 40f - lapNumber - fraction,
    )
}

private const val ViewModelTestSampleCount: Int = 140
