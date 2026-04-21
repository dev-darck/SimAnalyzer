package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.telemetry.api.model.car.wheels.WheelFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelsFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleTelemetry
import com.project.analyzer.telemetry.lmu.api.model.LmuWheel
import com.project.analyzer.telemetry.lmu.impl.common.LmuTelemetryConversions

internal fun mapWheels(telemetry: LmuVehicleTelemetry): WheelsFrame {
    val wheels = telemetry.wheels
    return WheelsFrame(
        fl = wheels.getOrNull(0)?.let { readWheel(it) },
        fr = wheels.getOrNull(1)?.let { readWheel(it) },
        rl = wheels.getOrNull(2)?.let { readWheel(it) },
        rr = wheels.getOrNull(3)?.let { readWheel(it) },
    )
}

private fun readWheel(w: LmuWheel): WheelFrame {
    val inner = w.temperature.getOrNull(0)?.let(LmuTelemetryConversions::kelvinToCelsius)
    val middle = w.temperature.getOrNull(1)?.let(LmuTelemetryConversions::kelvinToCelsius)
    val outer = w.temperature.getOrNull(2)?.let(LmuTelemetryConversions::kelvinToCelsius)
    val avg = listOfNotNull(inner, middle, outer).takeIf { it.isNotEmpty() }?.average()?.toFloat()

    return WheelFrame(
        pressurePsi = LmuTelemetryConversions.kpaToPsi(w.pressure),
        wear = w.wear.toFloat(),
        coreTempC = LmuTelemetryConversions.kelvinToCelsius(w.tireCarcassTemperature),
        innerTempC = inner,
        middleTempC = middle,
        outerTempC = outer,
        avgTempC = avg,
        brakeTempC = LmuTelemetryConversions.finiteFloat(w.brakeTemp),
        brakePressure = w.brakePressure.toFloat(),
        load = w.tireLoad.toFloat(),
        angularSpeed = w.rotation.toFloat(),
        suspensionTravel = w.suspensionDeflection.toFloat(),
        camberRad = w.camber.toFloat(),
        longitudinalForce = w.longitudinalForce.toFloat(),
        lateralForce = w.lateralForce.toFloat(),
    )
}
