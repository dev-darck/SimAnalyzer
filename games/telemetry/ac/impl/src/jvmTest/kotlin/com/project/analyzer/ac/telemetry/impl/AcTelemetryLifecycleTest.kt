package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.AcMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ace.AceMapper
import com.project.analyzer.ac.telemetry.impl.internal.poll.AcPollPipeline
import com.project.analyzer.ac.telemetry.impl.internal.poll.DataSourceType
import com.project.analyzer.ac.telemetry.impl.internal.poll.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.poll.PollResult
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcLegacyRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.ac.telemetry.impl.recording.AcTelemetryRecordingEmitter
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AcTelemetryLifecycleTest {

    @Test
    fun `launchTelemetry routes legacy snapshots through AcMapper`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val acMapper = mockk<AcMapper>()
        val aceMapper = mockk<AceMapper>()
        val recording = mockk<AcTelemetryRecordingEmitter>(relaxed = true)
        val pipeline = mockk<AcPollPipeline>()
        val channel = Channel<PollResult>(Channel.UNLIMITED)
        val snapshot = AcLegacyRawSnapshot(SPageFilePhysics(), SPageFileGraphics(), SPageFileStatic())
        val frame = TelemetryFrame(frameId = 1L)

        every { pipeline.start(any()) } returns channel
        coEvery { pipeline.stop() } returns Unit
        every { pipeline.release(any()) } just runs
        coEvery { acMapper.map(snapshot) } returns frame

        val lifecycle = AcTelemetryLifecycle(
            ioDispatcher = dispatcher,
            acMapper = acMapper,
            aceMapper = aceMapper,
            recordingEmitter = recording,
            pollPipeline = pipeline,
        )

        lifecycle.launchTelemetry()
        channel.send(PollResult.StateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE))
        channel.send(PollResult.Frame(snapshot))
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { acMapper.map(snapshot) }
        coVerify(exactly = 0) { aceMapper.map(any()) }
        verify(exactly = 1) { pipeline.release(snapshot) }
    }

    @Test
    fun `launchTelemetry routes ACE snapshots through AceMapper`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val acMapper = mockk<AcMapper>()
        val aceMapper = mockk<AceMapper>()
        val recording = mockk<AcTelemetryRecordingEmitter>(relaxed = true)
        val pipeline = mockk<AcPollPipeline>()
        val channel = Channel<PollResult>(Channel.UNLIMITED)
        val snapshot = AceRawSnapshot()
        val frame = TelemetryFrame(frameId = 2L)

        every { pipeline.start(any()) } returns channel
        coEvery { pipeline.stop() } returns Unit
        every { pipeline.release(any()) } just runs
        coEvery { aceMapper.map(snapshot) } returns frame

        val lifecycle = AcTelemetryLifecycle(
            ioDispatcher = dispatcher,
            acMapper = acMapper,
            aceMapper = aceMapper,
            recordingEmitter = recording,
            pollPipeline = pipeline,
        )

        lifecycle.launchTelemetry()
        channel.send(PollResult.StateChanged(GameConnectionState.IN_SESSION, DataSourceType.NATIVE))
        channel.send(PollResult.Frame(snapshot))
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) { aceMapper.map(snapshot) }
        coVerify(exactly = 0) { acMapper.map(any()) }
        verify(exactly = 1) { pipeline.release(snapshot) }
    }
}
