package com.project.analyzer.calibration.presentation.verify

import com.project.analyzer.calibration.di.OverlayDebugBus
import com.project.analyzer.calibration.presentation.model.toUi
import com.project.analyzer.calibration.presentation.verify.state.CalibrationVerifyState
import com.project.analyzer.calibration.presentation.verify.state.GateDebugInfo
import com.project.analyzer.math.Vec2
import com.project.analyzer.math.headingDegreesOrZero
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationCarPose
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationDebugGateDetector
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationDebugLapAnalyzer
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

internal class UiStatePublisher(
    private val state: MutableStateFlow<CalibrationVerifyState>,
    private val overlayDebugBus: OverlayDebugBus,
    private val lapAnalyzer: AcCalibrationDebugLapAnalyzer,
    private val gateDetector: AcCalibrationDebugGateDetector,
) {

    private val gateCrossedTimes = mutableMapOf<String, Long>()

    fun publish(nowNs: Long, carPose: AcCalibrationCarPose, calibration: TrackCalibration, speedKmh: Float) {
        val snapshot = lapAnalyzer.getSnapshot(nowNs)

        val headingDeg = carPose.headingDir.headingDegreesOrZero()

        val gateInfoList = buildGateInfo(carPose, calibration)

        state.update {
            it.copy(
                lapRunning = snapshot.isLapRunning,
                lapIndex = snapshot.completedLapsCount,
                currentLapMs = snapshot.currentLapTimeMs.toLong(),
                currentSectorIndex = snapshot.currentSectorIndex + 1,
                currentSectorMs = snapshot.currentSectorTimeMs.toLong(),
                lastLapMs = snapshot.lastLapTimeMs?.toLong(),
                bestLapMs = snapshot.bestLapTimeMs?.toLong(),
                lastS1Ms = snapshot.lastSectorsMs[0]?.toLong(),
                lastS2Ms = snapshot.lastSectorsMs[1]?.toLong(),
                lastS3Ms = snapshot.lastSectorsMs[2]?.toLong(),
                bestS1Ms = snapshot.bestSectorsMs[0]?.toLong(),
                bestS2Ms = snapshot.bestSectorsMs[1]?.toLong(),
                bestS3Ms = snapshot.bestSectorsMs[2]?.toLong(),
                gateDebugInfo = gateInfoList.toPersistentList(),
                currentPosition = carPose.position,
                currentForward = carPose.headingDir,
                headingDegrees = headingDeg,
            )
        }

        overlayDebugBus.update {
            it.copy(
                trackId = calibration.trackId,
                speedKmh = speedKmh,
                carPos = carPose.position,
                carDir = carPose.headingDir,
                gates = calibration.gates.mapValues { (_, gate) -> gate.toUi() }.toList(),
                gateInfo = gateInfoList,
                currentLapMs = snapshot.currentLapTimeMs.toLong(),
                currentSectorIndex = snapshot.currentSectorIndex + 1,
                currentSectorMs = snapshot.currentSectorTimeMs.toLong(),
                lastLapMs = snapshot.lastLapTimeMs?.toLong(),
                bestLapMs = snapshot.bestLapTimeMs?.toLong(),
                lastS1Ms = snapshot.lastSectorsMs[0]?.toLong(),
                lastS2Ms = snapshot.lastSectorsMs[1]?.toLong(),
                lastS3Ms = snapshot.lastSectorsMs[2]?.toLong(),
                bestS1Ms = snapshot.bestSectorsMs[0]?.toLong(),
                bestS2Ms = snapshot.bestSectorsMs[1]?.toLong(),
                bestS3Ms = snapshot.bestSectorsMs[2]?.toLong(),
            )
        }
    }

    fun clearGateCrossings() {
        gateCrossedTimes.clear()
    }

    fun markCrossingsUsingDetector(
        prev: AcCalibrationCarPose,
        cur: AcCalibrationCarPose,
        cal: TrackCalibration,
        context: CrossingContext,
    ) {
        fun mark(key: String, gate: Gate, allowed: Boolean) {
            if (!allowed) return
            if (gateCrossedTimes[key] != null) return

            if (gateDetector.hasCrossing(prev, cur, gate)) {
                gateCrossedTimes[key] = context.nowMs
            }
        }

        mark("SF", cal.startFinish, allowed = true)

        cal.sectors.find { it.index == 1 }?.finish?.let {
            mark("S1_F", it, allowed = context.isLapRunning && context.currentSectorIndex >= 1)
        }
        cal.sectors.find { it.index == 2 }?.finish?.let {
            mark("S2_F", it, allowed = context.isLapRunning && context.currentSectorIndex >= 2)
        }
    }

    private fun buildGateInfo(carPose: AcCalibrationCarPose, cal: TrackCalibration): List<GateDebugInfo> =
        cal.gates.map { (key, gate) ->
            val metrics = calculateGateMetrics(carPose.position, gate)
            buildGateDebugInfo(
                name = keyToName(key),
                gate = gate,
                carForward = carPose.headingDir,
                gateKey = key,
                metrics = metrics,
            )
        }

    private fun keyToName(key: String): String = when (key) {
        "SF" -> "Start/Finish"
        "S1_F" -> "S1 Finish (Split 1)"
        "S2_F" -> "S2 Finish (Split 2)"
        else -> key
    }

    private fun buildGateDebugInfo(
        name: String,
        gate: Gate,
        carForward: Vec2,
        gateKey: String,
        metrics: GateDebugMetrics,
    ): GateDebugInfo {
        val moveDir = if (carForward.len() > 0.01f) carForward.normalized() else carForward
        val directionDot = moveDir.dot(metrics.gateForward)

        val crossed = gateCrossedTimes[gateKey]

        return GateDebugInfo(
            name = name,
            distanceMeters = metrics.distanceMeters,
            isCrossed = crossed != null,
            lastCrossedTimeMs = crossed,
            gateForward = metrics.gateForward,
            directionDot = directionDot,
            signedDistanceFromPlane = metrics.signedDistanceFromPlane,
            isInside = metrics.isInside,
            margin = metrics.margin,
            dParallel = metrics.dParallel,
        )
    }

    private fun calculateGateMetrics(carPosition: Vec2, gate: Gate): GateDebugMetrics {
        val axes = gate.toDebugAxes()
        val delta = carPosition - axes.center
        val dParallel = delta.dot(axes.forward)
        val dLateral = delta.dot(axes.normal)
        return GateDebugMetrics(
            distanceMeters = delta.len(),
            signedDistanceFromPlane = delta.dot(axes.forward),
            gateForward = axes.forward,
            isInside = abs(dLateral) <= gate.halfWidthMeters,
            margin = gate.halfWidthMeters - abs(dLateral),
            dParallel = dParallel,
        )
    }

    private fun Gate.toDebugAxes(): GateDebugAxes {
        val forward = forwardV2().safeNormalized()
        return GateDebugAxes(
            center = centerV2(),
            forward = forward,
            normal = normalV2().safeNormalized(forward.perpLeft()),
        )
    }

    private val TrackCalibration.gates: Map<String, Gate>
        get() = listOfNotNull(
            "SF" to startFinish,
            sectors.find { it.index == 1 }?.finish?.let { "S1_F" to it },
            sectors.find { it.index == 2 }?.finish?.let { "S2_F" to it },
        ).toMap()

    internal data class CrossingContext(val currentSectorIndex: Int, val isLapRunning: Boolean, val nowMs: Long)

    private data class GateDebugAxes(val center: Vec2, val forward: Vec2, val normal: Vec2)

    private data class GateDebugMetrics(
        val distanceMeters: Float,
        val signedDistanceFromPlane: Float,
        val gateForward: Vec2,
        val isInside: Boolean,
        val margin: Float,
        val dParallel: Float,
    )
}

@Inject
internal class UiStatePublisherFactory(
    private val overlayDebugBus: OverlayDebugBus,
    private val gateDetector: AcCalibrationDebugGateDetector,
) {

    fun create(
        state: MutableStateFlow<CalibrationVerifyState>,
        lapAnalyzer: AcCalibrationDebugLapAnalyzer,
    ): UiStatePublisher = UiStatePublisher(
        state = state,
        overlayDebugBus = overlayDebugBus,
        lapAnalyzer = lapAnalyzer,
        gateDetector = gateDetector,
    )
}
