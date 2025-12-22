package com.project.analyzer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.project.analyzer.impl.di.createAppGraph
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

fun main() = application {
    val appGraph = createAppGraph()

    Window(
        onCloseRequest = ::exitApplication,
        title = "SimAnalayzer",
    ) {
        CompositionLocalProvider(
            LocalMetroViewModelFactory provides appGraph.metroViewModelFactory
        ) {
            App()
        }
    }
}
