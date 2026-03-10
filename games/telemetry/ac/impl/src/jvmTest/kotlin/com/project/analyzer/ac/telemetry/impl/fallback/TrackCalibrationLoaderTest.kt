package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrackCalibrationLoaderTest {

    @Test
    fun `load caches calibration for the same map key`() = runTest {
        val repository = FakeTrackCalibrationRepository(
            calibration = calibration(trackId = "brands_hatch_gp"),
        )
        val loader = TrackCalibrationLoader(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        val first = loader.load(trackId = "brands_hatch_gp", layoutId = "gp")
        val second = loader.load(trackId = "brands_hatch_gp", layoutId = "gp")

        assertEquals(first, second)
        assertEquals(1, repository.loadCalls)
    }

    @Test
    fun `load caches missing calibration result for the same map key`() = runTest {
        val repository = FakeTrackCalibrationRepository(calibration = null)
        val loader = TrackCalibrationLoader(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        val first = loader.load(trackId = "paul_ricard_3a", layoutId = "3a")
        val second = loader.load(trackId = "paul_ricard_3a", layoutId = "3a")

        assertNull(first)
        assertNull(second)
        assertEquals(1, repository.loadCalls)
    }

    @Test
    fun `cache updates cached calibration without extra repository load`() = runTest {
        val repository = FakeTrackCalibrationRepository(
            calibration = calibration(trackId = "imola_gp", source = TrackCalibrationSource.GAME),
        )
        val loader = TrackCalibrationLoader(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        val cachedCalibration = calibration(
            trackId = "imola_gp",
            source = TrackCalibrationSource.USER,
        )

        loader.load(trackId = "imola_gp", layoutId = "gp")
        loader.cache(cachedCalibration)
        val resolved = loader.load(trackId = "imola_gp", layoutId = "gp")

        assertEquals(cachedCalibration, resolved)
        assertEquals(1, repository.loadCalls)
    }

    @Test
    fun `cache replaces stale missing entry for combined track id without layout`() = runTest {
        val repository = FakeTrackCalibrationRepository(calibration = null)
        val loader = TrackCalibrationLoader(
            repository = repository,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        val savedCalibration = calibration(
            trackId = "paul_ricard_3a",
            source = TrackCalibrationSource.USER,
        ).copy(layoutId = "3a")

        val initial = loader.load(trackId = "paul_ricard_3a", layoutId = null)
        loader.cache(savedCalibration)
        val resolved = loader.load(trackId = "paul_ricard_3a", layoutId = null)

        assertNull(initial)
        assertEquals(savedCalibration, resolved)
        assertEquals(1, repository.loadCalls)
    }

    private fun calibration(
        trackId: String,
        source: TrackCalibrationSource = TrackCalibrationSource.USER,
    ): TrackCalibration = TrackCalibration(
        trackId = trackId,
        trackName = trackId,
        layoutId = "gp",
        createdAtEpochMs = 1_000L,
        source = source,
        referencePoint = ReferencePoint.FRONT_AXLE,
        startFinish = gate(centerX = 0f),
        sectors = listOf(
            SectorCalibration(
                index = 1,
                start = gate(centerX = 0f),
                finish = gate(centerX = 20f),
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
        private val calibration: TrackCalibration?,
    ) : TrackCalibrationRepository {

        var loadCalls: Int = 0
            private set

        override suspend fun save(calibration: TrackCalibration, source: TrackCalibrationSource) = Unit

        override suspend fun load(trackId: String, layoutId: String?): TrackCalibration? {
            loadCalls += 1
            return calibration
        }

        override suspend fun loadBySource(
            trackId: String,
            source: TrackCalibrationSource,
            layoutId: String?,
        ): TrackCalibration? = null

        override suspend fun loadAll(source: TrackCalibrationSource?): List<TrackCalibration> = emptyList()
    }
}
