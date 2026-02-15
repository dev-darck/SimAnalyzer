package com.project.analyzer.telemetry.lmu.impl.shm.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "suspensionDeflection",
    "rideHeight",
    "suspensionForce",
    "brakeTemp",
    "brakePressure",
    "rotation",
    "lateralPatchVel",
    "longitudinalPatchVel",
    "lateralGroundVel",
    "longitudinalGroundVel",
    "camber",
    "lateralForce",
    "longitudinalForce",
    "tireLoad",
    "gripFraction",
    "pressure",
    "temperature",
    "wear",
    "terrainName",
    "surfaceType",
    "flat",
    "detached",
    "staticUndeflectedRadius",
    "verticalTireDeflection",
    "wheelYLocation",
    "toe",
    "tireCarcassTemperature",
    "tireInnerLayerTemperature",
    "expansion",
)
internal class Rf2Wheel : Pack4Structure() {

    fun attach(p: Pointer, offset: Int) {
        useMemory(p, offset)
    }

    @JvmField
    var suspensionDeflection: Double = 0.0

    @JvmField
    var rideHeight: Double = 0.0

    @JvmField
    var suspensionForce: Double = 0.0

    @JvmField
    var brakeTemp: Double = 0.0

    @JvmField
    var brakePressure: Double = 0.0

    @JvmField
    var rotation: Double = 0.0

    @JvmField
    var lateralPatchVel: Double = 0.0

    @JvmField
    var longitudinalPatchVel: Double = 0.0

    @JvmField
    var lateralGroundVel: Double = 0.0

    @JvmField
    var longitudinalGroundVel: Double = 0.0

    @JvmField
    var camber: Double = 0.0

    @JvmField
    var lateralForce: Double = 0.0

    @JvmField
    var longitudinalForce: Double = 0.0

    @JvmField
    var tireLoad: Double = 0.0

    @JvmField
    var gripFraction: Double = 0.0

    @JvmField
    var pressure: Double = 0.0

    @JvmField
    var temperature: DoubleArray = DoubleArray(3)

    @JvmField
    var wear: Double = 0.0

    @JvmField
    var terrainName: ByteArray = ByteArray(16)

    @JvmField
    var surfaceType: Byte = 0

    @JvmField
    var flat: Byte = 0

    @JvmField
    var detached: Byte = 0

    @JvmField
    var staticUndeflectedRadius: Byte = 0

    @JvmField
    var verticalTireDeflection: Double = 0.0

    @JvmField
    var wheelYLocation: Double = 0.0

    @JvmField
    var toe: Double = 0.0

    @JvmField
    var tireCarcassTemperature: Double = 0.0

    @JvmField
    var tireInnerLayerTemperature: DoubleArray = DoubleArray(3)

    @JvmField
    var expansion: ByteArray = ByteArray(24)

    public companion object {

        public const val SIZE: Int = 260
    }
}
