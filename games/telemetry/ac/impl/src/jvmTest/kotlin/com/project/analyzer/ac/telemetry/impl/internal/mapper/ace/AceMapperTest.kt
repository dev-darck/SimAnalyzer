package com.project.analyzer.ac.telemetry.impl.internal.mapper.ace

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.internal.AcLapState
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.CarMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.DamageMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.EnvironmentMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.LapMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.SessionMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.WheelsMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.AcBaseFrameMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.AcSessionCache
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.damage.DamageFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelsFrame
import com.project.analyzer.telemetry.api.model.environment.EnvironmentFrame
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AceMapperTest {

    @Test
    fun `map uses ACE raw pages plus fallback scratch base frame`() = runTest {
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
        val mapper = AceMapper(
            baseFrameMapper = baseFrameMapper,
            sessionMapper = AcEvoSessionMapper(),
            lapMapper = AcEvoLapMapper(),
            carMapper = AcEvoCarMapper(),
            wheelsMapper = AcEvoWheelsMapper(),
            damageMapper = AcEvoDamageMapper(),
            environmentMapper = AcEvoEnvironmentMapper(),
        )

        val snapshot = AceRawSnapshot().apply {
            frameId = 4L
            timestampNs = 456L
            graphics.packetId = 1
            graphics.statusRaw = com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStatus.LIVE.rawValue
            graphics.currentLapTimeMsRaw = 81_234
            graphics.lastLaptimeMsRaw = 80_000
            graphics.bestLaptimeMsRaw = 79_500
            graphics.driverNameRaw[0] = 'L'.code.toByte()
            graphics.driverSurnameRaw[0] = 'N'.code.toByte()
            graphics.carModelRaw[0] = 'G'.code.toByte()
            graphics.displaySpeedKmhRaw = 255
            graphics.totalLapCountRaw = 3
            graphics.sessionState.phaseNameRaw[0] = 'R'.code.toByte()
            graphics.sessionState.timeLeftRaw[0] = '1'.code.toByte()
            physics.speedKmh = 255f
            statics.smVersionRaw[0] = '1'.code.toByte()
            statics.trackRaw[0] = 'M'.code.toByte()
            statics.numberOfSessionsRaw = 1
            statics.sessionNameRaw[0] = 'W'.code.toByte()
        }

        val frame = mapper.map(snapshot)
        val session = assertNotNull(frame.session)
        val lap = assertNotNull(frame.lap)
        val car = assertNotNull(frame.car)

        assertEquals(4L, frame.frameId)
        assertEquals(456L, frame.timestampNs)
        assertEquals(81_234, lap.currentLapTimeMs)
        assertEquals(255, car.displaySpeedKmh)
        assertNotNull(session.sessionName)
    }
}
