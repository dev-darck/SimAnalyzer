package com.project.analyzer.telemetry.recording.impl.file.model

internal class NsRateLimiter(private val intervalNs: Long) {

    private var nextNs: Long = 0L

    fun shouldLog(nowNs: Long): Boolean {
        if (nowNs >= nextNs) {
            nextNs = nowNs + intervalNs
            return true
        }
        return false
    }
}
