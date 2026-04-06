package com.project.analyzer.ac.telemetry.impl.recording.analysis

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayload
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayloadDecoder
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryTyreSnapshot
import com.project.analyzer.utils.shm.toBoolean
import com.project.analyzer.utils.shm.toKString
import com.sun.jna.Memory
import dev.zacsweers.metro.Inject

@Inject
internal class AcRecordedPayloadDecoder : RecordedTelemetryPayloadDecoder {

    override val payloadType: String = "ac_shm_v1"

    private val physicsSize: Int = SPageFilePhysics().size()
    private val graphicsSize: Int = SPageFileGraphics().size()
    private val staticSize: Int = SPageFileStatic().size()

    override fun decode(payload: ByteArray): RecordedTelemetryPayload? {
        if (payload.size < physicsSize + graphicsSize + staticSize) return null

        val memory = Memory(payload.size.toLong())
        memory.write(0, payload, 0, payload.size)

        val physics = SPageFilePhysics()
        physics.attach(memory.share(0))

        val graphics = SPageFileGraphics()
        graphics.attach(memory.share(physicsSize.toLong()))

        val statics = SPageFileStatic()
        statics.attach(memory.share((physicsSize + graphicsSize).toLong()))

        return RecordedTelemetryPayload(
            gameId = "ac",
            carModel = statics.carModel.toKString(),
            carLabel = statics.carModel.toKString(),
            trackLabel = statics.track.toKString(),
            tyreCompoundLabel = graphics.tyreCompound.toKString().ifBlank { statics.dryTyresName.toKString() },
            isRainTyres = graphics.rainTyres.toBoolean(),
            speedKmh = physics.speedKmh.takeIf(Float::isFinite),
            gear = physics.gear.takeIf { it >= -1 },
            rpm = physics.rpm.toFloat().takeIf(Float::isFinite)?.takeIf { it >= 0f },
            throttle = physics.gas.takeIf(Float::isFinite),
            brake = physics.brake.takeIf(Float::isFinite),
            steeringAngleRad = physics.steerAngle.takeIf(Float::isFinite),
            lateralG = physics.accG.getOrNull(0)?.takeIf(Float::isFinite),
            yawRateRad = physics.localAngularVel.getOrNull(1)?.takeIf(Float::isFinite),
            fuelLiters = physics.fuel.takeIf(Float::isFinite)?.takeIf { it >= 0f },
            fuelCapacityLiters = statics.maxFuel.takeIf(Float::isFinite)?.takeIf { it > 0f },
            tyreFl = physics.toTyreSnapshot(0),
            tyreFr = physics.toTyreSnapshot(1),
            tyreRl = physics.toTyreSnapshot(2),
            tyreRr = physics.toTyreSnapshot(3),
        )
    }
}

private fun SPageFilePhysics.toTyreSnapshot(index: Int): RecordedTelemetryTyreSnapshot = RecordedTelemetryTyreSnapshot(
    pressurePsi = wheelsPressure.getOrNull(index)?.takeIf(Float::isFinite),
    coreTempC = tyreCoreTemperature.getOrNull(index)?.takeIf(Float::isFinite),
    innerTempC = tyreTempI.getOrNull(index)?.takeIf(Float::isFinite),
    middleTempC = tyreTempM.getOrNull(index)?.takeIf(Float::isFinite),
    outerTempC = tyreTempO.getOrNull(index)?.takeIf(Float::isFinite),
    avgTempC = tyreTemp.getOrNull(index)?.takeIf(Float::isFinite),
    brakeTempC = brakeTemp.getOrNull(index)?.takeIf(Float::isFinite),
    slip = wheelSlip.getOrNull(index)?.takeIf(Float::isFinite),
    load = wheelLoad.getOrNull(index)?.takeIf(Float::isFinite),
)
