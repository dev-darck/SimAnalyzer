package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.internal.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.AcRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.PollResult
import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcMapper
import com.project.analyzer.ac.telemetry.impl.recording.AcTelemetryRecordingEmitter
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionPauseReason
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AcTelemetryLifecycleTest {

    @Test
    fun `DISCONNECTED -to- IN_SESSION emits SimConnected then SessionStarted then LapStarted`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val pollLoop = mockk<AcPollLoop>()
        val fallback = mockk<AcEvoFallbackShmPatcher>(relaxed = true)
        val mapper = mockk<AcMapper>()
        val sampleEmitter = mockk<AcTelemetryRecordingEmitter>(relaxed = true)

        val frame1 = mockk<TelemetryFrame>(relaxed = true)
        val lap1 = mockk<LapFrame>(relaxed = true) {
            every { currentLapIndex } returns 1
            every { validity } returns LapValidity.VALID
        }
        val session1 = mockk<SessionFrame>(relaxed = true) {
            every { sessionType } returns SessionType.PRACTICE
            every { car?.carModel } returns "carA"
            every { track?.trackId } returns "spa_gp"
        }
        every { frame1.lap } returns lap1
        every { frame1.session } returns session1

        coEvery { pollLoop.start(any()) } coAnswers {
            val cb = arg<(PollResult) -> Unit>(0)
            cb(PollResult.StateChanged(state = GameConnectionState.IN_SESSION, dataSource = mockk(relaxed = true)))
            cb(PollResult.Frame(snapshot = newSnapshot()))
        }

        coEvery { mapper.map(any()) } returns frame1

        val lifecycle = AcTelemetryLifecycle(
            pollLoop = pollLoop,
            fallback = fallback,
            mapper = mapper,
            recordingEmitter = sampleEmitter,
            ioDispatcher = dispatcher
        )

        val events = mutableListOf<TelemetryLifecycleEvent>()
        val frames = mutableListOf<TelemetryFrame>()

        val evJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            lifecycle.events.take(3).toList(events)
        }
        val frJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            lifecycle.frames.take(1).toList(frames)
        }

        lifecycle.launchTelemetry()
        testScheduler.advanceUntilIdle()

        evJob.join()
        frJob.join()

        assertTrue(events[0] is TelemetryLifecycleEvent.SimConnected)
        assertTrue(events[1] is TelemetryLifecycleEvent.SessionStarted)
        assertEquals(1, (events[2] as TelemetryLifecycleEvent.LapStarted).lapNumber)

        assertEquals(1, frames.size)
        assertEquals(frame1, frames[0])
    }

    @Test
    fun `lap index increment emits LapFinished with previous validity`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val pollLoop = mockk<AcPollLoop>()
        val fallback = mockk<AcEvoFallbackShmPatcher>(relaxed = true)
        val mapper = mockk<AcMapper>()
        val sampleEmitter = mockk<AcTelemetryRecordingEmitter>(relaxed = true)

        val frame1 = mockk<TelemetryFrame>(relaxed = true)
        val lap1 = mockk<LapFrame>(relaxed = true) {
            every { currentLapIndex } returns 1
            every { validity } returns LapValidity.VALID
        }
        val session = mockk<SessionFrame>(relaxed = true) {
            every { sessionType } returns SessionType.PRACTICE
            every { car?.carModel } returns "carA"
            every { track?.trackId } returns "spa_gp"
        }
        every { frame1.lap } returns lap1
        every { frame1.session } returns session

        val frame2 = mockk<TelemetryFrame>(relaxed = true)
        val lap2 = mockk<LapFrame>(relaxed = true) {
            every { currentLapIndex } returns 2
            every { validity } returns LapValidity.INVALID
        }
        every { frame2.lap } returns lap2
        every { frame2.session } returns session

        coEvery { pollLoop.start(any()) } coAnswers {
            val cb = arg<(PollResult) -> Unit>(0)
            cb(PollResult.StateChanged(GameConnectionState.IN_SESSION, mockk(relaxed = true)))
            cb(PollResult.Frame(snapshot = newSnapshot()))
            cb(PollResult.Frame(snapshot = newSnapshot()))
        }

        coEvery { mapper.map(any()) } returnsMany listOf(frame1, frame2)

        val lifecycle = AcTelemetryLifecycle(
            pollLoop = pollLoop,
            fallback = fallback,
            mapper = mapper,
            recordingEmitter = sampleEmitter,
            ioDispatcher = dispatcher
        )

        val events = mutableListOf<TelemetryLifecycleEvent>()
        val evJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            lifecycle.events.take(5).toList(events)
        }

        lifecycle.launchTelemetry()
        testScheduler.advanceUntilIdle()
        evJob.join()

        assertTrue(events[0] is TelemetryLifecycleEvent.SimConnected)
        assertTrue(events[1] is TelemetryLifecycleEvent.SessionStarted)

        val started = events.filterIsInstance<TelemetryLifecycleEvent.LapStarted>()
        assertEquals(2, started.size)
        assertEquals(1, started[0].lapNumber)

        val finished = events.filterIsInstance<TelemetryLifecycleEvent.LapFinished>().single()
        assertEquals(1, finished.lapNumber)
        assertEquals(LapValidity.VALID, finished.validity)
        assertEquals(2, started[1].lapNumber)
    }

    @Test
    fun `IN_SESSION -to- IN_MENU emits SessionPaused when running`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val pollLoop = mockk<AcPollLoop>()
        val fallback = mockk<AcEvoFallbackShmPatcher>(relaxed = true)
        val mapper = mockk<AcMapper>()
        val sampleEmitter = mockk<AcTelemetryRecordingEmitter>(relaxed = true)

        val frame = mockk<TelemetryFrame>(relaxed = true)
        val lap = mockk<LapFrame>(relaxed = true) { every { currentLapIndex } returns 1 }
        val session = mockk<SessionFrame>(relaxed = true) {
            every { sessionType } returns SessionType.PRACTICE
            every { car?.carModel } returns "carA"
            every { track?.trackId } returns "spa_gp"
        }
        every { frame.lap } returns lap
        every { frame.session } returns session

        coEvery { pollLoop.start(any()) } coAnswers {
            val cb = arg<(PollResult) -> Unit>(0)
            cb(PollResult.StateChanged(GameConnectionState.IN_SESSION, mockk(relaxed = true)))
            cb(PollResult.Frame(snapshot = newSnapshot()))
            cb(PollResult.StateChanged(GameConnectionState.IN_MENU, mockk(relaxed = true)))
        }

        coEvery { mapper.map(any()) } returns frame

        val lifecycle = AcTelemetryLifecycle(
            pollLoop = pollLoop,
            fallback = fallback,
            mapper = mapper,
            recordingEmitter = sampleEmitter,
            ioDispatcher = dispatcher
        )

        val events = mutableListOf<TelemetryLifecycleEvent>()
        val evJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            lifecycle.events.take(4).toList(events)
        }

        lifecycle.launchTelemetry()
        testScheduler.advanceUntilIdle()
        evJob.join()

        val paused = events.last() as TelemetryLifecycleEvent.SessionPaused
        assertEquals(SessionPauseReason.NOT_IN_SESSION, paused.reason)
        assertTrue(paused.sessionId > 0L)
    }

    @Test
    fun `disconnect ends session and emits SimDisconnected`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val pollLoop = mockk<AcPollLoop>(relaxUnitFun = true)
        val fallback = mockk<AcEvoFallbackShmPatcher>(relaxed = true)
        val mapper = mockk<AcMapper>()
        val sampleEmitter = mockk<AcTelemetryRecordingEmitter>(relaxed = true)

        val frame = mockk<TelemetryFrame>(relaxed = true)
        val lap = mockk<LapFrame>(relaxed = true) { every { currentLapIndex } returns 1 }
        val session = mockk<SessionFrame>(relaxed = true) {
            every { sessionType } returns SessionType.RACE
            every { car?.carModel } returns "carA"
            every { track?.trackId } returns "spa_gp"
        }
        every { frame.lap } returns lap
        every { frame.session } returns session

        coEvery { pollLoop.start(any()) } coAnswers {
            val cb = arg<(PollResult) -> Unit>(0)
            cb(PollResult.StateChanged(GameConnectionState.IN_SESSION, mockk(relaxed = true)))
            cb(PollResult.Frame(snapshot = newSnapshot()))
            cb(PollResult.StateChanged(GameConnectionState.DISCONNECTED, mockk(relaxed = true)))
        }

        coEvery { mapper.map(any()) } returns frame

        val lifecycle = AcTelemetryLifecycle(
            pollLoop = pollLoop,
            fallback = fallback,
            mapper = mapper,
            recordingEmitter = sampleEmitter,
            ioDispatcher = dispatcher
        )

        val events = mutableListOf<TelemetryLifecycleEvent>()
        val evJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            lifecycle.events.take(10).toList(events)
        }

        try {
            lifecycle.launchTelemetry()

            withTimeout(1_000) {
                while (events.none { it is TelemetryLifecycleEvent.SimDisconnected }) {
                    testScheduler.advanceUntilIdle()
                }
            }
        } finally {
            lifecycle.finishTelemetry()
        }

        evJob.cancel()

        val ended = events.filterIsInstance<TelemetryLifecycleEvent.SessionEnded>().last()
        assertEquals(SessionEndReason.SIM_DISCONNECTED, ended.reason)

        assertTrue(events.last { true } is TelemetryLifecycleEvent.SimDisconnected)
    }

    private fun newSnapshot(): AcRawSnapshot =
        AcRawSnapshot(
            physics = SPageFilePhysics(),
            graphics = SPageFileGraphics(),
            statics = SPageFileStatic(),
            timestampNs = 1L
        )
}
