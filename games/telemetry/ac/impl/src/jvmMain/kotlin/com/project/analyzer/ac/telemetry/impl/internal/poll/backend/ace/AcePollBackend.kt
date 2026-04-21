package com.project.analyzer.ac.telemetry.impl.internal.poll.backend.ace

import com.project.analyzer.ac.telemetry.impl.internal.poll.backend.AcPollBackend
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcPollSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ace.AceSharedMemoryView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoSessionType
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStaticPageView
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStatus

internal class AcePollBackend(private val view: AceSharedMemoryView) : AcPollBackend {

    private val physicsBuffer = ByteArray(SPageFilePhysics().size())
    private val graphicsBuffer = ByteArray(view.graphics.size())
    private val staticsBuffer = ByteArray(AcEvoStaticPageView.SIZE_BYTES)

    override val layout: AcSharedMemoryLayout = AcSharedMemoryLayout.ACEVO

    override fun isAnyAttached(): Boolean = true

    override fun hasNativeSignature(): Boolean {
        if (view.statics.smVersion.isNotBlank()) return true
        if (view.statics.acEvoVersion.isNotBlank()) return true
        if (view.statics.track.isNotBlank()) return true
        if (view.graphics.carModel.isNotBlank()) return true
        return false
    }

    override fun needsFallback(): Boolean = view.statics.track.isBlank() ||
        view.graphics.carModel.isBlank() ||
        maxOf(view.graphics.activeCars, view.graphics.totalDrivers.toInt()) <= 0 ||
        view.statics.numberOfSessions <= 0

    override fun physicsPacketId(): Int = view.physics.packetId

    override fun graphicsPacketId(): Int = view.graphics.packetId

    override fun graphicsStatus(): Int = view.graphics.status?.rawValue ?: 0

    override fun graphicsSession(): Int = when (view.statics.session) {
        AcEvoSessionType.TIME_ATTACK -> 4
        AcEvoSessionType.RACE -> 2
        AcEvoSessionType.HOT_STINT -> 3
        AcEvoSessionType.CRUISE -> 0
        AcEvoSessionType.UNKNOWN -> -1
    }

    override fun graphicsCompletedLaps(): Int = view.graphics.totalLapCount

    override fun graphicsCurrentTimeMs(): Int = view.graphics.currentLapTimeMs

    override fun graphicsSessionTimeLeftSec(): Float = view.graphics.sessionState.timeLeftMs.coerceAtLeast(0) / 1000f

    override fun graphicsDistanceTraveledMeters(): Float = view.graphics.currentKm.coerceAtLeast(0f) * 1000f

    override fun graphicsActiveCars(): Int = maxOf(view.graphics.activeCars, view.graphics.totalDrivers.toInt())

    override fun graphicsPosition(): Int = view.graphics.currentPos.toInt()

    override fun graphicsNormalizedCarPosition(): Float = view.graphics.npos

    override fun graphicsSessionIndex(): Int =
        ((view.statics.eventId and 0xFF) shl 8) or (view.statics.sessionId and 0xFF)

    override fun graphicsSetupMenuVisible(): Boolean =
        view.graphics.status != AcEvoStatus.LIVE && view.graphics.sessionState.uiEnableSetup

    override fun physicsRpm(): Int = view.physics.rpm

    override fun physicsSpeedKmh(): Float = view.physics.speedKmh

    override fun captureInto(snapshot: AcPollSnapshot) {
        val target = snapshot as? AceRawSnapshot ?: error("Expected AceRawSnapshot, got ${snapshot::class.simpleName}")
        copyPage(view.physics.pointer, target.physics.pointer, physicsBuffer)
        copyPage(view.graphics.pointer, target.graphics.pointer, graphicsBuffer)
        copyPage(view.statics.pointer, target.statics.pointer, staticsBuffer)
        target.physics.read()
        target.graphics.read()
        target.statics.read()
    }

    private fun copyPage(source: com.sun.jna.Pointer, target: com.sun.jna.Pointer, buffer: ByteArray) {
        source.read(0, buffer, 0, buffer.size)
        target.write(0, buffer, 0, buffer.size)
    }
}
