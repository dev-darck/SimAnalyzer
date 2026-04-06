@file:Suppress("LongParameterList")

package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SessionAnalysisCornerAnalysisResolverTest {

    private val resolver = SessionAnalysisCornerAnalysisResolver()

    @Test
    fun `resolve flags late apex missing trail braking and lockup against reference lap`() {
        val report = resolver.resolve(
            samples = buildReferenceLap() + buildLateApexLap(),
            bestLapBySegmentId = mapOf(1L to 1),
        )

        val selectedCorner = report.corners.singleOrNull { corner -> corner.lapNumber == 2 }

        assertNotNull(selectedCorner)
        requireNotNull(selectedCorner)
        assertEquals(1, selectedCorner.cornerNumber)
        assertEquals(SessionAnalysisCornerApexClassification.LateApex, selectedCorner.apexClassification)
        assertTrue("trailBrakingScore=${selectedCorner.trailBrakingScore}", selectedCorner.trailBrakingScore < 55)
        assertTrue(selectedCorner.coastingRatio > 0.45f)
        assertTrue(selectedCorner.wheelLockup)
        assertTrue(selectedCorner.timeLossMs > 0)
    }

    @Test
    fun `resolve uses geometry zone numbering when zones are provided`() {
        val report = resolver.resolve(
            samples = buildReferenceLap() + buildLateApexLap(),
            bestLapBySegmentId = mapOf(1L to 1),
            cornerZonesBySegmentId = mapOf(
                1L to listOf(
                    TrackMapCornerZone(
                        cornerNumber = 4,
                        startTrackPosition = 0.08f,
                        endTrackPosition = 0.34f,
                        apexTrackPosition = 0.24f,
                        peakCurvature = 0.028f,
                    ),
                ),
            ),
        )

        val selectedCorner = report.corners.singleOrNull { corner -> corner.lapNumber == 2 }

        assertNotNull(selectedCorner)
        requireNotNull(selectedCorner)
        assertEquals(4, selectedCorner.cornerNumber)
        assertEquals(SessionAnalysisCornerApexClassification.LateApex, selectedCorner.apexClassification)
    }

    @Test
    fun `resolve keeps geometry apex as reference position when zones are provided`() {
        val report = resolver.resolve(
            samples = buildReferenceLap() + buildLateApexLap(),
            bestLapBySegmentId = mapOf(1L to 1),
            cornerZonesBySegmentId = mapOf(
                1L to listOf(
                    TrackMapCornerZone(
                        cornerNumber = 4,
                        startTrackPosition = 0.08f,
                        endTrackPosition = 0.34f,
                        apexTrackPosition = 0.24f,
                        peakCurvature = 0.028f,
                    ),
                ),
            ),
        )

        val referenceCorner = report.referenceCornersBySegmentId[1L]?.singleOrNull()

        assertNotNull(referenceCorner)
        requireNotNull(referenceCorner)
        assertEquals(0.08f, referenceCorner.startTrackPosition)
        assertEquals(0.24f, referenceCorner.apexTrackPosition)
        assertEquals(0.34f, referenceCorner.endTrackPosition)
    }
}

private fun buildReferenceLap(): List<SessionAnalysisSample> {
    val fractions = listOf(0.05f, 0.08f, 0.12f, 0.16f, 0.20f, 0.24f, 0.28f, 0.32f, 0.36f, 0.40f)
    val speeds = listOf(170f, 166f, 150f, 132f, 112f, 95f, 106f, 124f, 146f, 160f)
    val steering = listOf(0f, 0.07f, 0.10f, 0.12f, 0.12f, 0.11f, 0.09f, 0.06f, 0.02f, 0f)
    val lateral = listOf(0.1f, 0.82f, 0.98f, 1.08f, 1.18f, 1.15f, 0.96f, 0.74f, 0.32f, 0.12f)
    val yaw = listOf(0f, 0.22f, 0.26f, 0.30f, 0.32f, 0.29f, 0.25f, 0.20f, 0.08f, 0f)
    val brake = listOf(0f, 0.32f, 0.30f, 0.24f, 0.18f, 0.10f, 0.06f, 0f, 0f, 0f)
    val throttle = listOf(1f, 0.22f, 0f, 0f, 0f, 0.08f, 0.22f, 0.46f, 0.84f, 1f)

    return buildLapSamples(
        lapNumber = 1,
        fractions = fractions,
        speeds = speeds,
        steering = steering,
        lateral = lateral,
        yaw = yaw,
        brake = brake,
        throttle = throttle,
        delta = listOf(0, 0, 2, 4, 6, 8, 8, 8, 8, 8),
    )
}

private fun buildLateApexLap(): List<SessionAnalysisSample> {
    val fractions = listOf(0.05f, 0.08f, 0.12f, 0.16f, 0.20f, 0.24f, 0.28f, 0.32f, 0.36f, 0.40f)
    val speeds = listOf(170f, 168f, 160f, 148f, 136f, 120f, 90f, 102f, 126f, 148f)
    val steering = listOf(0f, 0.07f, 0.10f, 0.12f, 0.13f, 0.13f, 0.11f, 0.09f, 0.03f, 0f)
    val lateral = listOf(0.1f, 0.86f, 1.00f, 1.08f, 1.12f, 1.18f, 1.22f, 0.92f, 0.38f, 0.12f)
    val yaw = listOf(0f, 0.22f, 0.26f, 0.29f, 0.31f, 0.33f, 0.34f, 0.24f, 0.10f, 0f)
    val brake = listOf(0f, 0.92f, 0.82f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    val throttle = listOf(1f, 0.12f, 0f, 0f, 0f, 0.04f, 0.08f, 0.30f, 0.92f, 1f)
    val frontSlip = listOf(0.04f, 0.24f, 0.22f, 0.05f, 0.04f, 0.04f, 0.04f, 0.05f, 0.05f, 0.04f)

    return buildLapSamples(
        lapNumber = 2,
        fractions = fractions,
        speeds = speeds,
        steering = steering,
        lateral = lateral,
        yaw = yaw,
        brake = brake,
        throttle = throttle,
        delta = listOf(0, 12, 28, 46, 74, 104, 138, 156, 164, 170),
        frontSlip = frontSlip,
    )
}

private fun buildLapSamples(
    lapNumber: Int,
    fractions: List<Float>,
    speeds: List<Float>,
    steering: List<Float>,
    lateral: List<Float>,
    yaw: List<Float>,
    brake: List<Float>,
    throttle: List<Float>,
    delta: List<Int>,
    frontSlip: List<Float> = List(fractions.size) { 0.04f },
    rearSlip: List<Float> = List(fractions.size) { 0.03f },
): List<SessionAnalysisSample> {
    return fractions.indices.map { index ->
        val fraction = fractions[index]
        SessionAnalysisSample(
            segmentId = 1L,
            frameId = lapNumber * 1_000L + index,
            timestampNs = lapNumber * 1_000_000_000L + index * 16_000_000L,
            lapNumber = lapNumber,
            sampleIndexInLap = index,
            trackPosition = fraction,
            trackX = fraction * 1_000f,
            trackY = (sin(fraction * 2f * PI).toFloat() * 10f),
            speedKmh = speeds[index],
            steeringAngleRad = steering[index],
            lateralG = lateral[index],
            yawRateRad = yaw[index],
            brake = brake[index],
            throttle = throttle[index],
            deltaToBestMs = delta[index],
            tyreFl = SessionAnalysisTyreState(slip = frontSlip[index]),
            tyreFr = SessionAnalysisTyreState(slip = frontSlip[index]),
            tyreRl = SessionAnalysisTyreState(slip = rearSlip[index]),
            tyreRr = SessionAnalysisTyreState(slip = rearSlip[index]),
        )
    }
}

