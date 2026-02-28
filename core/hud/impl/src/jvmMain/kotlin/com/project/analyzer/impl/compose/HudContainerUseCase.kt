package com.project.analyzer.impl.compose

import androidx.compose.ui.unit.IntOffset
import com.project.analyzer.hud.api.HudStoredPosition
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class HudContainerUseCase(private val repository: HudPreferencesRepository) {

    fun observeVisiblePanels(): Flow<Set<String>> = repository.observeVisiblePanels()

    fun observeInputLocked(): Flow<Boolean> = repository.observeInputLocked()

    fun observeHudOpacity(): Flow<Float> = repository.observeHudOpacity()

    suspend fun loadPositions(): Map<String, HudStoredPosition> = repository.getVisiblePanelsPositions()

    suspend fun saveVisiblePanels(ids: Set<String>) {
        repository.saveVisiblePanels(ids)
    }

    suspend fun savePosition(id: String, position: HudStoredPosition) {
        repository.savePosition(id, position)
    }

    suspend fun saveInputLocked(locked: Boolean) {
        repository.setInputLocked(locked)
    }

    suspend fun saveHudOpacity(opacity: Float) {
        repository.setHudOpacity(opacity)
    }

    fun applyVisiblePanels(state: HudUiState, visibleIds: Set<String>): HudUiState {
        val newVisiblePanels = visibleIds.associateWith { id -> state.visiblePanels[id] ?: 0 }
        return state.copy(visiblePanels = newVisiblePanels)
    }

    fun applyInputLocked(state: HudUiState, locked: Boolean): HudUiState = state.copy(inputLocked = locked)

    fun applyPositions(state: HudUiState, positions: Map<String, HudStoredPosition>): HudUiState = state.copy(
        positions = positions,
    )

    fun applyHudOpacity(state: HudUiState, opacity: Float): HudUiState = state.copy(
        hudOpacity = opacity.coerceIn(0f, 1f),
    )

    fun toggleInputLock(state: HudUiState): HudUiState = state.copy(inputLocked = !state.inputLocked)

    fun restartPanel(state: HudUiState, id: String): HudUiState = state.copy(
        visiblePanels = if (id in state.visiblePanels) {
            val newVersion = (state.visiblePanels[id] ?: 0) + 1
            state.visiblePanels + (id to newVersion)
        } else {
            state.visiblePanels
        },
    )

    fun savePositionLocally(state: HudUiState, id: String, position: HudStoredPosition): HudUiState =
        state.copy(positions = state.positions + (id to position))
}
