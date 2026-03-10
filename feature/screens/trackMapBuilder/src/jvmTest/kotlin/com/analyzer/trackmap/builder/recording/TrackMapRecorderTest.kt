package com.analyzer.trackmap.builder.recording

import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import com.project.analyzer.telemetry.api.contract.SimStatus
import com.project.analyzer.telemetry.api.contract.TelemetryGameSettings
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TrackMapRecorderTest {

    @Test
    fun `recorder processes telemetry frames into public state`() {
        runBlocking {
            val telemetry = FakeTelemetryLifecycle()
            val repository = FakeTrackMapRepository()
            val recorder = recorder(
                telemetry = telemetry,
                repository = repository,
            )

            recorder.start()
            yield()
            telemetry.emit(frame(index = 0, x = 1f))
            yield()

            val state = recorder.state.value
            assertTrue(state.recording)
            assertEquals(GameId.ACE.displayName, state.gameLabel)
            assertEquals("spa_gp", state.trackId)
            assertEquals("Spa GP", state.trackName)
            assertEquals(1, state.pointCount)
            assertNotNull(state.currentPosition)
        }
    }

    @Test
    fun `save surfaces repository failure and clears saving flag`() {
        runBlocking {
            val telemetry = FakeTelemetryLifecycle()
            val repository = FakeTrackMapRepository(
                saveError = IllegalStateException("disk full"),
            )
            val recorder = recorder(
                telemetry = telemetry,
                repository = repository,
            )

            recorder.start()
            yield()
            repeat(50) { index ->
                telemetry.emit(frame(index = index, x = 1f + index * 6f))
            }
            yield()
            recorder.stop()
            yield()

            recorder.save()
            yield()

            val state = recorder.state.value
            assertFalse(state.isSaving)
            assertEquals("Save failed: disk full", state.message)
            assertEquals(0, repository.saveCalls)
        }
    }

    private fun recorder(
        telemetry: FakeTelemetryLifecycle,
        repository: FakeTrackMapRepository,
    ): TrackMapRecorder = TrackMapRecorder(
        telemetry = telemetry,
        repository = repository,
        calibrationRepository = FakeTrackCalibrationRepository(),
        gameSettings = object : TelemetryGameSettings {
            override fun observeSelection(): Flow<GameSelection> = flowOf(GameSelection.Manual(GameId.ACE))

            override suspend fun currentSelection(): GameSelection = GameSelection.Manual(GameId.ACE)
        },
        defaultDispatcher = Dispatchers.Unconfined,
    )

    private fun frame(index: Int, x: Float): TelemetryFrame = TelemetryFrame(
        session = SessionFrame(
            status = SimStatus.LIVE,
            track = TrackInfo(
                trackId = "spa_gp",
                trackName = "Spa GP",
                sectorCount = 1,
            ),
        ),
        lap = LapFrame(
            currentLapIndex = 1,
            sectorCount = 1,
            currentSectorIndex = 0,
        ),
        car = CarFrame(
            speedKmh = 120f,
            worldPosition = Vec3(x, 0f, 1f),
            velocity = Vec3(1f, 0f, 0f),
        ),
        timestampNs = index.toLong() + 1L,
    )

    private class FakeTelemetryLifecycle : TelemetryLifecycle {

        private val mutableFrames = MutableSharedFlow<TelemetryFrame>(extraBufferCapacity = 64)

        override val frames: SharedFlow<TelemetryFrame> = mutableFrames.asSharedFlow()
        override val events: Flow<TelemetryLifecycleEvent> = emptyFlow()

        override suspend fun finishTelemetry() = Unit

        override suspend fun launchTelemetry() = Unit

        suspend fun emit(frame: TelemetryFrame) {
            mutableFrames.emit(frame)
        }
    }

    private class FakeTrackMapRepository(
        private val saveError: Throwable? = null,
    ) : TrackMapRepository {

        var saveCalls: Int = 0
            private set

        override suspend fun save(trackMap: TrackMap) {
            saveError?.let { throw it }
            saveCalls += 1
        }

        override suspend fun load(gameId: String, trackId: String, layoutId: String?): TrackMap? = null

        override suspend fun loadAll(gameId: String?): List<TrackMap> = emptyList()
    }

    private class FakeTrackCalibrationRepository : TrackCalibrationRepository {

        override suspend fun save(calibration: TrackCalibration, source: TrackCalibrationSource) = Unit

        override suspend fun load(trackId: String, layoutId: String?): TrackCalibration? = null

        override suspend fun loadBySource(
            trackId: String,
            source: TrackCalibrationSource,
            layoutId: String?,
        ): TrackCalibration? = null

        override suspend fun loadAll(source: TrackCalibrationSource?): List<TrackCalibration> = emptyList()
    }
}
