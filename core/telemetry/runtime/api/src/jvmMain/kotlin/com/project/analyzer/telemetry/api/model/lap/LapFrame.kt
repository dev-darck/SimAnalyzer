package com.project.analyzer.telemetry.api.model.lap

import com.project.analyzer.telemetry.api.contract.LapValidity

public data class LapFrame(
    val currentLapIndex: Int? = null, // 1-based
    val completedLaps: Int? = null,

    val currentLapTimeMs: Int? = null,
    val lastLapTimeMs: Int? = null,
    val bestLapTimeMs: Int? = null,
    val currentLapTimeLabel: String? = null,
    val lastLapTimeLabel: String? = null,
    val bestLapTimeLabel: String? = null,

    val sectorCount: Int? = null,
    val currentSectorIndex: Int? = null, // 0..sectorCount-1
    val lastSectorTimeMs: Int? = null,

    // Delta
    val deltaLapTimeMs: Int? = null,
    val isDeltaPositive: Boolean? = null,
    val estimatedLapTimeMs: Int? = null,
    val deltaLapTimeLabel: String? = null,
    val deltaLastLapTimeLabel: String? = null,
    val isDeltaLastPositive: Boolean? = null,
    val estimatedLapTimeLabel: String? = null,
    val totalTimeLabel: String? = null,

    // Split
    val splitTimeMs: Int? = null,

    val validity: LapValidity? = null,

    val sectors: List<SectorFrame> = emptyList(),
)
