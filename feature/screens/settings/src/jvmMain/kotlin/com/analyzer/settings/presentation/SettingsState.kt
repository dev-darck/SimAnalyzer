package com.analyzer.settings.presentation

import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.ThemeMode

internal data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.System,

    val samplingRateHz: Int = 70,
    val storageLocation: String = "",
    val isStorageLocationValid: Boolean = true,
    val storageLocationError: String? = null,
    val storageSizeBytes: Long? = null,
    val storageSizeLabel: String = "—",
    val recordingEnabled: Boolean = true,
    val recordingWarning: String? = null,
    val maxRecordedLaps: Int = 0,
    val hudEnabled: Boolean = true,
    val gameSelection: GameSelection = GameSelection.Auto,
    val gameSelectionUi: GameSelectionUi = buildGameSelectionUi(GameSelection.Auto),
)
