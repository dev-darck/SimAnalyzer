package com.project.analyzer.telemetry.lmu.impl.shm

import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2ScoringInfo
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleScoring
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleTelemetry
import java.io.Closeable

internal interface LmuSharedMemory : Closeable {

    fun isAttached(): Boolean
    fun readAll(): Boolean

    val telemetryVersion: Int
    val scoringVersion: Int
    val numVehicles: Int
    val playerIndexHint: Int?

    fun getScoringInfo(): Rf2ScoringInfo?
    fun getVehicleTelemetry(index: Int): Rf2VehicleTelemetry?
    fun getVehicleScoring(index: Int): Rf2VehicleScoring?

    fun copyTelemetryBytes(target: ByteArray, offset: Int, expectedVersion: Int): Boolean
    fun copyScoringBytes(target: ByteArray, offset: Int, expectedVersion: Int?): Boolean

    companion object {

        const val MAX_VEHICLES: Int = 128
        const val TELEMETRY_BUFFER_SIZE: Int =
            Rf2VehicleTelemetry.OFFSET + Rf2VehicleTelemetry.SIZE * MAX_VEHICLES
        const val SCORING_BUFFER_SIZE: Int =
            Rf2VehicleScoring.OFFSET + Rf2VehicleScoring.SIZE * MAX_VEHICLES
    }
}
