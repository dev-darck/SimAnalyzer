package com.project.analyzer.telemetry.api.model

import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.damage.DamageFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelsFrame
import com.project.analyzer.telemetry.api.model.environment.EnvironmentFrame
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.opponents.OpponentFrame
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.value.TelemetryValue

public data class TelemetryFrame(
    val frameId: Long? = null, // packetId/sequence

    val session: SessionFrame? = null,
    val lap: LapFrame? = null,
    val car: CarFrame? = null,
    val wheels: WheelsFrame? = null,
    val damage: DamageFrame? = null,
    val environment: EnvironmentFrame? = null,
    val opponents: List<OpponentFrame> = emptyList(),

    val extras: Map<String, TelemetryValue> = emptyMap(),
    var timestampNs: Long = 0L,
)
