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
}
