package com.project.analyzer.game.api

public data class GameConfig(
    val titlePatterns: List<String> = emptyList(),
    val processNames: List<String> = emptyList(),
    val windowClassNames: List<String> = emptyList()
)
