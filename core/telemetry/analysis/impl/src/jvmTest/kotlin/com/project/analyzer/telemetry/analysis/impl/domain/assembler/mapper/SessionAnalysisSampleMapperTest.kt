package com.project.analyzer.telemetry.analysis.impl.domain.assembler.mapper

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.handling.SessionAnalysisHandlingStateResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.tyre.SessionAnalysisTyreStateResolver
import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetryFrame
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionAnalysisSampleMapperTest {

    private val mapper = SessionAnalysisSampleMapper(
        handlingStateResolver = SessionAnalysisHandlingStateResolver(),
        tyreStateResolver = SessionAnalysisTyreStateResolver(),
    )

    @Test
    fun `maps extended payload fields and preserves lap sample index`() {
        val frames = listOf(
            frame(
                frameId = 10L,
                lapNumber = 3,
                payload = payload(
                    currentLapTimeMs = 12_345,
                    currentSectorIndex = 1,
                ),
            ),
            frame(
                frameId = 11L,
                lapNumber = 3,
                payload = payload(
                    currentLapTimeMs = 12_678,
                    currentSectorIndex = 2,
                ),
            ),
        )

        val samples = mapper.map(frames = frames, tyreProfile = null)

        assertEquals(2, samples.size)

        val first = samples[0]
        assertEquals(0, first.sampleIndexInLap)
        assertEquals(3.4f, first.fuelPerLapLiters)
        assertEquals(12_345, first.currentLapTimeMs)
        assertEquals(63_210, first.lastLapTimeMs)
        assertEquals(61_111, first.bestLapTimeMs)
        assertEquals(60_500, first.estimatedLapTimeMs)
        assertEquals(1, first.currentSectorIndex)
        assertEquals(20_222, first.lastSectorTimeMs)
        assertFalse(first.isLapValid!!)
        assertEquals(23.5f, first.airTempC)
        assertEquals(31.25f, first.roadTempC)
        assertEquals(58.5f, first.brakeBias)
        assertEquals(4, first.tcLevel)
        assertEquals(7, first.absLevel)
        assertTrue(first.pitLimiterOn!!)

        val second = samples[1]
        assertEquals(1, second.sampleIndexInLap)
        assertEquals(2, second.currentSectorIndex)
    }

    private fun frame(
        frameId: Long,
        lapNumber: Int,
        payload: RecordedTelemetryPayload,
    ): DecodedRecordedTelemetryFrame = DecodedRecordedTelemetryFrame(
        segmentId = 1L,
        frameId = frameId,
        timestampNs = frameId * 1_000_000L,
        lapNumber = lapNumber,
        sectorIndex = 1,
        payload = payload,
    )

    private fun payload(
        currentLapTimeMs: Int,
        currentSectorIndex: Int,
    ): RecordedTelemetryPayload = RecordedTelemetryPayload(
        gameId = "ace",
        speedKmh = 152.5f,
        gear = 4,
        rpm = 6_850f,
        throttle = 0.81f,
        brake = 0.15f,
        steeringAngleRad = 0.18f,
        lateralG = 1.95f,
        yawRateRad = 0.92f,
        fuelLiters = 42.75f,
        fuelCapacityLiters = 95f,
        fuelPerLapLiters = 3.4f,
        currentLapTimeMs = currentLapTimeMs,
        lastLapTimeMs = 63_210,
        bestLapTimeMs = 61_111,
        estimatedLapTimeMs = 60_500,
        currentSectorIndex = currentSectorIndex,
        lastSectorTimeMs = 20_222,
        isLapValid = false,
        airTempC = 23.5f,
        roadTempC = 31.25f,
        brakeBias = 58.5f,
        tcLevel = 4,
        absLevel = 7,
        pitLimiterOn = true,
    )
}
