package com.project.analyzer.ac.telemetry.impl.recording.analysis

import com.project.analyzer.ac.telemetry.impl.internal.recording.AcRawFrameEncoder
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AceRecordedPayloadDecoderTest {

    @Test
    fun `decode reads ace raw payload and keeps native timing fields`() {
        val snapshot = AceRawSnapshot().apply {
            graphics.carModelRaw.writeCString("GT")
            graphics.driverNameRaw.writeCString("M")
            statics.trackRaw.writeCString("B")
            statics.trackConfigurationRaw.writeCString("I")
            statics.sessionRaw = 0
            graphics.sessionState.currentLapRaw = 7
            graphics.currentLapTimeMsRaw = 81_234
            graphics.lastLaptimeMsRaw = 80_111
            graphics.bestLaptimeMsRaw = 79_999
            graphics.predictedLapTimeMsRaw = 79_500
            graphics.gasPercentRaw = 0.65f
            graphics.brakePercentRaw = 0.15f
            graphics.rpmRaw = 4_321
            graphics.gearIntRaw = 4
            graphics.fuelLiterCurrentQuantityRaw = 48.5f
            graphics.maxFuelRaw = 110f
            graphics.airTemperatureCRaw = 24
            graphics.electronics.tcLevelRaw = 3
            graphics.electronics.absLevelRaw = 5
            graphics.electronics.brakeBiasRaw = 0.57f
            graphics.electronics.isPitLimiterOnRaw = 1
            graphics.tyreLf.tyrePressureRaw = 27.2f
            graphics.tyreLf.tyreTemperatureLeftRaw = 76f
            graphics.tyreLf.tyreTemperatureCenterRaw = 78f
            graphics.tyreLf.tyreTemperatureRightRaw = 77f
            graphics.tyreLf.brakeTemperatureCRaw = 410f
            physics.speedKmh = 123.4f
            physics.steerAngle = 0.21f
            physics.localAngularVel[1] = 0.44f
            physics.roadTemp = 31f
            physics.wheelLoad[0] = 321f
        }

        val encoded = AcRawFrameEncoder().encode(snapshot)
        val payload = AceRecordedPayloadDecoder().decode(encoded.payload)

        requireNotNull(payload)
        assertEquals("ace", payload.gameId)
        assertEquals("GT", payload.carModel)
        assertEquals("B", payload.trackLabel)
        assertEquals("I", payload.trackLayoutLabel)
        assertEquals("TIME_ATTACK", payload.sessionTypeLabel)
        assertEquals(7, payload.lapNumber)
        assertEquals(81_234, payload.currentLapTimeMs)
        assertEquals(80_111, payload.lastLapTimeMs)
        assertEquals(79_999, payload.bestLapTimeMs)
        assertEquals(79_500, payload.estimatedLapTimeMs)
        assertEquals(123.4f, payload.speedKmh)
        assertEquals(4, payload.gear)
        assertEquals(4321f, payload.rpm)
        assertEquals(48.5f, payload.fuelLiters)
        assertEquals(110f, payload.fuelCapacityLiters)
        assertEquals(24f, payload.airTempC)
        assertEquals(31f, payload.roadTempC)
        assertEquals(0.57f, payload.brakeBias)
        assertEquals(3, payload.tcLevel)
        assertEquals(5, payload.absLevel)
        assertTrue(payload.pitLimiterOn == true)
        assertEquals(27.2f, payload.tyreFl?.pressurePsi)
        assertEquals(321f, payload.tyreFl?.load)
    }
}

private fun ByteArray.writeCString(value: String) {
    fill(0)
    val bytes = value.toByteArray(Charsets.UTF_8)
    val count = minOf(size - 1, bytes.size)
    bytes.copyInto(this, endIndex = count)
}
