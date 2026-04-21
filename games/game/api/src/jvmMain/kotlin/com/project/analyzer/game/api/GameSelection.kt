package com.project.analyzer.game.api

public sealed interface GameSelection {
    public data object Auto : GameSelection

    public data class Manual(val game: GameId) : GameSelection

    public companion object {

        public const val AUTO_ID: String = "auto"

        public fun fromPreference(value: String?, variantOverride: GameId? = null): GameSelection {
            if (value.isNullOrBlank() || value.equals(AUTO_ID, ignoreCase = true)) return Auto
            val game = GameId.fromId(value) ?: return Auto
            return Manual(resolveManualGame(game, variantOverride))
        }

        private fun resolveManualGame(game: GameId, variantOverride: GameId?): GameId {
            if (game != GameId.AC) return game

            return when (variantOverride) {
                GameId.AC,
                GameId.ACC,
                GameId.ACE,
                -> variantOverride

                null,
                GameId.LMU,
                -> game
            }
        }
    }
}

public fun GameSelection.toPreferenceValue(): String = when (this) {
    GameSelection.Auto -> GameSelection.AUTO_ID
    is GameSelection.Manual -> game.id
}
