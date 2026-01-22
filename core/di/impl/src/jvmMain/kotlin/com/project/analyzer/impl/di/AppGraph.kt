package com.project.analyzer.impl.di

import com.analyzer.settings.data.theme.ThemeRepository
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.hud.api.HudGraph
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.impl.compose.HudPreferences
import com.project.analyzer.navigation.api.NavigationGraph
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.preference.api.PreferenceGraph
import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@DependencyGraph(
    AppScope::class,
    additionalScopes = [SessionScope::class, ScreenScope::class, HudScope::class, NavigationScope::class]
)
interface AppGraph : ViewModelGraph, NavigationGraph, HudGraph, PreferenceGraph {

    val telemetryLifecycle: TelemetryLifecycle
    val themeRepository: ThemeRepository
    val hudPreferences: HudPreferences
}

fun createAppGraph(): AppGraph = createGraph()
