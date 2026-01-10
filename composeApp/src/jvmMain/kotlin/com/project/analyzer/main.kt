package com.project.analyzer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import com.project.analyzer.impl.compose.OverlayWindow
import com.project.analyzer.impl.di.createAppGraph
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import java.awt.SystemTray

suspend fun main() {
    val appGraph = createAppGraph()
    awaitApplication {
        CompositionLocalProvider(LocalMetroViewModelFactory provides appGraph.metroViewModelFactory) {
            SimAnalyzerTheme {
                var showAppWindow by remember { mutableStateOf(true) }
                var showOverlay by remember { mutableStateOf(appGraph.hudPanels.isNotEmpty()) }
                val isSystemTraySupported = remember { SystemTray.isSupported() }

                val appState = rememberWindowState(
                    position = WindowPosition(Alignment.Center)
                )

                CustomTray(
                    overlayVisible = showOverlay,
                    onMainAction = {
                        showAppWindow = true
                    },
                    onOverlayToggle = { showOverlay = !showOverlay }
                )

                Window(
                    visible = showAppWindow,
                    onCloseRequest = {
                        if (isSystemTraySupported) showAppWindow = false else exitApplication()
                    },
                    title = "SimAnalyzer",
                    state = appState,
                ) {

                }

                OverlayWindow(
                    visible = showOverlay,
                    onCloseRequest = { showOverlay = false },
                    panels = appGraph.hudPanels,
                    state = rememberWindowState(
                        placement = WindowPlacement.Maximized
                    )
                )
            }
        }
    }
    appGraph.telemetryLifecycle.finishTelemetry()
}
