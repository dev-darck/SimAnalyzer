package com.project.analyzer.live.domain.usecase

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class GearFilter(private val confirmNonForwardFor: Duration) {

    private var lastStableRaw: Int = 1
    private var pendingNonForwardRaw: Int? = null
    private var pendingSince: TimeMark? = null

    fun filter(raw: Int): Int {
        if (raw >= 2) {
            lastStableRaw = raw
            pendingNonForwardRaw = null
            pendingSince = null
            return raw
        }

        if (lastStableRaw == raw) {
            pendingNonForwardRaw = null
            pendingSince = null
            return raw
        }

        if (pendingNonForwardRaw != raw) {
            pendingNonForwardRaw = raw
            pendingSince = TimeSource.Monotonic.markNow()
            return lastStableRaw
        }

        val elapsed = pendingSince?.elapsedNow() ?: Duration.ZERO
        return if (elapsed >= confirmNonForwardFor) {
            lastStableRaw = raw
            pendingNonForwardRaw = null
            pendingSince = null
            raw
        } else {
            lastStableRaw
        }
    }
}
