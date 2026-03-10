package com.project.analyzer.impl.compose

import com.project.analyzer.hud.api.HudPreferencesStore
import com.project.analyzer.hud.api.HudStoredPosition
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow

@Inject
class HudPreferencesRepository(private val preferences: HudPreferencesStore) {

    fun observeVisiblePanels(): Flow<Set<String>> = preferences.observeVisiblePanels()

    fun observeHudEnabled(): Flow<Boolean> = preferences.observeHudEnabled()

    fun observeInputLocked(): Flow<Boolean> = preferences.observeInputLocked()

    fun observeHudOpacity(): Flow<Float> = preferences.observeHudOpacity()

    suspend fun setInputLocked(locked: Boolean) {
        preferences.setInputLocked(locked)
    }

    suspend fun setHudOpacity(opacity: Float) {
        preferences.setHudOpacity(opacity)
    }

    suspend fun getVisiblePanelsPositions(): Map<String, HudStoredPosition> = preferences.getVisiblePanelsPositions()

    suspend fun saveVisiblePanels(ids: Set<String>) {
        preferences.saveVisiblePanels(ids)
    }

    suspend fun getVisiblePanelsBackup(): Set<String> = preferences.getVisiblePanelsBackup()

    suspend fun saveVisiblePanelsBackup(ids: Set<String>) {
        preferences.saveVisiblePanelsBackup(ids)
    }

    suspend fun clearVisiblePanelsBackup() {
        preferences.clearVisiblePanelsBackup()
    }

    suspend fun savePosition(panelId: String, position: HudStoredPosition) {
        preferences.savePosition(panelId, position)
    }
}
