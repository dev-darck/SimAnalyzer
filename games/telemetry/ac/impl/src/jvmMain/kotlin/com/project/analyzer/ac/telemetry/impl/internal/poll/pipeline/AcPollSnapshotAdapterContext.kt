package com.project.analyzer.ac.telemetry.impl.internal.poll.pipeline

import com.project.analyzer.ac.telemetry.impl.fallback.AcFallbackPatchMemory
import com.project.analyzer.ac.telemetry.impl.internal.poll.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot

internal interface AcPollSnapshotAdapterContext {

    val snapshot: AceRawSnapshot
    val gameState: GameConnectionState
    val loopStartNanos: Long
    val shouldApplyFallbackPatch: Boolean

    fun withFallbackMemory(block: (AcFallbackPatchMemory) -> Unit)
}
