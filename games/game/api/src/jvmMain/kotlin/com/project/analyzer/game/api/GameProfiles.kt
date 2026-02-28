package com.project.analyzer.game.api

public object GameProfiles {

    public val profiles: List<GameProfile> = listOf(
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

    public fun detectorConfigs(): List<GameConfig> = buildList {
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

    public fun match(info: GameWindowInfo): GameProfile? {
        val processMatch = profiles.firstOrNull { it.config.matchesProcessName(info.processName) }
        if (processMatch != null) return processMatch

        val classMatch = profiles.firstOrNull { it.config.matchesClassName(info.className) }
        if (classMatch != null) return classMatch

        return profiles.firstOrNull { it.config.matchesTitle(info.title) }
    }
}

private fun GameConfig.matchesProcessName(processName: String): Boolean =
    processNames.any { processName.equals(it, ignoreCase = true) }

private fun GameConfig.matchesTitle(title: String): Boolean =
    titlePatterns.any { title.contains(it, ignoreCase = true) }

private fun GameConfig.matchesClassName(className: String): Boolean =
    windowClassNames.any { className.equals(it, ignoreCase = true) }
