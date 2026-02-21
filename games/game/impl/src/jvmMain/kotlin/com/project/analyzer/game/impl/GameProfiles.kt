package com.project.analyzer.game.impl

import com.project.analyzer.game.api.GameConfig
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameProfile

object GameProfiles {

    val profiles: List<GameProfile> = listOf(
        GameProfile(
            id = GameId.ACC,
            config = GameConfig(
                processNames = listOf(
                    "AC2-Win64-Shipping.exe",
                    "acc.exe",
                    "AssettoCorsaCompetizione.exe",
                ),
                titlePatterns = listOf("Competizione"),
            ),
        ),
        GameProfile(
            id = GameId.ACE,
            config = GameConfig(
                processNames = listOf(
                    "evo.exe",
                    "AssettoCorsaEVO.exe",
                    "acevo.exe",
                    "acevo",
                    "evo",
                ),
                titlePatterns = listOf("Evo"),
            ),
        ),
        GameProfile(
            id = GameId.AC,
            config = GameConfig(
                processNames = listOf("acs.exe", "AssettoCorsa.exe"),
                titlePatterns = listOf("Assetto Corsa"),
            ),
        ),
//        GameProfile(
//            id = GameId.LMU,
//            config = GameConfig(
//                processNames = listOf("LeMansUltimate.exe", "LeMansUltimate", "lmu.exe"),
//                titlePatterns = listOf("Le Mans Ultimate", "LeMans Ultimate", "LMU")
//            )
//        ),
    )

    fun detectorConfigs(): List<GameConfig> = buildList {
        profiles.forEach { profile ->
            val config = profile.config
            when {
                config.processNames.isNotEmpty() || config.windowClassNames.isNotEmpty() -> {
                    add(
                        GameConfig(
                            processNames = config.processNames,
                            windowClassNames = config.windowClassNames,
                        ),
                    )
                }

                config.titlePatterns.isNotEmpty() -> {
                    add(GameConfig(titlePatterns = config.titlePatterns))
                }
            }
        }
    }

    fun match(info: GameWindowInfo): GameProfile? {
        val processMatch = profiles.firstOrNull { it.config.matchesProcessName(info.processName) }
        if (processMatch != null) return processMatch

        val classMatch = profiles.firstOrNull { it.config.matchesClassName(info.className) }
        if (classMatch != null) return classMatch

        return profiles.firstOrNull { it.config.matchesTitle(info.title) }
    }
}
