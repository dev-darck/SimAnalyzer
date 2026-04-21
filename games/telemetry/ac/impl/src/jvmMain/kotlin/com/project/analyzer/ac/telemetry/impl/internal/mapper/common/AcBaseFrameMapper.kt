package com.project.analyzer.ac.telemetry.impl.internal.mapper.common

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.CarMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.DamageMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.EnvironmentMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.LapMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.SessionMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.ac.WheelsMapper
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.toKString
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class AcBaseFrameMapper(
    private val cache: AcSessionCache,
    private val sessionMapper: SessionMapper,
    private val lapMapper: LapMapper,
    private val carMapper: CarMapper,
    private val wheelsMapper: WheelsMapper,
    private val damageMapper: DamageMapper,
    private val environmentMapper: EnvironmentMapper,
    private val lapAnalyzer: FallbackLapAnalyzer,
) {

    init {
        cache.addSessionChangeListener { lapAnalyzer.resetSession() }
    }

    suspend fun map(
        frameId: Long,
        timestampNs: Long,
        physics: SPageFilePhysics,
        graphics: SPageFileGraphics,
        statics: SPageFileStatic,
        lapFallbackUsage: LapFallbackUsage = LapFallbackUsage.FULL,
    ): TelemetryFrame {
        cache.updateIfNeeded(graphics, statics)

        val normalizedTrackId = cache.trackInfo?.trackId?.takeIf { it.isNotBlank() }
            ?: TrackIdNormalizer
                .normalize(
                    track = statics.track.toKString(),
                    layout = statics.trackConfiguration.toKString().takeIf { it.isNotBlank() },
                )
                .takeIf { it.isNotBlank() }

        val calibration = lapAnalyzer.loadCalibration(normalizedTrackId)
        val sectorCountOverride = calibration?.sectors?.size?.coerceAtLeast(1)
        val lapSnapshot = if (calibration != null) {
            lapAnalyzer.processPhysicsFrame(
                timestampNs = timestampNs,
                physics = physics,
                sectorIndexHint0Based = graphics.currentSectorIndex.takeIf { it >= 0 },
                lastSectorTimeHintMs = graphics.lastSectorTime.takeIf { it > 0 },
            )
            lapAnalyzer.getSnapshot(timestampNs)
        } else {
            null
        }

        val sessionFrame = sessionMapper.map(graphics, statics).let { session ->
            val track = session.track
            if (sectorCountOverride != null && track != null) {
                session.copy(track = track.copy(sectorCount = sectorCountOverride))
            } else {
                session
            }
        }

        return TelemetryFrame(
            frameId = frameId,
            session = sessionFrame,
            lap = lapMapper.map(
                graphics = graphics,
                fallback = lapSnapshot,
                sectorCountOverride = sectorCountOverride,
                fallbackUsage = lapFallbackUsage,
            ),
            car = carMapper.map(physics, graphics, statics),
            wheels = wheelsMapper.map(physics, graphics),
            damage = damageMapper.map(physics),
            environment = environmentMapper.map(physics, graphics),
            timestampNs = timestampNs,
        )
    }
}
