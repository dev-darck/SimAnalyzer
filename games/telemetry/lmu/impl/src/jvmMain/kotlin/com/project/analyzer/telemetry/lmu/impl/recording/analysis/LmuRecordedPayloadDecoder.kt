package com.project.analyzer.telemetry.lmu.impl.recording.analysis

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
import kotlin.math.abs
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

        return RecordedTelemetryPayload(
            gameId = "lmu",
            carModel = telemetry.vehicleName.toCString().ifBlank { scoring?.vehicleFileName?.toCString() },
            carLabel = telemetry.vehicleName.toCString(),
            trackLabel = scoringInfo?.trackName?.toCString().orEmpty().ifBlank { telemetry.trackName.toCString() },
            vehicleClassHint = scoring?.vehClass?.toCString(),
            tyreCompoundLabel = telemetry.frontTireCompoundName.toCString()
                .ifBlank { telemetry.rearTireCompoundName.toCString() },
            speedKmh = calculateSpeedKmh(telemetry),
            gear = telemetry.gear,
            rpm = telemetry.engineRpm.toFloat().takeIf(Float::isFinite)?.takeIf { it >= 0f },
            throttle = telemetry.filteredThrottle.toFloat().takeIf(Float::isFinite),
            brake = telemetry.filteredBrake.toFloat().takeIf(Float::isFinite),
            steeringAngleRad = telemetry.filteredSteering.toFloat().takeIf(Float::isFinite),
            lateralG = calculateLateralG(telemetry),
            yawRateRad = telemetry.localRot.getOrNull(1)?.toFloat()?.takeIf(Float::isFinite),
            fuelLiters = telemetry.fuel.toFloat().takeIf(Float::isFinite)?.takeIf { it >= 0f },
            fuelCapacityLiters = telemetry.fuelCapacity.toFloat().takeIf(Float::isFinite)?.takeIf { it > 0f },
            tyreFl = telemetry.wheels.getOrNull(0)?.toRecordedWheelSample(),
            tyreFr = telemetry.wheels.getOrNull(1)?.toRecordedWheelSample(),
            tyreRl = telemetry.wheels.getOrNull(2)?.toRecordedWheelSample(),
            tyreRr = telemetry.wheels.getOrNull(3)?.toRecordedWheelSample(),
        )
    }

    private fun resolvePlayerIndex(
        memory: Memory,
        scoringOffset: Int,
        scoringVersion: Int,
        numVehicles: Int,
    ): Int {
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
        return (abs(lateral) / GRAVITY_MPS2).toFloat().takeIf(Float::isFinite)
    }
}

private fun Rf2Wheel.toRecordedWheelSample(): RecordedTelemetryTyreSnapshot {
    val inner = temperature.getOrNull(0)?.toFloat()?.takeIf(Float::isFinite)
    val middle = temperature.getOrNull(1)?.toFloat()?.takeIf(Float::isFinite)
    val outer = temperature.getOrNull(2)?.toFloat()?.takeIf(Float::isFinite)
    val avg = listOfNotNull(inner, middle, outer).takeIf { it.isNotEmpty() }?.average()?.toFloat()

    return RecordedTelemetryTyreSnapshot(
        pressurePsi = pressure.toFloat().takeIf(Float::isFinite),
        coreTempC = tireCarcassTemperature.toFloat().takeIf(Float::isFinite),
        innerTempC = inner,
        middleTempC = middle,
        outerTempC = outer,
        avgTempC = avg?.takeIf(Float::isFinite),
        brakeTempC = brakeTemp.toFloat().takeIf(Float::isFinite),
        load = tireLoad.toFloat().takeIf(Float::isFinite),
    )
}

private fun telemetryBufferSize(): Int = Rf2VehicleTelemetry.OFFSET + Rf2VehicleTelemetry.SIZE * LMU_MAX_VEHICLES

private const val LMU_MAX_VEHICLES: Int = 128
private const val LMU_NUM_VEHICLES_OFFSET: Int = 12
private const val GRAVITY_MPS2: Double = 9.80665
private const val KMH_MULTIPLIER: Double = 3.6
