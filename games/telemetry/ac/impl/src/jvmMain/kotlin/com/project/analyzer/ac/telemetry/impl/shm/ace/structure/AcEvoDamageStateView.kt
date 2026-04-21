package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "damageFrontRaw",
    "damageRearRaw",
    "damageLeftRaw",
    "damageRightRaw",
    "damageCenterRaw",
    "damageSuspensionLfRaw",
    "damageSuspensionRfRaw",
    "damageSuspensionLrRaw",
    "damageSuspensionRrRaw",
    "padding0",
)
public class AcEvoDamageStateView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var damageFrontRaw: Float = 0f

    @JvmField
    var damageRearRaw: Float = 0f

    @JvmField
    var damageLeftRaw: Float = 0f

    @JvmField
    var damageRightRaw: Float = 0f

    @JvmField
    var damageCenterRaw: Float = 0f

    @JvmField
    var damageSuspensionLfRaw: Float = 0f

    @JvmField
    var damageSuspensionRfRaw: Float = 0f

    @JvmField
    var damageSuspensionLrRaw: Float = 0f

    @JvmField
    var damageSuspensionRrRaw: Float = 0f

    @JvmField
    var padding0: ByteArray = ByteArray(92)

    val damageFront: Float get() = damageFrontRaw
    val damageRear: Float get() = damageRearRaw
    val damageLeft: Float get() = damageLeftRaw
    val damageRight: Float get() = damageRightRaw
    val damageCenter: Float get() = damageCenterRaw
    val damageSuspensionLf: Float get() = damageSuspensionLfRaw
    val damageSuspensionRf: Float get() = damageSuspensionRfRaw
    val damageSuspensionLr: Float get() = damageSuspensionLrRaw
    val damageSuspensionRr: Float get() = damageSuspensionRrRaw

    companion object {

        const val SIZE_BYTES: Int = 128
    }
}
