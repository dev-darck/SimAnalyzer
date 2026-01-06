package com.project.analyzer

import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.project.analyzer.impl.compose.OverlayWindow
import com.project.analyzer.impl.di.createAppGraph
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

fun main() = application {
    val appGraph = createAppGraph()

    CompositionLocalProvider(LocalMetroViewModelFactory provides appGraph.metroViewModelFactory) {
        MaterialTheme {
            var showAppWindow by remember { mutableStateOf(true) }
            var showOverlay by remember { mutableStateOf(false) }

            val appState = rememberWindowState(
                position = WindowPosition(Alignment.Center)
            )

            SetupTray(
                overlayVisible = showOverlay,
                mainAction = { showAppWindow = true },
                overLayAction = { showOverlay = !showOverlay }
            )

            Window(
                visible = showAppWindow,
                onCloseRequest = {
                    showAppWindow = false
                },
                title = "SimAnalyzer",
                state = appState,
            ) {

            }

            val overlayState = rememberWindowState(
                position = WindowPosition(Alignment.Center),
                placement = WindowPlacement.Fullscreen,
            )
            OverlayWindow(
                visible = showOverlay,
                onCloseRequest = { showOverlay = false },
                panels = appGraph.hudPanels,
                gameTitles = listOf("Assetto Corsa", "Evo"),
                state = overlayState
            )
        }
    }
}

@Composable
private fun ApplicationScope.SetupTray(
    overlayVisible: Boolean = true,
    mainAction: () -> Unit = {},
    overLayAction: () -> Unit = {}
) {
    val trayState = rememberTrayState()

    Tray(
        state = trayState,
        icon = rememberVectorPainter(Icons.Default.Home),
        tooltip = "SimAnalyzer",
        onAction = mainAction,
        menu = {
            if (overlayVisible) {
                Item("Hide hud", onClick = overLayAction)
            } else {
                Item("Show hud", onClick = overLayAction)
            }
            Separator()
            Item("Exit", onClick = ::exitApplication)
        }
    )
}
