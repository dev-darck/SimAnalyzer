package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.AcPollConfig
import com.project.analyzer.ac.telemetry.impl.internal.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.DataSourceType
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.PollResult
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.utils.shm.writeWString
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class AcPollLoopTest {

    private fun invokeDetect(loop: AcPollLoop): Any {
        val m = AcPollLoop::class.java.getDeclaredMethod("detectGameState")
        m.isAccessible = true
        return m.invoke(loop)
    }

    private fun getField(obj: Any, name: String): Any? {
        val f = obj.javaClass.getDeclaredField(name)
        f.isAccessible = true
        return f.get(obj)
    }

    private fun setField(obj: Any, name: String, value: Any?) {
        val f = obj.javaClass.getDeclaredField(name)
        f.isAccessible = true
        f.set(obj, value)
    }

    @Test
    fun `detectGameState returns DISCONNECTED when not attached`() {
        val shm = mockk<AcSharedMemory>()
        val cfg = mockk<AcPollConfig>(relaxed = true)

        every { shm.isAnyAttached() } returns false

        every { shm.physics } returns mockk(relaxed = true)
        every { shm.graphics } returns mockk(relaxed = true)
        every { shm.statics } returns mockk(relaxed = true)

        val loop = AcPollLoop(shm, cfg)

        val detection = invokeDetect(loop)
        assertEquals(GameConnectionState.DISCONNECTED, getField(detection, "state"))
        assertEquals(DataSourceType.NATIVE, getField(detection, "dataSource"))
    }

    @Test
    fun `detectGameState uses native graphics when status available`() {
        val shm = mockk<AcSharedMemory>()
        val cfg = mockk<AcPollConfig>(relaxed = true)

        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()

        every { shm.isAnyAttached() } returns true
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        graphics.packetId = 10
        graphics.status = 0
        graphics.completedLaps = 0
        graphics.iCurrentTime = 1
        statics.smVersion[0] = '1'
        statics.track.writeWString("monza")
        statics.carModel.writeWString("car")
        statics.numCars = 1
        statics.numberOfSessions = 1
        statics.sectorCount = 3

        val loop = AcPollLoop(shm, cfg)
        val detection = invokeDetect(loop)

        assertEquals(GameConnectionState.IN_MENU, getField(detection, "state"))
        assertEquals(DataSourceType.NATIVE, getField(detection, "dataSource"))
        assertEquals(false, getField(detection, "needsFallback"))
    }

    @Test
    fun `detectGameState fallback session becomes menu after stale threshold`() {
        val shm = mockk<AcSharedMemory>()
        val cfg = mockk<AcPollConfig>(relaxed = true)

        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()

        every { shm.isAnyAttached() } returns true
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        // no native graphics
        graphics.packetId = 0
        graphics.status = 0
        graphics.completedLaps = 0
        graphics.iCurrentTime = 0

        physics.rpm = 1000

        val loop = AcPollLoop(shm, cfg)

        var last: Any? = null
        var packetId = 100
        repeat(6) {
            physics.packetId = packetId++
            val detection = invokeDetect(loop)
            setField(loop, "currentState", getField(detection, "state"))
            last = detection
        }

        repeat(31) {
            val detection = invokeDetect(loop)
            setField(loop, "currentState", getField(detection, "state"))
            last = detection
        }

        val finalDetection = requireNotNull(last)
        assertEquals(GameConnectionState.IN_MENU, getField(finalDetection, "state"))
        assertEquals(DataSourceType.FALLBACK, getField(finalDetection, "dataSource"))
        assertEquals(true, getField(finalDetection, "needsFallback"))
    }

    @Test
    fun `start emits Frame in session only when packets change`() = runTest {
        val shm = mockk<AcSharedMemory>()
        val cfg = mockk<AcPollConfig>(relaxed = true)

        val physics = SPageFilePhysics()
        val graphics = SPageFileGraphics()
        val statics = SPageFileStatic()

        every { shm.isAnyAttached() } returns true
        every { shm.physics } returns physics
        every { shm.graphics } returns graphics
        every { shm.statics } returns statics

        graphics.packetId = 10
        graphics.status = 2
        graphics.completedLaps = 0
        graphics.iCurrentTime = 1

        var tick = 0
        val pIds = intArrayOf(1, 1, 2)
        val gIds = intArrayOf(1, 1, 1)

        coEvery { shm.readAll() } answers {
            val idx = tick.coerceAtMost(2)
            physics.packetId = pIds[idx]
            graphics.packetId = gIds[idx]
            tick++
        }

        every { cfg.pollIntervalNanos } returns 10_000_000L
        every { cfg.menuPollMs } returns 0L

        val loop = AcPollLoop(shm, cfg)

        val ch = Channel<PollResult>(capacity = Channel.UNLIMITED)
        val job = launch { loop.start { ch.trySend(it) } }

        val results = mutableListOf<PollResult>()

        withTimeout(1_000) {
            while (results.count { it is PollResult.Frame } < 2) {
                results += ch.receive()
            }
        }

        job.cancel()
        job.join()

        assertTrue(results.first() is PollResult.StateChanged)
        val frames = results.filterIsInstance<PollResult.Frame>()
        assertEquals(2, frames.size)
    }
}
