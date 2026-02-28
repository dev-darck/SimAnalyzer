package com.project.analyzer.game.impl

import com.project.analyzer.game.api.GameDetectorGraph
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes

@DependencyGraph(AppScope::class)
internal interface GameDetectorComponent : GameDetectorGraph {

    @DependencyGraph.Factory
    fun interface Factory {
        public fun create(@Includes deps: GameDetectorGraph.Dependencies): GameDetectorComponent
    }
}
