package com.project.analyzer.telemetry.analysis.impl.domain.assembler.mapper

import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayload
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionAnalysisHeaderMapperTest {

    @Test
    fun `uses payload labels when metadata is missing`() {
        val metadata = metadata(
            sessionType = null,
            airTempC = null,
            trackTempC = null,
        )
        val payload = RecordedTelemetryPayload(
            gameId = "ace",
            sessionTypeLabel = "TIME_ATTACK",
            sessionPhaseLabel = "GREEN",
            carLabel = "Mercedes-AMG GT2",
            trackLabel = "Brands Hatch",
            trackLayoutLabel = "Indy",
            airTempC = 19.5f,
            roadTempC = 27.25f,
        )

        val header = metadata.toSessionAnalysisHeader(firstPayload = payload)

        assertEquals("TIME_ATTACK", header.sessionTypeLabel)
        assertEquals("GREEN", header.sessionPhaseLabel)
        assertEquals("Mercedes-AMG GT2", header.carLabel)
        assertEquals("Brands Hatch", header.trackLabel)
        assertEquals("Indy", header.trackLayoutLabel)
        assertEquals(19.5f, header.airTempC)
        assertEquals(27.25f, header.trackTempC)
    }

    @Test
    fun `prefers metadata labels when available`() {
        val metadata = metadata(
            sessionType = "RACE",
            airTempC = 21.0f,
            trackTempC = 31.0f,
        )
        val payload = RecordedTelemetryPayload(
            gameId = "ace",
            sessionTypeLabel = "TIME_ATTACK",
            sessionPhaseLabel = "GREEN",
            airTempC = 19.5f,
            roadTempC = 27.25f,
        )

        val header = metadata.toSessionAnalysisHeader(firstPayload = payload)

        assertEquals("RACE", header.sessionTypeLabel)
        assertEquals("GREEN", header.sessionPhaseLabel)
        assertEquals(21.0f, header.airTempC)
        assertEquals(31.0f, header.trackTempC)
    }

    private fun metadata(
        sessionType: String?,
        airTempC: Float?,
        trackTempC: Float?,
    ): RecordedTelemetrySessionMetadata = RecordedTelemetrySessionMetadata(
        sessionId = 1L,
        gameId = "ace",
        sessionType = sessionType,
        carModel = "mercedes_amg_gt2",
        carName = "Mercedes-AMG GT2",
        trackId = "brands_hatch_indy",
        trackName = "Brands Hatch",
        airTempC = airTempC,
        trackTempC = trackTempC,
        startedAtMs = 123L,
        endedAtMs = null,
        dataSource = "native",
        payloadType = "ace_shm_v1",
        payloadSize = 1,
        samplingRateHz = 70,
        frameCount = 1,
        receivedFrames = 1,
        droppedFrames = 0,
        firstTimestampNs = 100L,
        lastTimestampNs = 100L,
        fileVersion = 1,
        indexVersion = 1,
        indexRecordSize = 1,
        indexFields = emptyList(),
        framesFile = "frames.bin",
        indexFile = "index.bin",
        eventsFile = "events.bin",
    )
}
