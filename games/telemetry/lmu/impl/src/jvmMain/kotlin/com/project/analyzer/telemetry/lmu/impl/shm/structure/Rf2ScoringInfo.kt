package com.project.analyzer.telemetry.lmu.impl.shm.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "trackName",
    "session",
    "currentEt",
    "endEt",
    "maxLaps",
    "lapDist",
    "gamePhase",
    "yellowFlagState",
    "sectorFlag",
    "startLight",
    "numRedLights",
    "inRealtime",
    "playerName",
    "plrFileName",
    "darkCloud",
    "raining",
    "ambientTemp",
    "trackTemp",
    "wind",
    "minPathWetness",
    "maxPathWetness",
    "gameMode",
    "isPasswordProtected",
    "serverPort",
    "serverPublicIP",
    "maxPlayers",
    "serverName",
    "startEt",
    "avgPathWetness",
    "expansion",
)
internal class Rf2ScoringInfo : Pack4Structure() {

    fun attach(p: Pointer, offset: Int) {
        useMemory(p, offset)
    }

    @JvmField
    var trackName: ByteArray = ByteArray(64)

    @JvmField
    var session: Int = 0

    @JvmField
    var currentEt: Double = 0.0

    @JvmField
    var endEt: Double = 0.0

    @JvmField
    var maxLaps: Int = 0

    @JvmField
    var lapDist: Double = 0.0

    @JvmField
    var gamePhase: Byte = 0

    @JvmField
    var yellowFlagState: Byte = 0

    @JvmField
    var sectorFlag: ByteArray = ByteArray(3)

    @JvmField
    var startLight: Byte = 0

    @JvmField
    var numRedLights: Byte = 0

    @JvmField
    var inRealtime: Byte = 0

    @JvmField
    var playerName: ByteArray = ByteArray(32)

    @JvmField
    var plrFileName: ByteArray = ByteArray(64)

    @JvmField
    var darkCloud: Double = 0.0

    @JvmField
    var raining: Double = 0.0

    @JvmField
    var ambientTemp: Double = 0.0

    @JvmField
    var trackTemp: Double = 0.0

    @JvmField
    var wind: DoubleArray = DoubleArray(3)

    @JvmField
    var minPathWetness: Double = 0.0

    @JvmField
    var maxPathWetness: Double = 0.0

    @JvmField
    var gameMode: Byte = 0

    @JvmField
    var isPasswordProtected: Byte = 0

    @JvmField
    var serverPort: Short = 0

    @JvmField
    var serverPublicIP: Int = 0

    @JvmField
    var maxPlayers: Int = 0

    @JvmField
    var serverName: ByteArray = ByteArray(32)

    @JvmField
    var startEt: Float = 0f

    @JvmField
    var avgPathWetness: Double = 0.0

    @JvmField
    var expansion: ByteArray = ByteArray(200)

    public companion object {

        public const val SIZE: Int = 528
        public const val OFFSET: Int = 12
    }
}
