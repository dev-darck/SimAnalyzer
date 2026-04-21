package com.project.analyzer.ac.telemetry.impl.internal.mapper.ace

import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.telemetry.api.model.car.wheels.WheelFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelsFrame
import dev.zacsweers.metro.Inject

@Inject
internal class AcEvoWheelsMapper {

    fun enrich(base: WheelsFrame?, snapshot: AceRawSnapshot): WheelsFrame {
        val graphics = snapshot.graphics
        val baseWheels = base ?: WheelsFrame()

        return baseWheels.copy(
            fl = enrichWheel(baseWheels.fl, graphics.tyreLf),
            fr = enrichWheel(baseWheels.fr, graphics.tyreRf),
            rl = enrichWheel(baseWheels.rl, graphics.tyreLr),
            rr = enrichWheel(baseWheels.rr, graphics.tyreRr),
            tyreCompound = graphics.currentTyreCompound.ifBlank { baseWheels.tyreCompound },
            isRainTyres = graphics.isWetTyreCompound,
            useSingleCompound = graphics.useSingleCompound,
        )
    }

    private fun enrichWheel(
        base: WheelFrame?,
        tyre: com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoTyreStateView,
    ): WheelFrame = (base ?: WheelFrame()).copy(
        pressurePsi = tyre.tyrePressure,
        coreTempC = tyre.tyreTemperatureC,
        innerTempC = tyre.tyreTemperatureLeft,
        middleTempC = tyre.tyreTemperatureCenter,
        outerTempC = tyre.tyreTemperatureRight,
        brakeTempC = tyre.brakeTemperatureC,
        brakePressure = tyre.brakePressure,
        slip = tyre.slip,
        normalizedPressure = tyre.tyreNormalizedPressure,
        normalizedTempLeft = tyre.tyreNormalizedTemperatureLeft,
        normalizedTempMiddle = tyre.tyreNormalizedTemperatureCenter,
        normalizedTempRight = tyre.tyreNormalizedTemperatureRight,
        normalizedBrakeTemp = tyre.brakeNormalizedTemperature,
        normalizedCoreTemp = tyre.tyreNormalizedTemperatureCore,
        compoundFront = tyre.tyreCompoundFront.ifBlank { null },
        compoundRear = tyre.tyreCompoundRear.ifBlank { null },
    )
}
