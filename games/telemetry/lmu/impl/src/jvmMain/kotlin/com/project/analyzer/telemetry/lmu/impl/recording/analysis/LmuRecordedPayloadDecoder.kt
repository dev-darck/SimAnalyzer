package com.project.analyzer.telemetry.lmu.impl.recording.analysis

import com.project.analyzer.telemetry.lmu.impl.common.LmuTelemetryConversions
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2ScoringInfo
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleScoring
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleTelemetry
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2Wheel
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayload
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayloadDecoder
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryTyreSnapshot
import com.project.analyzer.utils.shm.toCString
import com.sun.jna.Memory
import dev.zacsweers.metro.Inject
import kotlin.math.sqrt

@Inject
internal class LmuRecordedPayloadDecoder : RecordedTelemetryPayloadDecoder {

    override val payloadType: String = "lmu_shm_v1"

    override fun decode(payload: ByteArray): RecordedTelemetryPayload? {
        if (payload.size < telemetryBufferSize()) return null

        val memory = Memory(payload.size.toLong())
        memory.write(0, payload, 0, payload.size)

        val telemetryVersion = memory.getInt(0)
        if (telemetryVersion == 0) return null

        val numVehicles = memory.getInt(LMU_NUM_VEHICLES_OFFSET.toLong())
            .coerceIn(1, LMU_MAX_VEHICLES)
        val scoringOffset = telemetryBufferSize()
        val scoringVersion = if (payload.size >= scoringOffset + Int.SIZE_BYTES) {
            memory.getInt(scoringOffset.toLong())
        } else {
            0
        }

        val scoringInfo =
            if (scoringVersion > 0 && payload.size >= scoringOffset + Rf2ScoringInfo.OFFSET + Rf2ScoringInfo.SIZE) {
                Rf2ScoringInfo().apply {
                    attach(memory, scoringOffset + Rf2ScoringInfo.OFFSET)
                    read()
                }
            } else {
                null
            }

        val playerIndex = resolvePlayerIndex(
            memory = memory,
            scoringOffset = scoringOffset,
            scoringVersion = scoringVersion,
            numVehicles = numVehicles,
        )
        val telemetry = Rf2VehicleTelemetry().apply {
            attach(memory, Rf2VehicleTelemetry.OFFSET + playerIndex * Rf2VehicleTelemetry.SIZE)
            read()
        }
        val scoring = if (scoringVersion > 0) {
            Rf2VehicleScoring().apply {
                attach(memory, scoringOffset + Rf2VehicleScoring.OFFSET + playerIndex * Rf2VehicleScoring.SIZE)
                read()
            }
        } else {
            null
        }

        val telemetryVehicleName = telemetry.vehicleName.toCString()
        val scoringVehicleName = scoring?.vehicleName?.toCString().orEmpty()

        return RecordedTelemetryPayload(
            gameId = "lmu",
            carModel = telemetryVehicleName.ifBlank {
                scoringVehicleName
            }.ifBlank { scoring?.vehicleFileName?.toCString() },
            carLabel = telemetryVehicleName.ifBlank { scoringVehicleName },
            trackLabel = scoringInfo?.trackName?.toCString().orEmpty().ifBlank { telemetry.trackName.toCString() },
            vehicleClassHint = scoring?.vehClass?.toCString(),
            sessionTypeLabel = scoringInfo?.session?.let(LmuTelemetryConversions::sessionTypeLabel),
            sessionPhaseLabel = scoringInfo?.gamePhase?.toInt()?.let(LmuTelemetryConversions::sessionPhaseLabel),
            tyreCompoundLabel = telemetry.frontTireCompoundName.toCString()
                .ifBlank { telemetry.rearTireCompoundName.toCString() },
            lapNumber = resolveCompletedLaps(telemetry, scoring)?.plus(1),
            completedLaps = resolveCompletedLaps(telemetry, scoring),
            currentLapTimeMs = resolveCurrentLapTimeMs(scoringInfo, scoring, telemetry),
            lastLapTimeMs = scoring?.lastLapTime?.let(LmuTelemetryConversions::lapSecondsToMs),
            bestLapTimeMs = scoring?.bestLapTime?.let(LmuTelemetryConversions::lapSecondsToMs),
            currentSectorIndex = resolveCurrentSectorIndex(scoring, telemetry),
            lastSectorTimeMs = scoring?.lastCompletedSectorTimeMs(),
            isLapValid = telemetry.lapInvalidated.toInt() == 0,
            speedKmh = calculateSpeedKmh(telemetry),
            gear = LmuTelemetryConversions.commonGear(telemetry.gear),
            rpm = telemetry.engineRpm.toFloat().takeIf(Float::isFinite)?.takeIf { it >= 0f },
            throttle = telemetry.unfilteredThrottle.toFloat().takeIf(Float::isFinite),
            brake = telemetry.unfilteredBrake.toFloat().takeIf(Float::isFinite),
            steeringAngleRad = telemetry.unfilteredSteering.toFloat().takeIf(Float::isFinite),
            lateralG = calculateLateralG(telemetry),
            yawRateRad = telemetry.localRot.getOrNull(1)?.toFloat()?.takeIf(Float::isFinite),
            fuelLiters = telemetry.fuel.toFloat().takeIf(Float::isFinite)?.takeIf { it >= 0f },
            fuelCapacityLiters = telemetry.fuelCapacity.toFloat().takeIf(Float::isFinite)?.takeIf { it > 0f },
            airTempC = scoringInfo?.ambientTemp?.toFloat()?.takeIf(Float::isFinite),
            roadTempC = scoringInfo?.trackTemp?.toFloat()?.takeIf(Float::isFinite),
            brakeBias = LmuTelemetryConversions.frontBrakeBiasFromRearBias(telemetry.rearBrakeBias),
            tcLevel = telemetry.tc.toUnsignedInt(),
            absLevel = telemetry.abs.toUnsignedInt(),
            pitLimiterOn = telemetry.speedLimiterActive.toInt() != 0,
            tyreFl = telemetry.wheels.getOrNull(0)?.toRecordedWheelSample(),
            tyreFr = telemetry.wheels.getOrNull(1)?.toRecordedWheelSample(),
            tyreRl = telemetry.wheels.getOrNull(2)?.toRecordedWheelSample(),
            tyreRr = telemetry.wheels.getOrNull(3)?.toRecordedWheelSample(),
        )
    }

    private fun resolvePlayerIndex(memory: Memory, scoringOffset: Int, scoringVersion: Int, numVehicles: Int): Int {
        if (scoringVersion <= 0) return 0

        repeat(numVehicles) { index ->
            val scoring = Rf2VehicleScoring().apply {
                attach(memory, scoringOffset + Rf2VehicleScoring.OFFSET + index * Rf2VehicleScoring.SIZE)
                read()
            }
            if (scoring.isPlayer.toInt() != 0) return index
        }
        return 0
    }

    private fun calculateSpeedKmh(telemetry: Rf2VehicleTelemetry): Float? {
        val x = telemetry.localVel.getOrNull(0) ?: return null
        val y = telemetry.localVel.getOrNull(1) ?: return null
        val z = telemetry.localVel.getOrNull(2) ?: return null
        return (sqrt(x * x + y * y + z * z) * KMH_MULTIPLIER).toFloat().takeIf(Float::isFinite)
    }

    private fun calculateLateralG(telemetry: Rf2VehicleTelemetry): Float? {
        val lateral = telemetry.localAccel.getOrNull(0) ?: return null
        return (lateral / GRAVITY_MPS2).toFloat().takeIf(Float::isFinite)
    }

    private fun resolveCompletedLaps(telemetry: Rf2VehicleTelemetry, scoring: Rf2VehicleScoring?): Int? =
        scoring?.totalLaps?.toInt()?.takeIf { it >= 0 }
            ?: telemetry.lapNumber.takeIf { it >= 0 }

    private fun resolveCurrentLapTimeMs(
        scoringInfo: Rf2ScoringInfo?,
        scoring: Rf2VehicleScoring?,
        telemetry: Rf2VehicleTelemetry,
    ): Int? = if (scoringInfo != null && scoring != null) {
        LmuTelemetryConversions.currentLapTimeMs(scoringInfo.currentEt, scoring.lapStartEt)
    } else {
        LmuTelemetryConversions.currentLapTimeMs(telemetry.elapsedTime, telemetry.lapStartEt)
    }

    private fun resolveCurrentSectorIndex(scoring: Rf2VehicleScoring?, telemetry: Rf2VehicleTelemetry): Int? =
        scoring?.sector?.toInt()?.let(LmuTelemetryConversions::commonSectorIndex)
            ?: LmuTelemetryConversions.commonSectorIndex(telemetry.currentSector)
}

private fun Rf2Wheel.toRecordedWheelSample(): RecordedTelemetryTyreSnapshot {
    val inner = temperature.getOrNull(0)?.let(LmuTelemetryConversions::kelvinToCelsius)
    val middle = temperature.getOrNull(1)?.let(LmuTelemetryConversions::kelvinToCelsius)
    val outer = temperature.getOrNull(2)?.let(LmuTelemetryConversions::kelvinToCelsius)
    val avg = listOfNotNull(inner, middle, outer).takeIf { it.isNotEmpty() }?.average()?.toFloat()

    return RecordedTelemetryTyreSnapshot(
        pressurePsi = LmuTelemetryConversions.kpaToPsi(pressure),
        coreTempC = LmuTelemetryConversions.kelvinToCelsius(tireCarcassTemperature),
        innerTempC = inner,
        middleTempC = middle,
        outerTempC = outer,
        avgTempC = avg?.takeIf(Float::isFinite),
        brakeTempC = LmuTelemetryConversions.finiteFloat(brakeTemp),
        load = tireLoad.toFloat().takeIf(Float::isFinite),
    )
}

private fun Rf2VehicleScoring.lastCompletedSectorTimeMs(): Int? =
    when (LmuTelemetryConversions.commonSectorIndex(sector.toInt())) {
        0 -> lastSector3TimeSeconds()
        1 -> curSector1
        2 -> curSector2
        else -> null
    }?.let(LmuTelemetryConversions::lapSecondsToMs)

private fun Rf2VehicleScoring.lastSector3TimeSeconds(): Double? {
    if (lastLapTime <= 0.0 || lastSector1 <= 0.0 || lastSector2 <= 0.0) return null
    return (lastLapTime - lastSector1 - lastSector2).takeIf { it > 0.0 }
}

private fun Byte.toUnsignedInt(): Int = toInt() and 0xff

private fun telemetryBufferSize(): Int = Rf2VehicleTelemetry.OFFSET + Rf2VehicleTelemetry.SIZE * LMU_MAX_VEHICLES

private const val LMU_MAX_VEHICLES: Int = 128
private const val LMU_NUM_VEHICLES_OFFSET: Int = 12
private const val GRAVITY_MPS2: Double = 9.80665
private const val KMH_MULTIPLIER: Double = 3.6
