package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackFuelAnalyzer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FallbackFuelAnalyzerTest {

    @Test
    fun `first sync id initializes without computing`() {
        val a = FallbackFuelAnalyzer()

        a.processFrame(
            fuelLiters = 50f,
            completedLaps = 0,
            lastLapTimeMs = 0,
            startFinishSyncId = 1
        )

        val s = a.getSnapshot(currentFuelLiters = 50f)
        assertNull(s.fuelPerLapLiters)
        assertNull(s.fuelEstimatedLaps)
    }

    @Test
    fun `computes per lap on lap completion`() {
        val a = FallbackFuelAnalyzer()

        a.processFrame(50f, completedLaps = 0, lastLapTimeMs = 0, startFinishSyncId = 1)

        a.processFrame(48f, completedLaps = 1, lastLapTimeMs = 90000, startFinishSyncId = 1)

        val s = a.getSnapshot(currentFuelLiters = 48f)
        assertNotNull(s.fuelPerLapLiters)
        assertEquals(2.0f, s.fuelPerLapLiters, 1e-4f)
        assertNotNull(s.fuelEstimatedLaps)
        assertEquals(24.0f, s.fuelEstimatedLaps, 1e-3f)
    }

    @Test
    fun `startFinishSyncId change resets lap baseline`() {
        val a = FallbackFuelAnalyzer()

        a.processFrame(50f, 0, 0, startFinishSyncId = 1)
        a.processFrame(49f, 0, 0, startFinishSyncId = 1)

        a.processFrame(49f, 0, lastLapTimeMs = 90000, startFinishSyncId = 2)

        val s = a.getSnapshot(49f)
        assertNull(s.fuelPerLapLiters)
    }

    @Test
    fun `refuel resets baseline`() {
        val a = FallbackFuelAnalyzer()

        a.processFrame(10f, 0, 0, 1)
        a.processFrame(11f, 0, 0, 1)

        a.processFrame(9f, 1, 90000, 1)

        val s = a.getSnapshot(9f)
        assertNotNull(s.fuelPerLapLiters)
        assertEquals(2.0f, s.fuelPerLapLiters, 1e-3f)
    }

    @Test
    fun `completedLaps decrease resets baseline`() {
        val a = FallbackFuelAnalyzer()

        a.processFrame(50f, 2, 0, 1)
        a.processFrame(49f, 2, 0, 1)

        a.processFrame(49f, 0, 0, 1)

        a.processFrame(48f, 1, 90000, 1)

        val s = a.getSnapshot(48f)
        assertNotNull(s.fuelPerLapLiters)
        assertEquals(1.0f, s.fuelPerLapLiters, 1e-3f)
    }

    @Test
    fun `sync id change after previous estimate resets next lap fuel baseline`() {
        val a = FallbackFuelAnalyzer()

        a.processFrame(50f, completedLaps = 0, lastLapTimeMs = 0, startFinishSyncId = 1)
        a.processFrame(48f, completedLaps = 1, lastLapTimeMs = 90_000, startFinishSyncId = 1)

        a.processFrame(47.5f, completedLaps = 1, lastLapTimeMs = 0, startFinishSyncId = 2)
        a.processFrame(45.5f, completedLaps = 2, lastLapTimeMs = 90_000, startFinishSyncId = 2)

        val s = a.getSnapshot(45.5f)
        assertNotNull(s.lastLapFuelPerLapLiters)
        assertEquals(2.0f, s.lastLapFuelPerLapLiters, 1e-3f)
        assertNotNull(s.fuelPerLapLiters)
        assertEquals(2.0f, s.fuelPerLapLiters, 1e-3f)
    }
}
