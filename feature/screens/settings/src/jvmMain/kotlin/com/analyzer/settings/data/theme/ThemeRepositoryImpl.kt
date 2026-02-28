package com.analyzer.settings.data.theme

import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.preference.api.str
import com.project.analyzer.theme.ThemeMode
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
internal class ThemeRepositoryImpl(
    @param:UserPref
    private val preference: Preference,
) : ThemeRepository {
    override suspend fun loadTheme(): ThemeMode {
        val themeName = preference.get(THEME_MODE.str, ThemeMode.System.name)
        return ThemeMode.entries.find { theme -> themeName == theme.name } ?: ThemeMode.System
    }

    override fun observeThemeMode(): Flow<ThemeMode> = preference.observe(THEME_MODE.str, ThemeMode.System.name)
        .map { name ->
            ThemeMode.entries.find {
                it.name == name
            } ?: ThemeMode.System
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        preference.put(THEME_MODE.str to mode.name)
    }

    private companion object {
        const val THEME_MODE = "theme_mode"
    }
}
