package com.analyzer.settings.api

import com.project.analyzer.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

public interface ThemeRepository {

    public suspend fun loadTheme(): ThemeMode
    public fun observeThemeMode(): Flow<ThemeMode>
    public suspend fun setThemeMode(mode: ThemeMode)
}
