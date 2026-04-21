package com.project.analyzer.ac.telemetry.impl.internal.mapper.ac

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.internal.AcLapState
import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.AcBaseFrameMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.AcSessionCache
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcLegacyRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.damage.DamageFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelsFrame
import com.project.analyzer.telemetry.api.model.environment.EnvironmentFrame
import com.project.analyzer.utils.shm.writeWString
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AcMapperTest {

    @Test
    fun `map uses legacy snapshot pages directly`() = runTest {
        val cache = AcSessionCache()
        val baseFrameMapper = AcBaseFrameMapper(
            cache = cache,
            sessionMapper = SessionMapper(cache),
            lapMapper = LapMapper(cache, AcLapState(cache)),
            carMapper = mockk<CarMapper> { every { map(any(), any(), any()) } returns CarFrame() },
            wheelsMapper = mockk<WheelsMapper> { every { map(any(), any()) } returns WheelsFrame() },
            damageMapper = mockk<DamageMapper> { every { map(any()) } returns DamageFrame() },
            environmentMapper = mockk<EnvironmentMapper> { every { map(any(), any()) } returns EnvironmentFrame() },
            lapAnalyzer = mockk<FallbackLapAnalyzer>(relaxUnitFun = true) {
                every { loadCalibration(any()) } returns null
            },
        )
        val mapper = AcMapper(baseFrameMapper)

        val snapshot = AcLegacyRawSnapshot(
            physics = SPageFilePhysics(),
            graphics = SPageFileGraphics(),
            statics = SPageFileStatic(),
            timestampNs = 123L,
        ).apply {
            frameId = 7L
            statics.track.writeWString("Monza")
            statics.trackConfiguration.writeWString("GP")
            statics.sectorCount = 3
            graphics.completedLaps = 2
            graphics.iCurrentTime = 83_000
        }

        val frame = mapper.map(snapshot)
        assertEquals(7L, frame.frameId)
        assertEquals(123L, frame.timestampNs)
        assertEquals(83_000, frame.lap?.currentLapTimeMs)
    }
}
