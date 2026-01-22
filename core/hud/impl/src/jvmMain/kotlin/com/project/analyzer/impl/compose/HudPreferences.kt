package com.project.analyzer.impl.compose

import androidx.compose.ui.unit.IntOffset
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.preference.api.bool
import com.project.analyzer.preference.api.int
import com.project.analyzer.preference.api.strSet
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

@Inject
@SingleIn(AppScope::class)
class HudPreferences(
    @param:UserPref
    private val preference: Preference
) {

    fun observeVisiblePanels(): Flow<Set<String>> =
        preference.observe(KEY_VISIBLE_PANELS.strSet, emptySet())

    fun observeHudEnabled(): Flow<Boolean> = preference.observe(TELEMETRY_HUD_ENABLED.bool, true)
    suspend fun getVisiblePanelsPositions(): Map<String, IntOffset> {
        val listPanels = preference.get(KEY_VISIBLE_PANELS.strSet, emptySet())

        return listPanels.associateWith { getPosition(it, IntOffset.Zero) }
    }

    suspend fun saveVisiblePanels(ids: Set<String>) {
        preference.put(KEY_VISIBLE_PANELS.strSet to ids)
    }

    suspend fun getPosition(panelId: String, default: IntOffset): IntOffset {
        val x = preference.get(positionXKey(panelId), default.x)
        val y = preference.get(positionYKey(panelId), default.y)
        return IntOffset(x, y)
    }

    suspend fun savePosition(panelId: String, offset: IntOffset) {
        preference.put(positionXKey(panelId) to offset.x)
        preference.put(positionYKey(panelId) to offset.y)
    }

    private companion object {

        const val TELEMETRY_HUD_ENABLED = "telemetry_hud_enabled"
        const val KEY_VISIBLE_PANELS = "hud_visible_panels"
        fun positionXKey(id: String) = "hud_${id}_x".int
        fun positionYKey(id: String) = "hud_${id}_y".int
    }
}
