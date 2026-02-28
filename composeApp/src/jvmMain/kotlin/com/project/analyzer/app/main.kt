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
import com.project.analyzer.impl.di.createFeatureComponents
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.impl.rememberNavigationState
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.theme.ThemeMode
import com.project.analyzer.utils.SingleInstanceGuard
import com.project.analyzer.utils.logger.LogbackConfigurator
import com.project.analyzer.utils.resolveAppDirectories
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.awt.SystemTray

suspend fun main() {
    val appDirectories = resolveAppDirectories()
    SingleInstanceGuard.acquireOrExit(appDirectories.lockFile)
    val appGraph = createAppComponent(appDirectories)
    LogbackConfigurator.configure(appDirectories.logsDir)
    try {
        appGraph.appLifecycle.start()
        awaitApplication {
            CompositionLocalProvider(LocalMetroViewModelFactory provides appGraph.metroViewModelFactory) {
                val themeMode by appGraph.themeRepository.observeThemeMode().collectAsState(ThemeMode.System)

                SimAnalyzerTheme(themeMode = themeMode) {
                    CrashBoundary(appVersion = BuildConfig.VERSION_NAME) {
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
    val features = remember(appGraph) { appGraph.createFeatureComponents() }
    var showAppWindow by remember { mutableStateOf(true) }
    val showHud by appGraph.hudPreferences.observeHudEnabled().collectAsState(true)
    var showOverlay by remember { mutableStateOf(features.hud.hudPanels.isNotEmpty()) }
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

    if (showAppWindow) {
        Window(
            visible = true,
            onCloseRequest = { if (isSystemTraySupported) showAppWindow = false else exitApplication() },
            title = BuildConfig.APP_NAME,
            icon = painterResource(Res.drawable.app_icon),
            state = appState,
        ) {
            val density = LocalDensity.current
            LaunchedEffect(density) {
                val minW = with(density) { 800.dp.roundToPx() }
                val minH = with(density) { 600.dp.roundToPx() }

                window.minimumSize = Dimension(minW, minH)
            }

            FrameDecorator { decorator ->
                App(
                    providerFactory = features.navigation.entryProviderFactory,
                    navigationState = navigationState,
                    decorator = decorator,
                    onCloseRequest = { if (isSystemTraySupported) showAppWindow = false else exitApplication() },
                )
            }
        }
    }

    OverlayWindow(
        visible = showHud && showOverlay,
        onCloseRequest = { showOverlay = false },
        panels = features.hud.hudPanels,
        gameDetectorFactory = features.gameDetector.gameDetectorFactory,
        state = rememberWindowState(),
    )
}
