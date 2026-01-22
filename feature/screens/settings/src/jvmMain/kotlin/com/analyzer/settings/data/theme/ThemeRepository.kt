package com.analyzer.settings.data.theme

import com.project.analyzer.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {

    suspend fun loadTheme(): ThemeMode
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
