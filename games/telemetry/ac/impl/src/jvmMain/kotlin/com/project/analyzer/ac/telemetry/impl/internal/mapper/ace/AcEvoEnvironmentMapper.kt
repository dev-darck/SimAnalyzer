package com.project.analyzer.ac.telemetry.impl.internal.mapper.ace

import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.telemetry.api.model.environment.EnvironmentFrame
import dev.zacsweers.metro.Inject

@Inject
internal class AcEvoEnvironmentMapper {

    fun enrich(base: EnvironmentFrame?, snapshot: AceRawSnapshot): EnvironmentFrame {
        val physics = snapshot.physics
        val graphics = snapshot.graphics
        val statics = snapshot.statics
        val baseEnvironment = base ?: EnvironmentFrame()

        return baseEnvironment.copy(
            airTempC = graphics.airTemperatureC.toFloat(),
            roadTempC = physics.roadTemp.takeIf { it.isFinite() } ?: baseEnvironment.roadTempC,
            airDensity = physics.airDensity.takeIf { it.isFinite() } ?: baseEnvironment.airDensity,
            isStaticWeather = statics.isStaticWeather,
            nation = statics.nation.ifBlank { baseEnvironment.nation },
            longitude = statics.longitude.takeIf { it.isFinite() } ?: baseEnvironment.longitude,
            latitude = statics.latitude.takeIf { it.isFinite() } ?: baseEnvironment.latitude,
            startingAmbientTempC = statics.startingAmbientTemperatureC.takeIf { it.isFinite() }
                ?: baseEnvironment.startingAmbientTempC,
            startingRoadTempC = statics.startingGroundTemperatureC.takeIf { it.isFinite() }
                ?: baseEnvironment.startingRoadTempC,
            startingGripLabel = statics.startingGrip?.name ?: baseEnvironment.startingGripLabel,
            surfaceGrip = mapSurfaceGrip(statics.startingGrip) ?: baseEnvironment.surfaceGrip,
            clockSeconds = (graphics.timeOfDayHours * 3600 + graphics.timeOfDayMinutes * 60 + graphics.timeOfDaySeconds).toFloat(),
        )
    }

    private fun mapSurfaceGrip(
        value: com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStartingGrip?,
    ): Float? = when (value) {
        com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStartingGrip.GREEN -> 0f
        com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStartingGrip.FAST -> 0.5f
        com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStartingGrip.OPTIMUM -> 1f
        null -> null
    }
}
