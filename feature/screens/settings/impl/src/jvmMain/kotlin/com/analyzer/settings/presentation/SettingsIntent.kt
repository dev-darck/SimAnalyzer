package com.analyzer.settings.presentation

import com.analyzer.settings.api.AppCloseBehavior
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.ThemeMode

internal sealed interface SettingsIntent {
    data class ChangeTheme(val mode: ThemeMode) : SettingsIntent
    data class ChangeAppCloseBehavior(val behavior: AppCloseBehavior) : SettingsIntent
    data class ChangeSamplingRate(val hz: Int) : SettingsIntent
    data class ChangeStorageLocation(val path: String) : SettingsIntent
    data class ChangeStorageLocationInput(val path: String) : SettingsIntent
    data object CommitStorageLocationInput : SettingsIntent
    data class ChangeHudEnabled(val enabled: Boolean) : SettingsIntent
    data class ChangeRecordingEnabled(val enabled: Boolean) : SettingsIntent
    data object DismissRecordingEnabledNotice : SettingsIntent
    data class ChangeMaxRecordedLaps(val laps: Int) : SettingsIntent
    data class ChangeGameSelection(val selection: GameSelection) : SettingsIntent
    data object ConfirmLmuPluginInstall : SettingsIntent
    data object DismissLmuPluginDialog : SettingsIntent
}
