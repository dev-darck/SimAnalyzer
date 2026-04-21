package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.telemetry.lmu.api.model.LmuWheel
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2Wheel
import com.project.analyzer.utils.shm.toCString

internal fun Rf2Wheel.toModel(): LmuWheel = LmuWheel(
    suspensionDeflection = suspensionDeflection,
    rideHeight = rideHeight,
    suspensionForce = suspensionForce,
    brakeTemp = brakeTemp,
    brakePressure = brakePressure,
    rotation = rotation,
    lateralPatchVel = lateralPatchVel,
    longitudinalPatchVel = longitudinalPatchVel,
    lateralGroundVel = lateralGroundVel,
    longitudinalGroundVel = longitudinalGroundVel,
    camber = camber,
    lateralForce = lateralForce,
    longitudinalForce = longitudinalForce,
    tireLoad = tireLoad,
    gripFraction = gripFraction,
    pressure = pressure,
    temperature = temperature.copyOf(),
    wear = wear,
    terrainName = terrainName.toCString(),
    surfaceType = surfaceType.toUnsignedInt(),
    flat = flat.toBoolean(),
    detached = detached.toBoolean(),
    staticUndeflectedRadius = staticUndeflectedRadius.toUnsignedInt(),
    verticalTireDeflection = verticalTireDeflection,
    wheelYLocation = wheelYLocation,
    toe = toe,
    tireCarcassTemperature = tireCarcassTemperature,
    tireInnerLayerTemperature = tireInnerLayerTemperature.copyOf(),
    optimalTemp = optimalTemp,
    compoundIndex = compoundIndex.toUnsignedInt(),
    compoundType = compoundType.toUnsignedInt(),
    expansion = expansion.copyOf(),
)
