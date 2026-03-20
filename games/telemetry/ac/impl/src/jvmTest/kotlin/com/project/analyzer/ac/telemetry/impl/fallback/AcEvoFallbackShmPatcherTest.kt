package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackFuelAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.FuelSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoFileInfoExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import com.project.analyzer.ac.telemetry.impl.internal.AcSessionRestartHint
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.utils.TelemetryIdentityIds
import com.project.analyzer.utils.shm.writeWString
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class AcEvoFallbackShmPatcherTest {

    private fun readWString(a: CharArray): String {
        val n = a.indexOfFirst { it == '\u0000' }.let { if (it < 0) a.size else it }
        return a.concatToString(0, n)
    }

    private fun forceNativeEmpty(graphics: SPageFileGraphics, statics: SPageFileStatic) {
        graphics.packetId = 0
        statics.sectorCount = 0
        statics.numCars = 0
        statics.track.fill('\u0000')
        statics.carModel.fill('\u0000')
        statics.playerName.fill('\u0000')
        statics.playerSurname.fill('\u0000')
    }

    private fun sampleLapSnapshot() = LapTimingSnapshot(
        isActive = true,
        trackId = "spa_gp",
        isLapRunning = true,
        completedLapsCount = 0,
        currentLapTimeMs = 0,
        currentSectorTimeMs = 0,
        currentSectorIndex = 0,
        lastSectorTimeMs = null,
        lastLapTimeMs = null,
        bestLapTimeMs = null,
        lastSectorsMs = emptyList(),
        bestSectorsMs = emptyList(),
        currentLapValid = true,
        deltaLapTimeMs = null,
        isDeltaPositive = true,
        startFinishSyncId = 1
    )

    private fun sampleCalibration(trackId: String = "spa_gp"): TrackCalibration = TrackCalibration(
        trackId = trackId,
        trackName = trackId,
        layoutId = null,
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
    fun `sessionEpoch change resets analyzers and then processes physics in session when packetId valid`() {
        val extractor = mockk<AcEvoFileInfoExtractor>()
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        val info = EvoFileInfo(
            trackId = "spa_gp",
            trackName = "Spa",
            carModel = "car",
            driverName = "John Doe",
            sessionEpoch = 1L,
            penaltyId = null
        )
        every { extractor.poll() } returns info

        val physics = SPageFilePhysics().apply {
            packetId = 1
        }
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot()
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot(fuelPerLapLiters = null)

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)

        patcher.patchIfNeeded(
            shm = shm,
            loopStartNanos = 3_000_000_000L,
            gameState = GameConnectionState.IN_SESSION
        )

        verify(exactly = 1) { lapAnalyzer.resetSession() }
        verify(exactly = 1) { fuelAnalyzer.reset() }

        verify(exactly = 1) { lapAnalyzer.processPhysicsFrame(any(), physics, any(), any()) }
    }

    @Test
    fun `penalty is deduplicated - onPenaltyDetected only once per id`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        val info = EvoFileInfo(
            trackId = "spa_gp",
            trackName = "Spa",
            carModel = "car",
            driverName = "John Doe",
            sessionEpoch = 1L,
            penaltyId = "penalty#1"
        )
        every { extractor.poll() } returns info

        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot()
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)

        patcher.patchIfNeeded(shm, 3_000_000_000L, GameConnectionState.IN_MENU)

        forceNativeEmpty(graphics, statics)

        patcher.patchIfNeeded(shm, 3_100_000_000L, GameConnectionState.IN_MENU)

        verify(exactly = 1) { lapAnalyzer.onPenaltyDetected() }
        verify(exactly = 2) { extractor.clearPenalty() }

        assertEquals("spa_gp", readWString(statics.track))
        assertEquals("car", readWString(statics.carModel))
        assertEquals("John", readWString(statics.playerName))
        assertEquals("Doe", readWString(statics.playerSurname))
        assertEquals(TelemetryIdentityIds.stableCarId("car"), graphics.playerCarID)
        assertEquals(TelemetryIdentityIds.stableCarId("car"), graphics.carID[0])
    }

    @Test
    fun `processPhysicsFrame is called only when physics packetId changes`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        val info = EvoFileInfo(
            trackId = "spa_gp",
            trackName = "Spa",
            carModel = "car",
            driverName = "John Doe",
            sessionEpoch = 1L,
            penaltyId = null
        )
        every { extractor.poll() } returns info

        val physics = SPageFilePhysics().apply { packetId = 1 }
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot()
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)
        patcher.patchIfNeeded(shm, 3_000_000_000L, GameConnectionState.IN_SESSION)
        patcher.patchIfNeeded(shm, 3_050_000_000L, GameConnectionState.IN_SESSION)
        physics.packetId = 2
        patcher.patchIfNeeded(shm, 3_100_000_000L, GameConnectionState.IN_SESSION)

        verify(exactly = 2) { lapAnalyzer.processPhysicsFrame(any(), physics, any(), any()) }
    }

    @Test
    fun `session type is patched on graphics-only tick before physics packet gate`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returnsMany listOf(
            EvoFileInfo(
                trackId = "spa_gp",
                trackName = "Spa",
                carModel = "car",
                driverName = "John Doe",
                sessionEpoch = 1L,
                sessionType = EvoSessionType.QUALIFYING,
            ),
            EvoFileInfo(
                trackId = "spa_gp",
                trackName = "Spa",
                carModel = "car",
                driverName = "John Doe",
                sessionEpoch = 1L,
                sessionType = EvoSessionType.QUALIFYING,
            ),
        )

        val physics = SPageFilePhysics().apply { packetId = 10 }
        val graphics = SPageFileGraphics().apply {
            session = EvoSessionType.PRACTICE.shmValue
        }
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)
        graphics.session = EvoSessionType.PRACTICE.shmValue

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot()
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)

        patcher.patchIfNeeded(shm, 1_000_000_000L, GameConnectionState.IN_SESSION)
        assertEquals(EvoSessionType.QUALIFYING.shmValue, graphics.session)

        // Simulate native graphics-only drift/flap while physics packet stays unchanged.
        graphics.session = EvoSessionType.PRACTICE.shmValue

        // Same physics packet: fallback runtime processing is skipped, but session type in graphics
        // must still be patched to keep lifecycle stable on graphics-only ticks.
        patcher.patchIfNeeded(shm, 1_100_000_000L, GameConnectionState.IN_SESSION)

        assertEquals(EvoSessionType.QUALIFYING.shmValue, graphics.session)
        verify(exactly = 1) { lapAnalyzer.processPhysicsFrame(any(), physics, any(), any()) }
    }

    @Test
    fun `fallback patch keeps native timing when no calibration is available`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returns EvoFileInfo(
            trackId = "spa_gp",
            trackName = "Spa",
            carModel = "car",
            driverName = "John Doe",
            sessionEpoch = 1L,
            sessionType = EvoSessionType.PRACTICE,
        )

        val physics = SPageFilePhysics().apply { packetId = 1 }
        val graphics = SPageFileGraphics().apply {
            session = EvoSessionType.PRACTICE.shmValue
            completedLaps = 5
            iCurrentTime = 1_111
            iLastTime = 2_222
            iBestTime = 3_333
            currentSectorIndex = 2
            lastSectorTime = 444
            iSplit = 555
            isValidLap = 1
            iDeltaLapTime = 666
            isDeltaPositive = 0
            fuelXLap = 2.5f
            fuelEstimatedLaps = 12.5f
        }
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)
        graphics.session = EvoSessionType.PRACTICE.shmValue
        graphics.completedLaps = 5
        graphics.iCurrentTime = 1_111
        graphics.iLastTime = 2_222
        graphics.iBestTime = 3_333
        graphics.currentSectorIndex = 2
        graphics.lastSectorTime = 444
        graphics.iSplit = 555
        graphics.isValidLap = 1
        graphics.iDeltaLapTime = 666
        graphics.isDeltaPositive = 0
        graphics.fuelXLap = 2.5f
        graphics.fuelEstimatedLaps = 12.5f

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot().copy(
            completedLapsCount = 99,
            currentLapTimeMs = 9_999,
            currentSectorIndex = 3,
            currentSectorTimeMs = 888,
            lastSectorTimeMs = 777,
            lastLapTimeMs = 6_666,
            bestLapTimeMs = 5_555,
            currentLapValid = true,
            deltaLapTimeMs = 123,
            isDeltaPositive = true,
        )
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot(
            fuelPerLapLiters = 4.2f,
            fuelEstimatedLaps = 99f,
        )

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)
        patcher.patchIfNeeded(shm, 2_000_000_000L, GameConnectionState.IN_SESSION)

        assertEquals(5, graphics.completedLaps)
        assertEquals(1_111, graphics.iCurrentTime)
        assertEquals(2_222, graphics.iLastTime)
        assertEquals(3_333, graphics.iBestTime)
        assertEquals(2, graphics.currentSectorIndex)
        assertEquals(444, graphics.lastSectorTime)
        assertEquals(555, graphics.iSplit)
        assertEquals(1, graphics.isValidLap)
        assertEquals(666, graphics.iDeltaLapTime)
        assertEquals(0, graphics.isDeltaPositive)
        assertEquals(4.2f, graphics.fuelXLap)
        assertEquals(99f, graphics.fuelEstimatedLaps)
    }

    @Test
    fun `fallback patch clears stale fallback fuel values when analyzer has no estimate`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returns EvoFileInfo(
            trackId = "spa_gp",
            trackName = "Spa",
            carModel = "car",
            driverName = "John Doe",
            sessionEpoch = 1L,
            sessionType = EvoSessionType.PRACTICE,
        )

        val physics = SPageFilePhysics().apply { packetId = 1 }
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot()
        every { fuelAnalyzer.getSnapshot(any()) } returnsMany listOf(
            FuelSnapshot(fuelPerLapLiters = 4.2f, fuelEstimatedLaps = 99f),
            FuelSnapshot(),
        )

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)
        patcher.patchIfNeeded(shm, 2_000_000_000L, GameConnectionState.IN_SESSION)

        assertEquals(4.2f, graphics.fuelXLap)
        assertEquals(99f, graphics.fuelEstimatedLaps)

        physics.packetId = 2
        patcher.patchIfNeeded(shm, 3_000_000_000L, GameConnectionState.IN_SESSION)

        assertEquals(0f, graphics.fuelXLap)
        assertEquals(0f, graphics.fuelEstimatedLaps)
    }

    @Test
    fun `fallback patch overwrites statics identity from logfile`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returns EvoFileInfo(
            trackId = "fallback_track",
            trackName = "Fallback Track",
            carModel = "fallback_car",
            driverName = "Fallback Driver",
            sessionEpoch = 1L,
            sessionType = EvoSessionType.PRACTICE,
        )

        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics().apply { session = -1 }
        val statics = SPageFileStatic().apply {
            numCars = 12
            numberOfSessions = 4
            sectorCount = 3
            track.writeWString("native_track")
            carModel.writeWString("native_car")
            playerName.writeWString("Native")
            playerSurname.writeWString("Driver")
        }

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics
        every { lapAnalyzer.loadCalibration(any()) } returns null

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)
        patcher.patchIfNeeded(shm, 3_000_000_000L, GameConnectionState.IN_MENU)

        assertEquals("fallback_track", readWString(statics.track))
        assertEquals("fallback_car", readWString(statics.carModel))
        assertEquals("Fallback", readWString(statics.playerName))
        assertEquals("Driver", readWString(statics.playerSurname))
        assertEquals(12, statics.numCars)
        assertEquals(4, statics.numberOfSessions)
        assertEquals(3, statics.sectorCount)
    }

    @Test
    fun `fallback patch does not synthesize timing when calibration is absent`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returns EvoFileInfo(
            trackId = "spa_gp",
            trackName = "Spa",
            carModel = "car",
            driverName = "John Doe",
            sessionEpoch = 1L,
            sessionType = EvoSessionType.PRACTICE,
        )

        val physics = SPageFilePhysics().apply { packetId = 1 }
        val graphics = SPageFileGraphics().apply {
            session = EvoSessionType.PRACTICE.shmValue
            completedLaps = 7
            iCurrentTime = 43_210
            currentSectorIndex = 1
            iSplit = 9_999
            lastSectorTime = 30_000
            iLastTime = 120_000
            iBestTime = 119_000
        }
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)
        graphics.session = EvoSessionType.PRACTICE.shmValue
        graphics.completedLaps = 7
        graphics.iCurrentTime = 43_210
        graphics.currentSectorIndex = 1
        graphics.iSplit = 9_999
        graphics.lastSectorTime = 30_000
        graphics.iLastTime = 120_000
        graphics.iBestTime = 119_000

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot().copy(
            completedLapsCount = 1,
            currentLapTimeMs = 1_000,
            currentSectorIndex = 2,
            currentSectorTimeMs = 500,
            lastSectorTimeMs = 400,
            lastLapTimeMs = 90_000,
            bestLapTimeMs = 89_000,
        )
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)
        patcher.patchIfNeeded(shm, 2_500_000_000L, GameConnectionState.IN_SESSION)

        assertEquals(7, graphics.completedLaps)
        assertEquals(43_210, graphics.iCurrentTime)
        assertEquals(120_000, graphics.iLastTime)
        assertEquals(119_000, graphics.iBestTime)
        assertEquals(1, graphics.currentSectorIndex)
        assertEquals(30_000, graphics.lastSectorTime)
        assertEquals(9_999, graphics.iSplit)
    }

    @Test
    fun `fallback patch applies analyzer timing when calibration exists`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxed = true, relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returns EvoFileInfo(
            trackId = "spa_gp",
            trackName = "Spa",
            layoutId = "gp",
            carModel = "car",
            driverName = "John Doe",
            sessionEpoch = 1L,
            sessionType = EvoSessionType.PRACTICE,
        )

        val physics = SPageFilePhysics().apply { packetId = 1 }
        val graphics = SPageFileGraphics().apply { session = EvoSessionType.PRACTICE.shmValue }
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)
        graphics.session = EvoSessionType.PRACTICE.shmValue

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns sampleCalibration()
        every { lapAnalyzer.isSyncedToStartFinish() } returns false
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot().copy(
            completedLapsCount = 2,
            currentLapTimeMs = 12_000,
            currentSectorIndex = 2,
            currentSectorTimeMs = 4_500,
            lastSectorTimeMs = 32_500,
            lastLapTimeMs = 95_000,
            bestLapTimeMs = 94_000,
        )
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)
        patcher.patchIfNeeded(shm, 2_700_000_000L, GameConnectionState.IN_SESSION)

        assertEquals(2, graphics.completedLaps)
        assertEquals(12_000, graphics.iCurrentTime)
        assertEquals(95_000, graphics.iLastTime)
        assertEquals(94_000, graphics.iBestTime)
        assertEquals(2, graphics.currentSectorIndex)
        assertEquals(32_500, graphics.lastSectorTime)
        assertEquals(4_500, graphics.iSplit)
    }

    @Test
    fun `fallback patch does not overwrite populated graphics car id`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returns EvoFileInfo(
            trackId = "brands_hatch_indy",
            trackName = "Brands Hatch Indy",
            carModel = "ks_bmw_m4_gt3",
            driverName = "John Doe",
            sessionEpoch = 1L,
            sessionType = EvoSessionType.PRACTICE,
        )

        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics().apply {
            playerCarID = 77
            carID[0] = 77
        }
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)
        graphics.playerCarID = 77
        graphics.carID[0] = 77

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics
        every { lapAnalyzer.loadCalibration(any()) } returns null

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)
        patcher.patchIfNeeded(shm, 3_000_000_000L, GameConnectionState.IN_MENU)

        assertEquals(77, graphics.playerCarID)
        assertEquals(77, graphics.carID[0])
    }

    @Test
    fun `session epoch does not force graphics sessionIndex when native index is absent`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returns EvoFileInfo(
            trackId = "spa_gp",
            trackName = "Spa",
            carModel = "car",
            driverName = "John Doe",
            sessionEpoch = 5L,
            sessionType = EvoSessionType.PRACTICE,
        )

        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics().apply { sessionIndex = 0 }
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics
        every { lapAnalyzer.loadCalibration(any()) } returns null

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)
        patcher.patchIfNeeded(shm, 4_000_000_000L, GameConnectionState.IN_MENU)

        assertEquals(0, graphics.sessionIndex)
    }

    @Test
    fun `log sessionType boundary bumps synthetic session index and resets fallback runtime`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returnsMany listOf(
            EvoFileInfo(
                trackId = "brands_hatch_indy",
                trackName = "Brands Hatch Indy",
                carModel = "ks_bmw_m4_gt3",
                driverName = "John Doe",
                sessionEpoch = 1L,
                sessionType = EvoSessionType.PRACTICE,
            ),
            EvoFileInfo(
                trackId = "brands_hatch_indy",
                trackName = "Brands Hatch Indy",
                carModel = "ks_bmw_m4_gt3",
                driverName = "John Doe",
                sessionEpoch = 1L,
                sessionType = EvoSessionType.QUALIFYING,
            ),
        )

        val physics = SPageFilePhysics().apply { packetId = 1 }
        val graphics = SPageFileGraphics().apply {
            sessionIndex = 0
            session = EvoSessionType.PRACTICE.shmValue
        }
        val statics = SPageFileStatic().apply { isTimedRace = 1 }
        forceNativeEmpty(graphics, statics)
        graphics.session = EvoSessionType.PRACTICE.shmValue
        statics.isTimedRace = 1

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics
        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot()
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)

        patcher.patchIfNeeded(shm, 1_000_000_000L, GameConnectionState.IN_SESSION)
        assertEquals(0, graphics.sessionIndex)
        assertEquals(EvoSessionType.PRACTICE.shmValue, graphics.session)

        physics.packetId = 2

        patcher.patchIfNeeded(shm, 1_200_000_000L, GameConnectionState.IN_SESSION)

        assertEquals(1, graphics.sessionIndex)
        assertEquals(EvoSessionType.QUALIFYING.shmValue, graphics.session)
        verify(atLeast = 2) { lapAnalyzer.resetSession() }
        verify(atLeast = 2) { fuelAnalyzer.reset() }
    }

    @Test
    fun `fallback patch ignores analyzer timing until a calibration is loaded`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returnsMany listOf(
            EvoFileInfo(
                sessionEpoch = 1L,
                sessionType = EvoSessionType.PRACTICE,
                trackId = "paul_ricard_3a",
            ),
            EvoFileInfo(
                sessionEpoch = 1L,
                sessionType = EvoSessionType.PRACTICE,
                trackId = "paul_ricard_3a",
            ),
        )

        val physics = SPageFilePhysics().apply { packetId = 77 }
        val graphics = SPageFileGraphics().apply {
            session = EvoSessionType.PRACTICE.shmValue
            iCurrentTime = 10_000
            iSplit = 4_000
            currentSectorIndex = 1
            completedLaps = 0
        }
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)
        graphics.session = EvoSessionType.PRACTICE.shmValue
        graphics.iCurrentTime = 10_000
        graphics.iSplit = 4_000
        graphics.currentSectorIndex = 1
        graphics.completedLaps = 0

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics
        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot().copy(
            completedLapsCount = 1,
            currentLapTimeMs = 12_500,
            currentSectorTimeMs = 6_500,
            currentSectorIndex = 2,
            lastSectorTimeMs = 32_000,
            lastLapTimeMs = 95_000,
            bestLapTimeMs = 95_000,
        )
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)

        patcher.patchIfNeeded(shm, 1_000_000_000L, GameConnectionState.IN_SESSION)
        assertEquals(10_000, graphics.iCurrentTime)
        assertEquals(4_000, graphics.iSplit)
        assertEquals(0, graphics.completedLaps)

        patcher.patchIfNeeded(shm, 1_250_000_000L, GameConnectionState.IN_SESSION)
        assertEquals(10_000, graphics.iCurrentTime)
        assertEquals(4_000, graphics.iSplit)
        assertEquals(1, graphics.currentSectorIndex)
        assertEquals(0, graphics.completedLaps)

        verify(exactly = 1) { lapAnalyzer.processPhysicsFrame(any(), physics, any(), any()) }
    }

    @Test
    fun `fallback patch does not apply analyzer timing before the first physics packet`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returns EvoFileInfo(
            sessionEpoch = 1L,
            sessionType = EvoSessionType.PRACTICE,
            trackId = "paul_ricard_3a",
        )

        val physics = SPageFilePhysics().apply { packetId = 0 }
        val graphics = SPageFileGraphics().apply {
            session = EvoSessionType.PRACTICE.shmValue
            iCurrentTime = 5_000
            iSplit = 750
            currentSectorIndex = 0
        }
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)
        graphics.session = EvoSessionType.PRACTICE.shmValue
        graphics.iCurrentTime = 5_000
        graphics.iSplit = 750
        graphics.currentSectorIndex = 0

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics
        every { lapAnalyzer.loadCalibration(any()) } returns sampleCalibration(trackId = "paul_ricard_3a")
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot().copy(
            currentLapTimeMs = 7_321,
            currentSectorTimeMs = 1_111,
            currentSectorIndex = 1,
        )
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)
        patcher.patchIfNeeded(shm, 2_000_000_000L, GameConnectionState.IN_SESSION)

        assertEquals(5_000, graphics.iCurrentTime)
        assertEquals(750, graphics.iSplit)
        assertEquals(0, graphics.currentSectorIndex)
    }

    @Test
    fun `main menu restart hint is emitted only after session re-entry`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returnsMany listOf(
            EvoFileInfo(sessionEpoch = 1L),
            EvoFileInfo(sessionEpoch = 2L, sessionEpochStartedFromMainMenu = true),
            EvoFileInfo(sessionEpoch = 2L, sessionEpochStartedFromMainMenu = true),
            EvoFileInfo(sessionEpoch = 2L, sessionEpochStartedFromMainMenu = true),
            EvoFileInfo(sessionEpoch = 2L, sessionEpochStartedFromMainMenu = true),
        )

        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot()
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)

        assertEquals(
            AcSessionRestartHint.NONE,
            patcher.patchIfNeeded(shm, 1_000_000_000L, GameConnectionState.IN_SESSION),
        )
        assertEquals(
            AcSessionRestartHint.NONE,
            patcher.patchIfNeeded(shm, 1_200_000_000L, GameConnectionState.IN_SESSION),
        )
        assertEquals(
            AcSessionRestartHint.NONE,
            patcher.patchIfNeeded(shm, 1_400_000_000L, GameConnectionState.IN_MENU),
        )
        assertEquals(
            AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU,
            patcher.patchIfNeeded(shm, 1_600_000_000L, GameConnectionState.IN_SESSION),
        )
        assertEquals(
            AcSessionRestartHint.NONE,
            patcher.patchIfNeeded(shm, 1_800_000_000L, GameConnectionState.IN_SESSION),
        )
    }

    @Test
    fun `same group restart hint is emitted immediately in session`() {
        val extractor = mockk<AcEvoFileInfoExtractor>(relaxUnitFun = true)
        val lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true)
        val fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxUnitFun = true)

        every { extractor.poll() } returnsMany listOf(
            EvoFileInfo(sessionEpoch = 1L),
            EvoFileInfo(sessionEpoch = 2L),
            EvoFileInfo(sessionEpoch = 2L),
        )

        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()
        forceNativeEmpty(graphics, statics)

        val shm = mockk<AcSharedMemory>()
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        every { lapAnalyzer.loadCalibration(any()) } returns null
        every { lapAnalyzer.getSnapshot(any()) } returns sampleLapSnapshot()
        every { fuelAnalyzer.getSnapshot(any()) } returns FuelSnapshot()

        val patcher = AcEvoFallbackShmPatcher(extractor, lapAnalyzer, fuelAnalyzer)

        assertEquals(
            AcSessionRestartHint.NONE,
            patcher.patchIfNeeded(shm, 1_000_000_000L, GameConnectionState.IN_SESSION),
        )
        assertEquals(
            AcSessionRestartHint.PRESERVE_GROUP,
            patcher.patchIfNeeded(shm, 1_200_000_000L, GameConnectionState.IN_SESSION),
        )
        assertEquals(
            AcSessionRestartHint.NONE,
            patcher.patchIfNeeded(shm, 1_400_000_000L, GameConnectionState.IN_SESSION),
        )
    }
}
