package com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model

data class LapTimingSnapshot(
    val isActive: Boolean,
    val trackId: String?,
    val isLapRunning: Boolean,
    val completedLapsCount: Int,
    val currentLapTimeMs: Int,
    val currentSectorTimeMs: Int,
    val currentSectorIndex: Int,
    val lastSectorTimeMs: Int?,
    val lastLapTimeMs: Int?,
    val bestLapTimeMs: Int?,
    val lastSector1Ms: Int? = null,
    val lastSector2Ms: Int? = null,
    val lastSector3Ms: Int? = null,
    val bestSector1Ms: Int? = null,
    val bestSector2Ms: Int? = null,
    val bestSector3Ms: Int? = null,

    val currentLapValid: Boolean = true,
    val lastLapValid: Boolean = true,
    val bestValidLapTimeMs: Int? = null,

    val deltaLapTimeMs: Int? = null,
    val isDeltaPositive: Boolean = true,

    val startFinishSyncId: Int
)
