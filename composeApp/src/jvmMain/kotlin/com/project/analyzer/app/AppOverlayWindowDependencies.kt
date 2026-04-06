package com.project.analyzer.app

import com.project.analyzer.game.api.GameDetectorFactory
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.impl.compose.OverlayWindowDependencies
import com.project.analyzer.impl.di.AppComponent
import com.project.analyzer.impl.di.createGameDetectorGraph
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet

internal class AppOverlayWindowDependencies(private val appGraph: AppComponent) : OverlayWindowDependencies {

    override val panels: ImmutableSet<HudPanel> by lazy(LazyThreadSafetyMode.NONE) {
        appGraph.hudPanels.toImmutableSet()
    }

    override val gameDetectorFactory: GameDetectorFactory by lazy(LazyThreadSafetyMode.NONE) {
        appGraph.createGameDetectorGraph().gameDetectorFactory
    }
}
