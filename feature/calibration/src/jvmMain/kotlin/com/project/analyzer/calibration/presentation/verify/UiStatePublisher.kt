package com.project.analyzer.calibration.presentation.verify

import com.project.analyzer.ac.telemetry.impl.fallback.CarPose
import com.project.analyzer.ac.telemetry.impl.fallback.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.GateCrossingDetector
import com.project.analyzer.calibration.di.OverlayDebugBus
import com.project.analyzer.calibration.presentation.verify.state.CalibrationVerifyState
import com.project.analyzer.calibration.presentation.verify.state.GateDebugInfo
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.math.Vec2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.atan2

class UiStatePublisher(
    private val state: MutableStateFlow<CalibrationVerifyState>,
    private val overlayDebugBus: OverlayDebugBus,
    private val lapAnalyzer: FallbackLapAnalyzer,
    private val gateDetector: GateCrossingDetector
) {

    private val gateCrossedTimes = mutableMapOf<String, Long>()

    fun publish(
        nowNs: Long,
        carPose: CarPose,
        calibration: TrackCalibration,
        speedKmh: Float
    ) {
        val snapshot = lapAnalyzer.getSnapshot(nowNs)

        val headingDeg = if (carPose.headingDir.len() > 0.01f) {
            Math.toDegrees(atan2(carPose.headingDir.x.toDouble(), carPose.headingDir.y.toDouble())).toFloat()
        } else {
            0f
        }

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
                lastS1Ms = snapshot.lastSector1Ms?.toLong(),
                lastS2Ms = snapshot.lastSector2Ms?.toLong(),
                lastS3Ms = snapshot.lastSector3Ms?.toLong(),
                bestS1Ms = snapshot.bestSector1Ms?.toLong(),
                bestS2Ms = snapshot.bestSector2Ms?.toLong(),
                bestS3Ms = snapshot.bestSector3Ms?.toLong(),
                gateDebugInfo = gateInfoList,
                currentPosition = carPose.position,
                currentForward = carPose.headingDir,
                headingDegrees = headingDeg
            )
        }

        overlayDebugBus.update {
            it.copy(
                trackId = calibration.trackId,
                speedKmh = speedKmh,
                carPos = carPose.position,
                carDir = carPose.headingDir,
                gates = calibration.gates.toList(),
                gateInfo = gateInfoList,
                currentLapMs = snapshot.currentLapTimeMs.toLong(),
                currentSectorIndex = snapshot.currentSectorIndex + 1,
                currentSectorMs = snapshot.currentSectorTimeMs.toLong(),
                lastLapMs = snapshot.lastLapTimeMs?.toLong(),
                bestLapMs = snapshot.bestLapTimeMs?.toLong(),
                lastS1Ms = snapshot.lastSector1Ms?.toLong(),
                lastS2Ms = snapshot.lastSector2Ms?.toLong(),
                lastS3Ms = snapshot.lastSector3Ms?.toLong(),
                bestS1Ms = snapshot.bestSector1Ms?.toLong(),
                bestS2Ms = snapshot.bestSector2Ms?.toLong(),
                bestS3Ms = snapshot.bestSector3Ms?.toLong(),
            )
        }
    }

    fun clearGateCrossings() {
        gateCrossedTimes.clear()
    }

    fun markCrossingsUsingDetector(
        prev: CarPose,
        cur: CarPose,
        cal: TrackCalibration,
        currentSectorIndex: Int,
        isLapRunning: Boolean,
        nowMs: Long
    ) {
        fun mark(key: String, gate: Gate, allowed: Boolean) {
            if (!allowed) return
            if (gateCrossedTimes[key] != null) return

            val crossing = gateDetector.detectCrossing(prev, cur, gate)
            if (crossing != null) {
                gateCrossedTimes[key] = nowMs
            }
        }

        mark("SF", cal.startFinish, allowed = true)

        cal.sectors.find { it.index == 1 }?.finish?.let {
            mark("S1_F", it, allowed = isLapRunning && currentSectorIndex >= 1)
        }
        cal.sectors.find { it.index == 2 }?.finish?.let {
            mark("S2_F", it, allowed = isLapRunning && currentSectorIndex >= 2)
        }
    }

    private fun buildGateInfo(carPose: CarPose, cal: TrackCalibration): List<GateDebugInfo> {
        return cal.gates.map { (key, gate) ->
            val (inside, margin, dParallel) = calculateOutOfWidth(carPose.position, gate)
            buildGateDebugInfo(
                name = keyToName(key),
                gate = gate,
                carPosition = carPose.position,
                carForward = carPose.headingDir,
                gateKey = key,
                isInside = inside,
                margin = margin,
                dParallel = dParallel
            )
        }
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
        carPosition: Vec2,
        carForward: Vec2,
        gateKey: String,
        isInside: Boolean,
        margin: Float,
        dParallel: Float
    ): GateDebugInfo {
        val gateCenter = gate.centerV2()
        val gateForward = gate.forwardV2().normalized()
        val dist = (carPosition - gateCenter).len()

        val signedDistFromPlane = (carPosition - gateCenter).dot(gateForward)

        val moveDir = if (carForward.len() > 0.01f) carForward.normalized() else carForward
        val directionDot = moveDir.dot(gateForward)

        val crossed = gateCrossedTimes[gateKey]

        return GateDebugInfo(
            name = name,
            distanceMeters = dist,
            isCrossed = crossed != null,
            lastCrossedTimeMs = crossed,
            gateForward = gateForward,
            directionDot = directionDot,
            signedDistanceFromPlane = signedDistFromPlane,
            isInside = isInside,
            margin = margin,
            dParallel = dParallel,
            gate = gate
        )
    }

    private fun calculateOutOfWidth(carPosition: Vec2, gate: Gate): Triple<Boolean, Float, Float> {
        val delta = carPosition - gate.centerV2()
        val f = gate.forwardV2().safeNormalized()
        val n = gate.normalV2().safeNormalized(f.perpLeft())

        val dParallel = delta.dot(f)
        val dLateral = delta.dot(n)
        val inside = abs(dLateral) <= gate.halfWidthMeters
        val margin = gate.halfWidthMeters - abs(dLateral)

        return Triple(inside, margin, dParallel)
    }

    private val TrackCalibration.gates: Map<String, Gate>
        get() = listOfNotNull(
            "SF" to startFinish,
            sectors.find { it.index == 1 }?.finish?.let { "S1_F" to it },
            sectors.find { it.index == 2 }?.finish?.let { "S2_F" to it },
        ).toMap()
}
