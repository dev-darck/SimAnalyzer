package com.project.analyzer.telemetry.recording.api.recording

public class TelemetrySamplingGate(initialRateHz: Int) {

    @Volatile
    private var intervalNs: Long = if (initialRateHz <= 0) 0L else 1_000_000_000L / initialRateHz
    private var nextNs: Long = 0L

    public fun updateRate(rateHz: Int) {
        intervalNs = if (rateHz <= 0) 0L else 1_000_000_000L / rateHz
        nextNs = 0L
    }

    public fun shouldSample(nowNs: Long): Boolean {
        val interval = intervalNs
        if (interval <= 0L) return true
        if (nowNs >= nextNs) {
            nextNs = nowNs + interval
            return true
        }
        return false
    }
}
