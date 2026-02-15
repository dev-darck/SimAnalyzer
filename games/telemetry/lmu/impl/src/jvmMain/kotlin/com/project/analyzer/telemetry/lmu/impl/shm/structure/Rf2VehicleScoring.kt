package com.project.analyzer.telemetry.lmu.impl.shm.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "id",
    "driverName",
    "vehicleName",
    "totalLaps",
    "sector",
    "finishStatus",
    "lapDist",
    "pathLateral",
    "trackEdge",
    "bestSector1",
    "bestSector2",
    "bestLapTime",
    "lastSector1",
    "lastSector2",
    "lastLapTime",
    "curSector1",
    "curSector2",
    "numPitstops",
    "numPenalties",
    "isPlayer",
    "control",
    "inPits",
    "place",
    "vehClass",
    "timeBehindNext",
    "lapsBehindNext",
    "timeBehindLeader",
    "lapsBehindLeader",
    "lapStartEt",
    "pos",
    "localVel",
    "localAccel",
    "ori",
    "localRot",
    "localRotAccel",
    "headlights",
    "pitState",
    "serverScored",
    "individualPhase",
    "qualification",
    "timeBehindFastest",
    "lapsBehindFastest",
    "timeBehindRisk",
    "lapsBehindRisk",
    "isGhost",
    "numDuplicates",
    "safetyCarInstruction",
    "safetyCarPrivateInstruction",
    "pitLaneStartDist",
    "pitLaneEndDist",
    "maxSpeed",
    "vehicleFileName",
    "expansion",
)
internal class Rf2VehicleScoring : Pack4Structure() {

    fun attach(p: Pointer, offset: Int) {
        useMemory(p, offset)
    }

    @JvmField
    var id: Int = 0

    @JvmField
    var driverName: ByteArray = ByteArray(32)

    @JvmField
    var vehicleName: ByteArray = ByteArray(64)

    @JvmField
    var totalLaps: Short = 0

    @JvmField
    var sector: Byte = 0

    @JvmField
    var finishStatus: Byte = 0

    @JvmField
    var lapDist: Double = 0.0

    @JvmField
    var pathLateral: Double = 0.0

    @JvmField
    var trackEdge: Double = 0.0

    @JvmField
    var bestSector1: Double = 0.0

    @JvmField
    var bestSector2: Double = 0.0

    @JvmField
    var bestLapTime: Double = 0.0

    @JvmField
    var lastSector1: Double = 0.0

    @JvmField
    var lastSector2: Double = 0.0

    @JvmField
    var lastLapTime: Double = 0.0

    @JvmField
    var curSector1: Double = 0.0

    @JvmField
    var curSector2: Double = 0.0

    @JvmField
    var numPitstops: Short = 0

    @JvmField
    var numPenalties: Short = 0

    @JvmField
    var isPlayer: Byte = 0

    @JvmField
    var control: Byte = 0

    @JvmField
    var inPits: Byte = 0

    @JvmField
    var place: Byte = 0

    @JvmField
    var vehClass: ByteArray = ByteArray(32)

    @JvmField
    var timeBehindNext: Double = 0.0

    @JvmField
    var lapsBehindNext: Int = 0

    @JvmField
    var timeBehindLeader: Double = 0.0

    @JvmField
    var lapsBehindLeader: Int = 0

    @JvmField
    var lapStartEt: Double = 0.0

    @JvmField
    var pos: DoubleArray = DoubleArray(3)

    @JvmField
    var localVel: DoubleArray = DoubleArray(3)

    @JvmField
    var localAccel: DoubleArray = DoubleArray(3)

    @JvmField
    var ori: DoubleArray = DoubleArray(9)

    @JvmField
    var localRot: DoubleArray = DoubleArray(3)

    @JvmField
    var localRotAccel: DoubleArray = DoubleArray(3)

    @JvmField
    var headlights: Byte = 0

    @JvmField
    var pitState: Byte = 0

    @JvmField
    var serverScored: Byte = 0

    @JvmField
    var individualPhase: Byte = 0

    @JvmField
    var qualification: Int = 0

    @JvmField
    var timeBehindFastest: Double = 0.0

    @JvmField
    var lapsBehindFastest: Int = 0

    @JvmField
    var timeBehindRisk: Double = 0.0

    @JvmField
    var lapsBehindRisk: Int = 0

    @JvmField
    var isGhost: Byte = 0

    @JvmField
    var numDuplicates: Byte = 0

    @JvmField
    var safetyCarInstruction: Byte = 0

    @JvmField
    var safetyCarPrivateInstruction: Byte = 0

    @JvmField
    var pitLaneStartDist: Double = 0.0

    @JvmField
    var pitLaneEndDist: Double = 0.0

    @JvmField
    var maxSpeed: Double = 0.0

    @JvmField
    var vehicleFileName: ByteArray = ByteArray(64)

    @JvmField
    var expansion: ByteArray = ByteArray(4)

    public companion object {

        public const val SIZE: Int = 584
        public const val OFFSET: Int = 540
    }
}
