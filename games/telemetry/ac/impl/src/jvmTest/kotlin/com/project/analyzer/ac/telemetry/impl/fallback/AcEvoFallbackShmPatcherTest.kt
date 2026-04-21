package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackFuelAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.EvoFileInfoSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.internal.poll.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcSessionRestartHint
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.utils.shm.writeWString
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class AcEvoFallbackShmPatcherTest {

    @Test
    fun `patchIfNeeded applies logfile identity to fallback patch memory`() {
        val extractor = mockk<EvoFileInfoSource> {
            every { poll() } returns EvoFileInfo(
                sessionEpoch = 1L,
                trackId = "fallback_track",
                carModel = "fallback_car",
            )
        }
        val patcher = AcEvoFallbackShmPatcher(
            fileInfoExtractor = extractor,
            lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxed = true),
            fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxed = true),
        )
        val memory = TestFallbackPatchMemory()

        val hint = patcher.patchIfNeeded(
            shm = memory,
            loopStartNanos = 1L,
            gameState = GameConnectionState.IN_MENU,
            applyShmPatch = true,
        )

        assertEquals(AcSessionRestartHint.NONE, hint)
        assertEquals("fallback_track", read(memory.statics.track))
        assertEquals("fallback_car", read(memory.statics.carModel))
    }

    @Test
    fun `patchIfNeeded returns NONE when logfile has no signal`() {
        val extractor = mockk<EvoFileInfoSource> {
            every { poll() } returns EvoFileInfo()
        }
        val patcher = AcEvoFallbackShmPatcher(
            fileInfoExtractor = extractor,
            lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxed = true),
            fuelAnalyzer = mockk<FallbackFuelAnalyzer>(relaxed = true),
        )
        val memory = TestFallbackPatchMemory().apply {
            statics.track.writeWString("existing_track")
        }

        val hint = patcher.patchIfNeeded(
            shm = memory,
            loopStartNanos = 1L,
            gameState = GameConnectionState.IN_MENU,
            applyShmPatch = true,
        )

        assertEquals(AcSessionRestartHint.NONE, hint)
        assertEquals("existing_track", read(memory.statics.track))
    }

    private fun read(chars: CharArray): String {
        val end = chars.indexOfFirst { it == '\u0000' }.let { if (it < 0) chars.size else it }
        return chars.concatToString(0, end)
    }

    private class TestFallbackPatchMemory : AcFallbackPatchMemory {
        override val physics: SPageFilePhysics = SPageFilePhysics()
        override val graphics: SPageFileGraphics = SPageFileGraphics()
        override val statics: SPageFileStatic = SPageFileStatic()
    }
}
