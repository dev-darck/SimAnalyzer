package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.internal.AcRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.ac.telemetry.impl.shm.structure.toKString
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class AcMapper(
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

    suspend fun map(snapshot: AcRawSnapshot): TelemetryFrame {
        val physics = snapshot.physics
        val graphics = snapshot.graphics
        val statics = snapshot.statics

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
            lapAnalyzer.processPhysicsFrame(snapshot.timestampNs, physics)
            lapAnalyzer.getSnapshot(snapshot.timestampNs)
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
            frameId = snapshot.frameId,
            session = sessionFrame,
            lap = lapMapper.map(
                graphics = graphics,
                fallback = lapSnapshot,
                sectorCountOverride = sectorCountOverride,
            ),
            car = carMapper.map(physics, graphics, statics),
            wheels = wheelsMapper.map(physics, graphics),
            damage = damageMapper.map(physics),
            environment = environmentMapper.map(physics, graphics),
            timestampNs = snapshot.timestampNs,
        )
    }
}
