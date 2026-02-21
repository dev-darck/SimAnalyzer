package com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model

data class LapTimingSnapshot(
    val isActive: Boolean,
    val trackId: String?,
    val isLapRunning: Boolean,
    val completedLapsCount: Int,
    val currentLapTimeMs: Int,
    val currentSectorTimeMs: Int,
    /** 0..(sectorCount-1) */
    val currentSectorIndex: Int,
    val lastSectorTimeMs: Int?,
    val lastLapTimeMs: Int?,
    /** Best VALID lap time (ms). */
    val bestLapTimeMs: Int?,

    /** Per-sector times for the last completed lap (ms). Size == sectorCount, values may be null. */
    val lastSectorsMs: List<Int?> = emptyList(),
    /** Best VALID per-sector times (ms). Size == sectorCount, values may be null. */
    val bestSectorsMs: List<Int?> = emptyList(),

    val currentLapValid: Boolean = true,

    /** Current lap delta vs bestLapTimeMs (ms). */
    val deltaLapTimeMs: Int? = null,
    val isDeltaPositive: Boolean = true,

    val startFinishSyncId: Int,
)
