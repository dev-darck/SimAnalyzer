package com.project.analyzer.telemetry.analysis.impl.domain.assembler

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndexFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class SessionAnalysisTrackMapAssemblerTest {

    private val assembler = SessionAnalysisTrackMapAssembler()

    @Test
    fun `build selects clean complete lap over longer invalid lap`() {
        val result = assembler.build(
            samples = buildCircularLap(
                lapNumber = 1,
                repeats = 2,
                flags = TelemetryFrameIndexFlags.INVALID_LAP,
            ) + buildCircularLap(lapNumber = 2),
            preferredSegmentId = SegmentId,
            preferredBestLapNumber = null,
        )

        assertNotNull(result.trackMap)
        assertEquals(2, result.sourceLapNumber)
    }

    @Test
    fun `build falls back to shortest closed lap when all laps are invalid`() {
        val result = assembler.build(
            samples = buildCircularLap(
                lapNumber = 1,
                repeats = 2,
                flags = TelemetryFrameIndexFlags.INVALID_LAP,
            ) + buildCircularLap(
                lapNumber = 2,
                flags = TelemetryFrameIndexFlags.INVALID_LAP,
            ),
            preferredSegmentId = SegmentId,
            preferredBestLapNumber = null,
        )

        assertNotNull(result.trackMap)
        assertEquals(2, result.sourceLapNumber)
    }
}

private fun buildCircularLap(
    lapNumber: Int,
    repeats: Int = 1,
    flags: Int = 0,
): List<SessionAnalysisSample> {
    val sampleCount = CirclePointCount * repeats
    return (0 until sampleCount).map { index ->
        val angle = (index % CirclePointCount).toDouble() / CirclePointCount.toDouble() * PI * 2.0
        SessionAnalysisSample(
            segmentId = SegmentId,
            frameId = lapNumber * 10_000L + index,
            timestampNs = lapNumber * 1_000_000_000L + index * 20_000_000L,
            lapNumber = lapNumber,
            flags = flags,
            sampleIndexInLap = index,
            trackX = (cos(angle) * CircleRadiusMeters).toFloat(),
            trackY = (sin(angle) * CircleRadiusMeters).toFloat(),
        )
    }
}

private const val SegmentId: Long = 1L
private const val CirclePointCount: Int = 64
private const val CircleRadiusMeters: Float = 50f

