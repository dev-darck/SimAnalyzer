package com.project.analyzer.impl.compose

import androidx.compose.ui.unit.IntOffset
import com.project.analyzer.hud.api.DefaultHudBackgroundOpacity
import com.project.analyzer.hud.api.HudPreferencesStore
import com.project.analyzer.hud.api.HudStoredPosition
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.preference.api.bool
import com.project.analyzer.preference.api.float
import com.project.analyzer.preference.api.int
import com.project.analyzer.preference.api.strSet
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

@Inject
@SingleIn(AppScope::class)
internal class HudPreferences(
    @param:UserPref
    private val preference: Preference,
) : HudPreferencesStore {

    override fun observeVisiblePanels(): Flow<Set<String>> = preference.observe(KEY_VISIBLE_PANELS.strSet, emptySet())

    override fun observeHudEnabled(): Flow<Boolean> = preference.observe(TELEMETRY_HUD_ENABLED.bool, true)
    override fun observeInputLocked(): Flow<Boolean> = preference.observe(KEY_INPUT_LOCKED.bool, false)
    override fun observeHudOpacity(): Flow<Float> =
        preference.observe(KEY_HUD_OPACITY.float, DefaultHudBackgroundOpacity)

    override suspend fun setInputLocked(locked: Boolean) {
        preference.put(KEY_INPUT_LOCKED.bool to locked)
    }

    override suspend fun setHudOpacity(opacity: Float) {
        preference.put(KEY_HUD_OPACITY.float to opacity.coerceIn(0f, 1f))
    }

    override suspend fun getVisiblePanelsPositions(): Map<String, HudStoredPosition> {
        val listPanels = preference.get(KEY_VISIBLE_PANELS.strSet, emptySet())

        return buildMap {
            listPanels.forEach { panelId ->
                readPositionOrNull(panelId)?.let { put(panelId, it) }
            }
        }
    }

    override suspend fun saveVisiblePanels(ids: Set<String>) {
        preference.put(KEY_VISIBLE_PANELS.strSet to ids)
    }

    override suspend fun getVisiblePanelsBackup(): Set<String> = preference.get(
        KEY_VISIBLE_PANELS_BACKUP.strSet,
        emptySet(),
    )

    override suspend fun saveVisiblePanelsBackup(ids: Set<String>) {
        preference.put(KEY_VISIBLE_PANELS_BACKUP.strSet to ids)
    }

    override suspend fun clearVisiblePanelsBackup() {
        preference.put(KEY_VISIBLE_PANELS_BACKUP.strSet to emptySet())
    }

    override suspend fun savePosition(panelId: String, position: HudStoredPosition) {
        when (position) {
            is HudStoredPosition.Absolute -> {
                preference.put(positionXKey(panelId) to position.offset.x)
                preference.put(positionYKey(panelId) to position.offset.y)
                preference.remove(positionXFractionKey(panelId))
                preference.remove(positionYFractionKey(panelId))
            }

            is HudStoredPosition.Normalized -> {
                preference.put(positionXFractionKey(panelId) to position.xFraction.coerceIn(0f, 1f))
                preference.put(positionYFractionKey(panelId) to position.yFraction.coerceIn(0f, 1f))
                preference.remove(positionXKey(panelId))
                preference.remove(positionYKey(panelId))
            }
        }
    }

    private suspend fun readPositionOrNull(panelId: String): HudStoredPosition? {
        val xFraction = preference.getOrNull(positionXFractionKey(panelId))
        val yFraction = preference.getOrNull(positionYFractionKey(panelId))
        if (xFraction != null && yFraction != null && xFraction.isFinite() && yFraction.isFinite()) {
            return HudStoredPosition.Normalized(
                xFraction = xFraction.coerceIn(0f, 1f),
                yFraction = yFraction.coerceIn(0f, 1f),
            )
        }

        val x = preference.getOrNull(positionXKey(panelId))
        val y = preference.getOrNull(positionYKey(panelId))
        if (x != null && y != null) {
            return HudStoredPosition.Absolute(IntOffset(x, y))
        }

        return null
    }

    private companion object {
        const val TELEMETRY_HUD_ENABLED = "telemetry_hud_enabled"
        const val KEY_VISIBLE_PANELS = "hud_visible_panels"
        const val KEY_VISIBLE_PANELS_BACKUP = "hud_visible_panels_backup"
        const val KEY_INPUT_LOCKED = "hud_input_locked"
        const val KEY_HUD_OPACITY = "hud_opacity"
        fun positionXKey(id: String) = "hud_${id}_x".int
        fun positionYKey(id: String) = "hud_${id}_y".int
        fun positionXFractionKey(id: String) = "hud_${id}_x_fraction".float
        fun positionYFractionKey(id: String) = "hud_${id}_y_fraction".float
    }
}
