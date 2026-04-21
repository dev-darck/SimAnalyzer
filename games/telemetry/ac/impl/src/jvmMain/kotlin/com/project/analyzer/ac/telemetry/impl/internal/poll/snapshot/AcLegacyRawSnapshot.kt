package com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot

import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic

internal class AcLegacyRawSnapshot(
    val physics: SPageFilePhysics,
    val graphics: SPageFileGraphics,
    val statics: SPageFileStatic,
    override var timestampNs: Long = 0L,
) : AcPollSnapshot {

    override val layout: AcSharedMemoryLayout = AcSharedMemoryLayout.LEGACY

    override var frameId: Long = 0L

    override var sessionRestartHint: AcSessionRestartHint = AcSessionRestartHint.NONE
}
