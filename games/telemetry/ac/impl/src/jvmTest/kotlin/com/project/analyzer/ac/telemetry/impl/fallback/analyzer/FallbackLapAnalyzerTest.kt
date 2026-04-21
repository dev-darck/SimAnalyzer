package com.project.analyzer.ac.telemetry.impl.fallback.analyzer

import com.project.analyzer.ac.telemetry.impl.fallback.TrackCalibrationLoader
import com.project.analyzer.ac.telemetry.impl.fallback.detector.GateCrossingDetector
import com.project.analyzer.ac.telemetry.impl.fallback.detector.model.GateCrossing
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FallbackLapAnalyzerTest {

    @Test
    fun `native sector index completes lap when geometry misses intermediate gates`() {
        val calibration = calibration(trackId = "imola_gp")
        val gateDetector = mockk<GateCrossingDetector>()
        var startFinishCalls = 0
        every { gateDetector.detectCrossing(any(), any(), any()) } answers {
            val gate = thirdArg<Gate>()
            when {
                gate === calibration.startFinish && startFinishCalls++ == 0 -> forwardCrossing()
                else -> null
            }
        }

        val analyzer = FallbackLapAnalyzer(
            calibrationLoader = mockk<TrackCalibrationLoader>(relaxed = true),
            gateDetector = gateDetector,
            ioDispatcher = Dispatchers.Default,
        )
        analyzer.loadCalibration(calibration.trackId, calibration)

        analyzer.processPhysicsFrame(1_000_000_000L, physicsAt(x = -1f, packetId = 1), sectorIndexHint0Based = 0)
        analyzer.processPhysicsFrame(2_000_000_000L, physicsAt(x = 1f, packetId = 2), sectorIndexHint0Based = 0)
        analyzer.processPhysicsFrame(
            timestampNs = 3_000_000_000L,
            physics = physicsAt(x = 2f, packetId = 3),
            sectorIndexHint0Based = 1,
            lastSectorTimeHintMs = 1_000,
        )
        analyzer.processPhysicsFrame(
            timestampNs = 4_000_000_000L,
            physics = physicsAt(x = 3f, packetId = 4),
            sectorIndexHint0Based = 2,
            lastSectorTimeHintMs = 1_000,
        )
        analyzer.processPhysicsFrame(
            timestampNs = 5_000_000_000L,
            physics = physicsAt(x = 4f, packetId = 5),
            sectorIndexHint0Based = 0,
            lastSectorTimeHintMs = 1_000,
        )

        val snapshot = analyzer.getSnapshot(5_000_000_000L)
        assertEquals(1, snapshot.completedLapsCount)
        assertEquals(3_000, snapshot.lastLapTimeMs)
        assertEquals(1_000, snapshot.lastSectorTimeMs)
        assertEquals(0, snapshot.currentLapTimeMs)
    }

    @Test
    fun `invalid start finish sequence realigns instead of accumulating multi lap time`() {
        val calibration = calibration(trackId = "imola_gp")
        val gateDetector = mockk<GateCrossingDetector>()
        var startFinishCalls = 0
        every { gateDetector.detectCrossing(any(), any(), any()) } answers {
            val gate = thirdArg<Gate>()
            when {
                gate === calibration.startFinish && startFinishCalls++ in setOf(0, 2) -> forwardCrossing()
                else -> null
            }
        }

        val analyzer = FallbackLapAnalyzer(
            calibrationLoader = mockk<TrackCalibrationLoader>(relaxed = true),
            gateDetector = gateDetector,
            ioDispatcher = Dispatchers.Default,
        )
        analyzer.loadCalibration(calibration.trackId, calibration)

        analyzer.processPhysicsFrame(1_000_000_000L, physicsAt(x = -1f, packetId = 1), sectorIndexHint0Based = 0)
        analyzer.processPhysicsFrame(2_000_000_000L, physicsAt(x = 1f, packetId = 2), sectorIndexHint0Based = 0)
        analyzer.processPhysicsFrame(3_000_000_000L, physicsAt(x = 2f, packetId = 3), sectorIndexHint0Based = 0)
        analyzer.processPhysicsFrame(4_000_000_000L, physicsAt(x = 3f, packetId = 4), sectorIndexHint0Based = 0)
        analyzer.processPhysicsFrame(5_000_000_000L, physicsAt(x = 4f, packetId = 5), sectorIndexHint0Based = 0)

        val snapshot = analyzer.getSnapshot(5_000_000_000L)
        assertEquals(0, snapshot.completedLapsCount)
        assertNull(snapshot.lastLapTimeMs)
        assertEquals(1_000, snapshot.currentLapTimeMs)
        assertEquals(2, snapshot.startFinishSyncId)
    }

    @Test
    fun `backward native sector hint is ignored and does not fabricate lap`() {
        val calibration = calibration(trackId = "imola_gp")
        val gateDetector = mockk<GateCrossingDetector>()
        var startFinishCalls = 0
        every { gateDetector.detectCrossing(any(), any(), any()) } answers {
            val gate = thirdArg<Gate>()
            when {
                gate === calibration.startFinish && startFinishCalls++ == 0 -> forwardCrossing()
                else -> null
            }
        }

        val analyzer = FallbackLapAnalyzer(
            calibrationLoader = mockk<TrackCalibrationLoader>(relaxed = true),
            gateDetector = gateDetector,
            ioDispatcher = Dispatchers.Default,
        )
        analyzer.loadCalibration(calibration.trackId, calibration)

        analyzer.processPhysicsFrame(1_000_000_000L, physicsAt(x = -1f, packetId = 1), sectorIndexHint0Based = 0)
        analyzer.processPhysicsFrame(2_000_000_000L, physicsAt(x = 1f, packetId = 2), sectorIndexHint0Based = 0)
        analyzer.processPhysicsFrame(
            timestampNs = 3_000_000_000L,
            physics = physicsAt(x = 2f, packetId = 3),
            sectorIndexHint0Based = 1,
            lastSectorTimeHintMs = 1_000,
        )
        analyzer.processPhysicsFrame(
            timestampNs = 4_000_000_000L,
            physics = physicsAt(x = 3f, packetId = 4),
            sectorIndexHint0Based = 0,
            lastSectorTimeHintMs = 1_000,
        )

        val snapshot = analyzer.getSnapshot(4_000_000_000L)
        assertEquals(0, snapshot.completedLapsCount)
        assertNull(snapshot.lastLapTimeMs)
        assertEquals(1, snapshot.currentSectorIndex)
        assertEquals(2_000, snapshot.currentLapTimeMs)
    }

    private fun calibration(trackId: String): TrackCalibration = TrackCalibration(
        trackId = trackId,
        trackName = trackId,
        layoutId = "gp",
        createdAtEpochMs = 1_000L,
        source = TrackCalibrationSource.USER,
        referencePoint = ReferencePoint.FRONT_AXLE,
        startFinish = gate(centerX = 0f),
        sectors = listOf(
            SectorCalibration(index = 1, start = gate(centerX = 0f), finish = gate(centerX = 10f)),
            SectorCalibration(index = 2, start = gate(centerX = 10f), finish = gate(centerX = 20f)),
            SectorCalibration(index = 3, start = gate(centerX = 20f), finish = gate(centerX = 0f)),
        ),
    )

    private fun gate(centerX: Float): Gate = Gate.create(
        center = Vec2(centerX, 0f),
        forward = Vec2(1f, 0f),
        normal = Vec2(0f, 1f),
        halfWidthMeters = 6f,
    )

    private fun forwardCrossing(): GateCrossing = GateCrossing(
        interpolationFactor = 1f,
        isForwardDirection = true,
        hitPoint = Vec2.Zero,
        outsideByMeters = 0f,
    )

    private fun physicsAt(x: Float, packetId: Int): SPageFilePhysics = SPageFilePhysics().apply {
        this.packetId = packetId
        speedKmh = 120f
        heading = 0f
        tyreContactPoint = floatArrayOf(
            x - 0.8f, 0f, 0f,
            x + 0.8f, 0f, 0f,
            x - 2.8f, 0f, 0f,
            x - 1.2f, 0f, 0f,
        )
    }
}
