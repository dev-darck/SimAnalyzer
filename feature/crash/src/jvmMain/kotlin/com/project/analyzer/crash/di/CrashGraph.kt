package com.project.analyzer.crash.di

import com.project.analyzer.crash.presentation.CrashViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph

@DependencyGraph(AppScope::class)
internal interface CrashGraph {

    val crashViewModel: CrashViewModel
}
