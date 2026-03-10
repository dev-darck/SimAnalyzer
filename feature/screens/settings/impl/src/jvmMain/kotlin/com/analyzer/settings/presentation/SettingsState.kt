package com.analyzer.settings.presentation

import com.analyzer.settings.data.telemetry.StorageValidationResult
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.ThemeMode

internal enum class StorageSizeUnit {
    B,
    KB,
    MB,
    GB,
    TB,
}

internal sealed interface StorageSizeInfo {
    data object Unknown : StorageSizeInfo
    data object Zero : StorageSizeInfo
    data class Value(val size: Double, val fractionDigits: Int, val unit: StorageSizeUnit) : StorageSizeInfo
}

internal data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.System,
    val samplingRateHz: Int = 70,
    val storageLocation: String = "",
    val isStorageLocationValid: Boolean = true,
    val storageLocationError: StorageValidationResult? = null,
    val storageSizeInfo: StorageSizeInfo = StorageSizeInfo.Unknown,
    val recordingEnabled: Boolean = false,
    val recordingWarning: RecordingWarningKind? = null,
    val showRecordingEnabledNotice: Boolean = false,
    val maxRecordedLaps: Int = 0,
    val hudEnabled: Boolean = true,
    val gameSelection: GameSelection = GameSelection.Auto,
    val gameSelectionUi: GameSelectionUi = buildGameSelectionUi(GameSelection.Auto),
)
