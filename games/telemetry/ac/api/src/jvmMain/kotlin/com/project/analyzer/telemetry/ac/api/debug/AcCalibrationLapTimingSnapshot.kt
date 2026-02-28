package com.project.analyzer.telemetry.ac.api.debug

public data class AcCalibrationLapTimingSnapshot(
    public val isActive: Boolean,
    public val trackId: String?,
    public val isLapRunning: Boolean,
    public val completedLapsCount: Int,
    public val currentLapTimeMs: Int,
    public val currentSectorTimeMs: Int,
    public val currentSectorIndex: Int,
    public val lastSectorTimeMs: Int?,
    public val lastLapTimeMs: Int?,
    public val bestLapTimeMs: Int?,
    public val lastSectorsMs: List<Int?> = emptyList(),
    public val bestSectorsMs: List<Int?> = emptyList(),
    public val currentLapValid: Boolean = true,
    public val deltaLapTimeMs: Int? = null,
    public val isDeltaPositive: Boolean = true,
    public val startFinishSyncId: Int,
)
