package com.project.analyzer

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.project.analyzer.calibration.presentation.setup.LocalWindow
import com.project.analyzer.impl.di.createAppGraph
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

fun main() = application {
    val appGraph = createAppGraph()

    Window(
        onCloseRequest = ::exitApplication,
        title = "SimAnalyzer – Telemetry Raw Data",
    ) {
        CompositionLocalProvider(
            LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
            LocalWindow provides window
        ) {
            MaterialTheme {

            }
        }
    }

//    if (overlayOpen) {
//        Window(
//            onCloseRequest = { overlayOpen = false },
//            title = "Overlay",
//            transparent = true,
//            undecorated = true,
//            resizable = false,
//            alwaysOnTop = true,
//            focusable = true,
//            state = rememberWindowState()
//        ) {
//            val overlayState by appGraph.overlayDebugBus.state.collectAsState()
//            val hitRegions = remember { InMemoryHitRegions() }
//            val controller = remember { OverlayRegionController(hitRegions) }
//            val scope = rememberCoroutineScope()
//
//            DisposableEffect(Unit) {
//                controller.attach(window)
//                WindowsOverlayMode.applyBaseStyles(window)
//                window.background = java.awt.Color(0, 0, 0, 0)
//
//                val autoUpdater = OverlayRegionAutoUpdater(window, hitRegions, controller)
//                autoUpdater.start(scope)
//
//                scope.launch {
//                    WindowsGameWindowTracker(
//                        overlayWindow = window,
//                        titleContainsAny = listOf("Assetto Corsa", "Evo")
//                    ).startTracking()
//                }
//
//                onDispose { }
//            }
//
//            MaterialTheme {
//                OverlayHud(overlayState, hitRegions, controller)
//            }
//        }
//    }
}
