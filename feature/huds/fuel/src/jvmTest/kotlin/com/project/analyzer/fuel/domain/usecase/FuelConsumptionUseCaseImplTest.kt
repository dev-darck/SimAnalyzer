package com.project.analyzer.fuel.domain.usecase

import com.project.analyzer.fuel.data.model.SavedFuelData
import com.project.analyzer.fuel.domain.model.FuelResult
import com.project.analyzer.fuel.domain.predictor.FuelConsumptionEngine
import com.project.analyzer.fuel.domain.repository.FuelRepository
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.FuelFrame
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class FuelConsumptionUseCaseImplTest {

    @Test
    fun `session type replacement keeps fuel runtime for same track and car`() = runBlocking {
        val telemetry = FakeTelemetry()
        val repository = FakeFuelRepository()
        val useCase = FuelConsumptionUseCaseImpl(
            telemetry = telemetry,
            engine = FuelConsumptionEngine(),
            repository = repository,
        )
        val results = mutableListOf<FuelResult>()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            useCase.fuelEstimates.collect { results += it }
        }

        telemetry.emitEvent(TelemetryLifecycleEvent.SessionStarted(session(1L, SessionType.PRACTICE)))
        delay(10)
        telemetry.emitFrame(frame(timestampNs = 1_000_000_000L, fuelLiters = 50f, sessionType = SessionType.PRACTICE))
        delay(10)
        telemetry.emitEvent(
            TelemetryLifecycleEvent.SessionEnded(
                sessionId = 1L,
                reason = SessionEndReason.REPLACED_BY_NEW_SESSION,
            ),
        )
        delay(10)
        telemetry.emitEvent(TelemetryLifecycleEvent.SessionStarted(session(2L, SessionType.QUALIFYING)))
        delay(10)
        telemetry.emitFrame(
            frame(
                timestampNs = 2_000_000_000L,
                fuelLiters = 49.5f,
                sessionType = SessionType.QUALIFYING,
                currentLapIndex = 2,
                completedLaps = 1,
            ),
        )

        delay(50)
        job.cancel()

        assertEquals(1, results.filterIsInstance<FuelResult.Reset>().size)
        assertEquals(2, results.filterIsInstance<FuelResult.Data>().size)
        assertEquals(0, repository.updateCalls.size)
    }

    @Test
    fun `same type replacement resets fuel runtime`() = runBlocking {
        val telemetry = FakeTelemetry()
        val useCase = FuelConsumptionUseCaseImpl(
            telemetry = telemetry,
            engine = FuelConsumptionEngine(),
            repository = FakeFuelRepository(),
        )
        val results = mutableListOf<FuelResult>()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            useCase.fuelEstimates.collect { results += it }
        }

        telemetry.emitEvent(TelemetryLifecycleEvent.SessionStarted(session(1L, SessionType.PRACTICE)))
        delay(10)
        telemetry.emitFrame(frame(timestampNs = 1_000_000_000L, fuelLiters = 50f, sessionType = SessionType.PRACTICE))
        delay(10)
        telemetry.emitEvent(
            TelemetryLifecycleEvent.SessionEnded(
                sessionId = 1L,
                reason = SessionEndReason.REPLACED_BY_NEW_SESSION,
            ),
        )
        delay(10)
        telemetry.emitEvent(TelemetryLifecycleEvent.SessionStarted(session(2L, SessionType.PRACTICE)))
        delay(10)
        telemetry.emitFrame(frame(timestampNs = 2_000_000_000L, fuelLiters = 49.5f, sessionType = SessionType.PRACTICE))

        delay(50)
        job.cancel()

        assertEquals(2, results.filterIsInstance<FuelResult.Reset>().size)
    }

    @Test
    fun `main menu replacement still ends and resets fuel runtime`() = runBlocking {
        val telemetry = FakeTelemetry()
        val useCase = FuelConsumptionUseCaseImpl(
            telemetry = telemetry,
            engine = FuelConsumptionEngine(),
            repository = FakeFuelRepository(),
        )
        val results = mutableListOf<FuelResult>()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            useCase.fuelEstimates.collect { results += it }
        }

        telemetry.emitEvent(TelemetryLifecycleEvent.SessionStarted(session(1L, SessionType.RACE)))
        delay(10)
        telemetry.emitFrame(frame(timestampNs = 1_000_000_000L, fuelLiters = 50f, sessionType = SessionType.RACE))
        delay(10)
        telemetry.emitEvent(
            TelemetryLifecycleEvent.SessionEnded(
                sessionId = 1L,
                reason = SessionEndReason.REPLACED_AFTER_MAIN_MENU,
            ),
        )
        delay(10)
        telemetry.emitEvent(TelemetryLifecycleEvent.SessionStarted(session(2L, SessionType.RACE)))
        delay(10)
        telemetry.emitFrame(frame(timestampNs = 2_000_000_000L, fuelLiters = 49.5f, sessionType = SessionType.RACE))

        delay(50)
        job.cancel()

        assertEquals(2, results.filterIsInstance<FuelResult.Reset>().size)
        assertEquals(1, results.filterIsInstance<FuelResult.SessionEnded>().size)
    }

    @Test
    fun `paused frames do not emit no data`() = runBlocking {
        val telemetry = FakeTelemetry()
        val useCase = FuelConsumptionUseCaseImpl(
            telemetry = telemetry,
            engine = FuelConsumptionEngine(),
            repository = FakeFuelRepository(),
        )
        val results = mutableListOf<FuelResult>()

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            useCase.fuelEstimates.collect { results += it }
        }

        telemetry.emitEvent(TelemetryLifecycleEvent.SessionStarted(session(1L, SessionType.PRACTICE)))
        delay(10)
        telemetry.emitEvent(TelemetryLifecycleEvent.SessionPaused(sessionId = 1L))
        delay(10)
        telemetry.emitFrame(frame(timestampNs = 1_000_000_000L, fuelLiters = 50f, sessionType = SessionType.PRACTICE))

        delay(50)
        job.cancel()

        assertEquals(1, results.filterIsInstance<FuelResult.SessionPaused>().size)
        assertEquals(0, results.filterIsInstance<FuelResult.NoData>().size)
    }

    private fun session(sessionId: Long, sessionType: SessionType): SessionInfo = SessionInfo(
        sessionId = sessionId,
        sessionType = sessionType,
        carModel = "ks_bmw_m4_gt3",
        trackId = "brands_hatch_indy",
        carId = 1333049901,
    )

    private fun frame(
        timestampNs: Long,
        fuelLiters: Float,
        sessionType: SessionType,
        currentLapIndex: Int = 1,
        completedLaps: Int = 0,
    ): TelemetryFrame = TelemetryFrame(
        timestampNs = timestampNs,
        car = CarFrame(
            fuel = FuelFrame(
                fuelLiters = fuelLiters,
                maxFuelLiters = 110f,
            ),
            speedKmh = 120f,
        ),
        session = SessionFrame(
            sessionType = sessionType,
            completedLaps = completedLaps,
            car = CarInfo(
                carModel = "ks_bmw_m4_gt3",
                carId = 1333049901,
            ),
            track = TrackInfo(trackId = "brands_hatch_indy", lengthMeters = 1929f),
        ),
        lap = LapFrame(
            currentLapIndex = currentLapIndex,
            completedLaps = completedLaps,
        ),
    )

    private class FakeTelemetry : TelemetryLifecycle {

        override val frames = MutableSharedFlow<TelemetryFrame>(replay = 1, extraBufferCapacity = 16)
        override val events = MutableSharedFlow<TelemetryLifecycleEvent>(replay = 1, extraBufferCapacity = 16)

        override suspend fun finishTelemetry() = Unit
        override suspend fun launchTelemetry() = Unit

        suspend fun emitFrame(frame: TelemetryFrame) {
            frames.emit(frame)
        }

        suspend fun emitEvent(event: TelemetryLifecycleEvent) {
            events.emit(event)
        }
    }

    private class FakeFuelRepository : FuelRepository {

        val updateCalls = mutableListOf<String>()

        override suspend fun updateIfBetter(
            carId: Int,
            trackId: String,
            peakLitersPerLap: Double?,
            bestValidLapTimeMs: Int?,
        ) {
            updateCalls += "$carId|$trackId|$peakLitersPerLap|$bestValidLapTimeMs"
        }

        override suspend fun load(carId: Int, trackId: String): SavedFuelData? = null

        override suspend fun clear(carId: Int, trackId: String) = Unit
    }
}
