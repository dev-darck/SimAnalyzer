package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackFuelAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.FuelSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoFileInfoExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
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

        verify(exactly = 1) { lapAnalyzer.processPhysicsFrame(any(), physics) }
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

        verify(exactly = 2) { lapAnalyzer.processPhysicsFrame(any(), physics) }
    }
}
