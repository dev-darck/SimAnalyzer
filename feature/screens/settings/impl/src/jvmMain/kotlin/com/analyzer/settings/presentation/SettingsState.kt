package com.analyzer.settings.presentation

import androidx.compose.runtime.Stable
import com.analyzer.settings.api.AppCloseBehavior
import com.analyzer.settings.domain.model.LmuPluginInstallStep
import com.analyzer.settings.domain.model.LmuPluginSetupDetails
import com.analyzer.settings.domain.model.StorageValidationResult
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.ThemeMode

internal enum class StorageSizeUnit {
    B,
    KB,
    MB,
    GB,
    TB,
}

@Stable
internal sealed interface StorageSizeInfo {
    data object Unknown : StorageSizeInfo
    data object Zero : StorageSizeInfo
    data class Value(val size: Double, val fractionDigits: Int, val unit: StorageSizeUnit) : StorageSizeInfo
}

internal enum class LmuPluginDialogPhase {
    Prompt,
    Installing,
    Success,
    Failure,
    MissingGame,
}

@Stable
internal data class LmuPluginDialogState(
    val phase: LmuPluginDialogPhase,
    val details: LmuPluginSetupDetails,
    val progressStep: LmuPluginInstallStep? = null,
    val detailMessage: String? = null,
)

internal data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.System,
    val appCloseBehavior: AppCloseBehavior = AppCloseBehavior.AskEveryTime,
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
    val lmuPluginDialog: LmuPluginDialogState? = null,
)
