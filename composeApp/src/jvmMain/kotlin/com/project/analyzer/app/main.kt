package com.project.analyzer.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import com.project.analyzer.crash.presentation.CrashBoundary
import com.project.analyzer.impl.compose.OverlayWindow
import com.project.analyzer.impl.di.AppGraph
import com.project.analyzer.impl.di.createAppGraph
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.utils.LogbackConfigurator
import com.project.analyzer.utils.SingleInstanceGuard
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import java.awt.Dimension
import java.awt.SystemTray

suspend fun main() {
    SingleInstanceGuard.acquireOrExit()
    val appGraph = createAppGraph()
    LogbackConfigurator.configure()
    awaitApplication {
        CompositionLocalProvider(LocalMetroViewModelFactory provides appGraph.metroViewModelFactory) {
            SimAnalyzerTheme {
                CrashBoundary {
                    App(appGraph)
                }
            }
        }
    }
    appGraph.telemetryLifecycle.finishTelemetry()
    SingleInstanceGuard.release()
}

@Composable
private fun ApplicationScope.App(appGraph: AppGraph) {
    var showAppWindow by remember { mutableStateOf(true) }
    var showOverlay by remember { mutableStateOf(appGraph.hudPanels.isNotEmpty()) }
    val isSystemTraySupported = remember { SystemTray.isSupported() }

    val appState = rememberWindowState(
        position = WindowPosition(Alignment.Center)
    )

    CustomTray(
        brandName = BuildConfig.APP_NAME,
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
        title = BuildConfig.APP_NAME,
        state = appState,
    ) {
        val density = LocalDensity.current
        LaunchedEffect(density) {
            val minW = with(density) { 800.dp.roundToPx() }
            val minH = with(density) { 500.dp.roundToPx() }
            window.minimumSize = Dimension(minW, minH)
        }

        App(appGraph.entryProviderFactory)
    }

    OverlayWindow(
        visible = showOverlay,
        onCloseRequest = { showOverlay = false },
        panels = appGraph.hudPanels,
        state = rememberWindowState(
            placement = WindowPlacement.Maximized,
        )
    )
}
