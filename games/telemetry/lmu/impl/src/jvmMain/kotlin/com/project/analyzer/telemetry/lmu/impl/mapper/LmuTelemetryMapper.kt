package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuTelemetrySnapshot
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
internal class LmuTelemetryMapper {

    fun map(snapshot: LmuTelemetrySnapshot): TelemetryFrame {
        val telemetry = snapshot.telemetry
        val scoring = snapshot.scoring
        return TelemetryFrame(
            frameId = snapshot.frameId,
            session = mapSession(telemetry, scoring),
            lap = mapLap(telemetry, scoring),
            car = mapCar(telemetry),
            wheels = mapWheels(telemetry),
            extras = mapExtras(telemetry, scoring),
            timestampNs = snapshot.timestampNs,
        )
    }
}
