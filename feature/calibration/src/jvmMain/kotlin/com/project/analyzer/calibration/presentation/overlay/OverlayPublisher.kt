package com.project.analyzer.calibration.presentation.overlay

import com.project.analyzer.calibration.di.OverlayDebugBus
import com.project.analyzer.calibration.presentation.overlay.state.CapturePoint
import com.project.analyzer.calibration.presentation.verify.state.GateDebugInfo
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.math.Vec2
import kotlin.math.abs

class OverlayPublisher(
    private val overlayDebugBus: OverlayDebugBus
) {

    data class Timing(
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

    fun publish(
        trackId: String?,
        speedKmh: Float?,
        carPos: Vec2?,
        carDir: Vec2?,
        gates: Map<String, Gate>,
        gateInfoOverride: List<GateDebugInfo>? = null,
        timing: Timing? = null,
        lastCapturePoint: CapturePoint? = null,
        pendingCapturePosition: Vec2? = null,
    ) {
        val gateInfo = gateInfoOverride ?: buildGateInfo(carPos, carDir, gates)

        overlayDebugBus.update {
            it.copy(
                trackId = trackId,
                speedKmh = speedKmh,
                carPos = carPos,
                carDir = carDir,
                gates = gates.toList(),
                gateInfo = gateInfo,

                lastCapturePoint = lastCapturePoint ?: it.lastCapturePoint,
                pendingCapturePosition = pendingCapturePosition,

                currentLapMs = timing?.currentLapMs,

                currentSectorIndex = timing?.currentSectorIndex,
                currentSectorMs = timing?.currentSectorMs,

                lastS1Ms = timing?.lastS1Ms,
                bestS1Ms = timing?.bestS1Ms,
                lastS2Ms = timing?.lastS2Ms,
                bestS2Ms = timing?.bestS2Ms,
                lastS3Ms = timing?.lastS3Ms,
                bestS3Ms = timing?.bestS3Ms,
            )
        }
    }

    private fun buildGateInfo(
        carPos: Vec2?,
        carDir: Vec2?,
        gates: Map<String, Gate>
    ): List<GateDebugInfo> {
        if (carPos == null || carDir == null) return emptyList()

        val carDirN = carDir.safeNormalized(Vec2(0f, 1f))

        return gates.map { (key, gate) ->
            val delta = carPos - gate.centerV2()
            val f = gate.forwardV2().safeNormalized(Vec2(0f, 1f))
            val n = gate.normalV2().safeNormalized(f.perpLeft())

            val dParallel = delta.dot(f)
            val dLateral = delta.dot(n)

            val inside = abs(dLateral) <= gate.halfWidthMeters
            val margin = gate.halfWidthMeters - abs(dLateral)

            GateDebugInfo(
                name = keyToName(key),
                distanceMeters = delta.len(),
                isCrossed = false,
                lastCrossedTimeMs = null,
                gateForward = f,
                directionDot = carDirN.dot(f),
                signedDistanceFromPlane = dParallel,
                isInside = inside,
                margin = margin,
                dParallel = dParallel,
                gate = gate
            )
        }
    }

    private fun keyToName(key: String): String = when (key) {
        "SF" -> "Start/Finish"
        "S1_F" -> "S1 Finish (Split 1)"
        "S2_F" -> "S2 Finish (Split 2)"
        else -> key
    }
}
