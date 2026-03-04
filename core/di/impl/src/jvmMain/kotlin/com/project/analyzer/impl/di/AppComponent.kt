package com.project.analyzer.impl.di

import com.analyzer.settings.api.ThemeRepository
import com.project.analyzer.api.di.AppLifecycle
import com.project.analyzer.game.api.GameDetectorGraph
import com.project.analyzer.game.impl.createGameDetectorComponent
import com.project.analyzer.hud.api.HudGraph
import com.project.analyzer.hud.api.HudPreferencesStore
import com.project.analyzer.navigation.api.NavigationGraph
import com.project.analyzer.utils.AppDirectoriesImpl
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

interface AppComponent :
    ViewModelGraph,
    NavigationGraph,
    HudGraph,
    GameDetectorGraph.Dependencies {

    val appLifecycle: AppLifecycle
    val themeRepository: ThemeRepository
    val hudPreferences: HudPreferencesStore
}

fun AppComponent.createGameDetectorGraph(): GameDetectorGraph = createGameDetectorComponent(this)

fun createAppComponent(appDirectories: AppDirectoriesImpl): AppComponent =
    createGraphFactory<AppGraph.Factory>().create(
        object : AppGraph.Dependencies {
            override val appDirectories: AppDirectoriesImpl = appDirectories
        },
    )
