package com.project.analyzer.ac.telemetry.impl.recording.analysis

import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStaticPageView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoTyreStateView
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayload
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayloadDecoder
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryTyreSnapshot
import com.sun.jna.Memory
import dev.zacsweers.metro.Inject

@Inject
internal class AceRecordedPayloadDecoder : RecordedTelemetryPayloadDecoder {

    override val payloadType: String = "ace_shm_v1"

    private val physicsSize: Int = SPageFilePhysics().size()
    private val graphicsSize: Int = AcEvoGraphicsPageView.SIZE_BYTES
    private val staticSize: Int = AcEvoStaticPageView.SIZE_BYTES

    override fun decode(payload: ByteArray): RecordedTelemetryPayload? {
        if (payload.size < physicsSize + graphicsSize + staticSize) return null

        val memory = Memory(payload.size.toLong())
        memory.write(0, payload, 0, payload.size)

        val physics = SPageFilePhysics()
        physics.attach(memory.share(0))

        val graphics = AcEvoGraphicsPageView()
        graphics.attachMemory(memory.share(physicsSize.toLong()))

        val statics = AcEvoStaticPageView()
        statics.attachMemory(memory.share((physicsSize + graphicsSize).toLong()))

        return RecordedTelemetryPayload(
            gameId = "ace",
            carModel = graphics.carModel.ifBlank { null },
            carLabel = graphics.carModel.ifBlank { null },
            trackLabel = statics.track.ifBlank { null },
            trackLayoutLabel = statics.trackConfiguration.ifBlank { null },
            sessionTypeLabel = statics.session.name.takeIf { it != "UNKNOWN" },
            sessionPhaseLabel = graphics.sessionState.phaseName.ifBlank { null },
            tyreCompoundLabel = graphics.currentTyreCompound.ifBlank { null },
            isRainTyres = graphics.isWetTyreCompound,
            lapNumber = graphics.sessionState.currentLap.takeIf { it > 0 }
                ?: graphics.totalLapCount.takeIf { it >= 0 }?.plus(1),
            completedLaps = graphics.totalLapCount.takeIf { it >= 0 },
            currentLapTimeMs = graphics.currentLapTimeMs.takeIf { it >= 0 },
            lastLapTimeMs = graphics.lastLaptimeMs.takeIf { it > 0 },
            bestLapTimeMs = graphics.bestLaptimeMs.takeIf { it > 0 },
            estimatedLapTimeMs = graphics.predictedLapTimeMs.takeIf { it > 0 },
            isLapValid = graphics.isValidLap,
            speedKmh = physics.speedKmh.takeIf(Float::isFinite),
            gear = graphics.gearInt,
            rpm = graphics.rpm.toFloat().takeIf(Float::isFinite)?.takeIf { it >= 0f },
            throttle = graphics.gasPercent.takeIf(Float::isFinite),
            brake = graphics.brakePercent.takeIf(Float::isFinite),
            steeringAngleRad = physics.steerAngle.takeIf(Float::isFinite),
            lateralG = graphics.gForcesX.takeIf(Float::isFinite),
            yawRateRad = physics.localAngularVel.getOrNull(1)?.takeIf(Float::isFinite),
            fuelLiters = graphics.fuelLiterCurrentQuantity.takeIf { it.isFinite() && it >= 0f },
            fuelCapacityLiters = graphics.maxFuel.takeIf { it.isFinite() && it > 0f },
            fuelPerLapLiters = firstPositive(graphics.fuelPerLap, graphics.fuelLiterPerLap),
            airTempC = graphics.airTemperatureC.toFloat().takeIf(Float::isFinite),
            roadTempC = physics.roadTemp.takeIf(Float::isFinite),
            brakeBias = graphics.electronics.brakeBias.takeIf(Float::isFinite),
            tcLevel = graphics.electronics.tcLevel.toInt().takeIf { it >= 0 },
            absLevel = graphics.electronics.absLevel.toInt().takeIf { it >= 0 },
            pitLimiterOn = graphics.electronics.isPitLimiterOn,
            tyreFl = graphics.tyreLf.toRecordedTyreSnapshot(physics, 0),
            tyreFr = graphics.tyreRf.toRecordedTyreSnapshot(physics, 1),
            tyreRl = graphics.tyreLr.toRecordedTyreSnapshot(physics, 2),
            tyreRr = graphics.tyreRr.toRecordedTyreSnapshot(physics, 3),
        )
    }

    private fun firstPositive(primary: Float, fallback: Float): Float? = when {
        primary.isFinite() && primary > 0f -> primary
        fallback.isFinite() && fallback > 0f -> fallback
        else -> null
    }
}

private fun AcEvoTyreStateView.toRecordedTyreSnapshot(
    physics: SPageFilePhysics,
    index: Int,
): RecordedTelemetryTyreSnapshot = RecordedTelemetryTyreSnapshot(
    pressurePsi = tyrePressure.takeIf(Float::isFinite),
    coreTempC = tyreTemperatureC.takeIf(Float::isFinite),
    innerTempC = tyreTemperatureLeft.takeIf(Float::isFinite),
    middleTempC = tyreTemperatureCenter.takeIf(Float::isFinite),
    outerTempC = tyreTemperatureRight.takeIf(Float::isFinite),
    avgTempC = listOfNotNull(
        tyreTemperatureLeft.takeIf(Float::isFinite),
        tyreTemperatureCenter.takeIf(Float::isFinite),
        tyreTemperatureRight.takeIf(Float::isFinite),
    ).takeIf(List<Float>::isNotEmpty)?.average()?.toFloat(),
    brakeTempC = brakeTemperatureC.takeIf(Float::isFinite),
    slip = slip.takeIf(Float::isFinite),
    load = physics.wheelLoad.getOrNull(index)?.takeIf(Float::isFinite),
)
