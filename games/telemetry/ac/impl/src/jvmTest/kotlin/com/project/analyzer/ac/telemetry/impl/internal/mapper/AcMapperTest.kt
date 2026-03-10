package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.AcLapState
import com.project.analyzer.ac.telemetry.impl.internal.AcRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.damage.DamageFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelsFrame
import com.project.analyzer.telemetry.api.model.environment.EnvironmentFrame
import com.project.analyzer.utils.shm.writeWString
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AcMapperTest {

    private fun sampleCalibration(trackId: String = "paul_ricard_3c"): TrackCalibration = TrackCalibration(
        trackId = trackId,
        trackName = trackId,
        layoutId = "3c",
        createdAtEpochMs = 1L,
        source = TrackCalibrationSource.GAME,
        referencePoint = ReferencePoint.FRONT_AXLE,
        startFinish = Gate.create(
            center = Vec2(0f, 0f),
            forward = Vec2(1f, 0f),
            normal = Vec2(0f, 1f),
            halfWidthMeters = 8f,
        ),
        sectors = listOf(
            SectorCalibration(
                index = 1,
                start = Gate.create(
                    center = Vec2(0f, 0f),
                    forward = Vec2(1f, 0f),
                    normal = Vec2(0f, 1f),
                    halfWidthMeters = 8f,
                ),
                finish = Gate.create(
                    center = Vec2(10f, 0f),
                    forward = Vec2(1f, 0f),
                    normal = Vec2(0f, 1f),
                    halfWidthMeters = 8f,
                ),
            ),
            SectorCalibration(
                index = 2,
                start = Gate.create(
                    center = Vec2(10f, 0f),
                    forward = Vec2(1f, 0f),
                    normal = Vec2(0f, 1f),
                    halfWidthMeters = 8f,
                ),
                finish = Gate.create(
                    center = Vec2(20f, 0f),
                    forward = Vec2(1f, 0f),
                    normal = Vec2(0f, 1f),
                    halfWidthMeters = 8f,
                ),
            ),
            SectorCalibration(
                index = 3,
                start = Gate.create(
                    center = Vec2(20f, 0f),
                    forward = Vec2(1f, 0f),
                    normal = Vec2(0f, 1f),
                    halfWidthMeters = 8f,
                ),
                finish = Gate.create(
                    center = Vec2(0f, 0f),
                    forward = Vec2(1f, 0f),
                    normal = Vec2(0f, 1f),
                    halfWidthMeters = 8f,
                ),
            ),
        ),
    )

    @Test
    fun `map uses graphics timing when calibration is absent`() = runTest {
        val cache = AcSessionCache()
        val sessionMapper = SessionMapper(cache)
        val lapMapper = LapMapper(cache, AcLapState(cache))
        val carMapper = mockk<CarMapper>()
        val wheelsMapper = mockk<WheelsMapper>()
        val damageMapper = mockk<DamageMapper>()
        val environmentMapper = mockk<EnvironmentMapper>()
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)

        every { carMapper.map(any(), any(), any()) } returns CarFrame()
        every { wheelsMapper.map(any(), any()) } returns WheelsFrame()
        every { damageMapper.map(any()) } returns DamageFrame()
        every { environmentMapper.map(any(), any()) } returns EnvironmentFrame()
        every { lapAnalyzer.loadCalibration(any()) } returns null

        val mapper = AcMapper(
            cache = cache,
            sessionMapper = sessionMapper,
            lapMapper = lapMapper,
            carMapper = carMapper,
            wheelsMapper = wheelsMapper,
            damageMapper = damageMapper,
            environmentMapper = environmentMapper,
            lapAnalyzer = lapAnalyzer,
        )

        val snapshot = AcRawSnapshot(
            physics = SPageFilePhysics(),
            graphics = SPageFileGraphics(),
            statics = SPageFileStatic(),
            timestampNs = 123L,
        ).apply {
            statics.track.writeWString("Paul Ricard")
            statics.trackConfiguration.writeWString("Layout 3C")
            statics.sectorCount = 3
            graphics.completedLaps = 1
            graphics.iCurrentTime = 12_345
            graphics.currentSectorIndex = 1
            graphics.iSplit = 4_321
            graphics.lastSectorTime = 30_000
            graphics.iLastTime = 90_000
            graphics.iBestTime = 88_000
        }

        val frame = mapper.map(snapshot)
        val lap = assertNotNull(frame.lap)
        val track = assertNotNull(frame.session?.track)

        assertEquals(3, track.sectorCount)
        assertEquals(12_345, lap.currentLapTimeMs)
        assertEquals(4_321, lap.splitTimeMs)
        assertEquals(1, lap.currentSectorIndex)
        assertEquals(30_000, lap.lastSectorTimeMs)
        assertEquals(90_000, lap.lastLapTimeMs)
        assertEquals(88_000, lap.bestLapTimeMs)
        assertEquals(3, lap.sectorCount)
    }

    @Test
    fun `map uses fallback analyzer timing when calibration exists`() = runTest {
        val cache = AcSessionCache()
        val sessionMapper = SessionMapper(cache)
        val lapMapper = LapMapper(cache, AcLapState(cache))
        val carMapper = mockk<CarMapper>()
        val wheelsMapper = mockk<WheelsMapper>()
        val damageMapper = mockk<DamageMapper>()
        val environmentMapper = mockk<EnvironmentMapper>()
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)

        every { carMapper.map(any(), any(), any()) } returns CarFrame()
        every { wheelsMapper.map(any(), any()) } returns WheelsFrame()
        every { damageMapper.map(any()) } returns DamageFrame()
        every { environmentMapper.map(any(), any()) } returns EnvironmentFrame()
        every { lapAnalyzer.loadCalibration(any()) } returns sampleCalibration()
        every { lapAnalyzer.getSnapshot(any()) } returns LapTimingSnapshot(
            isActive = true,
            trackId = "paul_ricard_3c",
            isLapRunning = true,
            completedLapsCount = 2,
            currentLapTimeMs = 22_222,
            currentSectorTimeMs = 7_777,
            currentSectorIndex = 2,
            lastSectorTimeMs = 31_111,
            lastLapTimeMs = 92_000,
            bestLapTimeMs = 91_000,
            lastSectorsMs = listOf(30_000, 31_000, 32_000),
            bestSectorsMs = listOf(29_000, 30_000, 31_000),
            currentLapValid = true,
            deltaLapTimeMs = null,
            isDeltaPositive = true,
            startFinishSyncId = 1,
        )

        val mapper = AcMapper(
            cache = cache,
            sessionMapper = sessionMapper,
            lapMapper = lapMapper,
            carMapper = carMapper,
            wheelsMapper = wheelsMapper,
            damageMapper = damageMapper,
            environmentMapper = environmentMapper,
            lapAnalyzer = lapAnalyzer,
        )

        val snapshot = AcRawSnapshot(
            physics = SPageFilePhysics(),
            graphics = SPageFileGraphics(),
            statics = SPageFileStatic(),
            timestampNs = 123L,
        ).apply {
            statics.track.writeWString("Paul Ricard")
            statics.trackConfiguration.writeWString("Layout 3C")
            statics.sectorCount = 3
            graphics.completedLaps = 9
            graphics.iCurrentTime = 99_999
            graphics.currentSectorIndex = 1
            graphics.iSplit = 44_444
            graphics.lastSectorTime = 33_333
            graphics.iLastTime = 130_000
            graphics.iBestTime = 120_000
        }

        val frame = mapper.map(snapshot)
        val lap = assertNotNull(frame.lap)
        val track = assertNotNull(frame.session?.track)

        assertEquals(3, track.sectorCount)
        assertEquals(22_222, lap.currentLapTimeMs)
        assertEquals(7_777, lap.splitTimeMs)
        assertEquals(2, lap.currentSectorIndex)
        assertEquals(31_111, lap.lastSectorTimeMs)
        assertEquals(92_000, lap.lastLapTimeMs)
        assertEquals(91_000, lap.bestLapTimeMs)
        assertEquals(3, lap.sectorCount)
        assertEquals(listOf(30_000, 31_000, 32_000), lap.sectors.map { it.timeMs })
        assertEquals(listOf(29_000, 30_000, 31_000), lap.sectors.map { it.bestTimeMs })
    }
}
