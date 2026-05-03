package com.analyzer.session.details.presentation

import com.analyzer.session.details.presentation.model.SessionDetailCompareLapUi
import com.analyzer.session.details.presentation.model.SessionLapRowUi
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal data class SessionDetailCompareSelection(
    val isSelectionMode: Boolean = false,
    val selectedLaps: PersistentList<SessionDetailCompareLapUi> = persistentListOf(),
) {

    fun start(): SessionDetailCompareSelection =
        if (isSelectionMode) this else copy(isSelectionMode = true)

    fun cancel(): SessionDetailCompareSelection =
        if (!isSelectionMode && selectedLaps.isEmpty()) this else SessionDetailCompareSelection()

    fun toggle(
        visibleLaps: List<SessionLapRowUi>,
        segmentId: Long,
        lapNumber: Int,
    ): SessionDetailCompareSelection {
        if (!isSelectionMode) return this
        val lap = visibleLaps.firstOrNull { row ->
            row.segmentId == segmentId && row.lapNumber == lapNumber
        } ?: return this
        val existingIndex = selectedLaps.indexOfFirst { selected ->
            selected.segmentId == segmentId && selected.lapNumber == lapNumber
        }
        val nextSelectedLaps = when {
            existingIndex >= 0 -> selectedLaps.removeAt(existingIndex)
            selectedLaps.size >= 2 -> selectedLaps
            selectedLaps.isNotEmpty() && selectedLaps.first().segmentId != segmentId -> selectedLaps
            else -> {
                selectedLaps.add(
                    SessionDetailCompareLapUi(
                        segmentId = lap.segmentId,
                        lapNumber = lap.lapNumber,
                        lapLabel = lap.lapLabel,
                        sessionTypeLabel = lap.sessionTypeLabel,
                        totalTimeMs = lap.totalTimeMs,
                    ),
                )
            }
        }
        return if (nextSelectedLaps === selectedLaps) {
            this
        } else {
            copy(selectedLaps = nextSelectedLaps)
        }
    }
}
