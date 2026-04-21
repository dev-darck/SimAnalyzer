package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "damageRaw",
    "fuelRaw",
    "tyresLfRaw",
    "tyresRfRaw",
    "tyresLrRaw",
    "tyresRrRaw",
    "padding0",
)
public class AcEvoPitInfoView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var damageRaw: Byte = 0

    @JvmField
    var fuelRaw: Byte = 0

    @JvmField
    var tyresLfRaw: Byte = 0

    @JvmField
    var tyresRfRaw: Byte = 0

    @JvmField
    var tyresLrRaw: Byte = 0

    @JvmField
    var tyresRrRaw: Byte = 0

    @JvmField
    var padding0: ByteArray = ByteArray(58)

    val damage: Int get() = damageRaw.toAceInt8()
    val fuel: Int get() = fuelRaw.toAceInt8()
    val tyresLf: Int get() = tyresLfRaw.toAceInt8()
    val tyresRf: Int get() = tyresRfRaw.toAceInt8()
    val tyresLr: Int get() = tyresLrRaw.toAceInt8()
    val tyresRr: Int get() = tyresRrRaw.toAceInt8()

    companion object {

        const val SIZE_BYTES: Int = 64
    }
}
