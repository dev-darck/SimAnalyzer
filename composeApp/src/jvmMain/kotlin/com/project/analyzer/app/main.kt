package com.project.analyzer.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import com.analyzer.settings.api.AppCloseBehavior
import com.project.analyzer.app.frame.FrameDecorator
import com.project.analyzer.app.tray.CustomTray
import com.project.analyzer.composeApp.Res.Res
import com.project.analyzer.composeApp.Res.app_icon
import com.project.analyzer.crash.presentation.CrashBoundary
import com.project.analyzer.impl.compose.OverlayWindow
import com.project.analyzer.impl.di.AppComponent
import com.project.analyzer.impl.di.createAppComponent
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.theme.ThemeMode
import com.project.analyzer.utils.SingleInstanceGuard
import com.project.analyzer.utils.logger.LogbackConfigurator
import com.project.analyzer.utils.resolveAppDirectories
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.awt.SystemTray

suspend fun main() {
    val appDirectories = resolveAppDirectories(Dispatchers.IO)
    SingleInstanceGuard.acquireOrExit(appDirectories.lockFile, Dispatchers.IO)
    LogbackConfigurator.configure(appDirectories.logsDir)
    val appGraph = createAppComponent(appDirectories)
    val mainViewModel = MainViewModel(
        closeBehaviorRepository = appGraph.appCloseBehaviorRepository,
        initialCloseBehavior = appGraph.appCloseBehaviorRepository.loadCloseBehavior(),
    )
    try {
        awaitApplication {
            CompositionLocalProvider(LocalMetroViewModelFactory provides appGraph.metroViewModelFactory) {
                val themeMode by appGraph.themeRepository.observeThemeMode().collectAsState(ThemeMode.System)

                SimAnalyzerTheme(themeMode = themeMode) {
                    CrashBoundary(
                        createCrashReportUseCase = appGraph.createCrashReportUseCase,
                        appVersion = BuildConfig.VERSION_NAME,
                    ) {
                        App(
                            appGraph = appGraph,
                            mainViewModel = mainViewModel,
                        )
                    }
                }
            }
        }
    } finally {
        runCatching { appGraph.appLifecycle.stop() }
        SingleInstanceGuard.release(Dispatchers.IO)
    }
}

@Composable
private fun ApplicationScope.App(appGraph: AppComponent, mainViewModel: MainViewModel) {
    var showAppWindow by remember { mutableStateOf(true) }
    val mainState by mainViewModel.state.collectAsState()
    val showHud by appGraph.hudPreferences.observeHudEnabled().collectAsState(true)
    var showOverlay by remember { mutableStateOf(true) }
    val isSystemTraySupported = remember { SystemTray.isSupported() }
    val scope = rememberCoroutineScope()
    val navigationState = appGraph.navigationHost.rememberNavigationState()

    val appState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
    )

    LaunchedEffect(appGraph) {
        appGraph.appLifecycle.start()
    }

    val overlayDependencies = remember(appGraph) {
        AppOverlayWindowDependencies(appGraph)
    }

    fun applyCloseAction(action: AppCloseAction) {
        when (action) {
            AppCloseAction.Ask -> Unit
            AppCloseAction.Exit -> exitApplication()
            AppCloseAction.MinimizeToTray -> showAppWindow = false
        }
    }

    fun handleMainWindowCloseRequest() {
        applyCloseAction(mainViewModel.handleWindowCloseRequest(isSystemTraySupported))
    }

    CustomTray(
        brandName = BuildConfig.APP_NAME,
        overlayVisible = showHud && showOverlay,
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
            onCloseRequest = ::handleMainWindowCloseRequest,
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
                    navigationHost = appGraph.navigationHost,
                    navigationState = navigationState,
                    decorator = decorator,
                    onCloseRequest = ::handleMainWindowCloseRequest,
                )
            }

            if (mainState.showCloseBehaviorDialog) {
                AppCloseBehaviorDialog(
                    rememberDecision = mainState.rememberCloseBehaviorDecision,
                    onRememberDecisionChange = {
                        mainViewModel.dispatch(MainIntent.ChangeRememberCloseBehaviorDecision(it))
                    },
                    onDismissRequest = { mainViewModel.dispatch(MainIntent.DismissCloseBehaviorDialog) },
                    onExitClick = {
                        scope.launch {
                            applyCloseAction(mainViewModel.confirmCloseBehavior(AppCloseBehavior.Exit))
                        }
                    },
                    onMinimizeToTrayClick = {
                        scope.launch {
                            applyCloseAction(mainViewModel.confirmCloseBehavior(AppCloseBehavior.MinimizeToTray))
                        }
                    },
                )
            }
        }
    }

    OverlayWindow(
        visible = showHud && showOverlay,
        onCloseRequest = { showOverlay = false },
        dependencies = overlayDependencies,
        state = rememberWindowState(),
    )
}
