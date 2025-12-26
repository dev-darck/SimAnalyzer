package com.project.analyzer.ac.telemetry.impl.internal

import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcSessionCache
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.model.lap.SectorFrame
import com.project.analyzer.telemetry.ac.api.model.lap.SectorStatus
import com.project.analyzer.telemetry.ac.api.model.lap.SectorValidity
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.max

@Inject
@SingleIn(SessionScope::class)
class AcLapState(
    private val cache: AcSessionCache
) {
    private var lastSectorIndex: Int = -1
    private var sectors: MutableList<SectorFrame> = mutableListOf()

    fun onFrame(
        currentSectorIndex: Int,
        lastSectorTimeMs: Int,
        isValidLap: Boolean
    ): List<SectorFrame> {
        val count = max(0, cache.sectorCount)
        if (count <= 0) return emptyList()

        if (sectors.size != count) {
            initSectors(count)
        }

        val cur = currentSectorIndex.coerceIn(0, count - 1)

        updateSectorStatuses(cur)

        if (lastSectorIndex != -1 && cur != lastSectorIndex) {
            recordCompletedSector(
                completedIdx = lastSectorIndex,
                count = count,
                lastSectorTimeMs = lastSectorTimeMs,
                isValidLap = isValidLap
            )
        }

        lastSectorIndex = cur
        return sectors.toList()
    }

    fun reset() {
        lastSectorIndex = -1
        sectors.clear()
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
                invalidReason = null
            )
        }
        lastSectorIndex = -1
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

    private fun recordCompletedSector(
        completedIdx: Int,
        count: Int,
        lastSectorTimeMs: Int,
        isValidLap: Boolean
    ) {
        if (completedIdx !in 0 until count) return

        val completed = sectors[completedIdx]
        val validity = if (isValidLap) SectorValidity.VALID else SectorValidity.INVALID

        sectors[completedIdx] = completed.copy(
            timeMs = lastSectorTimeMs.takeIf { it > 0 },
            status = SectorStatus.COMPLETED,
            validity = validity
        )
    }
}
