package com.project.analyzer.ac.telemetry.impl.internal.poll.backend

import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcPollSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout

internal interface AcPollBackend {

    val layout: AcSharedMemoryLayout

    fun isAnyAttached(): Boolean

    fun hasNativeSignature(): Boolean

    fun needsFallback(): Boolean

    fun physicsPacketId(): Int

    fun graphicsPacketId(): Int

    fun graphicsStatus(): Int

    fun graphicsSession(): Int

    fun graphicsCompletedLaps(): Int

    fun graphicsCurrentTimeMs(): Int

    fun graphicsSessionTimeLeftSec(): Float

    fun graphicsDistanceTraveledMeters(): Float

    fun graphicsActiveCars(): Int

    fun graphicsPosition(): Int

    fun graphicsNormalizedCarPosition(): Float

    fun graphicsSessionIndex(): Int

    fun graphicsSetupMenuVisible(): Boolean

    fun physicsRpm(): Int

    fun physicsSpeedKmh(): Float

    fun captureInto(snapshot: AcPollSnapshot)

    fun hasGraphicsSignal(): Boolean {
        if (graphicsPacketId() > 0) return true
        if (graphicsStatus() != 0) return true
        if (graphicsCurrentTimeMs() > 0) return true
        if (graphicsCompletedLaps() > 0) return true
        if (graphicsSessionTimeLeftSec() > 0f) return true
        if (graphicsDistanceTraveledMeters() > 1f) return true
        if (graphicsActiveCars() > 0) return true
        if (graphicsPosition() > 0) return true
        if (graphicsNormalizedCarPosition() > 0f) return true

        return false
    }
}
