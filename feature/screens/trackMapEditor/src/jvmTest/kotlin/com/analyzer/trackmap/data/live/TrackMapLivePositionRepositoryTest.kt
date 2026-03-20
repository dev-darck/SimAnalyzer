package com.analyzer.trackmap.data.live

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.math.Vec2
import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import com.project.analyzer.telemetry.api.contract.SimStatus
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class TrackMapLivePositionRepositoryTest {

    @Test
    fun `observe reanchors from initial fallback point to live telemetry position`() = runBlocking {
        val telemetry = FakeTelemetryLifecycle()
        val repository = TrackMapLivePositionRepositoryImpl(
            telemetryFrames = telemetry,
            defaultDispatcher = Dispatchers.Default,
        )
        val positions = CopyOnWriteArrayList<Vec2?>()

        val collectJob = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.observe(sampleItem()).collect { position ->
                positions += position
            }
        }

        awaitLastPosition(
            positions = positions,
            expected = null,
        )

        telemetry.emit(
            frame(
                positionX = null,
                speedKmh = 0f,
            ),
        )
        awaitLastPosition(
            positions = positions,
            expected = Vec2(0f, 0f),
        )

        telemetry.emit(
            frame(
                positionX = 80f,
                speedKmh = 120f,
            ),
        )
        awaitLastPosition(
            positions = positions,
            expected = Vec2(80f, 0f),
        )

        collectJob.cancelAndJoin()
    }

    private fun sampleItem(): TrackMapLibraryItem {
        val points = listOf(Vec2(0f, 0f), Vec2(100f, 0f))
        return TrackMapLibraryItem(
            map = TrackMap(
                gameId = "ac",
                trackId = "monza",
                trackName = "Monza",
                createdAtEpochMs = 1L,
                referencePoint = ReferencePoint.FRONT_AXLE,
                points = points.map(TrackMapPoint::from),
                bounds = TrackMapBounds(minX = 0f, minY = 0f, maxX = 100f, maxY = 0f),
            ),
            points = points,
            leftWidthsMeters = listOf(5f, 5f),
            rightWidthsMeters = listOf(5f, 5f),
            averageTrackWidthMeters = 10f,
            pitPoints = emptyList(),
            bounds = TrackMapBounds(minX = 0f, minY = 0f, maxX = 100f, maxY = 0f),
            distanceMeters = 100f,
            pitEntryPoint = null,
            pitExitPoint = null,
        )
    }

    private fun frame(positionX: Float?, speedKmh: Float): TelemetryFrame =
        TelemetryFrame(
            session = SessionFrame(
                status = SimStatus.LIVE,
                track = TrackInfo(
                    trackId = "monza",
                    trackName = "Monza",
                    normalizedLapPosition = 0f,
                ),
            ),
            car = CarFrame(
                speedKmh = speedKmh,
                worldPosition = positionX?.let { x -> Vec3(x, 0f, 0f) },
            ),
        )

    private suspend fun awaitLastPosition(
        positions: CopyOnWriteArrayList<Vec2?>,
        expected: Vec2?,
    ) {
        withTimeout(2.seconds) {
            while (positions.lastOrNull() != expected) {
                delay(20.milliseconds)
            }
        }
    }

    private class FakeTelemetryLifecycle : TelemetryLifecycle {

        private val mutableFrames = MutableSharedFlow<TelemetryFrame>(extraBufferCapacity = 16)

        override val frames: SharedFlow<TelemetryFrame> = mutableFrames.asSharedFlow()
        override val events: Flow<TelemetryLifecycleEvent> = emptyFlow()

        override suspend fun finishTelemetry() = Unit

        override suspend fun launchTelemetry() = Unit

        suspend fun emit(frame: TelemetryFrame) {
            mutableFrames.emit(frame)
        }
    }
}
