package com.project.analyzer.telemetry.lmu.impl.shm

import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2ScoringInfo
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleScoring
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleTelemetry
import com.project.analyzer.utils.shm.WinMappedRegion
import com.sun.jna.Pointer

internal class LmuRf2PluginSharedMemory : LmuSharedMemory {

    private val telemetryRegion = WinMappedRegion(LmuRFactor2ShmNames.TELEMETRY)
    private val scoringRegion = WinMappedRegion(LmuRFactor2ShmNames.SCORING)

    private var telemetryPointer: Pointer? = null
    private var scoringPointer: Pointer? = null

    override var telemetryVersion: Int = 0
        private set
    override var scoringVersion: Int = 0
        private set
    override var numVehicles: Int = 0
        private set
    override val playerIndexHint: Int? = null

    private val vehicleTelemetry = Rf2VehicleTelemetry()
    private val vehicleScoring = Rf2VehicleScoring()
    private val scoringInfo = Rf2ScoringInfo()

    override fun isAttached(): Boolean = telemetryPointer != null

    override fun readAll(): Boolean {
        if (telemetryPointer == null) {
            telemetryPointer = telemetryRegion.openReadOnly()
        }
        if (scoringPointer == null) {
            scoringPointer = scoringRegion.openReadOnly()
        }

        val tp = telemetryPointer ?: return false

        val v1 = tp.getInt(VERSION_OFFSET.toLong())
        val v2 = tp.getInt(VERSION_END_OFFSET.toLong())
        if (v1 != v2 || v1 == 0) return false

        telemetryVersion = v1
        numVehicles = tp.getInt(NUM_VEHICLES_OFFSET.toLong()).coerceIn(0, LmuSharedMemory.MAX_VEHICLES)

        val sp = scoringPointer
        if (sp != null) {
            val sv1 = sp.getInt(VERSION_OFFSET.toLong())
            val sv2 = sp.getInt(VERSION_END_OFFSET.toLong())
            if (sv1 == sv2) {
                scoringVersion = sv1
            }
        }

        return true
    }

    override fun getVehicleTelemetry(index: Int): Rf2VehicleTelemetry? {
        val tp = telemetryPointer ?: return null
        if (index !in 0 until LmuSharedMemory.MAX_VEHICLES) return null

        val offset = Rf2VehicleTelemetry.OFFSET +
            index * Rf2VehicleTelemetry.SIZE

        vehicleTelemetry.attach(tp, offset)
        vehicleTelemetry.read()
        return vehicleTelemetry
    }

    override fun getScoringInfo(): Rf2ScoringInfo? {
        val sp = scoringPointer ?: return null
        scoringInfo.attach(sp, Rf2ScoringInfo.OFFSET)
        scoringInfo.read()
        return scoringInfo
    }

    override fun getVehicleScoring(index: Int): Rf2VehicleScoring? {
        val sp = scoringPointer ?: return null
        if (index !in 0 until LmuSharedMemory.MAX_VEHICLES) return null

        val offset = Rf2VehicleScoring.OFFSET +
            index * Rf2VehicleScoring.SIZE

        vehicleScoring.attach(sp, offset)
        vehicleScoring.read()
        return vehicleScoring
    }

    override fun close() {
        val closingTelemetry = telemetryPointer
        val closingScoring = scoringPointer

        telemetryRegion.close()
        scoringRegion.close()
        telemetryPointer = null
        scoringPointer = null

        if (closingTelemetry != null) {
            LeakCanaryRuntime.watch(closingTelemetry, "LmuRf2PluginSharedMemory.telemetryPointer")
        }
        if (closingScoring != null) {
            LeakCanaryRuntime.watch(closingScoring, "LmuRf2PluginSharedMemory.scoringPointer")
        }
        LeakCanaryRuntime.watch(this, "LmuRf2PluginSharedMemory")
    }

    override fun copyTelemetryBytes(target: ByteArray, offset: Int, expectedVersion: Int): Boolean {
        val tp = telemetryPointer ?: return false
        if (target.size < offset + LmuSharedMemory.TELEMETRY_BUFFER_SIZE) return false

        val v1 = tp.getInt(VERSION_OFFSET.toLong())
        val v2 = tp.getInt(VERSION_END_OFFSET.toLong())
        if (v1 != v2 || v1 != expectedVersion || v1 == 0) return false

        tp.read(0, target, offset, LmuSharedMemory.TELEMETRY_BUFFER_SIZE)

        val vAfter = tp.getInt(VERSION_OFFSET.toLong())
        val vAfterEnd = tp.getInt(VERSION_END_OFFSET.toLong())
        return vAfter == vAfterEnd && vAfter == expectedVersion
    }

    override fun copyScoringBytes(target: ByteArray, offset: Int, expectedVersion: Int?): Boolean {
        val expected = expectedVersion ?: return false
        val sp = scoringPointer ?: return false
        if (target.size < offset + LmuSharedMemory.SCORING_BUFFER_SIZE) return false

        val v1 = sp.getInt(VERSION_OFFSET.toLong())
        val v2 = sp.getInt(VERSION_END_OFFSET.toLong())
        if (v1 != v2 || v1 != expected) return false

        sp.read(0, target, offset, LmuSharedMemory.SCORING_BUFFER_SIZE)

        val vAfter = sp.getInt(VERSION_OFFSET.toLong())
        val vAfterEnd = sp.getInt(VERSION_END_OFFSET.toLong())
        return vAfter == vAfterEnd && vAfter == expected
    }

    private companion object {
        const val VERSION_OFFSET = 0
        const val VERSION_END_OFFSET = 4
        const val NUM_VEHICLES_OFFSET = 12
    }
}
