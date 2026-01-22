package com.analyzer.settings.presentation

import com.project.analyzer.theme.ThemeMode

internal data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.System,

    val samplingRateHz: Int = 70,
    val storageLocation: String = "",
    val isStorageLocationValid: Boolean = true,
    val storageLocationError: String? = null,
    val hudEnabled: Boolean = true
)
