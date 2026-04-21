package com.project.analyzer.ac.telemetry.impl.internal.mapper.ac

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.AcLapState
import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.AcSessionCache
import com.project.analyzer.ac.telemetry.impl.internal.mapper.common.LapFallbackUsage
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.toBoolean
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.lap.SectorFrame
import com.project.analyzer.telemetry.api.model.lap.SectorStatus
import com.project.analyzer.telemetry.api.model.lap.SectorValidity
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.max

@Inject
@SingleIn(SessionScope::class)
class LapMapper(private val cache: AcSessionCache, private val lapState: AcLapState) {

    private var lastCompletedLaps: Int = -1
    private var lastLapSectors: List<SectorFrame> = emptyList()
    private var usingFallback: Boolean = false

    init {
        cache.addSessionChangeListener { reset() }
    }

    fun reset() {
        lastCompletedLaps = -1
        lastLapSectors = emptyList()
        lapState.fullReset()
        usingFallback = false
    }

    fun map(
        graphics: SPageFileGraphics,
        fallback: LapTimingSnapshot? = null,
        sectorCountOverride: Int? = null,
        fallbackUsage: LapFallbackUsage = LapFallbackUsage.FULL,
    ): LapFrame {
        val useFallbackSectorData = fallback?.isActive == true && sectorCountOverride != null
        val useFallbackTiming = useFallbackSectorData && fallbackUsage == LapFallbackUsage.FULL
        if (useFallbackSectorData != usingFallback) {
            lastCompletedLaps = -1
            lastLapSectors = emptyList()
            lapState.fullReset()
            usingFallback = useFallbackSectorData
        }

        val completedLaps = if (useFallbackTiming) fallback.completedLapsCount else graphics.completedLaps
        val isValidLap = if (useFallbackTiming) fallback.currentLapValid else graphics.isValidLap.toBoolean()
        val lastLapTimeMs = if (useFallbackTiming) {
            fallback.lastLapTimeMs?.takeIf { it > 0 }
        } else {
            graphics.iLastTime.takeIf { it > 0 }
        }
        val bestLapMs = if (useFallbackTiming) {
            fallback.bestLapTimeMs?.takeIf { it > 0 }
        } else {
            graphics.iBestTime.takeIf { it > 0 }
        }
        val currentLapTimeMs = if (useFallbackTiming) fallback.currentLapTimeMs else graphics.iCurrentTime

        val sectorCount = (sectorCountOverride ?: cache.sectorCount).coerceAtLeast(1)
        val currentSectorIndex =
            (if (useFallbackSectorData) fallback.currentSectorIndex else graphics.currentSectorIndex).coerceIn(
                0,
                sectorCount - 1,
            )
        val lastSectorTime = if (useFallbackSectorData) fallback.lastSectorTimeMs ?: 0 else graphics.lastSectorTime

        val sectorsToShow = if (useFallbackSectorData) {
            buildFallbackSectors(fallback, sectorCount)
        } else {
            val currentSectors = lapState.onFrame(
                currentSectorIndex = currentSectorIndex,
                lastSectorTimeMs = lastSectorTime,
                isValidLap = isValidLap,
                sectorCountOverride = sectorCountOverride,
            )

            if (completedLaps != lastCompletedLaps) {
                if (lastCompletedLaps != -1) {
                    lastLapSectors = lapState.getLastCompletedLapSectors()
                    lapState.reset()
                }
                lastCompletedLaps = completedLaps
            }

            if (currentSectors.all { it.timeMs == null } && lastLapSectors.isNotEmpty()) {
                lastLapSectors
            } else {
                currentSectors
            }
        }

        return LapFrame(
            currentLapIndex = (completedLaps + 1).takeIf { it > 0 },
            completedLaps = completedLaps,

            currentLapTimeMs = currentLapTimeMs,
            lastLapTimeMs = lastLapTimeMs,
            bestLapTimeMs = bestLapMs,

            sectorCount = sectorCount,
            currentSectorIndex = currentSectorIndex,
            lastSectorTimeMs = lastSectorTime.takeIf { it > 0 },

            deltaLapTimeMs = if (useFallbackTiming) fallback.deltaLapTimeMs else graphics.iDeltaLapTime,
            isDeltaPositive = if (useFallbackTiming) fallback.isDeltaPositive else graphics.isDeltaPositive.toBoolean(),
            estimatedLapTimeMs = graphics.iEstimatedLapTime.takeIf { it > 0 },

            splitTimeMs = if (useFallbackSectorData) {
                fallback.currentSectorTimeMs.takeIf { it > 0 }
            } else {
                graphics.iSplit.takeIf { it > 0 }
            },

            validity = LapValidity.fromBoolean(isValidLap),

            sectors = sectorsToShow,
        )
    }

    private fun buildFallbackSectors(snapshot: LapTimingSnapshot, sectorCount: Int): List<SectorFrame> {
        val count = max(sectorCount, max(snapshot.lastSectorsMs.size, snapshot.bestSectorsMs.size))
        if (count <= 0) return emptyList()

        val currentIdx = snapshot.currentSectorIndex.coerceIn(0, count - 1)

        return List(count) { idx ->
            val time = snapshot.lastSectorsMs.getOrNull(idx)
            val best = snapshot.bestSectorsMs.getOrNull(idx)
            val delta = if (time != null && best != null) time - best else null

            val status = if (!snapshot.isLapRunning) {
                SectorStatus.UNKNOWN
            } else {
                when {
                    idx < currentIdx -> SectorStatus.COMPLETED
                    idx == currentIdx -> SectorStatus.IN_PROGRESS
                    else -> SectorStatus.NOT_STARTED
                }
            }

            val validity = when {
                time == null -> SectorValidity.UNKNOWN
                snapshot.currentLapValid -> SectorValidity.VALID
                else -> SectorValidity.INVALID
            }

            SectorFrame(
                index = idx,
                timeMs = time,
                bestTimeMs = best,
                deltaToBestMs = delta,
                status = status,
                validity = validity,
            )
        }
    }
}
