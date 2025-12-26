package com.project.analyzer.calibration.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.calibration.data.AcTelemetrySampleProvider
import com.project.analyzer.calibration.data.model.Gate
import com.project.analyzer.calibration.data.model.Pose2D
import com.project.analyzer.calibration.data.model.TrackCalibration
import com.project.analyzer.calibration.domain.usecase.LoadTrackCalibrationUseCase
import com.project.analyzer.calibration.presentation.state.CalibrationVerifyState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

@Inject
@ViewModelKey(CalibrationVerifyViewModel::class)
@ContributesIntoMap(AppScope::class)
class CalibrationVerifyViewModel(
    private val loadUseCase: LoadTrackCalibrationUseCase,
    private val sampleProvider: AcTelemetrySampleProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(CalibrationVerifyState())
    val state: StateFlow<CalibrationVerifyState> = _state

    private var job: Job? = null

    private var hasPrev = false
    private var prevPose: Pose2D? = null
    private var prevNs: Long = 0L

    private var lapRunning = false
    private var lapStartNs = 0L
    private var sectorStartNs = 0L
    private var sectorIndex = 1
    private var lapIndex = 0

    private var lastLapMs: Long? = null
    private var bestLapMs: Long? = null

    private var currentLapSectors = longArrayOf(-1L, -1L, -1L)
    private var completedLapSectors = longArrayOf(-1L, -1L, -1L)

    private var bestSectors: Array<Long?> = arrayOf(null, null, null)

    private val lastGateTriggerNs = mutableMapOf<String, Long>()
    private val gateCooldownNs = 900_000_000L

    fun start(trackId: String) {
        job?.cancel()
        resetRuntime(keepLoaded = true)

        viewModelScope.launch {
            _state.update { it.copy(trackId = trackId, message = "Loading $trackId…") }

            val cal = loadUseCase.load(trackId)
            if (cal == null) {
                _state.update {
                    it.copy(
                        calibration = null,
                        isRunning = false,
                        message = "No calibration file for $trackId"
                    )
                }
                return@launch
            }

            sampleProvider.setReferencePoint(cal.referencePoint)

            _state.update {
                it.copy(
                    trackId = trackId,
                    calibration = cal,
                    isRunning = true,
                    message = "Loaded. Drive and cross SF/sectors to verify."
                )
            }

            job = viewModelScope.launch {
                sampleProvider.sample.collect { sample ->
                    val pose = sample.pose ?: return@collect
                    val nowNs = sample.timestampNs.takeIf { it > 0L } ?: System.nanoTime()
                    if (hasPrev && nowNs <= prevNs) {
                        val fixed = prevNs + 1
                        onPose(fixed, pose, cal)
                        return@collect
                    }
                    onPose(nowNs, pose, cal)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.update { it.copy(isRunning = false, message = "Stopped") }
    }

    fun resetSession() {
        resetRuntime(keepLoaded = true)
        _state.update {
            it.copy(
                lapRunning = false,
                lapIndex = 0,
                currentLapMs = 0L,
                currentSectorIndex = 1,
                currentSectorMs = 0L,
                lastLapMs = null,
                bestLapMs = null,
                lastS1Ms = null,
                lastS2Ms = null,
                lastS3Ms = null,
                bestS1Ms = null,
                bestS2Ms = null,
                bestS3Ms = null,
                lastEvent = "Reset session",
                events = emptyList(),
                message = "Session reset"
            )
        }
    }

    private fun resetRuntime(keepLoaded: Boolean) {
        hasPrev = false
        prevPose = null
        prevNs = 0L

        lapRunning = false
        lapStartNs = 0L
        sectorStartNs = 0L
        sectorIndex = 1
        lapIndex = 0

        lastLapMs = null
        bestLapMs = null

        currentLapSectors = longArrayOf(-1L, -1L, -1L)
        completedLapSectors = longArrayOf(-1L, -1L, -1L)
        bestSectors = arrayOf(null, null, null)

        lastGateTriggerNs.clear()

        if (!keepLoaded) _state.value = CalibrationVerifyState()
    }

    private fun onPose(nowNs: Long, pose: Pose2D, cal: TrackCalibration) {
        if (hasPrev && nowNs <= prevNs) return
        if (!hasPrev) {
            hasPrev = true
            prevPose = pose
            prevNs = nowNs
            publish(nowNs)
            return
        }

        val p0 = prevPose ?: return
        val t0 = prevNs

        if (!lapRunning) {
            val crossNs = crossedAtNs(p0, pose, cal.startFinish, t0, nowNs)
            if (crossNs != null && shouldTrigger("SF", crossNs)) {
                lapRunning = true
                lapStartNs = crossNs
                sectorStartNs = crossNs
                sectorIndex = 1
                currentLapSectors = longArrayOf(-1L, -1L, -1L)
                mark("SF", crossNs)
                pushEvent("Lap START @ SF")
            }

            prevPose = pose
            prevNs = nowNs
            publish(nowNs)
            return
        }

        val nextFinish: Gate? = when (sectorIndex) {
            1 -> cal.sectors.firstOrNull { it.index == 1 }?.finish
            2 -> cal.sectors.firstOrNull { it.index == 2 }?.finish
            else -> null
        }

        val nf = nextFinish
        if (nf != null) {
            val centerDist = (pose.pos - nf.center).length()
            if (centerDist < 40f) {
                pushEvent("Next gate=S$sectorIndex distToCenter=%.1fm".format(centerDist))
            }
        }

        if (nextFinish != null) {
            val key = "S${sectorIndex}_F"
            val crossNs = crossedAtNs(p0, pose, nextFinish, t0, nowNs)
            if (crossNs != null && shouldTrigger(key, crossNs) && movingForward(p0, pose, nextFinish)) {
                val sectorMs = (crossNs - sectorStartNs) / 1_000_000L
                currentLapSectors[sectorIndex - 1] = sectorMs
                updateBestSector(sectorIndex, sectorMs)

                sectorStartNs = crossNs
                sectorIndex += 1

                mark(key, crossNs)
                pushEvent("Sector ${sectorIndex - 1} = ${formatMs(sectorMs)}")
            }
        }

        val sfCrossNs = crossedAtNs(p0, pose, cal.startFinish, t0, nowNs)
        if (sfCrossNs != null && shouldTrigger("SF", sfCrossNs) && movingForward(p0, pose, cal.startFinish)) {
            val s3ms = (sfCrossNs - sectorStartNs) / 1_000_000L
            currentLapSectors[2] = s3ms
            updateBestSector(3, s3ms)

            val lapMs = (sfCrossNs - lapStartNs) / 1_000_000L
            lastLapMs = lapMs
            if (bestLapMs == null || lapMs < bestLapMs!!) bestLapMs = lapMs
            lapIndex += 1

            completedLapSectors = currentLapSectors.copyOf()

            pushEvent("Lap DONE = ${formatMs(lapMs)}")

            lapStartNs = sfCrossNs
            sectorStartNs = sfCrossNs
            sectorIndex = 1
            currentLapSectors = longArrayOf(-1L, -1L, -1L)

            mark("SF", sfCrossNs)
        }

        prevPose = pose
        prevNs = nowNs
        publish(nowNs)
    }

    private fun publish(nowNs: Long) {
        val curLapMs = if (lapRunning) (nowNs - lapStartNs) / 1_000_000L else 0L
        val curSectorMs = if (lapRunning) (nowNs - sectorStartNs) / 1_000_000L else 0L

        _state.update {
            it.copy(
                lapRunning = lapRunning,
                lapIndex = lapIndex,
                currentLapMs = curLapMs,
                currentSectorIndex = sectorIndex,
                currentSectorMs = curSectorMs,

                lastLapMs = lastLapMs,
                bestLapMs = bestLapMs,

                lastS1Ms = completedLapSectors[0].takeIf { v -> v >= 0 },
                lastS2Ms = completedLapSectors[1].takeIf { v -> v >= 0 },
                lastS3Ms = completedLapSectors[2].takeIf { v -> v >= 0 },

                bestS1Ms = bestSectors[0],
                bestS2Ms = bestSectors[1],
                bestS3Ms = bestSectors[2],
            )
        }
    }

    private fun updateBestSector(index: Int, ms: Long) {
        val i = index - 1
        val cur = bestSectors[i]
        if (cur == null || ms < cur) bestSectors[i] = ms
    }

    private fun pushEvent(text: String) {
        _state.update {
            val list = (listOf(text) + it.events).take(12)
            it.copy(lastEvent = text, events = list, message = null)
        }
    }

    private fun shouldTrigger(key: String, nowNs: Long): Boolean {
        val last = lastGateTriggerNs[key] ?: return true
        return (nowNs - last) >= gateCooldownNs
    }

    private fun mark(key: String, nowNs: Long) {
        lastGateTriggerNs[key] = nowNs
    }

    private fun movingForward(prev: Pose2D, cur: Pose2D, gate: Gate): Boolean {
        val v = cur.pos - prev.pos
        if (v.length() < 0.05f) return true
        return v.dot(gate.forward) > 0f
    }

    private fun signedDistance(pose: Pose2D, gate: Gate): Float =
        (pose.pos - gate.center).dot(gate.normal)

    private fun crossedAtNs(prev: Pose2D, cur: Pose2D, gate: Gate, prevNs: Long, curNs: Long): Long? {
        val d0 = signedDistance(prev, gate)
        val d1 = signedDistance(cur, gate)

        if (abs(d0) > gate.triggerRadiusMeters && abs(d1) > gate.triggerRadiusMeters) return null

        val crossed = (d0 <= 0f && d1 > 0f)

        if (!crossed) return null

        val denom = (d0 - d1)
        if (abs(denom) < 1e-6f) return null

        val alpha = (d0 / denom).coerceIn(0f, 1f)

        val seg = cur.pos - prev.pos
        val p = prev.pos + seg * alpha

        val rel = p - gate.center

        val along = abs(rel.dot(gate.normal))
        if (along > gate.debugHalfWidthMeters) return null

        val dist = abs(rel.dot(gate.forward))
        if (dist > gate.triggerRadiusMeters) return null

        if (seg.length() >= 0.10f) {
            val approach = seg.normalized().dot(gate.forward)
            if (approach < 0.5f) return null
        }

        return prevNs + ((curNs - prevNs) * alpha).toLong()
    }


    private fun formatMs(ms: Long): String {
        val safe = if (ms < 0) 0L else ms
        val hours = safe / 3_600_000L
        val minutes = (safe % 3_600_000L) / 60_000L
        val seconds = (safe % 60_000L) / 1_000L
        val millis = safe % 1_000L
        return "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, millis)
    }
}
