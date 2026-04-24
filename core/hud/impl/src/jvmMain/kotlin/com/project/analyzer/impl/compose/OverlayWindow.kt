package com.project.analyzer.impl.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.project.analyzer.impl.setup.game.OverlayController
import com.project.analyzer.impl.setup.region.HitRegions
import com.project.analyzer.impl.setup.region.internal.InMemoryHitRegions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.swing.Swing
import java.util.concurrent.Executors

@Composable
fun OverlayWindow(
    onCloseRequest: () -> Unit,
    dependencies: OverlayWindowDependencies,
    visible: Boolean = true,
    state: WindowState = rememberWindowState(),
) {
    if (!visible) return

    val panels = remember(dependencies) { dependencies.panels }
    val gameDetectorFactory = remember(dependencies) { dependencies.gameDetectorFactory }

    val scope = rememberCoroutineScope()
    val hitRegions: HitRegions = remember { InMemoryHitRegions() }

    val winApiDispatcher = remember {
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "winapi-overlay").apply { isDaemon = true }
        }.asCoroutineDispatcher()
    }

    val gameDetector = remember {
        gameDetectorFactory.create(
            requireForeground = true,
            coroutineDispatcher = winApiDispatcher,
        )
    }

    val overlayController = remember(gameDetector, hitRegions) {
        OverlayController(
            gameDetector = gameDetector,
            hitRegions = hitRegions,
            coroutineDispatcher = winApiDispatcher,
            swingDispatcher = Dispatchers.Swing,
        )
    }

    val overlayState by overlayController.state.collectAsState()
    val overlayWindowVisible = visible && overlayState.isVisible

    Window(
        visible = overlayWindowVisible,
        onCloseRequest = onCloseRequest,
        title = "HUD Overlay",
        transparent = true,
        undecorated = true,
        resizable = false,
        alwaysOnTop = true,
        focusable = false,
        state = state,
    ) {
        DisposableEffect(Unit) {
            overlayController.attach(window = window, scope = scope)
            onDispose {
                overlayController.detach()
                winApiDispatcher.close()
            }
        }

        LaunchedEffect(overlayWindowVisible) {
            overlayController.onComposeWindowVisibilityChanged(overlayWindowVisible)
        }

        if (overlayState.isVisible) {
            HudHost(
                panels = panels,
                hitRegions = hitRegions,
                overlayController = overlayController,
            )
        }
    }
}
