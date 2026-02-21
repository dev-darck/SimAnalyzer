package com.project.analyzer.live.domain.usecase

import com.project.analyzer.live.presentation.components.Sector
import com.project.analyzer.live.presentation.components.ValueStatus
import kotlin.time.Duration
import kotlin.time.TimeSource

internal class SectorsFilter(private val holdAfterLap: Duration) {

    private var prevLapCount: Int? = null

    private var lastNonEmpty: List<Sector>? = null

    private var latchedAfterFinish: List<Sector>? = null
    private var holdUntil: TimeSource.Monotonic.ValueTimeMark? = null

    fun filter(current: List<Sector>, lapCount: Int): List<Sector> {
        val now = TimeSource.Monotonic.markNow()

        val prev = prevLapCount
        val lapFinished = (prev != null && lapCount > prev)
        prevLapCount = lapCount

        val currentIsEmpty = isPlaceholder(current)

        if (!currentIsEmpty) {
            lastNonEmpty = current
        }

        if (lapFinished) {
            latchedAfterFinish = if (!currentIsEmpty) current else lastNonEmpty
            holdUntil = now + holdAfterLap
        }

        val until = holdUntil
        val inHoldWindow = (until != null && now < until)

        if (inHoldWindow && currentIsEmpty && latchedAfterFinish != null) {
            return latchedAfterFinish!!
        }

        return current
    }

    private fun isPlaceholder(list: List<Sector>): Boolean = list.all {
        it.value == "--.--" && it.status == ValueStatus.NORMAL
    }
}
