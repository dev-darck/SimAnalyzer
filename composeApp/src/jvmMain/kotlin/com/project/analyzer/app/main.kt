package com.project.analyzer.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import com.project.analyzer.composeApp.Res.Res
import com.project.analyzer.composeApp.Res.app_icon
import com.project.analyzer.crash.presentation.CrashBoundary
import com.project.analyzer.impl.compose.OverlayWindow
import com.project.analyzer.impl.di.AppComponent
import com.project.analyzer.impl.di.createAppComponent
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.impl.rememberNavigationState
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.theme.ThemeMode
import com.project.analyzer.utils.LogbackConfigurator
import com.project.analyzer.utils.SingleInstanceGuard
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.awt.SystemTray

suspend fun main() {
    SingleInstanceGuard.acquireOrExit()
    val appGraph = createAppComponent()
    LogbackConfigurator.configure()
    try {
        appGraph.appLifecycle.start()
        awaitApplication {
            CompositionLocalProvider(LocalMetroViewModelFactory provides appGraph.metroViewModelFactory) {
                var themeMode by remember { mutableStateOf(ThemeMode.System) }

                LaunchedEffect(Unit) {
                    appGraph.themeRepository.observeThemeMode().collect { mode ->
                        themeMode = mode
                    }
                }

                SimAnalyzerTheme(themeMode = themeMode) {
                    CrashBoundary {
                        App(appGraph)
                    }
                }
            }
        }
    } finally {
        runCatching { appGraph.appLifecycle.stop() }
        SingleInstanceGuard.release()
    }
}

@Composable
private fun ApplicationScope.App(appGraph: AppComponent) {
    var showAppWindow by remember { mutableStateOf(true) }
    val showHud by appGraph.hudPreferences.observeHudEnabled().collectAsState(true)
    var showOverlay by remember { mutableStateOf(appGraph.hudPanels.isNotEmpty()) }
    val isSystemTraySupported = remember { SystemTray.isSupported() }
    val navigationState = rememberNavigationState()

    val appState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
    )

    CustomTray(
        brandName = BuildConfig.APP_NAME,
        overlayVisible = showOverlay,
        onMainAction = { showAppWindow = true },
        onOverlayToggle = { showOverlay = !showOverlay },
        onOpenSession = {
            showAppWindow = true
            navigationState.switchTopLevel(Root.Session)
        },
    )

    Window(
        visible = showAppWindow,
        onCloseRequest = { if (isSystemTraySupported) showAppWindow = false else exitApplication() },
        title = BuildConfig.APP_NAME,
        icon = painterResource(Res.drawable.app_icon),
        state = appState,
    ) {
        val density = LocalDensity.current
        LaunchedEffect(density) {
            val minW = with(density) { 800.dp.roundToPx() }
            val minH = with(density) { 500.dp.roundToPx() }
            window.minimumSize = Dimension(minW, minH)
        }

        FrameDecorator { decorator ->
            App(
                providerFactory = appGraph.entryProviderFactory,
                navigationState = navigationState,
                decorator = decorator,
                onCloseRequest = { if (isSystemTraySupported) showAppWindow = false else exitApplication() },
            )
        }
    }

    OverlayWindow(
        visible = showHud && showOverlay,
        onCloseRequest = { showOverlay = false },
        panels = appGraph.hudPanels,
        state = rememberWindowState(),
    )
}
