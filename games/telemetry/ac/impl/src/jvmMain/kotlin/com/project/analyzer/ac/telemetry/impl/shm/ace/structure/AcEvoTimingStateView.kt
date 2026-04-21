package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "currentLaptimeRaw",
    "deltaCurrentRaw",
    "padding0",
    "deltaCurrentPRaw",
    "lastLaptimeRaw",
    "deltaLastRaw",
    "padding1",
    "deltaLastPRaw",
    "bestLaptimeRaw",
    "idealLaptimeRaw",
    "totalTimeRaw",
    "isInvalidRaw",
    "padding2",
)
public class AcEvoTimingStateView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var currentLaptimeRaw: ByteArray = ByteArray(15)

    @JvmField
    var deltaCurrentRaw: ByteArray = ByteArray(15)

    @JvmField
    var padding0: ByteArray = ByteArray(2)

    @JvmField
    var deltaCurrentPRaw: Int = 0

    @JvmField
    var lastLaptimeRaw: ByteArray = ByteArray(15)

    @JvmField
    var deltaLastRaw: ByteArray = ByteArray(15)

    @JvmField
    var padding1: ByteArray = ByteArray(2)

    @JvmField
    var deltaLastPRaw: Int = 0

    @JvmField
    var bestLaptimeRaw: ByteArray = ByteArray(15)

    @JvmField
    var idealLaptimeRaw: ByteArray = ByteArray(15)

    @JvmField
    var totalTimeRaw: ByteArray = ByteArray(15)

    @JvmField
    var isInvalidRaw: Byte = 0

    @JvmField
    var padding2: ByteArray = ByteArray(138)

    val currentLaptime: String get() = currentLaptimeRaw.toAceCString()
    val deltaCurrent: String get() = deltaCurrentRaw.toAceCString()
    val deltaCurrentP: Int get() = deltaCurrentPRaw
    val lastLaptime: String get() = lastLaptimeRaw.toAceCString()
    val deltaLast: String get() = deltaLastRaw.toAceCString()
    val deltaLastP: Int get() = deltaLastPRaw
    val bestLaptime: String get() = bestLaptimeRaw.toAceCString()
    val idealLaptime: String get() = idealLaptimeRaw.toAceCString()
    val totalTime: String get() = totalTimeRaw.toAceCString()
    val isInvalid: Boolean get() = isInvalidRaw.toAceBool()

    companion object {

        const val SIZE_BYTES: Int = 256
    }
}
