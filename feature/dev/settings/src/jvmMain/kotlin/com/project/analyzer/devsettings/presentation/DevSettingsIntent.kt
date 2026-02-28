package com.project.analyzer.devsettings.presentation

internal sealed interface DevSettingsIntent {
    data class ToggleHudPanel(val id: String, val enabled: Boolean) : DevSettingsIntent
}
