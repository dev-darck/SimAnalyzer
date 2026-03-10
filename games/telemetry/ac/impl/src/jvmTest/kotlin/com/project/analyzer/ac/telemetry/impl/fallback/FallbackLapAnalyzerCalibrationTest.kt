package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.detector.GateCrossingDetector
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FallbackLapAnalyzerCalibrationTest {

    @Test
    fun `loadCalibration null marks active and returns null`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val analyzer = FallbackLapAnalyzer(
            calibrationLoader = TrackCalibrationLoader(
                repository = FakeTrackCalibrationRepository(),
                ioDispatcher = dispatcher,
            ),
            gateDetector = mockk<GateCrossingDetector>(relaxed = true),
            ioDispatcher = dispatcher,
        )

        val calibration = analyzer.loadCalibration(trackId = null)

        assertNull(calibration)
    }

    @Test
    fun `loadCalibration loads same track only once`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeTrackCalibrationRepository(
            calibrations = mapOf("spa_gp" to calibration("spa_gp")),
        )
        val analyzer = FallbackLapAnalyzer(
            calibrationLoader = TrackCalibrationLoader(
                repository = repository,
                ioDispatcher = dispatcher,
            ),
            gateDetector = mockk<GateCrossingDetector>(relaxed = true),
            ioDispatcher = dispatcher,
        )

        val first = analyzer.loadCalibration("spa_gp")
        runCurrent()
        val second = analyzer.loadCalibration("spa_gp")

        assertNull(first)
        assertEquals("spa_gp", second?.trackId)
        assertEquals(1, repository.loadCalls)
    }

    @Test
    fun `loadCalibration caches missing calibration`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeTrackCalibrationRepository()
        val analyzer = FallbackLapAnalyzer(
            calibrationLoader = TrackCalibrationLoader(
                repository = repository,
                ioDispatcher = dispatcher,
            ),
            gateDetector = mockk<GateCrossingDetector>(relaxed = true),
            ioDispatcher = dispatcher,
        )

        val first = analyzer.loadCalibration("missing")
        runCurrent()
        val second = analyzer.loadCalibration("missing")

        assertNull(first)
        assertNull(second)
        assertEquals(1, repository.loadCalls)
    }

    @Test
    fun `loadCalibration reuses loader cache after switching away and back`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = FakeTrackCalibrationRepository(
            calibrations = mapOf(
                "spa_gp" to calibration("spa_gp"),
                "monza_gp" to calibration("monza_gp"),
            ),
        )
        val analyzer = FallbackLapAnalyzer(
            calibrationLoader = TrackCalibrationLoader(
                repository = repository,
                ioDispatcher = dispatcher,
            ),
            gateDetector = mockk<GateCrossingDetector>(relaxed = true),
            ioDispatcher = dispatcher,
        )

        val firstSpa = analyzer.loadCalibration("spa_gp")
        runCurrent()
        val loadedSpa = analyzer.loadCalibration("spa_gp")
        val monza = analyzer.loadCalibration("monza_gp")
        runCurrent()
        val loadedMonza = analyzer.loadCalibration("monza_gp")
        val secondSpa = analyzer.loadCalibration("spa_gp")

        assertNull(firstSpa)
        assertEquals("spa_gp", loadedSpa?.trackId)
        assertNull(monza)
        assertEquals("monza_gp", loadedMonza?.trackId)
        assertEquals("spa_gp", secondSpa?.trackId)
        assertEquals(2, repository.loadCalls)
    }

    private fun calibration(trackId: String): TrackCalibration = TrackCalibration(
        trackId = trackId,
        trackName = trackId,
        layoutId = "gp",
        createdAtEpochMs = 1_000L,
        source = TrackCalibrationSource.USER,
        referencePoint = ReferencePoint.FRONT_AXLE,
        startFinish = gate(0f),
        sectors = listOf(
            SectorCalibration(
                index = 1,
                start = gate(0f),
                finish = gate(10f),
            ),
        ),
    )

    private fun gate(centerX: Float): Gate = Gate.create(
        center = Vec2(centerX, 0f),
        forward = Vec2(1f, 0f),
        normal = Vec2(0f, 1f),
        halfWidthMeters = 6f,
    )

    private class FakeTrackCalibrationRepository(
        private val calibrations: Map<String, TrackCalibration> = emptyMap(),
    ) : TrackCalibrationRepository {

        var loadCalls: Int = 0
            private set

        override suspend fun save(calibration: TrackCalibration, source: TrackCalibrationSource) = Unit

        override suspend fun load(trackId: String, layoutId: String?): TrackCalibration? {
            loadCalls += 1
            return calibrations[trackId]
        }

        override suspend fun loadBySource(
            trackId: String,
            source: TrackCalibrationSource,
            layoutId: String?,
        ): TrackCalibration? = null

        override suspend fun loadAll(source: TrackCalibrationSource?): List<TrackCalibration> = emptyList()
    }
}
