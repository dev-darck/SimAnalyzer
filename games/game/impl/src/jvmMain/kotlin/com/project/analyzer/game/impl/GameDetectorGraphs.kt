package com.project.analyzer.game.impl

import com.project.analyzer.game.api.GameDetectorGraph
import dev.zacsweers.metro.createGraphFactory

public fun createGameDetectorComponent(deps: GameDetectorGraph.Dependencies): GameDetectorGraph =
    createGraphFactory<GameDetectorComponent.Factory>().create(deps)
