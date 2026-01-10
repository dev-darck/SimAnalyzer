package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.api.di.SessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
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
