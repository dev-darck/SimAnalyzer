package com.project.analyzer.calibration.presentation.overlay.state

import com.project.analyzer.calibration.presentation.model.CalibrationGateUi
import com.project.analyzer.calibration.presentation.verify.state.GateDebugInfo
import com.project.analyzer.math.Vec2

data class CapturePoint(val position: Vec2, val forward: Vec2, val label: String)

data class OverlayDebugState(
    val trackId: String? = null,
    val speedKmh: Float? = null,
    val carPos: Vec2? = null,
    val carDir: Vec2? = null,
    val gates: List<Pair<String, CalibrationGateUi>> = emptyList(),
    val gateInfo: List<GateDebugInfo> = emptyList(),

    val lastCapturePoint: CapturePoint? = null,
    val pendingCapturePosition: Vec2? = null,

    val currentLapMs: Long? = null,
    val lastLapMs: Long? = null,
    val bestLapMs: Long? = null,

    val currentSectorIndex: Int? = null,
    val currentSectorMs: Long? = null,

    val lastS1Ms: Long? = null,
    val bestS1Ms: Long? = null,
    val lastS2Ms: Long? = null,
    val bestS2Ms: Long? = null,
    val lastS3Ms: Long? = null,
    val bestS3Ms: Long? = null,
)
