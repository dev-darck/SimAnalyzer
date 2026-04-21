package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.poll.AcPollConfig
import com.project.analyzer.ac.telemetry.impl.internal.poll.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.poll.DataSourceType
import com.project.analyzer.ac.telemetry.impl.internal.poll.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.poll.PollResult
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcLegacyRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.ac.AcLegacySharedMemoryView
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.AcUnknownSharedMemoryView
import com.project.analyzer.ac.telemetry.impl.shm.ace.AceSharedMemoryView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStatus
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.utils.shm.writeWString
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AcPollLoopTest {

    @Test
    fun `detectGameState returns DISCONNECTED when no active shared memory view exists`() {
        val shm = mockk<AcSharedMemory>()
        val cfg = mockk<AcPollConfig>(relaxed = true)

        every { shm.view } returns AcUnknownSharedMemoryView
        every { shm.layout } returns AcUnknownSharedMemoryView.layout
        every { shm.readAll() } just runs
        every { shm.close() } just runs

        val loop = AcPollLoop(shm, cfg)

        val detection = invokeDetect(loop)
        assertEquals(GameConnectionState.DISCONNECTED, getField(detection, "state"))
        assertEquals(DataSourceType.NATIVE, getField(detection, "dataSource"))
    }

    @Test
    fun `detectGameState uses legacy backend when legacy view is active`() {
        val shm = mockk<AcSharedMemory>()
        val cfg = mockk<AcPollConfig>(relaxed = true)
        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()

        graphics.packetId = 10
        graphics.status = 0
        graphics.iCurrentTime = 1
        statics.smVersion[0] = '1'
        statics.track.writeWString("monza")
        statics.carModel.writeWString("car")
        statics.numCars = 1
        statics.numberOfSessions = 1
        statics.sectorCount = 3

        every { shm.view } returns AcLegacySharedMemoryView(physics, graphics, statics)
        every { shm.layout } returns com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout.LEGACY
        every { shm.readAll() } just runs
        every { shm.close() } just runs

        val loop = AcPollLoop(shm, cfg)
        val detection = invokeDetect(loop)

        assertEquals(GameConnectionState.IN_MENU, getField(detection, "state"))
        assertEquals(DataSourceType.NATIVE, getField(detection, "dataSource"))
        assertEquals(false, getField(detection, "needsFallback"))
    }

    @Test
    fun `start emits ACE snapshot when ACE shared memory view is active`() = runTest {
        val shm = mockk<AcSharedMemory>()
        val cfg = mockk<AcPollConfig>()
        val physics = SPageFilePhysics()
        val graphics = com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoGraphicsPageView()
        val statics = com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStaticPageView()

        physics.packetId = 1
        physics.rpm = 5000
        physics.speedKmh = 200f
        graphics.packetId = 1
        graphics.statusRaw = AcEvoStatus.LIVE.rawValue
        graphics.currentLapTimeMsRaw = 10
        graphics.activeCarsRaw = 20
        statics.smVersionRaw[0] = '1'.code.toByte()
        statics.trackRaw[0] = 'm'.code.toByte()
        statics.numberOfSessionsRaw = 1

        every { cfg.reconnectDelayMs } returns 1
        every { cfg.menuPollMs } returns 1
        every { cfg.pollIntervalNanos } returns 1_000_000L
        every { cfg.maxDriftNanos } returns Long.MAX_VALUE

        every { shm.view } returns AceSharedMemoryView(physics, graphics, statics)
        every { shm.layout } returns com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout.ACEVO
        every { shm.readAll() } answers {
            physics.packetId += 1
            graphics.packetId += 1
        }
        every { shm.close() } just runs

        val loop = AcPollLoop(shm, cfg)
        val results = mutableListOf<PollResult>()
        val job: Job = backgroundScope.launch {
            loop.start { result ->
                results += result
                if (results.count { it is PollResult.Frame } >= 1) {
                    throw kotlinx.coroutines.CancellationException("done")
                }
            }
        }

        try {
            job.join()
        } catch (_: Throwable) {
        }
        delay(10)

        val frame = results.filterIsInstance<PollResult.Frame>().first()
        assertIs<AceRawSnapshot>(frame.snapshot)
    }

    private fun invokeDetect(loop: AcPollLoop): Any {
        val method = AcPollLoop::class.java.getDeclaredMethod("detectGameState")
        method.isAccessible = true
        return method.invoke(loop)
    }

    private fun getField(obj: Any, name: String): Any? {
        val field = obj.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(obj)
    }
}
