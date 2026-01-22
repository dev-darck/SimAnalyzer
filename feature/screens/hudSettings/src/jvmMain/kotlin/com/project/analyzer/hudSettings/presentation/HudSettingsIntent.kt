package com.project.analyzer.hudSettings.presentation

internal sealed interface HudSettingsIntent {
    data class TogglePanel(val id: String, val enable: Boolean) : HudSettingsIntent
    data class OnShowPanel(val id: String) : HudSettingsIntent
}
