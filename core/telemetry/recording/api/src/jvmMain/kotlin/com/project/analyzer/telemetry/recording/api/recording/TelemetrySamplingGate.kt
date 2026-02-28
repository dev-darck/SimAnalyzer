package com.project.analyzer.telemetry.recording.api.recording

import java.util.concurrent.atomic.AtomicLong

public class TelemetrySamplingGate(initialRateHz: Int) {

    private val intervalNs = AtomicLong(intervalForRate(initialRateHz))
    private val nextNs = AtomicLong(0L)

    public fun updateRate(rateHz: Int) {
        intervalNs.set(intervalForRate(rateHz))
        nextNs.set(0L)
    }

    public fun shouldSample(nowNs: Long): Boolean {
        val interval = intervalNs.get()
        if (interval <= 0L) return true
        while (true) {
            val next = nextNs.get()
            if (next == 0L) {
                if (nextNs.compareAndSet(0L, nowNs + interval)) {
                    return true
                }
                continue
            }
            if (nowNs < next) return false
            val intervalsMissed = ((nowNs - next) / interval) + 1L
            val nextScheduled = next + (intervalsMissed * interval)
            if (nextNs.compareAndSet(next, nextScheduled)) {
                return true
            }
        }
    }

    private companion object {
        fun intervalForRate(rateHz: Int): Long = if (rateHz <= 0) 0L else 1_000_000_000L / rateHz
    }
}
