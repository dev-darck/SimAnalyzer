package com.analyzer.session.analysis.presentation.pipeline

import com.analyzer.session.analysis.presentation.builder.lap.resolveLapFractions
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap

private const val BUCKET_COUNT: Int = 420

/**
 * Resamples the selected and reference laps onto one normalized track-position axis so charts and
 * overlays can compare laps with different sample densities.
 */
internal fun buildComparisonPoints(
    selectedSamples: List<SessionAnalysisSample>,
    referenceSamples: List<SessionAnalysisSample>,
    trackMap: SessionAnalysisTrackMap?,
): List<SessionAnalysisComparisonPointUi> {
    val selected = selectedSamples.toComparisonSeries(trackMap)
    if (selected.size < 2) return emptyList()

    val referenceInterpolator = referenceSamples
        .toComparisonSeries(trackMap)
        .takeIf { series -> series.size >= 2 }
        ?.let(::SessionAnalysisInterpolator)
    val selectedInterpolator = SessionAnalysisInterpolator(selected)
    val coverageStart = selected.first().trackPosition
    val coverageEnd = selected.last().trackPosition
    if (coverageEnd - coverageStart <= 0.0001f) return emptyList()

    return List(BUCKET_COUNT) { index ->
        val normalizedFraction = index.toFloat() / (BUCKET_COUNT - 1).toFloat()
        val trackPosition = coverageStart + (coverageEnd - coverageStart) * normalizedFraction
        val selectedPoint = selectedInterpolator.at(trackPosition)
        val referencePoint = referenceInterpolator?.at(trackPosition)

        SessionAnalysisComparisonPointUi(
            fraction = trackPosition,
            trackPosition = trackPosition,
            selectedFrameId = selectedPoint.frameId,
            selectedElapsedMs = selectedPoint.elapsedMs,
            referenceElapsedMs = referencePoint?.elapsedMs,
            deltaMs = if (selectedPoint.elapsedMs != null && referencePoint?.elapsedMs != null) {
                selectedPoint.elapsedMs - referencePoint.elapsedMs
            } else {
                null
            },
            selectedSpeedKmh = selectedPoint.speedKmh,
            referenceSpeedKmh = referencePoint?.speedKmh,
            selectedThrottle = selectedPoint.throttle,
            referenceThrottle = referencePoint?.throttle,
            selectedBrake = selectedPoint.brake,
            referenceBrake = referencePoint?.brake,
            selectedSteeringAngleRad = selectedPoint.steeringAngleRad,
            referenceSteeringAngleRad = referencePoint?.steeringAngleRad,
            selectedLateralG = selectedPoint.lateralG,
            referenceLateralG = referencePoint?.lateralG,
            selectedYawRateRad = selectedPoint.yawRateRad,
            referenceYawRateRad = referencePoint?.yawRateRad,
            selectedGear = selectedPoint.gear,
            referenceGear = referencePoint?.gear,
            selectedRpm = selectedPoint.rpm,
            referenceRpm = referencePoint?.rpm,
            selectedFuelLiters = selectedPoint.fuelLiters,
            referenceFuelLiters = referencePoint?.fuelLiters,
        )
    }
}

/**
 * Resolves stable track fractions before interpolation so the resulting series stays ordered by
 * travel around the lap instead of raw frame order.
 */
private fun List<SessionAnalysisSample>.toComparisonSeries(
    trackMap: SessionAnalysisTrackMap?,
): List<SessionAnalysisComparisonSample> {
    if (isEmpty()) return emptyList()

    val ordered = sortedBy(SessionAnalysisSample::sampleIndexInLap)
    val lapStartNs = ordered.first().timestampNs
    val resolvedTrackPositions = resolveLapFractions(samples = ordered, trackMap = trackMap)
        .normalizeComparisonFractions()

    return ordered
        .mapIndexed { index, sample ->
            SessionAnalysisComparisonSample(
                trackPosition = resolvedTrackPositions[index],
                frameId = sample.frameId,
                elapsedMs = ((sample.timestampNs - lapStartNs) / 1_000_000L).toInt().takeIf { it >= 0 },
                speedKmh = sample.speedKmh,
                throttle = sample.throttle,
                brake = sample.brake,
                steeringAngleRad = sample.steeringAngleRad,
                lateralG = sample.lateralG,
                yawRateRad = sample.yawRateRad,
                gear = sample.gear,
                rpm = sample.rpm,
                fuelLiters = sample.fuelLiters,
            )
        }
        .sortedBy(SessionAnalysisComparisonSample::trackPosition)
}

private fun List<Float>.normalizeComparisonFractions(): List<Float> {
    if (isEmpty()) return emptyList()

    val start = first().takeIf(Float::isFinite) ?: 0f
    val end = last().takeIf(Float::isFinite) ?: start
    val span = end - start
    if (span <= 0.0001f) {
        val denominator = lastIndex.coerceAtLeast(1).toFloat()
        return indices.map { index -> index.toFloat() / denominator }
    }

    return map { fraction ->
        ((fraction - start) / span).coerceIn(0f, 1f)
    }
}
