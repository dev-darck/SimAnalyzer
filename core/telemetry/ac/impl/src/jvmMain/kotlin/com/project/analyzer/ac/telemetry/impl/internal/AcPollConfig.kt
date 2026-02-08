package com.project.analyzer.ac.telemetry.impl.internal

data class AcPollConfig(
    /** Poll interval for active session (~60Hz) */
    val pollIntervalNanos: Long = 16_666_667L,
    /** Poll interval when game is not running (1 second) */
    val gameNotRunningPollMs: Long = 1000L,
    /** Poll interval when in menu (200ms = 5Hz) */
    val menuPollMs: Long = 200L,
    /** Delay before reconnect after error */
    val reconnectDelayMs: Long = 2000L,
)
