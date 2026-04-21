package com.project.analyzer.ac.telemetry.impl.internal.poll.backend.ac

import com.project.analyzer.ac.telemetry.impl.internal.poll.backend.AcPollBackend
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcLegacyRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcPollSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout
import com.project.analyzer.ac.telemetry.impl.shm.ac.AcLegacySharedMemoryView
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.toKString

internal class AcLegacyPollBackend(private val view: AcLegacySharedMemoryView) : AcPollBackend {

    private val physicsSize = SPageFilePhysics().size()
    private val graphicsSize = SPageFileGraphics().size()
    private val staticsSize = SPageFileStatic().size()
    private val physicsBuffer = ByteArray(physicsSize)
    private val graphicsBuffer = ByteArray(graphicsSize)
    private val staticsBuffer = ByteArray(staticsSize)

    override val layout: AcSharedMemoryLayout = AcSharedMemoryLayout.LEGACY

    override fun isAnyAttached(): Boolean = true

    override fun hasNativeSignature(): Boolean {
        val statics = view.statics
        if (statics.smVersion.toKString().isNotBlank()) return true
        if (statics.acVersion.toKString().isNotBlank()) return true
        if (statics.track.toKString().isNotBlank()) return true
        if (statics.carModel.toKString().isNotBlank()) return true
        return false
    }

    override fun needsFallback(): Boolean {
        val statics = view.statics
        return statics.track.toKString().isBlank() ||
            statics.carModel.toKString().isBlank() ||
            statics.numCars <= 0 ||
            statics.numberOfSessions <= 0 ||
            statics.sectorCount <= 0
    }

    override fun physicsPacketId(): Int = view.physics.packetId

    override fun graphicsPacketId(): Int = view.graphics.packetId

    override fun graphicsStatus(): Int = view.graphics.status

    override fun graphicsSession(): Int = view.graphics.session

    override fun graphicsCompletedLaps(): Int = view.graphics.completedLaps

    override fun graphicsCurrentTimeMs(): Int = view.graphics.iCurrentTime

    override fun graphicsSessionTimeLeftSec(): Float = view.graphics.sessionTimeLeft

    override fun graphicsDistanceTraveledMeters(): Float = view.graphics.distanceTraveled

    override fun graphicsActiveCars(): Int = view.graphics.activeCars

    override fun graphicsPosition(): Int = view.graphics.position

    override fun graphicsNormalizedCarPosition(): Float = view.graphics.normalizedCarPosition

    override fun graphicsSessionIndex(): Int = view.graphics.sessionIndex

    override fun graphicsSetupMenuVisible(): Boolean = view.graphics.isSetupMenuVisible != 0

    override fun physicsRpm(): Int = view.physics.rpm

    override fun physicsSpeedKmh(): Float = view.physics.speedKmh

    override fun captureInto(snapshot: AcPollSnapshot) {
        val target = snapshot as? AcLegacyRawSnapshot ?: error(
            "Expected AcLegacyRawSnapshot, got ${snapshot::class.simpleName}",
        )
        copyPage(view.physics.pointer, target.physics.pointer, physicsBuffer, physicsSize)
        copyPage(view.graphics.pointer, target.graphics.pointer, graphicsBuffer, graphicsSize)
        copyPage(view.statics.pointer, target.statics.pointer, staticsBuffer, staticsSize)
        target.physics.read()
        target.graphics.read()
        target.statics.read()
    }

    private fun copyPage(source: com.sun.jna.Pointer, target: com.sun.jna.Pointer, buffer: ByteArray, size: Int) {
        source.read(0, buffer, 0, size)
        target.write(0, buffer, 0, size)
    }
}
