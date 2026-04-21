package com.project.analyzer.telemetry.lmu.impl.shm

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2ScoringInfo
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleScoring
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2VehicleTelemetry
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
internal class DefaultLmuSharedMemory : LmuSharedMemory {

    private val native = LmuNativeSharedMemory()
    private val plugin = LmuRf2PluginSharedMemory()

    private var active: LmuSharedMemory? = null

    override val telemetryVersion: Int
        get() = active?.telemetryVersion ?: 0
    override val scoringVersion: Int
        get() = active?.scoringVersion ?: 0
    override val numVehicles: Int
        get() = active?.numVehicles ?: 0
    override val playerIndexHint: Int?
        get() = active?.playerIndexHint

    override fun isAttached(): Boolean = active?.isAttached() == true

    override fun readAll(): Boolean {
        val current = active
        if (current != null && current.readAll()) return true

        if (native.readAll()) {
            active = native
            return true
        }

        if (plugin.readAll()) {
            active = plugin
            return true
        }

        active = null
        return false
    }

    override fun getScoringInfo(): Rf2ScoringInfo? = active?.getScoringInfo()

    override fun getVehicleTelemetry(index: Int): Rf2VehicleTelemetry? = active?.getVehicleTelemetry(index)

    override fun getVehicleScoring(index: Int): Rf2VehicleScoring? = active?.getVehicleScoring(index)

    override fun copyTelemetryBytes(target: ByteArray, offset: Int, expectedVersion: Int): Boolean =
        active?.copyTelemetryBytes(target, offset, expectedVersion) == true

    override fun copyScoringBytes(target: ByteArray, offset: Int, expectedVersion: Int?): Boolean =
        active?.copyScoringBytes(target, offset, expectedVersion) == true

    override fun close() {
        native.close()
        plugin.close()
        active = null
        LeakCanaryRuntime.watch(this, "DefaultLmuSharedMemory")
    }
}
