package com.project.analyzer.impl.di

import com.project.analyzer.game.api.GameDetectorGraph
import com.project.analyzer.hud.api.HudGraph
import com.project.analyzer.navigation.api.NavigationGraph

class AppFeatureComponents internal constructor(private val app: AppComponent) {
    val navigation: NavigationGraph = app
    val hud: HudGraph = app

    val gameDetector: GameDetectorGraph by lazy(LazyThreadSafetyMode.NONE) {
        app.createGameDetectorGraph()
    }
}

fun AppComponent.createFeatureComponents(): AppFeatureComponents = AppFeatureComponents(this)
