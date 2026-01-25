package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.detector.GateCrossingDetector
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FallbackLapAnalyzerCalibrationTest {

    @Test
    fun `loadCalibration null marks active and returns null`() {
        val loader = mockk<TrackCalibrationLoader>(relaxed = true)
        val detector = mockk<GateCrossingDetector>(relaxed = true)
        val a = FallbackLapAnalyzer(loader, detector)

        val c = a.loadCalibration(trackId = null)
        assertNull(c)
    }

    @Test
    fun `loadCalibration loads when trackId changes and caches`() {
        val loader = mockk<TrackCalibrationLoader>()
        val detector = mockk<GateCrossingDetector>(relaxed = true)
        val a = FallbackLapAnalyzer(loader, detector)

        val cal = mockk<TrackCalibration>(relaxed = true)
        every { loader.load("spa_gp") } returns cal

        val first = a.loadCalibration("spa_gp")
        val second = a.loadCalibration("spa_gp")

        assertEquals(cal, first)
        assertEquals(cal, second)

        verify(exactly = 1) { loader.load("spa_gp") }
    }

    @Test
    fun `loadCalibration returns null and does not spam loader for same missing id`() {
        val loader = mockk<TrackCalibrationLoader>()
        val detector = mockk<GateCrossingDetector>(relaxed = true)
        val a = FallbackLapAnalyzer(loader, detector)

        every { loader.load("missing") } returns null

        val first = a.loadCalibration("missing")
        val second = a.loadCalibration("missing")

        assertNull(first)
        assertNull(second)

        verify(exactly = 1) { loader.load("missing") }
    }
}
