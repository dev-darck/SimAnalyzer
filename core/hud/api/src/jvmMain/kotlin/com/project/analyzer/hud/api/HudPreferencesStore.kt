package com.project.analyzer.hud.api

import kotlinx.coroutines.flow.Flow

public interface HudPreferencesStore {
    public fun observeVisiblePanels(): Flow<Set<String>>
    public fun observeHudEnabled(): Flow<Boolean>
    public fun observeInputLocked(): Flow<Boolean>
    public fun observeHudOpacity(): Flow<Float>

    public suspend fun setInputLocked(locked: Boolean)
    public suspend fun setHudOpacity(opacity: Float)
    public suspend fun getVisiblePanelsPositions(): Map<String, HudStoredPosition>
    public suspend fun saveVisiblePanels(ids: Set<String>)
    public suspend fun getVisiblePanelsBackup(): Set<String>
    public suspend fun saveVisiblePanelsBackup(ids: Set<String>)
    public suspend fun clearVisiblePanelsBackup()
    public suspend fun savePosition(panelId: String, position: HudStoredPosition)
}
