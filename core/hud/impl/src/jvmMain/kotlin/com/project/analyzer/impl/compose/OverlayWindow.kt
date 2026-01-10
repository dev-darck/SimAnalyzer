package com.project.analyzer.impl.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.impl.setup.game.GameConfig
import com.project.analyzer.impl.setup.game.GameDetector
import com.project.analyzer.impl.setup.game.OverlayController
import com.project.analyzer.impl.setup.region.HitRegions
import com.project.analyzer.impl.setup.region.internal.InMemoryHitRegions
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

@Composable
fun OverlayWindow(
    onCloseRequest: () -> Unit,
    panels: Set<HudPanel>,
    visible: Boolean = true,
    state: WindowState = rememberWindowState(),
) {
    if (!visible) return

    Window(
        visible = true,
        onCloseRequest = onCloseRequest,
        title = "HUD Overlay",
        transparent = true,
        undecorated = true,
        resizable = false,
        alwaysOnTop = true,
        focusable = false,
        state = state
    ) {
        val scope = rememberCoroutineScope()
        val hitRegions: HitRegions = remember { InMemoryHitRegions() }

        val winApiDispatcher = remember {
            Executors.newSingleThreadExecutor { r ->
                Thread(r, "winapi-overlay").apply { isDaemon = true }
            }.asCoroutineDispatcher()
        }

        val gameDetector = remember {
            val configs = listOf(
                GameConfig(
                    titlePatterns = listOf("Evo"),
                    processNames = listOf("evo.exe", "AssettoCorsaEVO.exe", "acevo", "evo")
                )
            )
            GameDetector(configs = configs, coroutineDispatcher = winApiDispatcher)
        }

        val overlayController = remember(gameDetector, hitRegions) {
            OverlayController(
                gameDetector = gameDetector,
                hitRegions = hitRegions,
                coroutineDispatcher = winApiDispatcher
            )
        }

        val overlayState by overlayController.state.collectAsState()

        DisposableEffect(Unit) {
            overlayController.attach(window = window, scope = scope)
            onDispose {
                overlayController.detach()
                winApiDispatcher.close()
            }
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
