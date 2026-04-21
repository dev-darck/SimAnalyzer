package com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot

import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemoryLayout

public sealed interface AcPollSnapshot {

    val layout: AcSharedMemoryLayout

    var timestampNs: Long

    var frameId: Long

    var sessionRestartHint: AcSessionRestartHint
}
