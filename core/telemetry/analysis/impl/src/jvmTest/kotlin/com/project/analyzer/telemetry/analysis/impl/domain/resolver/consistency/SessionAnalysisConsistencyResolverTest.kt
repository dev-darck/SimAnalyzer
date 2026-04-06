@file:Suppress("LongParameterList")

package com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionAnalysisConsistencyResolverTest {

    private val resolver = SessionAnalysisConsistencyResolver()

    @Test
    fun `resolve penalizes corners with unstable brake point apex speed and line`() {
        val report = resolver.resolve(
            corners = listOf(
                buildCornerConsistencyInput(
                    lapNumber = 1,
                    brakePoint = 0.18f,
                    apexSpeed = 104f,
                    exitSpeed = 142f,
                    throttlePickup = 0.28f,
                    x = 10f,
                    y = 6f
                ),
                buildCornerConsistencyInput(
                    lapNumber = 2,
                    brakePoint = 0.22f,
                    apexSpeed = 95f,
                    exitSpeed = 128f,
                    throttlePickup = 0.34f,
                    x = 14f,
                    y = 8f
                ),
                buildCornerConsistencyInput(
                    lapNumber = 3,
                    brakePoint = 0.27f,
                    apexSpeed = 88f,
                    exitSpeed = 118f,
                    throttlePickup = 0.40f,
                    x = 18f,
                    y = 11f
                ),
            ),
        )

        val corner = report.corners.single()

        assertEquals(listOf(1, 2, 3), corner.affectedLaps)
        assertTrue(corner.score < 75)
        assertTrue(report.overallScore < 75)
        assertTrue((corner.lineVarianceMeters ?: 0f) > 1f)
    }

    @Test
    fun `resolve ignores single-lap corners`() {
        val report = resolver.resolve(
            corners = listOf(
                buildCornerConsistencyInput(
                    lapNumber = 1,
                    brakePoint = 0.18f,
                    apexSpeed = 104f,
                    exitSpeed = 142f,
                    throttlePickup = 0.28f,
                    x = 10f,
                    y = 6f,
                ),
            ),
        )

        assertTrue(report.corners.isEmpty())
        assertEquals(100, report.overallScore)
    }

    @Test
    fun `resolve keeps perfect score for identical two-lap repeats`() {
        val report = resolver.resolve(
            corners = listOf(
                buildCornerConsistencyInput(
                    lapNumber = 1,
                    brakePoint = 0.20f,
                    apexSpeed = 100f,
                    exitSpeed = 140f,
                    throttlePickup = 0.30f,
                    x = 12f,
                    y = 8f,
                ),
                buildCornerConsistencyInput(
                    lapNumber = 2,
                    brakePoint = 0.20f,
                    apexSpeed = 100f,
                    exitSpeed = 140f,
                    throttlePickup = 0.30f,
                    x = 12f,
                    y = 8f,
                ),
            ),
        )

        val corner = report.corners.single()

        assertEquals(100, corner.score)
        assertEquals(100, report.overallScore)
        assertEquals(0f, corner.brakePointStdPct)
        assertEquals(0f, corner.apexSpeedStdKmh)
    }
}

private fun buildCornerConsistencyInput(
    lapNumber: Int,
    brakePoint: Float,
    apexSpeed: Float,
    exitSpeed: Float,
    throttlePickup: Float,
    x: Float,
    y: Float,
): SessionAnalysisCornerAnalysis {
    val sample = SessionAnalysisSample(
        segmentId = 1L,
        lapNumber = lapNumber,
        sampleIndexInLap = 20,
        trackPosition = 0.24f,
        trackX = x,
        trackY = y,
    )
    return SessionAnalysisCornerAnalysis(
        segmentId = 1L,
        lapNumber = lapNumber,
        cornerNumber = 3,
        score = 100,
        representativeSample = sample,
        startTrackPosition = 0.16f,
        apexTrackPosition = 0.24f,
        endTrackPosition = 0.34f,
        brakePointTrackPosition = brakePoint,
        throttlePickupTrackPosition = throttlePickup,
        apexSpeedKmh = apexSpeed,
        exitSpeedKmh = exitSpeed,
    )
}

