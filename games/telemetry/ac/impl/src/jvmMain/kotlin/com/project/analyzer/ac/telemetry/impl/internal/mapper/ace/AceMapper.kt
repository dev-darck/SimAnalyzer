package com.project.analyzer.ac.telemetry.impl.internal.mapper.ace

import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.AcBaseFrameMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.LapFallbackUsage
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.value.TelemetryValue
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
internal class AceMapper(
    private val baseFrameMapper: AcBaseFrameMapper,
    private val sessionMapper: AcEvoSessionMapper,
    private val lapMapper: AcEvoLapMapper,
    private val carMapper: AcEvoCarMapper,
    private val wheelsMapper: AcEvoWheelsMapper,
    private val damageMapper: AcEvoDamageMapper,
    private val environmentMapper: AcEvoEnvironmentMapper,
) {

    suspend fun map(snapshot: AceRawSnapshot): TelemetryFrame {
        snapshot.fallback.seedFrom(snapshot)
        val baseFrame = baseFrameMapper.map(
            frameId = snapshot.frameId,
            timestampNs = snapshot.timestampNs,
            physics = snapshot.physics,
            graphics = snapshot.fallback.graphics,
            statics = snapshot.fallback.statics,
            lapFallbackUsage = LapFallbackUsage.SECTORS_ONLY,
        )

        return baseFrame.copy(
            session = sessionMapper.enrich(baseFrame.session, snapshot),
            lap = lapMapper.enrich(baseFrame.lap, snapshot),
            car = carMapper.enrich(baseFrame.car, snapshot),
            wheels = wheelsMapper.enrich(baseFrame.wheels, snapshot),
            damage = damageMapper.enrich(baseFrame.damage, snapshot),
            environment = environmentMapper.enrich(baseFrame.environment, snapshot),
            extras = buildAceExtras(snapshot),
        )
    }

    private fun buildAceExtras(snapshot: AceRawSnapshot): Map<String, TelemetryValue> {
        val graphics = snapshot.graphics
        val statics = snapshot.statics

        return buildMap {
            put("acevo.focusedCarIdA", TelemetryValue.LongVal(graphics.focusedCarIdA))
            put("acevo.focusedCarIdB", TelemetryValue.LongVal(graphics.focusedCarIdB))
            put("acevo.playerCarIdA", TelemetryValue.LongVal(graphics.playerCarIdA))
            put("acevo.playerCarIdB", TelemetryValue.LongVal(graphics.playerCarIdB))
            put("acevo.smVersion", TelemetryValue.StringVal(statics.smVersion))
            put("acevo.version", TelemetryValue.StringVal(statics.acEvoVersion))
            put("acevo.raceCutGainedTimeMs", TelemetryValue.IntVal(graphics.raceCutGainedTimeMs))
            put("acevo.distanceToDeadline", TelemetryValue.IntVal(graphics.distanceToDeadline))
            put("acevo.raceCutCurrentDelta", TelemetryValue.FloatVal(graphics.raceCutCurrentDelta))
        }
    }
}
