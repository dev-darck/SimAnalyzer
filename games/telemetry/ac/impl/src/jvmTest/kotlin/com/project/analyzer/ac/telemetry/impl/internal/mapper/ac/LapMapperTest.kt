package com.project.analyzer.ac.telemetry.impl.internal.mapper.ac

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.AcLapState
import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.AcSessionCache
import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.LapFallbackUsage
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import kotlin.test.Test
import kotlin.test.assertEquals

class LapMapperTest {

    @Test
    fun `sectors only fallback keeps native lap timing and uses fallback sectors`() {
        val cache = AcSessionCache()
        val mapper = LapMapper(cache, AcLapState(cache))
        val graphics = SPageFileGraphics().apply {
            completedLaps = 4
            iCurrentTime = 89_321
            iLastTime = 88_000
            iBestTime = 87_500
            isValidLap = 1
            iDeltaLapTime = -120
            isDeltaPositive = 0
            iSplit = 0
            currentSectorIndex = -1
            lastSectorTime = 0
        }
        val fallback = LapTimingSnapshot(
            isActive = true,
            trackId = "brands_hatch_indy",
            isLapRunning = true,
            completedLapsCount = 7,
            currentLapTimeMs = 95_000,
            currentSectorTimeMs = 31_000,
            currentSectorIndex = 1,
            lastSectorTimeMs = 30_500,
            lastLapTimeMs = 94_000,
            bestLapTimeMs = 93_500,
            lastSectorsMs = listOf(30_500, null, null),
            bestSectorsMs = listOf(30_000, 29_900, 29_800),
            currentLapValid = false,
            deltaLapTimeMs = 850,
            isDeltaPositive = true,
            startFinishSyncId = 12,
        )

        val frame = mapper.map(
            graphics = graphics,
            fallback = fallback,
            sectorCountOverride = 3,
            fallbackUsage = LapFallbackUsage.SECTORS_ONLY,
        )

        assertEquals(4, frame.completedLaps)
        assertEquals(89_321, frame.currentLapTimeMs)
        assertEquals(88_000, frame.lastLapTimeMs)
        assertEquals(87_500, frame.bestLapTimeMs)
        assertEquals(-120, frame.deltaLapTimeMs)
        assertEquals(false, frame.isDeltaPositive)
        assertEquals(1, frame.currentSectorIndex)
        assertEquals(30_500, frame.lastSectorTimeMs)
        assertEquals(31_000, frame.splitTimeMs)
        assertEquals(30_500, frame.sectors.getOrNull(0)?.timeMs)
    }
}
