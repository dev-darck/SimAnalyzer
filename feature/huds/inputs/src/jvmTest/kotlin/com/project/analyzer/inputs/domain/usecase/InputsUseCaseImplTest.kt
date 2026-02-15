package com.project.analyzer.inputs.domain.usecase

import com.project.analyzer.inputs.domain.model.InputsResult
import com.project.analyzer.inputs.settings.InputHudSettings
import com.project.analyzer.inputs.settings.repository.InputHudSettingsRepository
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.ControlsFrame
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InputsUseCaseImplTest {

    @Test
    fun emitsSampleFromTelemetryControls() = runBlocking {
        val telemetry = FakeTelemetry()
        val repo = FakeSettingsRepo()
        val useCase = InputsUseCaseImpl(telemetry = telemetry, settingsRepo = repo)

        val deferred = async {
            useCase.results.filterIsInstance<InputsResult.Sample>().first()
        }

        telemetry.frames.emit(
            TelemetryFrame(
                timestampNs = 123_000_000L,
                car = CarFrame(
                    controls = ControlsFrame(
                        throttle = 0.7f,
                        brake = 0.2f,
                        clutch = 0.1f,
                        steerAngle = 0.3f
                    )
                )
            )
        )

        val sample = deferred.await()
        assertIs<InputsResult.Sample>(sample)

        assertEquals(0.7f, sample.throttle)
        assertEquals(0.2f, sample.brake)
        assertEquals(0.1f, sample.clutch)
        assertEquals(0.3f, sample.steerRadians)
    }

    @Test
    fun emitsZeroSampleWhenControlsMissing() = runBlocking {
        val telemetry = FakeTelemetry()
        val repo = FakeSettingsRepo()
        val useCase = InputsUseCaseImpl(telemetry = telemetry, settingsRepo = repo)

        val deferred = async {
            useCase.results.filterIsInstance<InputsResult.Sample>().first()
        }

        telemetry.frames.emit(TelemetryFrame(timestampNs = 1L))

        val sample = deferred.await()
        assertIs<InputsResult.Sample>(sample)

        assertEquals(0f, sample.throttle)
        assertEquals(0f, sample.brake)
        assertEquals(0f, sample.clutch)
        assertEquals(0f, sample.steerRadians)
    }

    @Test
    fun mapsLifecycleEvents() = runBlocking {
        val telemetry = FakeTelemetry()
        val repo = FakeSettingsRepo()
        val useCase = InputsUseCaseImpl(telemetry = telemetry, settingsRepo = repo)

        run {
            val deferred = async {
                useCase.results.filterIsInstance<InputsResult.SessionStarted>().first()
            }
            telemetry.events.emit(
                TelemetryLifecycleEvent.SessionStarted(
                    session = SessionInfo(
                        sessionId = 42L,
                        sessionType = SessionType.PRACTICE,
                        carModel = "car",
                        trackId = "track"
                    )
                )
            )
            val r = deferred.await()
            assertEquals(42L, r.sessionId)
        }

        run {
            val deferred = async {
                useCase.results.filterIsInstance<InputsResult.SessionPaused>().first()
            }
            telemetry.events.emit(TelemetryLifecycleEvent.SessionPaused(sessionId = 42L))
            val r = deferred.await()
            assertEquals(42L, r.sessionId)
        }

        run {
            val deferred = async {
                useCase.results.filterIsInstance<InputsResult.SessionResumed>().first()
            }
            telemetry.events.emit(TelemetryLifecycleEvent.SessionResumed(sessionId = 42L))
            val r = deferred.await()
            assertEquals(42L, r.sessionId)
        }

        run {
            val deferred = async {
                useCase.results.filterIsInstance<InputsResult.SessionEnded>().first()
            }
            telemetry.events.emit(TelemetryLifecycleEvent.SessionEnded(sessionId = 42L))
            val r = deferred.await()
            assertEquals(42L, r.sessionId)
        }
    }

    private class FakeTelemetry : TelemetryLifecycle {

        override val frames = MutableSharedFlow<TelemetryFrame>(
            replay = 1,
            extraBufferCapacity = 16
        )
        override val events = MutableSharedFlow<TelemetryLifecycleEvent>(
            replay = 1,
            extraBufferCapacity = 16
        )

        override suspend fun finishTelemetry() = Unit
        override suspend fun launchTelemetry() = Unit
    }
    private class FakeSettingsRepo : InputHudSettingsRepository {
        override val data: Flow<InputHudSettings> = MutableStateFlow(InputHudSettings())
        override suspend fun update(inputHudSettings: InputHudSettings) = Unit
    }

    private suspend fun Flow<TelemetryLifecycleEvent>.emit(ev: TelemetryLifecycleEvent) {
        (this as? MutableSharedFlow<TelemetryLifecycleEvent>)
            ?.emit(ev)
            ?: error("FakeTelemetry.events must be MutableSharedFlow in tests")
    }
}
