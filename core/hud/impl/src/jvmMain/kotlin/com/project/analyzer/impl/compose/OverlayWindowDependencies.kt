package com.project.analyzer.impl.compose

import com.project.analyzer.game.api.GameDetectorFactory
import com.project.analyzer.hud.api.HudPanel
import kotlinx.collections.immutable.ImmutableSet

interface OverlayWindowDependencies {

    val panels: ImmutableSet<HudPanel>
    val gameDetectorFactory: GameDetectorFactory
}
