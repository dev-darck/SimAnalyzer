package com.analyzer.session.data.repository.impl

import com.analyzer.session.data.model.LapSummary
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndexFlags
import kotlin.math.absoluteValue

internal data class IndexAnalysis(
    val laps: List<LapSummary>,
    val distanceKm: Double,
)

internal data class IndexRecord(
    val timestampNs: Long,
    val speedKmh: Float?,
    val lap: Int,
    val sector: Int,
    val flags: Int,
)

internal class IndexAnalyzer {

    private val lapBuilders = mutableMapOf<Int, LapBuilder>()
    private var currentLap: Int? = null
    private var currentSector: Int? = null
    private var currentSectorStartNs: Long = 0L

    private var prevTimestampNs: Long? = null
    private var prevSpeedKmh: Float? = null
    private var distanceKm = 0.0

    fun consume(record: IndexRecord) {
        distanceKm += integrateDistance(prevTimestampNs, prevSpeedKmh, record.timestampNs, record.speedKmh)
        prevTimestampNs = record.timestampNs
        prevSpeedKmh = record.speedKmh

        val lap = normalizeLap(record.lap) ?: return
        val builder = lapBuilders.getOrPut(lap) { LapBuilder(lap) }
        if (builder.startNs == null) {
            builder.startNs = record.timestampNs
        }

        if (record.flags and TelemetryFrameIndexFlags.INVALID_LAP != 0) {
            builder.invalid = true
        }
        if (record.flags and (TelemetryFrameIndexFlags.IN_PIT or TelemetryFrameIndexFlags.IN_PIT_LANE) != 0) {
            builder.inPit = true
        }

        if (currentLap == null) {
            currentLap = lap
            currentSector = normalizeSector(record.sector)
            currentSectorStartNs = record.timestampNs
            return
        }

        if (lap != currentLap) {
            val prevLapBuilder = lapBuilders[currentLap]
            if (prevLapBuilder != null) {
                val sector = currentSector
                if (sector != null && !prevLapBuilder.sectorTimes.containsKey(sector)) {
                    prevLapBuilder.sectorTimes[sector] = record.timestampNs - currentSectorStartNs
                }
                prevLapBuilder.endNs = record.timestampNs
            }
            currentLap = lap
            currentSector = normalizeSector(record.sector)
            currentSectorStartNs = record.timestampNs
            return
        }

        val normalizedSector = normalizeSector(record.sector)
        if (normalizedSector != null && normalizedSector != currentSector) {
            val sector = currentSector
            if (sector != null && !builder.sectorTimes.containsKey(sector)) {
                builder.sectorTimes[sector] = record.timestampNs - currentSectorStartNs
            }
            currentSector = normalizedSector
            currentSectorStartNs = record.timestampNs
        }
    }

    fun build(): IndexAnalysis {
        val laps = lapBuilders.values
            .sortedBy { it.lap }
            .map { builder -> builder.toSummary() }
        return IndexAnalysis(laps, distanceKm)
    }

    private fun integrateDistance(
        prevTimestampNs: Long?,
        prevSpeedKmh: Float?,
        timestampNs: Long,
        speedKmh: Float?,
    ): Double {
        if (prevTimestampNs == null || prevSpeedKmh == null || speedKmh == null) return 0.0
        val dtSec = (timestampNs - prevTimestampNs).toDouble() / 1_000_000_000.0
        if (dtSec <= 0.0) return 0.0
        val avgSpeed = ((prevSpeedKmh + speedKmh) * 0.5f).absoluteValue
        if (!avgSpeed.isFinite()) return 0.0
        return avgSpeed * dtSec / 3600.0
    }

    private fun normalizeLap(lap: Int): Int? = lap.takeIf { it > 0 }

    private fun normalizeSector(sector: Int): Int? = when {
        sector in 0..2 -> sector
        sector == 3 -> 2
        else -> null
    }
}

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
        val complete = start != null && end != null
        val totalMs = if (complete) {
            ((end - start) / 1_000_000L).toInt()
        } else null

        val sectorTimesMs = (0..2).map { index ->
            sectorTimes[index]?.let { (it / 1_000_000L).toInt() }
        }

        return LapSummary(
            lap = lap,
            totalTimeMs = totalMs,
            sectorTimesMs = sectorTimesMs,
            invalid = invalid,
            inPit = inPit,
            complete = complete,
        )
    }
}
