package com.project.analyzer.ac.telemetry.impl.internal

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class AcPollConfig(
    /** Poll interval for active session. 350 Hz ≈ 2.857 ms. */
    val pollIntervalNanos: Long = POLL_350_HZ,
    /** Poll interval when in menu (5 Hz). */
    val menuPollMs: Long = 200.milliseconds.inWholeMilliseconds,
    /** Delay before reconnect after error. */
    val reconnectDelayMs: Long = 2.seconds.inWholeMilliseconds,
    /**
     * Maximum allowed drift before the scheduler resets its target clock.
     * If the loop falls behind by more than this, we snap to `now`
     * instead of trying to "catch up" with a burst of zero-sleep iterations.
     */
    val maxDriftNanos: Long = pollIntervalNanos * 8,
) {

    companion object {

        /** 350 Hz — one tick every ~2.857 ms. */
        val POLL_350_HZ: Long = (1.seconds / 350).inWholeNanoseconds

        /** 60 Hz — one tick every ~16.667 ms. */
        val POLL_60_HZ: Long = (1.seconds / 60).inWholeNanoseconds
    }
}
