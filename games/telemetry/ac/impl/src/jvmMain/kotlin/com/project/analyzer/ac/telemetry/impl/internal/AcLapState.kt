package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcSessionCache
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.lap.SectorFrame
import com.project.analyzer.telemetry.api.model.lap.SectorStatus
import com.project.analyzer.telemetry.api.model.lap.SectorValidity
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.max

@Inject
@SingleIn(SessionScope::class)
class AcLapState(private val cache: AcSessionCache) {

    private var lastSectorIndex: Int = -1
    private var lastRecordedSectorTimeMs: Int = -1
    private var sectors: MutableList<SectorFrame> = mutableListOf()

    private var lastCompletedLapSectors: List<SectorFrame> = emptyList()

    private var pendingLastSectorIndex: Int? = null
    private var pendingLastSectorValidity: Boolean = true

    fun onFrame(
        currentSectorIndex: Int,
        lastSectorTimeMs: Int,
        isValidLap: Boolean,
        sectorCountOverride: Int? = null,
    ): List<SectorFrame> {
        val count = max(0, sectorCountOverride ?: cache.sectorCount)
        if (count <= 0) return emptyList()

        if (sectors.size != count) {
            initSectors(count)
        }

        val cur = currentSectorIndex.coerceIn(0, count - 1)

        val isLapCrossing = lastSectorIndex == count - 1 && cur == 0

        if (isLapCrossing) {
            if (lastSectorTimeMs > 0 && lastSectorTimeMs != lastRecordedSectorTimeMs) {
                recordCompletedSector(
                    completedIdx = lastSectorIndex,
                    count = count,
                    lastSectorTimeMs = lastSectorTimeMs,
                    isValidLap = isValidLap,
                )
                lastRecordedSectorTimeMs = lastSectorTimeMs
                pendingLastSectorIndex = null
            } else {
                pendingLastSectorIndex = lastSectorIndex
                pendingLastSectorValidity = isValidLap

                val completed = sectors[lastSectorIndex]
                sectors[lastSectorIndex] = completed.copy(
                    status = SectorStatus.COMPLETED,
                    validity = if (isValidLap) SectorValidity.VALID else SectorValidity.INVALID,
                )
            }

            lastCompletedLapSectors = sectors.toList()
        } else if (lastSectorIndex != -1 && cur != lastSectorIndex) {
            if (lastSectorTimeMs > 0 && lastSectorTimeMs != lastRecordedSectorTimeMs) {
                recordCompletedSector(
                    completedIdx = lastSectorIndex,
                    count = count,
                    lastSectorTimeMs = lastSectorTimeMs,
                    isValidLap = isValidLap,
                )
                lastRecordedSectorTimeMs = lastSectorTimeMs
            }
        }

        if (pendingLastSectorIndex != null &&
            lastSectorTimeMs > 0 &&
            lastSectorTimeMs != lastRecordedSectorTimeMs
        ) {
            val pendingIdx = pendingLastSectorIndex!!

            if (pendingIdx in sectors.indices) {
                sectors[pendingIdx] = sectors[pendingIdx].copy(
                    timeMs = lastSectorTimeMs,
                    status = SectorStatus.COMPLETED,
                    validity = if (pendingLastSectorValidity) SectorValidity.VALID else SectorValidity.INVALID,
                )
            }

            lastCompletedLapSectors = lastCompletedLapSectors.mapIndexed { idx, sector ->
                if (idx == pendingIdx) {
                    sector.copy(timeMs = lastSectorTimeMs)
                } else {
                    sector
                }
            }

            lastRecordedSectorTimeMs = lastSectorTimeMs
            pendingLastSectorIndex = null
        }

        if (cur == 0 && lastSectorIndex == 0) {
            val lastSectorIdx = count - 1
            val lastSector = lastCompletedLapSectors.getOrNull(lastSectorIdx)
            if (lastSector != null && lastSector.timeMs == null &&
                lastSectorTimeMs > 0 && lastSectorTimeMs != lastRecordedSectorTimeMs
            ) {
                lastCompletedLapSectors = lastCompletedLapSectors.mapIndexed { idx, sector ->
                    if (idx == lastSectorIdx) {
                        sector.copy(timeMs = lastSectorTimeMs)
                    } else {
                        sector
                    }
                }

                if (lastSectorIdx in sectors.indices && sectors[lastSectorIdx].timeMs == null) {
                    sectors[lastSectorIdx] = sectors[lastSectorIdx].copy(timeMs = lastSectorTimeMs)
                }

                lastRecordedSectorTimeMs = lastSectorTimeMs
            }
        }

        updateSectorStatuses(cur)
        lastSectorIndex = cur

        return sectors.toList()
    }

    fun getLastCompletedLapSectors(): List<SectorFrame> = lastCompletedLapSectors

    fun reset() {
        lastSectorIndex = -1
        lastRecordedSectorTimeMs = -1
        sectors.clear()
    }

    fun fullReset() {
        reset()
        lastCompletedLapSectors = emptyList()
    }

    private fun initSectors(count: Int) {
        sectors = MutableList(count) { idx ->
            SectorFrame(
                index = idx,
                timeMs = null,
                bestTimeMs = null,
                deltaToBestMs = null,
                status = SectorStatus.NOT_STARTED,
                validity = SectorValidity.UNKNOWN,
                invalidReason = null,
            )
        }
        lastSectorIndex = -1
        lastRecordedSectorTimeMs = -1
    }

    private fun updateSectorStatuses(currentIdx: Int) {
        sectors = sectors.mapIndexed { idx, sector ->
            when {
                idx < currentIdx && sector.status != SectorStatus.COMPLETED ->
                    sector.copy(status = SectorStatus.COMPLETED)

                idx == currentIdx && sector.status != SectorStatus.IN_PROGRESS ->
                    sector.copy(status = SectorStatus.IN_PROGRESS)

                idx > currentIdx && sector.status == SectorStatus.UNKNOWN ->
                    sector.copy(status = SectorStatus.NOT_STARTED)

                else -> sector
            }
        }.toMutableList()
    }

    private fun recordCompletedSector(completedIdx: Int, count: Int, lastSectorTimeMs: Int, isValidLap: Boolean) {
        if (completedIdx !in 0 until count) return

        val completed = sectors[completedIdx]
        val validity = if (isValidLap) SectorValidity.VALID else SectorValidity.INVALID

        sectors[completedIdx] = completed.copy(
            timeMs = lastSectorTimeMs.takeIf { it > 0 },
            status = SectorStatus.COMPLETED,
            validity = validity,
        )
    }
}
