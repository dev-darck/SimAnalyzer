package com.project.analyzer.impl.compose

import com.project.analyzer.hud.api.HudStoredPosition
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class HudContainerUseCaseImpl(private val repository: HudPreferencesRepository) : HudContainerUseCase {

    override fun observeVisiblePanels(): Flow<Set<String>> = repository.observeVisiblePanels()

    override fun observeInputLocked(): Flow<Boolean> = repository.observeInputLocked()

    override fun observeHudOpacity(): Flow<Float> = repository.observeHudOpacity()

    override suspend fun loadPositions(): Map<String, HudStoredPosition> = repository.getVisiblePanelsPositions()

    override suspend fun saveVisiblePanels(ids: Set<String>) {
        repository.saveVisiblePanels(ids)
    }

    override suspend fun savePosition(id: String, position: HudStoredPosition) {
        repository.savePosition(id, position)
    }

    override suspend fun saveInputLocked(locked: Boolean) {
        repository.setInputLocked(locked)
    }

    override suspend fun saveHudOpacity(opacity: Float) {
        repository.setHudOpacity(opacity)
    }

    override fun applyVisiblePanels(state: HudUiState, visibleIds: Set<String>): HudUiState {
        val newVisiblePanels = visibleIds.associateWith { id -> state.visiblePanels[id] ?: 0 }
        return state.copy(visiblePanels = newVisiblePanels)
    }

    override fun applyInputLocked(state: HudUiState, locked: Boolean): HudUiState = state.copy(inputLocked = locked)

    override fun applyPositions(state: HudUiState, positions: Map<String, HudStoredPosition>): HudUiState = state.copy(
        positions = positions,
    )

    override fun applyHudOpacity(state: HudUiState, opacity: Float): HudUiState = state.copy(
        hudOpacity = opacity.coerceIn(0f, 1f),
    )

    override fun toggleInputLock(state: HudUiState): HudUiState = state.copy(inputLocked = !state.inputLocked)

    override fun restartPanel(state: HudUiState, id: String): HudUiState = state.copy(
        visiblePanels = if (id in state.visiblePanels) {
            val newVersion = (state.visiblePanels[id] ?: 0) + 1
            state.visiblePanels + (id to newVersion)
        } else {
            state.visiblePanels
        },
    )

    override fun savePositionLocally(state: HudUiState, id: String, position: HudStoredPosition): HudUiState =
        state.copy(positions = state.positions + (id to position))
}
