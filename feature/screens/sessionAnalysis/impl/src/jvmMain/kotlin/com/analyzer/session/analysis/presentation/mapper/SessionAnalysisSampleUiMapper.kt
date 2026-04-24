package com.analyzer.session.analysis.presentation.mapper

import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTyreUi
import com.analyzer.session.analysis.presentation.model.toUi
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState

/**
 * Maps raw analysis samples into UI-friendly telemetry points with formatted tyre submodels.
 */
internal fun List<SessionAnalysisSample>.toUiSamples(): List<SessionAnalysisSampleUi> {
    val lapStartNs = firstOrNull()?.timestampNs ?: 0L
    return map { sample ->
        sample.toUi(
            elapsedMs = ((sample.timestampNs - lapStartNs) / 1_000_000L).toInt().takeIf { it >= 0 },
        )
    }
}

internal fun SessionAnalysisSample.toUi(elapsedMs: Int?): SessionAnalysisSampleUi = SessionAnalysisSampleUi(
    frameId = frameId,
    lapNumber = lapNumber,
    sectorIndex = sectorIndex,
    sampleIndexInLap = sampleIndexInLap,
    trackPosition = trackPosition,
    trackX = trackX,
    trackY = trackY,
    elapsedMs = elapsedMs,
    speedKmh = speedKmh,
    gear = gear,
    rpm = rpm,
    throttle = throttle,
    brake = brake,
    steeringAngleRad = steeringAngleRad,
    lateralG = lateralG,
    yawRateRad = yawRateRad,
    deltaToBestMs = deltaToBestMs,
    fuelLiters = fuelLiters,
    fuelCapacityLiters = fuelCapacityLiters,
    handlingState = handlingState.toUi(),
    tyreFl = tyreFl?.toUi(),
    tyreFr = tyreFr?.toUi(),
    tyreRl = tyreRl?.toUi(),
    tyreRr = tyreRr?.toUi(),
)

internal fun SessionAnalysisTyreState.toUi(): SessionAnalysisTyreUi = SessionAnalysisTyreUi(
    pressurePsi = pressurePsi,
    coreTempC = coreTempC,
    innerTempC = innerTempC,
    middleTempC = middleTempC,
    outerTempC = outerTempC,
    avgTempC = avgTempC,
    brakeTempC = brakeTempC,
    slip = slip,
    load = load,
    tempBand = tempBand?.toUi(),
)
