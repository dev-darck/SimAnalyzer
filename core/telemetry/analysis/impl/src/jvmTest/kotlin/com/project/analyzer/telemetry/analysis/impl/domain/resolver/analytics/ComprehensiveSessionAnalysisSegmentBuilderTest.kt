package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.header.SessionAnalysisHeader
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComprehensiveSessionAnalysisSegmentBuilderTest {

    private val builder = ComprehensiveSessionAnalysisSegmentBuilder()

    @Test
    fun `build ignores invalid laps when resolving straight losses`() {
        val report = report(
            laps = listOf(
                lap(lapNumber = 1, durationMs = 90_000, isValid = true),
                lap(lapNumber = 2, durationMs = 120_000, isValid = false),
            ),
            samples = straightSamples(
                lapNumber = 1,
                startTimestampNs = 0L,
                stepMs = 120L,
            ) + straightSamples(
                lapNumber = 2,
                startTimestampNs = 10_000_000_000L,
                stepMs = 2_500L,
            ),
        )

        val segments = builder.build(
            report = report,
            cornerAnalyses = emptyList(),
            cornerReport = cornerReportWithLinearReferences(),
        )

        val straightSegments = segments.filter { segment -> segment.segmentType.name == "STRAIGHT" }
        assertFalse(straightSegments.isEmpty())
        assertTrue(straightSegments.all { segment -> segment.timeDelta == 0L })
    }

    @Test
    fun `build resolves start finish straight from wrapped corner reference`() {
        val segments = builder.build(
            report = report(
                laps = listOf(
                    lap(lapNumber = 1, durationMs = 90_000, isValid = true),
                    lap(lapNumber = 2, durationMs = 90_400, isValid = true),
                ),
                samples = straightSamples(lapNumber = 1, startTimestampNs = 0L, stepMs = 100L) +
                    straightSamples(lapNumber = 2, startTimestampNs = 10_000_000_000L, stepMs = 120L),
            ),
            cornerAnalyses = emptyList(),
            cornerReport = cornerReportWithWrappedLastCorner(),
        )

        val startFinish = segments.firstOrNull { segment -> segment.segmentName == "Start/finish straight" }

        requireNotNull(startFinish)
        assertEquals(0.05f, startFinish.startPosition, 0.0001f)
        assertEquals(0.15f, startFinish.endPosition, 0.0001f)
        assertTrue(segments.none { segment -> segment.segmentName == "Final straight" })
    }
}

private fun report(
    laps: List<SessionAnalysisLap>,
    samples: List<SessionAnalysisSample>,
): SessionAnalysisReport = SessionAnalysisReport(
    sessionId = 1L,
    header = SessionAnalysisHeader(
        gameId = "test",
        sessionTypeLabel = "Practice",
        carLabel = "GT3",
        trackLabel = "Test Track",
        startedAtMs = 1_000L,
    ),
    vehicleClass = SessionAnalysisVehicleClass.GT3,
    segments = listOf(
        SessionAnalysisSegment(
            segmentId = 1L,
            sessionTypeLabel = "Practice",
            sessionLabel = "Practice",
            carLabel = "GT3",
            trackLabel = "Test Track",
            startedAtMs = 1_000L,
            endedAtMs = 91_000L,
        ),
    ),
    laps = laps,
    samples = samples,
)

private fun lap(
    lapNumber: Int,
    durationMs: Int,
    isValid: Boolean,
): SessionAnalysisLap = SessionAnalysisLap(
    segmentId = 1L,
    sessionTypeLabel = "Practice",
    lapNumber = lapNumber,
    isValid = isValid,
    isPitLap = false,
    isComplete = true,
    durationMs = durationMs,
)

private fun cornerReportWithLinearReferences(): SessionAnalysisCornerAnalysisReport =
    SessionAnalysisCornerAnalysisReport(
        referenceCornersBySegmentId = mapOf(
            1L to listOf(
                cornerReference(cornerNumber = 1, start = 0.18f, end = 0.28f),
                cornerReference(cornerNumber = 2, start = 0.55f, end = 0.66f),
            ),
        ),
    )

private fun cornerReportWithWrappedLastCorner(): SessionAnalysisCornerAnalysisReport =
    SessionAnalysisCornerAnalysisReport(
        referenceCornersBySegmentId = mapOf(
            1L to listOf(
                cornerReference(cornerNumber = 1, start = 0.15f, end = 0.24f),
                cornerReference(cornerNumber = 2, start = 0.46f, end = 0.56f),
                cornerReference(cornerNumber = 3, start = 0.84f, end = 0.05f),
            ),
        ),
    )

private fun cornerReference(
    cornerNumber: Int,
    start: Float,
    end: Float,
): SessionAnalysisCornerReference = SessionAnalysisCornerReference(
    cornerNumber = cornerNumber,
    startTrackPosition = start,
    apexTrackPosition = start + 0.03f,
    endTrackPosition = end,
    brakePointTrackPosition = start,
    throttlePickupTrackPosition = end,
    entrySpeedKmh = 180f,
    apexSpeedKmh = 120f,
    exitSpeedKmh = 170f,
)

private fun straightSamples(
    lapNumber: Int,
    startTimestampNs: Long,
    stepMs: Long,
): List<SessionAnalysisSample> {
    val positions = listOf(0.02f, 0.08f, 0.14f, 0.22f, 0.34f, 0.50f, 0.64f, 0.78f, 0.92f)
    return positions.mapIndexed { index, trackPosition ->
        SessionAnalysisSample(
            segmentId = 1L,
            frameId = lapNumber * 100L + index,
            timestampNs = startTimestampNs + index * stepMs * 1_000_000L,
            lapNumber = lapNumber,
            sampleIndexInLap = index,
            trackPosition = trackPosition,
            trackX = trackPosition * 1_000f,
            trackY = trackPosition * 50f,
            speedKmh = 170f + index,
            throttle = 0.85f,
            brake = 0.02f,
            steeringAngleRad = 0.03f,
        )
    }
}
