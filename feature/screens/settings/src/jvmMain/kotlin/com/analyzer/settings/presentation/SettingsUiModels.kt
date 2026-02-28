package com.analyzer.settings.presentation

import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection

internal data class GameSelectionOptionUi(val selection: GameSelection)

internal data class GameSelectionUi(val selection: GameSelection, val options: List<GameSelectionOptionUi>)

internal fun defaultGameSelectionOptions(): List<GameSelectionOptionUi> = listOf(
    GameSelectionOptionUi(selection = GameSelection.Auto),
    GameSelectionOptionUi(selection = GameSelection.Manual(GameId.AC)),
    GameSelectionOptionUi(selection = GameSelection.Manual(GameId.ACC)),
    GameSelectionOptionUi(selection = GameSelection.Manual(GameId.ACE)),
//    GameSelectionOptionUi(
//        selection = GameSelection.Manual(GameId.LMU)
//    ),
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
        options = options,
    )
}

private fun GameId.isAcFamily(): Boolean = when (this) {
    GameId.AC, GameId.ACC, GameId.ACE -> true
//    GameId.LMU -> false
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
