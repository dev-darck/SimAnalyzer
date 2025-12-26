package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.internal.AcRawSnapshot
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
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
) {

    fun map(snapshot: AcRawSnapshot): TelemetryFrame {
        val physics = snapshot.physics
        val graphics = snapshot.graphics
        val statics = snapshot.statics

        cache.updateIfNeeded(graphics, statics)

        return TelemetryFrame(
            frameId = snapshot.frameId,
            session = sessionMapper.map(graphics, statics),
            lap = lapMapper.map(graphics),
            car = carMapper.map(physics, graphics, statics),
            wheels = wheelsMapper.map(physics, graphics),
            damage = damageMapper.map(physics),
            environment = environmentMapper.map(physics, graphics),
            timestampNs = snapshot.timestampNs
        )
    }
}
