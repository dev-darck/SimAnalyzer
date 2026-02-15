package com.project.analyzer.impl.di

import com.analyzer.settings.data.theme.ThemeRepository
import com.project.analyzer.api.di.AppLifecycle
import com.project.analyzer.hud.api.HudGraph
import com.project.analyzer.impl.compose.HudPreferences
import com.project.analyzer.navigation.api.NavigationGraph
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

interface AppComponent : ViewModelGraph, NavigationGraph, HudGraph {

    val appLifecycle: AppLifecycle
    val themeRepository: ThemeRepository
    val hudPreferences: HudPreferences
}

fun createAppComponent(): AppComponent = createGraph<AppGraph>()
