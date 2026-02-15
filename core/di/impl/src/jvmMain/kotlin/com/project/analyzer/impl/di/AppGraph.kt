package com.project.analyzer.impl.di

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.preference.api.PreferenceGraph
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph

@DependencyGraph(
    AppScope::class,
    additionalScopes = [SessionScope::class, ScreenScope::class, HudScope::class, NavigationScope::class]
)
interface AppGraph : AppComponent, PreferenceGraph
