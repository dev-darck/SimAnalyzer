package com.project.analyzer.game.api

public const val AC_KEY: String = "ac_series"
public const val LMU_KEY: String = "lmu"

public enum class GameId(public val id: String, public val displayName: String) {
    AC(AC_KEY, "Assetto Corsa"),
    ACC(AC_KEY, "Assetto Corsa Competizione"),
    ACE(AC_KEY, "Assetto Corsa Evo"),
    ;

    //    LMU(LMU_KEY, "Le Mans Ultimate");

    public companion object {

        public fun fromId(value: String?): GameId? {
            if (value.isNullOrBlank()) return null

            return entries.firstOrNull { it.id.equals(value, ignoreCase = true) }
        }
    }
}
