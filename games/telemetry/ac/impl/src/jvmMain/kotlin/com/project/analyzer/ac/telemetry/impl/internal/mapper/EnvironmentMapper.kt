package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.environment.EnvironmentFrame
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class EnvironmentMapper {

    fun map(physics: SPageFilePhysics, graphics: SPageFileGraphics): EnvironmentFrame = EnvironmentFrame(
        airTempC = physics.airTemp,
        roadTempC = physics.roadTemp,
        airDensity = physics.airDensity,

        windSpeedMps = graphics.windSpeed,
        windDirectionDeg = Math.toDegrees(graphics.windDirection.toDouble()).toFloat(),

        rainIntensity = normalizeRainIntensity(graphics.rainIntensity),
        rainIntensityIn10min = graphics.rainIntensityIn10min,
        rainIntensityIn30min = graphics.rainIntensityIn30min,

        surfaceGrip = graphics.surfaceGrip,
        trackGripStatus = graphics.trackGripStatus,

        clockSeconds = graphics.clock,
    )

    /**
     * Normalizes rain intensity from 0-3 to 0.0-1.0
     */
    private fun normalizeRainIntensity(rawValue: Int): Float = (rawValue.coerceIn(0, 3) / 3f)
}
