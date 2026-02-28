package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic

class AcRawSnapshot(
    val physics: SPageFilePhysics,
    val graphics: SPageFileGraphics,
    val statics: SPageFileStatic,
    var timestampNs: Long = 0L,
) {

    var frameId: Long = 0L
        internal set

    var sessionRestartHint: AcSessionRestartHint = AcSessionRestartHint.NONE
}
