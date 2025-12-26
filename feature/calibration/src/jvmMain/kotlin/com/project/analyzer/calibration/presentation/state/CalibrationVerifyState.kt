package com.project.analyzer.calibration.presentation.state

import com.project.analyzer.calibration.data.model.TrackCalibration

data class CalibrationVerifyState(
    val trackId: String = "",
    val calibration: TrackCalibration? = null,

    val isRunning: Boolean = false,
    val message: String? = null,

    // live
    val lapRunning: Boolean = false,
    val lapIndex: Int = 0,
    val currentLapMs: Long = 0L,
    val currentSectorIndex: Int = 1, // 1..3
    val currentSectorMs: Long = 0L,

    val lastLapMs: Long? = null,
    val bestLapMs: Long? = null,

    val lastS1Ms: Long? = null,
    val lastS2Ms: Long? = null,
    val lastS3Ms: Long? = null,

    val bestS1Ms: Long? = null,
    val bestS2Ms: Long? = null,
    val bestS3Ms: Long? = null,

    val lastEvent: String? = null,
    val events: List<String> = emptyList()
)
