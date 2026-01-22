package com.analyzer.settings.presentation

import com.project.analyzer.theme.ThemeMode

internal sealed interface SettingsIntent {
    data class ChangeTheme(val mode: ThemeMode) : SettingsIntent
    data class ChangeSamplingRate(val hz: Int) : SettingsIntent
    data class ChangeStorageLocation(val path: String) : SettingsIntent
    data class ChangeHudEnabled(val enabled: Boolean) : SettingsIntent
}
