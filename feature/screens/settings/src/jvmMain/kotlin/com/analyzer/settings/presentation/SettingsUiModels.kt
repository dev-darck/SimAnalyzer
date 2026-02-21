package com.analyzer.settings.presentation

import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import java.util.Locale

internal data class GameSelectionOptionUi(val selection: GameSelection, val label: String, val subtitle: String)

internal data class GameSelectionUi(
    val selection: GameSelection,
    val label: String,
    val subtitle: String,
    val tag: String,
    val isAuto: Boolean,
    val options: List<GameSelectionOptionUi>,
)

internal fun defaultGameSelectionOptions(): List<GameSelectionOptionUi> = listOf(
    GameSelectionOptionUi(
        selection = GameSelection.Auto,
        label = "Auto detect",
        subtitle = "Waits for a supported game window",
    ),
    GameSelectionOptionUi(
        selection = GameSelection.Manual(GameId.AC),
        label = GameId.AC.displayName,
        subtitle = "Manual selection enabled",
    ),
    GameSelectionOptionUi(
        selection = GameSelection.Manual(GameId.ACC),
        label = GameId.ACC.displayName,
        subtitle = "Manual selection enabled",
    ),
    GameSelectionOptionUi(
        selection = GameSelection.Manual(GameId.ACE),
        label = GameId.ACE.displayName,
        subtitle = "Manual selection enabled",
    ),
//    GameSelectionOptionUi(
//        selection = GameSelection.Manual(GameId.LMU),
//        label = GameId.LMU.displayName,
//        subtitle = "Manual selection enabled"
//    ),
)

internal fun buildGameSelectionUi(
    selection: GameSelection,
    variantOverride: GameId? = null,
    options: List<GameSelectionOptionUi> = defaultGameSelectionOptions(),
): GameSelectionUi {
    val preferredSelection = if (selection is GameSelection.Manual &&
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
    val isAuto = selection is GameSelection.Auto
    return GameSelectionUi(
        selection = selected.selection,
        label = selected.label,
        subtitle = selected.subtitle,
        tag = if (isAuto) "AUTO" else "MANUAL",
        isAuto = isAuto,
        options = options,
    )
}

private fun GameId.isAcFamily(): Boolean = when (this) {
    GameId.AC, GameId.ACC, GameId.ACE -> true
//    GameId.LMU -> false
}

internal fun buildRecordingWarning(recordingEnabled: Boolean, samplingRateHz: Int, maxRecordedLaps: Int): String? {
    if (!recordingEnabled) return null

    val highRate = samplingRateHz >= 80
    val unlimited = maxRecordedLaps == 0
    val manyLaps = maxRecordedLaps >= 50

    return when {
        unlimited && highRate -> "Unlimited laps at high Hz can create very large files."
        unlimited -> "Unlimited laps can create very large files."
        highRate && manyLaps -> "High Hz with many laps can create very large files."
        highRate -> "High Hz recording can create large files."
        manyLaps -> "Many recorded laps can create large files."
        else -> null
    }
}

internal fun formatStorageSizeLabel(bytes: Long?): String {
    val value = bytes ?: return "—"
    if (value <= 0) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = value.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex += 1
    }

    val format = when {
        size >= 100 -> "%.0f"
        size >= 10 -> "%.1f"
        else -> "%.2f"
    }
    return String.format(Locale.US, "$format ${units[unitIndex]}", size)
}
