package com.project.analyzer.telemetry.lmu.impl.shm

import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2ScoringInfo
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleScoring
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleTelemetry
import com.project.analyzer.utils.shm.WinMappedRegion
import com.sun.jna.Pointer

internal class LmuNativeSharedMemory : LmuSharedMemory {

    private val region = WinMappedRegion(listOf(NATIVE_MAPPING_NAME, "Local\\$NATIVE_MAPPING_NAME"))

    private var pointer: Pointer? = null

    override var telemetryVersion: Int = 0
        private set
    override var scoringVersion: Int = 0
        private set
    override var numVehicles: Int = 0
        private set
    override var playerIndexHint: Int? = null
        private set

    private val vehicleTelemetry = Rf2VehicleTelemetry()
    private val vehicleScoring = Rf2VehicleScoring()
    private val scoringInfo = Rf2ScoringInfo()

    override fun isAttached(): Boolean = pointer != null

    override fun readAll(): Boolean {
        if (pointer == null) {
            pointer = region.openReadOnly()
        }

        val p = pointer ?: return false
        val activeVehicles = p.getByte(NATIVE_TELEMETRY_OFFSET.toLong()).toUnsignedInt()
        val scoringVehicles = p.getInt((NATIVE_SCORING_INFO_OFFSET + NATIVE_SCORING_NUM_VEHICLES_OFFSET).toLong())
        val resolvedVehicles = maxOf(activeVehicles, scoringVehicles).coerceIn(0, NATIVE_MAX_VEHICLES)
        val rawPlayerIndex = p.getByte(NATIVE_TELEMETRY_PLAYER_INDEX_OFFSET.toLong()).toUnsignedInt()
        val playerHasVehicle = p.getByte(NATIVE_TELEMETRY_PLAYER_HAS_VEHICLE_OFFSET.toLong()).toInt() != 0

        if (resolvedVehicles <= 0 || !playerHasVehicle) {
            numVehicles = 0
            playerIndexHint = null
            return false
        }

        val version = p.getInt(NATIVE_TELEMETRY_EVENT_OFFSET.toLong())
        if (version == 0) return false

        numVehicles = resolvedVehicles
        playerIndexHint = rawPlayerIndex.takeIf { it in 0 until resolvedVehicles }
        telemetryVersion = version
        scoringVersion = version
        return true
    }

    override fun getVehicleTelemetry(index: Int): Rf2VehicleTelemetry? {
        val p = pointer ?: return null
        if (index !in 0 until NATIVE_MAX_VEHICLES) return null

        vehicleTelemetry.attach(p, NATIVE_TELEMETRY_INFO_OFFSET + index * Rf2VehicleTelemetry.SIZE)
        vehicleTelemetry.read()
        return vehicleTelemetry
    }

    override fun getScoringInfo(): Rf2ScoringInfo? {
        val p = pointer ?: return null
        readScoringInfo(p, scoringInfo)
        return scoringInfo
    }

    override fun getVehicleScoring(index: Int): Rf2VehicleScoring? {
        val p = pointer ?: return null
        if (index !in 0 until NATIVE_MAX_VEHICLES) return null

        vehicleScoring.attach(p, NATIVE_VEHICLE_SCORING_OFFSET + index * Rf2VehicleScoring.SIZE)
        vehicleScoring.read()
        return vehicleScoring
    }

    override fun copyTelemetryBytes(target: ByteArray, offset: Int, expectedVersion: Int): Boolean {
        val p = pointer ?: return false
        if (target.size < offset + LmuSharedMemory.TELEMETRY_BUFFER_SIZE) return false
        if (p.getInt(NATIVE_TELEMETRY_EVENT_OFFSET.toLong()) != expectedVersion) return false

        target.writeIntLe(offset + VERSION_OFFSET, expectedVersion)
        target.writeIntLe(offset + VERSION_END_OFFSET, expectedVersion)
        target.writeIntLe(offset + NUM_VEHICLES_OFFSET, numVehicles)

        val vehicleCount = numVehicles.coerceIn(0, NATIVE_MAX_VEHICLES)
        val bytesToCopy = vehicleCount * Rf2VehicleTelemetry.SIZE
        p.read(
            NATIVE_TELEMETRY_INFO_OFFSET.toLong(),
            target,
            offset + Rf2VehicleTelemetry.OFFSET,
            bytesToCopy,
        )

        return p.getInt(NATIVE_TELEMETRY_EVENT_OFFSET.toLong()) == expectedVersion
    }

    override fun copyScoringBytes(target: ByteArray, offset: Int, expectedVersion: Int?): Boolean {
        val expected = expectedVersion ?: return false
        val p = pointer ?: return false
        if (target.size < offset + LmuSharedMemory.SCORING_BUFFER_SIZE) return false
        if (p.getInt(NATIVE_TELEMETRY_EVENT_OFFSET.toLong()) != expected) return false

        target.writeIntLe(offset + VERSION_OFFSET, expected)
        target.writeIntLe(offset + VERSION_END_OFFSET, expected)

        p.read(
            NATIVE_SCORING_INFO_OFFSET.toLong(),
            target,
            offset + Rf2ScoringInfo.OFFSET,
            Rf2ScoringInfo.SIZE,
        )

        val vehicleCount = numVehicles.coerceIn(0, NATIVE_MAX_VEHICLES)
        val bytesToCopy = vehicleCount * Rf2VehicleScoring.SIZE
        p.read(
            NATIVE_VEHICLE_SCORING_OFFSET.toLong(),
            target,
            offset + Rf2VehicleScoring.OFFSET,
            bytesToCopy,
        )

        return p.getInt(NATIVE_TELEMETRY_EVENT_OFFSET.toLong()) == expected
    }

    override fun close() {
        val closingPointer = pointer
        region.close()
        pointer = null

        if (closingPointer != null) {
            LeakCanaryRuntime.watch(closingPointer, "LmuNativeSharedMemory.pointer")
        }
        LeakCanaryRuntime.watch(this, "LmuNativeSharedMemory")
    }

    private fun readScoringInfo(pointer: Pointer, target: Rf2ScoringInfo) {
        val base = NATIVE_SCORING_INFO_OFFSET.toLong()
        target.trackName = pointer.getByteArray(base + 0, 64)
        target.session = pointer.getInt(base + 64)
        target.currentEt = pointer.getDouble(base + 68)
        target.endEt = pointer.getDouble(base + 76)
        target.maxLaps = pointer.getInt(base + 84)
        target.lapDist = pointer.getDouble(base + 88)
        target.resultsStreamPointer = pointer.getByteArray(base + 96, 8)
        target.numVehicles = pointer.getInt(base + 104)
        target.gamePhase = pointer.getByte(base + 108)
        target.yellowFlagState = pointer.getByte(base + 109)
        target.sectorFlag = pointer.getByteArray(base + 110, 3)
        target.startLight = pointer.getByte(base + 113)
        target.numRedLights = pointer.getByte(base + 114)
        target.inRealtime = pointer.getByte(base + 115)
        target.playerName = pointer.getByteArray(base + 116, 32)
        target.plrFileName = pointer.getByteArray(base + 148, 64)
        target.darkCloud = pointer.getDouble(base + 212)
        target.raining = pointer.getDouble(base + 220)
        target.ambientTemp = pointer.getDouble(base + 228)
        target.trackTemp = pointer.getDouble(base + 236)
        target.wind = doubleArrayOf(
            pointer.getDouble(base + 244),
            pointer.getDouble(base + 252),
            pointer.getDouble(base + 260),
        )
        target.minPathWetness = pointer.getDouble(base + 268)
        target.maxPathWetness = pointer.getDouble(base + 276)
        target.gameMode = pointer.getByte(base + 284)
        target.isPasswordProtected = pointer.getByte(base + 285)
        target.serverPort = pointer.getShort(base + 286)
        target.serverPublicIP = pointer.getInt(base + 288)
        target.maxPlayers = pointer.getInt(base + 292)
        target.serverName = pointer.getByteArray(base + 296, 32)
        target.startEt = pointer.getFloat(base + 328)
        target.avgPathWetness = pointer.getDouble(base + 332)
        target.expansion = pointer.getByteArray(base + 340, 200)
        target.vehiclePointer = pointer.getByteArray(base + 540, 8)
    }

    private fun Byte.toUnsignedInt(): Int = toInt() and 0xff

    private fun ByteArray.writeIntLe(offset: Int, value: Int) {
        this[offset] = (value and 0xff).toByte()
        this[offset + 1] = ((value ushr 8) and 0xff).toByte()
        this[offset + 2] = ((value ushr 16) and 0xff).toByte()
        this[offset + 3] = ((value ushr 24) and 0xff).toByte()
    }

    private companion object {
        const val NATIVE_MAPPING_NAME = "LMU_Data"
        const val NATIVE_MAX_VEHICLES = 104

        const val NATIVE_TELEMETRY_EVENT_OFFSET = 11 * Int.SIZE_BYTES
        const val VERSION_OFFSET = 0
        const val VERSION_END_OFFSET = 4
        const val NUM_VEHICLES_OFFSET = 12

        const val NATIVE_SCORING_INFO_OFFSET = 1_632
        const val NATIVE_SCORING_INFO_SIZE = 548
        const val NATIVE_SCORING_STREAM_SIZE_OFFSET = NATIVE_SCORING_INFO_OFFSET + NATIVE_SCORING_INFO_SIZE
        const val NATIVE_VEHICLE_SCORING_OFFSET = NATIVE_SCORING_STREAM_SIZE_OFFSET + Long.SIZE_BYTES
        const val NATIVE_SCORING_NUM_VEHICLES_OFFSET = 104

        const val NATIVE_TELEMETRY_OFFSET = 128_460
        const val NATIVE_TELEMETRY_PLAYER_INDEX_OFFSET = NATIVE_TELEMETRY_OFFSET + 1
        const val NATIVE_TELEMETRY_PLAYER_HAS_VEHICLE_OFFSET = NATIVE_TELEMETRY_OFFSET + 2
        const val NATIVE_TELEMETRY_INFO_OFFSET = NATIVE_TELEMETRY_OFFSET + 4
    }
}
