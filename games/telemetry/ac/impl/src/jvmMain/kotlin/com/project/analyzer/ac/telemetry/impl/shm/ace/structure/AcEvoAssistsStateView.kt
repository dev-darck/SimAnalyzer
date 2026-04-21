package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "autoGearRaw",
    "autoBlipRaw",
    "autoClutchRaw",
    "autoClutchOnStartRaw",
    "manualIgnitionEStartRaw",
    "autoPitLimiterRaw",
    "standingStartAssistRaw",
    "padding0",
    "autoSteerRaw",
    "arcadeStabilityControlRaw",
    "padding1",
)
public class AcEvoAssistsStateView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var autoGearRaw: Byte = 0

    @JvmField
    var autoBlipRaw: Byte = 0

    @JvmField
    var autoClutchRaw: Byte = 0

    @JvmField
    var autoClutchOnStartRaw: Byte = 0

    @JvmField
    var manualIgnitionEStartRaw: Byte = 0

    @JvmField
    var autoPitLimiterRaw: Byte = 0

    @JvmField
    var standingStartAssistRaw: Byte = 0

    @JvmField
    var padding0: Byte = 0

    @JvmField
    var autoSteerRaw: Float = 0f

    @JvmField
    var arcadeStabilityControlRaw: Float = 0f

    @JvmField
    var padding1: ByteArray = ByteArray(48)

    val autoGear: Int get() = autoGearRaw.toAceUInt8()
    val autoBlip: Int get() = autoBlipRaw.toAceUInt8()
    val autoClutch: Int get() = autoClutchRaw.toAceUInt8()
    val autoClutchOnStart: Int get() = autoClutchOnStartRaw.toAceUInt8()
    val manualIgnitionEStart: Int get() = manualIgnitionEStartRaw.toAceUInt8()
    val autoPitLimiter: Int get() = autoPitLimiterRaw.toAceUInt8()
    val standingStartAssist: Int get() = standingStartAssistRaw.toAceUInt8()
    val autoSteer: Float get() = autoSteerRaw
    val arcadeStabilityControl: Float get() = arcadeStabilityControlRaw

    companion object {

        const val SIZE_BYTES: Int = 64
    }
}
