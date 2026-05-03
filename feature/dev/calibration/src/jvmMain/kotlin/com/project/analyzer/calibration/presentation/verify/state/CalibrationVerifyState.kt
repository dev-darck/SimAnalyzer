package com.project.analyzer.calibration.presentation.verify.state

import com.project.analyzer.math.Vec2
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

data class CalibrationTrackUi(val trackId: String, val trackName: String)

data class GateDebugInfo(
    val name: String,
    val distanceMeters: Float,
    val isCrossed: Boolean = false,
    val lastCrossedTimeMs: Long? = null,
    val gateForward: Vec2? = null,
    val directionDot: Float? = null,
    val signedDistanceFromPlane: Float? = null,
    val isInside: Boolean,
    val margin: Float,
    val dParallel: Float,
)

enum class EditingGate {
    START_FINISH,
    SECTOR_1_FINISH,
    SECTOR_2_FINISH,
}

data class CalibrationVerifyState(
    val trackId: String = "",
    val calibration: CalibrationTrackUi? = null,

    val isRunning: Boolean = false,
    val message: String? = null,

    val lapRunning: Boolean = false,
    val lapIndex: Int = 0,
    val currentLapMs: Long = 0L,
    val currentSectorIndex: Int = 1,
    val currentSectorMs: Long = 0L,
    val halfWidthMeters: Float = 0F,

    val lastLapMs: Long? = null,
    val bestLapMs: Long? = null,

    val lastS1Ms: Long? = null,
    val lastS2Ms: Long? = null,
    val lastS3Ms: Long? = null,

    val bestS1Ms: Long? = null,
    val bestS2Ms: Long? = null,
    val bestS3Ms: Long? = null,

    val lastEvent: String? = null,
    val events: List<String> = emptyList(),
    val debugTelemetry: String? = null,

    val gateDebugInfo: PersistentList<GateDebugInfo> = persistentListOf(),

    val editingGate: EditingGate? = null,
    val isCapturing: Boolean = false,

    val currentPosition: Vec2? = null,
    val currentForward: Vec2? = null,
    val speedKmh: Float = 0f,
    val headingDegrees: Float = 0f,
)
