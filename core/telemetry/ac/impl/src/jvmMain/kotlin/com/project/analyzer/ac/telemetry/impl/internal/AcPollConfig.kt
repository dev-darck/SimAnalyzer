package com.project.analyzer.ac.telemetry.impl.internal

data class AcPollConfig(
    val targetHz: Int = 360,
    val reconnectDelayMs: Long = 1000,
    val gameNotRunningPollMs: Long = 500,
) {
    val pollIntervalNanos: Long = 1_000_000_000L / targetHz
}
