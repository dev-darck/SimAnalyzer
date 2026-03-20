package com.analyzer.session.data.analysis

import com.analyzer.session.data.model.LapSummary

internal class LapBuilder(
    val lap: Int,
    var startNs: Long? = null,
    var endNs: Long? = null,
    val sectorTimes: MutableMap<Int, Long> = mutableMapOf(),
    var invalid: Boolean = false,
    var inPit: Boolean = false,
) {

    fun toSummary(): LapSummary {
        val start = startNs
        val end = endNs
        val sectorTimesMs = (0..2).map { index ->
            sectorTimes[index]?.let { (it / 1_000_000L).toInt() }
        }
        val hasFullSectorSet = sectorTimesMs.all { it != null }
        val complete = start != null && end != null && hasFullSectorSet
        val totalMs = if (complete) {
            ((end - start) / 1_000_000L).toInt()
        } else {
            null
        }

        return LapSummary(
            lap = lap,
            sessionType = null,
            totalTimeMs = totalMs,
            sectorTimesMs = sectorTimesMs,
            invalid = invalid,
            inPit = inPit,
            complete = complete,
        )
    }
}
