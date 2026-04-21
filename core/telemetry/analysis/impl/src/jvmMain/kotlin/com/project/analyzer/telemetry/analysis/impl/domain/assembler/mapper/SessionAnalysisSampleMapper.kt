package com.project.analyzer.telemetry.analysis.impl.domain.assembler.mapper

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.impl.domain.extension.hasUsableLapNumber
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.handling.SessionAnalysisHandlingStateResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.tyre.SessionAnalysisTyreStateResolver
import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetryFrame
import dev.zacsweers.metro.Inject

/**
 * Converts decoded frames into analysis samples while normalizing nullable sim-specific telemetry fields.
 */
@Inject
internal class SessionAnalysisSampleMapper(
    private val handlingStateResolver: SessionAnalysisHandlingStateResolver,
    private val tyreStateResolver: SessionAnalysisTyreStateResolver,
) {

    fun map(
        frames: List<DecodedRecordedTelemetryFrame>,
        tyreProfile: SessionAnalysisTyreProfile?,
    ): List<SessionAnalysisSample> {
        val sampleIndexByLap = linkedMapOf<Pair<Long, Int>, Int>()
        return frames
            .asSequence()
            .filter(DecodedRecordedTelemetryFrame::hasUsableLapNumber)
            .map { frame ->
                val lapKey = frame.segmentId to frame.lapNumber
                val nextSampleIndex = sampleIndexByLap.getOrDefault(lapKey, 0)
                sampleIndexByLap[lapKey] = nextSampleIndex + 1

                SessionAnalysisSample(
                    segmentId = frame.segmentId,
                    frameId = frame.frameId,
                    timestampNs = frame.timestampNs,
                    lapNumber = frame.lapNumber,
                    sectorIndex = frame.sectorIndex,
                    flags = frame.flags,
                    sampleIndexInLap = nextSampleIndex,
                    trackPosition = frame.trackPosition,
                    trackX = frame.trackX,
                    trackY = frame.trackY,
                    headingRad = frame.headingRad,
                    speedKmh = frame.payload.speedKmh,
                    gear = frame.payload.gear,
                    rpm = frame.payload.rpm,
                    throttle = frame.payload.throttle,
                    brake = frame.payload.brake,
                    steeringAngleRad = frame.payload.steeringAngleRad,
                    lateralG = frame.payload.lateralG,
                    yawRateRad = frame.payload.yawRateRad,
                    fuelLiters = frame.payload.fuelLiters,
                    fuelCapacityLiters = frame.payload.fuelCapacityLiters,
                    fuelPerLapLiters = frame.payload.fuelPerLapLiters,
                    currentLapTimeMs = frame.payload.currentLapTimeMs,
                    lastLapTimeMs = frame.payload.lastLapTimeMs,
                    bestLapTimeMs = frame.payload.bestLapTimeMs,
                    estimatedLapTimeMs = frame.payload.estimatedLapTimeMs,
                    currentSectorIndex = frame.payload.currentSectorIndex,
                    lastSectorTimeMs = frame.payload.lastSectorTimeMs,
                    isLapValid = frame.payload.isLapValid,
                    airTempC = frame.payload.airTempC,
                    roadTempC = frame.payload.roadTempC,
                    brakeBias = frame.payload.brakeBias,
                    tcLevel = frame.payload.tcLevel,
                    absLevel = frame.payload.absLevel,
                    pitLimiterOn = frame.payload.pitLimiterOn,
                    handlingState = handlingStateResolver.resolve(
                        speedKmh = frame.payload.speedKmh,
                        steeringAngleRad = frame.payload.steeringAngleRad,
                        lateralG = frame.payload.lateralG,
                        yawRateRad = frame.payload.yawRateRad,
                        throttle = frame.payload.throttle,
                        brake = frame.payload.brake,
                    ),
                    tyreFl = tyreStateResolver.resolve(frame.payload.tyreFl, tyreProfile),
                    tyreFr = tyreStateResolver.resolve(frame.payload.tyreFr, tyreProfile),
                    tyreRl = tyreStateResolver.resolve(frame.payload.tyreRl, tyreProfile),
                    tyreRr = tyreStateResolver.resolve(frame.payload.tyreRr, tyreProfile),
                )
            }
            .toList()
    }
}
