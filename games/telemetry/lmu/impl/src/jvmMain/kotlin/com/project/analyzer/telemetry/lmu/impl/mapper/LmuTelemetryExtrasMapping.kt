package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.telemetry.api.model.value.TelemetryValue
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleScoring
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleTelemetry

internal fun mapExtras(telemetry: LmuVehicleTelemetry, scoring: LmuVehicleScoring?): Map<String, TelemetryValue> =
    buildMap {
        put("deltaTime", TelemetryValue.DoubleVal(telemetry.deltaTime))
        put("engineOilTempC", TelemetryValue.DoubleVal(telemetry.engineOilTemp))
        scoring?.lapDist?.let { put("lapDist", TelemetryValue.DoubleVal(it)) }
        scoring?.place?.let { put("position", TelemetryValue.IntVal(it)) }
        scoring?.timeBehindNext?.let { put("gapAhead", TelemetryValue.DoubleVal(it)) }
        scoring?.timeBehindLeader?.let { put("gapBehind", TelemetryValue.DoubleVal(it)) }
    }
