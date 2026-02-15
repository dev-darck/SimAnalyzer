package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.telemetry.api.model.car.wheels.WheelFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelsFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuVehicleTelemetry
import com.project.analyzer.telemetry.lmu.api.model.LmuWheel

internal fun mapWheels(telemetry: LmuVehicleTelemetry): WheelsFrame {
    val wheels = telemetry.wheels
    return WheelsFrame(
        fl = wheels.getOrNull(0)?.let { readWheel(it) },
        fr = wheels.getOrNull(1)?.let { readWheel(it) },
        rl = wheels.getOrNull(2)?.let { readWheel(it) },
        rr = wheels.getOrNull(3)?.let { readWheel(it) }
    )
}

private fun readWheel(w: LmuWheel): WheelFrame {
    val inner = w.temperature[0].toFloat()
    val middle = w.temperature[1].toFloat()
    val outer = w.temperature[2].toFloat()
    val avg = (inner + middle + outer) / TEMP_AVG_DIVISOR

    return WheelFrame(
        pressurePsi = w.pressure.toFloat(),
        wear = w.wear.toFloat(),
        coreTempC = w.tireCarcassTemperature.toFloat(),
        innerTempC = inner,
        middleTempC = middle,
        outerTempC = outer,
        avgTempC = avg,
        brakeTempC = w.brakeTemp.toFloat(),
        brakePressure = w.brakePressure.toFloat(),
        load = w.tireLoad.toFloat(),
        angularSpeed = w.rotation.toFloat(),
        suspensionTravel = w.suspensionDeflection.toFloat(),
        camberRad = w.camber.toFloat(),
        longitudinalForce = w.longitudinalForce.toFloat(),
        lateralForce = w.lateralForce.toFloat()
    )
}

private const val TEMP_AVG_DIVISOR = 3f
