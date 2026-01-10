package com.project.analyzer.crash.di

import com.project.analyzer.crash.presentation.CrashViewModel
import dev.zacsweers.metro.DependencyGraph

@DependencyGraph(CrashScope::class)
interface CrashGraph {

    val crashViewModel: CrashViewModel
}
