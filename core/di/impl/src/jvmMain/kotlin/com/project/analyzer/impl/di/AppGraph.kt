package com.project.analyzer.impl.di

import com.project.analyzer.api.di.AppEnvironment
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.Main
import com.project.analyzer.navigation.api.NavigationGraph
import com.project.analyzer.telemetry.ac.api.TelemetryDataSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import kotlinx.coroutines.CoroutineDispatcher

@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph, NavigationGraph {

    val env: AppEnvironment

    val telemetryDataSource: TelemetryDataSource

    @IO
    val io: CoroutineDispatcher

    @Default
    val default: CoroutineDispatcher

    @Main
    val main: CoroutineDispatcher
}

fun createAppGraph(): AppGraph = createGraph()
