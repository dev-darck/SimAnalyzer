package com.project.analyzer.impl.compose

import com.project.analyzer.hud.api.HudStoredPosition
import kotlinx.coroutines.flow.Flow

interface HudContainerUseCase {

    fun observeVisiblePanels(): Flow<Set<String>>
    fun observeInputLocked(): Flow<Boolean>
    fun observeHudOpacity(): Flow<Float>
    suspend fun loadPositions(): Map<String, HudStoredPosition>
    suspend fun saveVisiblePanels(ids: Set<String>)
    suspend fun savePosition(id: String, position: HudStoredPosition)
    suspend fun saveInputLocked(locked: Boolean)
    suspend fun saveHudOpacity(opacity: Float)
}
