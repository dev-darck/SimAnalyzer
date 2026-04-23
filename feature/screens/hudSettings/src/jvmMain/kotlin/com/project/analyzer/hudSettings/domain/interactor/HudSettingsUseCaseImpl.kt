package com.project.analyzer.hudSettings.domain.interactor

import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.HudPreferencesStore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow

class HudSettingsUseCaseImpl(
    private val hudPreferencesStore: HudPreferencesStore,
    private val panels: () -> Set<HudPanel>,
) : HudSettingsUseCase {

    override fun loadPanels(): ImmutableList<HudPanel> = panels()
        .filter { panel -> !panel.isDevOnly }
        .sortedBy { panel -> panel.id }
        .toImmutableList()

    override fun observeVisiblePanels(): Flow<Set<String>> = hudPreferencesStore.observeVisiblePanels()

    override fun observeHudOpacity(): Flow<Float> = hudPreferencesStore.observeHudOpacity()

    override suspend fun updateVisiblePanels(currentVisibleIds: Set<String>, id: String, enable: Boolean) {
        val next = if (enable) currentVisibleIds + id else currentVisibleIds - id
        hudPreferencesStore.saveVisiblePanels(next)
    }

    override suspend fun updateHudOpacity(opacity: Float) {
        hudPreferencesStore.setHudOpacity(opacity.coerceIn(0f, 1f))
    }
}
