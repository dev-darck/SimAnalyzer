package com.project.analyzer.ac.telemetry.impl.internal.poll

import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcPollSnapshot

/**
 * Result of each poll iteration
 */
internal sealed interface PollResult {

    /** Game state changed (connected/disconnected/menu/session) */
    data class StateChanged(val state: GameConnectionState, val dataSource: DataSourceType = DataSourceType.NATIVE) :
        PollResult

    /** New telemetry frame available */
    data class Frame(val snapshot: AcPollSnapshot) : PollResult
}
