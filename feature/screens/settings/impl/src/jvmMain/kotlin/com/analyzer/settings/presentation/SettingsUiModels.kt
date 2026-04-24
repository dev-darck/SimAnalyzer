package com.analyzer.settings.presentation

import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionDefaults
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal data class GameSelectionOptionUi(val selection: GameSelection)

internal data class GameSelectionUi(val selection: GameSelection, val options: ImmutableList<GameSelectionOptionUi>)

internal fun defaultGameSelectionOptions(): List<GameSelectionOptionUi> = listOf(
    GameSelectionOptionUi(selection = GameSelection.Auto),
    GameSelectionOptionUi(selection = GameSelection.Manual(GameId.AC)),
    GameSelectionOptionUi(selection = GameSelection.Manual(GameId.ACC)),
    GameSelectionOptionUi(selection = GameSelection.Manual(GameId.ACE)),
    GameSelectionOptionUi(selection = GameSelection.Manual(GameId.LMU)),
)

internal fun buildGameSelectionUi(
    selection: GameSelection,
    variantOverride: GameId? = null,
    options: List<GameSelectionOptionUi> = defaultGameSelectionOptions(),
): GameSelectionUi {
    val preferredSelection = if (
        selection is GameSelection.Manual &&
        selection.game == GameId.AC &&
        variantOverride != null &&
        variantOverride.isAcFamily()
    ) {
        GameSelection.Manual(variantOverride)
    } else {
        selection
    }
    val selected = options.firstOrNull { it.selection == preferredSelection }
        ?: options.firstOrNull { it.selection == selection }
        ?: options.first()
    return GameSelectionUi(
        selection = selected.selection,
        options = options.toImmutableList(),
    )
}

private fun GameId.isAcFamily(): Boolean = when (this) {
    GameId.AC, GameId.ACC, GameId.ACE -> true
    GameId.LMU -> false
}

internal enum class RecordingWarningKind {
    UnlimitedHighRate,
    Unlimited,
    HighRateManyLaps,
    HighRate,
    ManyLaps,
}

internal fun buildRecordingWarning(
    recordingEnabled: Boolean,
    samplingRateHz: Int,
    maxRecordedLaps: Int,
): RecordingWarningKind? {
    if (!recordingEnabled) return null

    val highRate = samplingRateHz >= 80
    val unlimited = maxRecordedLaps == 0
    val manyLaps = maxRecordedLaps >= 50

    return when {
        unlimited && highRate -> RecordingWarningKind.UnlimitedHighRate
        unlimited -> RecordingWarningKind.Unlimited
        highRate && manyLaps -> RecordingWarningKind.HighRateManyLaps
        highRate -> RecordingWarningKind.HighRate
        manyLaps -> RecordingWarningKind.ManyLaps
        else -> null
    }
}

internal data class LmuPluginDialogDetailsUi(
    val latestVersion: String? = null,
    val repositoryUrl: String = "",
    val downloadPageUrl: String? = null,
    val archiveSha256: String? = null,
    val pluginSha256: String? = null,
    val gameInstallDir: String? = null,
    val pluginTargetPath: String? = null,
    val configTargetPath: String? = null,
)

internal enum class LmuPluginInstallStepUi {
    ResolvingSource,
    DownloadingPackage,
    ValidatingPackage,
    ExtractingPlugin,
    WritingConfiguration,
    Finalizing,
}

internal enum class StorageValidationUi {
    Empty,
    NotAbsolutePath,
    NotADirectory,
    NotWritable,
    CannotCreate,
}

internal object TelemetryAcquisitionUiLimits {
    const val MIN_RATE = TelemetryAcquisitionDefaults.MIN_SAMPLING_RATE_HZ
    const val MAX_RATE = TelemetryAcquisitionDefaults.MAX_SAMPLING_RATE_HZ
    const val MIN_LAPS = TelemetryAcquisitionDefaults.MIN_MAX_RECORDED_LAPS
    const val MAX_LAPS = TelemetryAcquisitionDefaults.MAX_MAX_RECORDED_LAPS
    const val MID_RATE = (MIN_RATE + MAX_RATE) / 2
    const val MID_LAPS = (MIN_LAPS + MAX_LAPS) / 2
}
