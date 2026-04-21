package com.project.analyzer.telemetry.impl

import com.project.analyzer.game.api.GameDetectorFactory
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.game.api.GameWindowDetector
import com.project.analyzer.game.api.GameWindowInfo
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryGameSettings
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TelemetryLifecycleRouterTest {

    @Test
    fun `late subscriber receives session bootstrap before replayed lap event`() = runBlocking {
        val gameSettings = FakeTelemetryGameSettings(GameSelection.Manual(GameId.ACE))
        val source = FakeTelemetryLifecycle()
        val router = TelemetryLifecycleRouter(
            gameSettings = gameSettings,
            gameLifecycles = mapOf(GameId.ACE.id to lazy<TelemetryLifecycle> { source }),
            ioDispatcher = kotlinx.coroutines.Dispatchers.Default,
            gameDetectorFactory = FakeGameDetectorFactory(),
        )

        router.launchTelemetry()
        delay(50)

        source.events.emit(
            TelemetryLifecycleEvent.SessionStarted(
                session = SessionInfo(
                    sessionId = 7L,
                    sessionType = SessionType.TIME_ATTACK,
                    carModel = "Mercedes-AMG GT2",
                    trackId = "brands_hatch_indy",
                    carId = 123,
                ),
            ),
        )
        delay(20)
        source.events.emit(TelemetryLifecycleEvent.LapStarted(lapNumber = 1))
        delay(20)

        val collected = mutableListOf<TelemetryLifecycleEvent>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            router.events
                .take(2)
                .toList(collected)
        }

        delay(50)
        job.cancel()

        assertEquals(2, collected.size)
        val sessionStarted = assertIs<TelemetryLifecycleEvent.SessionStarted>(collected[0])
        assertEquals(7L, sessionStarted.session.sessionId)
        val lapStarted = assertIs<TelemetryLifecycleEvent.LapStarted>(collected[1])
        assertEquals(1, lapStarted.lapNumber)

        router.finishTelemetry()
    }

    @Test
    fun `cleanup does not duplicate disconnect events emitted by source finish`() = runBlocking {
        val gameSettings = FakeTelemetryGameSettings(GameSelection.Manual(GameId.LMU))
        val source = FakeTelemetryLifecycle(
            onFinish = {
                events.emit(TelemetryLifecycleEvent.SessionEnded(7L, SessionEndReason.SIM_DISCONNECTED))
                events.emit(TelemetryLifecycleEvent.SimDisconnected)
                delay(20)
            },
        )
        val router = TelemetryLifecycleRouter(
            gameSettings = gameSettings,
            gameLifecycles = mapOf(GameId.LMU.id to lazy<TelemetryLifecycle> { source }),
            ioDispatcher = kotlinx.coroutines.Dispatchers.Default,
            gameDetectorFactory = FakeGameDetectorFactory(),
        )
        val collected = mutableListOf<TelemetryLifecycleEvent>()
        val collector = launch {
            router.events.toList(collected)
        }

        router.launchTelemetry()
        delay(50)

        source.events.emit(
            TelemetryLifecycleEvent.SessionStarted(
                session = SessionInfo(
                    sessionId = 7L,
                    sessionType = SessionType.RACE,
                    carModel = "Oreca 07",
                    trackId = "le_mans",
                    carId = 24,
                ),
            ),
        )
        withTimeout(1_000) {
            while (collected.none { it is TelemetryLifecycleEvent.SessionStarted }) {
                delay(10)
            }
        }

        router.finishTelemetry()
        delay(50)
        collector.cancel()

        assertEquals(
            expected = 1,
            actual = collected.count { event ->
                event is TelemetryLifecycleEvent.SessionEnded &&
                    event.sessionId == 7L &&
                    event.reason == SessionEndReason.SIM_DISCONNECTED
            },
        )
        assertEquals(
            expected = 1,
            actual = collected.count { it == TelemetryLifecycleEvent.SimDisconnected },
        )
    }

    private class FakeTelemetryGameSettings(
        initialSelection: GameSelection,
    ) : TelemetryGameSettings {
        private val selection = MutableStateFlow(initialSelection)

        override fun observeSelection(): Flow<GameSelection> = selection

        override suspend fun currentSelection(): GameSelection = selection.value
    }

    private class FakeTelemetryLifecycle(
        private val onFinish: suspend FakeTelemetryLifecycle.() -> Unit = {},
    ) : TelemetryLifecycle {
        override val events = MutableSharedFlow<TelemetryLifecycleEvent>(replay = 1, extraBufferCapacity = 16)
        override val frames = MutableSharedFlow<TelemetryFrame>(replay = 1, extraBufferCapacity = 16)

        override suspend fun launchTelemetry() = Unit

        override suspend fun finishTelemetry() {
            onFinish()
        }
    }

    private class FakeGameDetectorFactory : GameDetectorFactory {
        override fun create(
            requireForeground: Boolean,
            coroutineDispatcher: kotlinx.coroutines.CoroutineDispatcher,
        ): GameWindowDetector = Proxy.newProxyInstance(
            GameWindowDetector::class.java.classLoader,
            arrayOf(GameWindowDetector::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "observeGameWindow" -> emptyFlow<GameWindowInfo?>()
                "setOverlayHwnd" -> Unit
                else -> error("Unexpected GameWindowDetector method: ${method.name}")
            }
        } as GameWindowDetector
    }
}
