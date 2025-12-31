package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.telemetry.ac.api.model.math.Vec2
import dev.zacsweers.metro.Inject
import kotlin.math.abs

data class FallbackLapSnapshot(
    val active: Boolean,
    val trackId: String?,
    val lapRunning: Boolean,
    val completedLaps: Int,

    val currentLapMs: Int,
    val currentSectorMs: Int,

    val currentSectorIndex0: Int, // 0..2
    val lastSectorTimeMs: Int?,

    val lastLapMs: Int?,
    val bestLapMs: Int?,
)

@Inject
class FallbackLapAnalyzer(
    private val loader: TrackCalibrationLoader,
) {

    private var active = false
    private var currentTrackId: String? = null
    private var calibration: TrackCalibrationDto? = null

    private var hasPrev = false
    private var prevPos = Vec2(0f, 0f)

    private var lapRunning = false
    private var syncedToSf = false

    private var lapStartNs = 0L
    private var sectorStartNs = 0L
    private var sectorIndex = 1

    private var completedLaps = 0
    private var lastLapMs: Int? = null
    private var bestLapMs: Int? = null
    private var lastSectorTimeMs: Int? = null

    private val lastGateTriggerNs = mutableMapOf<String, Long>()
    private val gateCooldownNs = 900_000_000L

    fun reset() {
        active = false
        currentTrackId = null
        calibration = null

        hasPrev = false
        prevPos = Vec2(0f, 0f)

        lapRunning = false
        syncedToSf = false

        lapStartNs = 0L
        sectorStartNs = 0L
        sectorIndex = 1

        completedLaps = 0
        lastLapMs = null
        bestLapMs = null
        lastSectorTimeMs = null

        lastGateTriggerNs.clear()
    }

    fun onPhysics(loopStartNanos: Long, physics: SPageFilePhysics, trackId: String?) {
        val id = trackId?.takeIf { it.isNotBlank() } ?: run {
            active = true
            return
        }

        if (id != currentTrackId) {
            val cal = loader.load(id) ?: run {
                reset()
                active = true
                currentTrackId = id
                return
            }

            currentTrackId = id
            calibration = cal
            active = true

            hasPrev = false
            lapRunning = false
            syncedToSf = false

            sectorIndex = 1
            completedLaps = 0
            lastLapMs = null
            bestLapMs = null
            lastSectorTimeMs = null
            lastGateTriggerNs.clear()
        }

        val cal = calibration ?: return
        val pos = extractFrontAxlePosXZ(physics) ?: run {
            active = true
            return
        }

        if (!hasPrev) {
            prevPos = pos
            hasPrev = true
            return
        }

        if (!lapRunning) {
            lapRunning = true
            syncedToSf = false
            lapStartNs = loopStartNanos
            sectorStartNs = loopStartNanos
            sectorIndex = 1
            lastSectorTimeMs = null
        }

        val sf = cal.startFinish

        if (!syncedToSf) {
            if (shouldTrigger(loopStartNanos, "SF") &&
                crossed(prevPos, pos, sf) &&
                movingForward(prevPos, pos, sf)
            ) {
                syncedToSf = true

                completedLaps = 0
                lastLapMs = null
                bestLapMs = null
                lastSectorTimeMs = null

                lapStartNs = loopStartNanos
                sectorStartNs = loopStartNanos
                sectorIndex = 1

                mark(loopStartNanos, "SF")
            }

            prevPos = pos
            return
        }

        val nextGate = when (sectorIndex) {
            1 -> cal.sectors.firstOrNull { it.index == 1 }?.finish
            2 -> cal.sectors.firstOrNull { it.index == 2 }?.finish
            else -> null
        }

        if (nextGate != null) {
            val key = "S${sectorIndex}_F"
            if (shouldTrigger(loopStartNanos, key) &&
                crossed(prevPos, pos, nextGate) &&
                movingForward(prevPos, pos, nextGate)
            ) {
                lastSectorTimeMs = ((loopStartNanos - sectorStartNs) / 1_000_000L).toInt()
                sectorStartNs = loopStartNanos
                sectorIndex += 1
                mark(loopStartNanos, key)
            }
        }

        if (shouldTrigger(loopStartNanos, "SF") &&
            crossed(prevPos, pos, sf) &&
            movingForward(prevPos, pos, sf)
        ) {
            lastSectorTimeMs = ((loopStartNanos - sectorStartNs) / 1_000_000L).toInt()

            val lapMs = ((loopStartNanos - lapStartNs) / 1_000_000L).toInt()
            lastLapMs = lapMs
            bestLapMs = bestLapMs?.let { minOf(it, lapMs) } ?: lapMs
            completedLaps += 1

            lapStartNs = loopStartNanos
            sectorStartNs = loopStartNanos
            sectorIndex = 1

            mark(loopStartNanos, "SF")
        }

        prevPos = pos
    }

    fun resetSessionKeepCalibration() {
        hasPrev = false
        prevPos = Vec2(0f, 0f)

        lapRunning = false
        syncedToSf = false

        lapStartNs = 0L
        sectorStartNs = 0L
        sectorIndex = 1

        completedLaps = 0
        lastLapMs = null
        bestLapMs = null
        lastSectorTimeMs = null

        lastGateTriggerNs.clear()
        active = true
    }

    fun snapshot(nowNs: Long): FallbackLapSnapshot {
        val currentLapMs = if (lapRunning) ((nowNs - lapStartNs) / 1_000_000L).toInt() else 0
        val currentSectorMs = if (lapRunning) ((nowNs - sectorStartNs) / 1_000_000L).toInt() else 0
        val sector0 = if (!lapRunning) 0 else (sectorIndex - 1).coerceIn(0, 2)

        return FallbackLapSnapshot(
            active = active,
            trackId = currentTrackId,
            lapRunning = lapRunning,
            completedLaps = completedLaps,
            currentLapMs = currentLapMs,
            currentSectorMs = currentSectorMs,
            currentSectorIndex0 = sector0,
            lastSectorTimeMs = lastSectorTimeMs,
            lastLapMs = lastLapMs,
            bestLapMs = bestLapMs,
        )
    }

    private fun extractFrontAxlePosXZ(physics: SPageFilePhysics): Vec2? {
        val a = physics.tyreContactPoint
        if (a.size < 12) return null

        val flX = a[0]
        val flZ = a[2]
        val frX = a[3]
        val frZ = a[5]

        if ((flX == 0f && flZ == 0f && frX == 0f && frZ == 0f)) return null

        val pos = Vec2((flX + frX) * 0.5f, (flZ + frZ) * 0.5f)

         println("[FallbackLapAnalyzer] FL=($flX, $flZ) FR=($frX, $frZ) -> frontAxle=$pos")

        return pos
    }

    private fun shouldTrigger(nowNs: Long, key: String): Boolean {
        val last = lastGateTriggerNs[key] ?: return true
        return (nowNs - last) >= gateCooldownNs
    }

    private fun mark(nowNs: Long, key: String) {
        lastGateTriggerNs[key] = nowNs
    }

    internal fun signedDistanceToGateLine(pos: Vec2, gate: GateDto): Float {
        // Distance across the gate line (normal points left from heading)
        val c = gate.centerV2()
        val across = gate.normalV2()
        return (pos - c).dot(across)
    }

    internal fun nearGate(pos: Vec2, gate: GateDto): Boolean {
        val c = gate.centerV2()
        val acrossDir = gate.normalV2()

        val dist = abs((pos - c).dot(gate.forwardV2()))
        if (dist > gate.triggerRadiusMeters) return false

        val along = abs((pos - c).dot(acrossDir))
        if (along > gate.debugHalfWidthMeters) return false

        return true
    }

    private fun crossed(prevPos: Vec2, pos: Vec2, gate: GateDto): Boolean {
        val d0 = signedDistanceToGateLine(prevPos, gate)
        val d1 = signedDistanceToGateLine(pos, gate)
        val n0 = abs((prevPos - gate.centerV2()).dot(gate.normalV2()))
        val n1 = abs((pos - gate.centerV2()).dot(gate.normalV2()))

        if (n0 > gate.debugHalfWidthMeters && n1 > gate.debugHalfWidthMeters) return false
        if (!((d0 <= 0f && d1 >= 0f) || (d0 >= 0f && d1 <= 0f))) return false

        val denom = d0 - d1
        if (abs(denom) < 1e-6f) return false
        val alpha = (d0 / denom).coerceIn(0f, 1f)
        val p = Vec2(
            prevPos.x + (pos.x - prevPos.x) * alpha,
            prevPos.y + (pos.y - prevPos.y) * alpha
        )

        val rel = p - gate.centerV2()
        val across = abs(rel.dot(gate.normalV2()))
        if (across > gate.debugHalfWidthMeters) return false
        val along = abs(rel.dot(gate.forwardV2()))
        if (along > gate.triggerRadiusMeters) return false

        val seg = pos - prevPos
        if (seg.length() >= 0.10f) {
            val approach = seg.normalized().dot(gate.forwardV2())
            if (approach < 0.2f) return false
        }

        return true
    }

    private fun movingForward(prevPos: Vec2, pos: Vec2, gate: GateDto): Boolean {
        val v = Vec2(pos.x - prevPos.x, pos.y - prevPos.y)
        val len = v.length()
        if (len < 0.02f) return false
        val approach = v.normalized().dot(gate.forwardV2())
        return approach > 0.2f
    }
}
