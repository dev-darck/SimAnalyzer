package com.project.analyzer.ac.telemetry.impl.internal.pipeline

import com.project.analyzer.ac.telemetry.impl.internal.AcRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory

internal interface AcPollSnapshotAdapterContext {

    val snapshot: AcRawSnapshot
    val gameState: GameConnectionState
    val loopStartNanos: Long

    fun withSharedMemory(block: (AcSharedMemory) -> Unit)
}
