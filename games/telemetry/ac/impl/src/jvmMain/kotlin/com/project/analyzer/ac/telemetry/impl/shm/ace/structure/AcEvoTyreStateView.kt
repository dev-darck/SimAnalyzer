package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "slipRaw",
    "lockRaw",
    "padding0",
    "tyrePressureRaw",
    "tyreTemperatureCRaw",
    "brakeTemperatureCRaw",
    "brakePressureRaw",
    "tyreTemperatureLeftRaw",
    "tyreTemperatureCenterRaw",
    "tyreTemperatureRightRaw",
    "tyreCompoundFrontRaw",
    "tyreCompoundRearRaw",
    "padding1",
    "tyreNormalizedPressureRaw",
    "tyreNormalizedTemperatureLeftRaw",
    "tyreNormalizedTemperatureCenterRaw",
    "tyreNormalizedTemperatureRightRaw",
    "brakeNormalizedTemperatureRaw",
    "tyreNormalizedTemperatureCoreRaw",
    "padding2",
)
public class AcEvoTyreStateView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var slipRaw: Float = 0f

    @JvmField
    var lockRaw: Byte = 0

    @JvmField
    var padding0: ByteArray = ByteArray(3)

    @JvmField
    var tyrePressureRaw: Float = 0f

    @JvmField
    var tyreTemperatureCRaw: Float = 0f

    @JvmField
    var brakeTemperatureCRaw: Float = 0f

    @JvmField
    var brakePressureRaw: Float = 0f

    @JvmField
    var tyreTemperatureLeftRaw: Float = 0f

    @JvmField
    var tyreTemperatureCenterRaw: Float = 0f

    @JvmField
    var tyreTemperatureRightRaw: Float = 0f

    @JvmField
    var tyreCompoundFrontRaw: ByteArray = ByteArray(33)

    @JvmField
    var tyreCompoundRearRaw: ByteArray = ByteArray(33)

    @JvmField
    var padding1: ByteArray = ByteArray(2)

    @JvmField
    var tyreNormalizedPressureRaw: Float = 0f

    @JvmField
    var tyreNormalizedTemperatureLeftRaw: Float = 0f

    @JvmField
    var tyreNormalizedTemperatureCenterRaw: Float = 0f

    @JvmField
    var tyreNormalizedTemperatureRightRaw: Float = 0f

    @JvmField
    var brakeNormalizedTemperatureRaw: Float = 0f

    @JvmField
    var tyreNormalizedTemperatureCoreRaw: Float = 0f

    @JvmField
    var padding2: ByteArray = ByteArray(128)

    val slip: Float get() = slipRaw
    val lock: Boolean get() = lockRaw.toAceBool()
    val tyrePressure: Float get() = tyrePressureRaw
    val tyreTemperatureC: Float get() = tyreTemperatureCRaw
    val brakeTemperatureC: Float get() = brakeTemperatureCRaw
    val brakePressure: Float get() = brakePressureRaw
    val tyreTemperatureLeft: Float get() = tyreTemperatureLeftRaw
    val tyreTemperatureCenter: Float get() = tyreTemperatureCenterRaw
    val tyreTemperatureRight: Float get() = tyreTemperatureRightRaw
    val tyreCompoundFront: String get() = tyreCompoundFrontRaw.toAceCString()
    val tyreCompoundRear: String get() = tyreCompoundRearRaw.toAceCString()
    val tyreNormalizedPressure: Float get() = tyreNormalizedPressureRaw
    val tyreNormalizedTemperatureLeft: Float get() = tyreNormalizedTemperatureLeftRaw
    val tyreNormalizedTemperatureCenter: Float get() = tyreNormalizedTemperatureCenterRaw
    val tyreNormalizedTemperatureRight: Float get() = tyreNormalizedTemperatureRightRaw
    val brakeNormalizedTemperature: Float get() = brakeNormalizedTemperatureRaw
    val tyreNormalizedTemperatureCore: Float get() = tyreNormalizedTemperatureCoreRaw

    public companion object {

        public const val SIZE_BYTES: Int = 256
    }
}
