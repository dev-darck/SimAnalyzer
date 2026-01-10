package com.project.analyzer.ac.telemetry.impl.internal

/**
 * Result of each poll iteration
 */
sealed interface PollResult {

    /** Game state changed (connected/disconnected/menu/session) */
    data class StateChanged(
        val state: GameConnectionState,
        val dataSource: DataSourceType = DataSourceType.NATIVE
    ) : PollResult

    /** New telemetry frame available */
    data class Frame(val snapshot: AcRawSnapshot) : PollResult
}
