package com.project.analyzer.telemetry.lmu.impl.recording.analysis

import com.project.analyzer.telemetry.lmu.impl.shm.LmuSharedMemory
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2ScoringInfo
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleScoring
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleTelemetry
import com.sun.jna.Memory
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class LmuRecordedPayloadDecoderTest {

    @Test
    fun decodeMapsScoringTimingAndWheelUnits() {
        val payload = testPayload()
        val decoded = assertNotNull(LmuRecordedPayloadDecoder().decode(payload))

        assertEquals("lmu", decoded.gameId)
        assertEquals("Circuit de la Sarthe", decoded.trackLabel)
        assertEquals("Alpine Endurance Team 2025 #35:EC", decoded.carLabel)
        assertEquals("Hyper", decoded.vehicleClassHint)
        assertEquals("PRACTICE", decoded.sessionTypeLabel)
        assertEquals("Green flag", decoded.sessionPhaseLabel)
        assertEquals(5, decoded.lapNumber)
        assertEquals(4, decoded.completedLaps)
        assertTrue(decoded.currentLapTimeMs in 55_509..55_510)
        assertEquals(265_690, decoded.lastLapTimeMs)
        assertEquals(1, decoded.currentSectorIndex)
        assertEquals(47_790, decoded.lastSectorTimeMs)
        assertEquals(3, decoded.gear)
        assertEquals(0.75f, decoded.throttle)
        assertEquals(28.4f, decoded.airTempC)
        assertClose(0.573f, decoded.brakeBias)
        assertClose(27.86f, decoded.tyreFl?.pressurePsi)
        assertClose(65.55f, decoded.tyreFl?.innerTempC)
        assertClose(96.95f, decoded.tyreFl?.coreTempC)
    }

    private fun testPayload(): ByteArray {
        val scoringOffset = LmuSharedMemory.TELEMETRY_BUFFER_SIZE
        val payloadSize = scoringOffset + LmuSharedMemory.SCORING_BUFFER_SIZE
        val memory = Memory(payloadSize.toLong())
        memory.setInt(0, 1)
        memory.setInt(4, 1)
        memory.setInt(12, 2)
        memory.setInt(scoringOffset.toLong(), 1)

        Rf2ScoringInfo().apply {
            attach(memory, scoringOffset + Rf2ScoringInfo.OFFSET)
            trackName = ascii("Circuit de la Sarthe", trackName.size)
            session = 1
            currentEt = 1350.20
            endEt = 21605.0
            maxLaps = Int.MAX_VALUE
            lapDist = 13_624.0
            numVehicles = 2
            gamePhase = 5.toByte()
            inRealtime = 1.toByte()
            playerName = ascii("Pol Dirct", playerName.size)
            ambientTemp = 28.4
            trackTemp = 46.4
            raining = 0.0
            avgPathWetness = 0.0
            write()
        }

        Rf2VehicleScoring().apply {
            attach(memory, scoringOffset + Rf2VehicleScoring.OFFSET + Rf2VehicleScoring.SIZE)
            driverName = ascii("Pol Dirct", driverName.size)
            vehicleName = ascii("Alpine Endurance Team 2025 #35:EC", vehicleName.size)
            totalLaps = 4.toShort()
            sector = 2.toByte()
            lapDist = 1_900.4
            bestLapTime = 260.12
            lastLapTime = 265.69
            curSector1 = 47.79
            curSector2 = -1.0
            isPlayer = 1.toByte()
            place = 25.toByte()
            vehClass = ascii("Hyper", vehClass.size)
            lapStartEt = 1294.69
            write()
        }

        Rf2VehicleTelemetry().apply {
            attach(memory, Rf2VehicleTelemetry.OFFSET + Rf2VehicleTelemetry.SIZE)
            id = 24
            elapsedTime = 1350.20
            lapNumber = 4
            lapStartEt = 1294.69
            vehicleName = ascii("Alpine Endurance Team 2025 #35:EC", vehicleName.size)
            trackName = ascii("Circuit de la Sarthe", trackName.size)
            gear = 2
            engineRpm = 7_500.0
            engineMaxRpm = 9_000.0
            unfilteredThrottle = 0.75
            unfilteredBrake = 0.10
            unfilteredSteering = -0.05
            fuel = 75.7
            fuelCapacity = 115.0
            rearBrakeBias = 0.427
            tc = 7.toByte()
            abs = 5.toByte()
            frontTireCompoundName = ascii("Soft", frontTireCompoundName.size)
            wheels[0].pressure = 192.1
            wheels[0].temperature = doubleArrayOf(338.7, 342.8, 341.7)
            wheels[0].tireCarcassTemperature = 370.1
            wheels[0].brakeTemp = 564.5
            wheels[0].tireLoad = 3_200.0
            write()
        }

        return ByteArray(payloadSize).also { target ->
            memory.read(0, target, 0, target.size)
        }
    }

    private fun ascii(value: String, size: Int): ByteArray =
        ByteArray(size).also { bytes ->
            value.toByteArray(Charsets.US_ASCII)
                .copyInto(bytes, endIndex = minOf(value.length, size - 1))
        }

    private fun assertClose(expected: Float, actual: Float?, tolerance: Float = 0.01f) {
        val value = assertNotNull(actual)
        assertTrue(abs(expected - value) <= tolerance, "expected=$expected actual=$value")
    }
}
