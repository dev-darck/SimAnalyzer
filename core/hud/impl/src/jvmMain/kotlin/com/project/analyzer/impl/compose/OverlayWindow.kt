package com.project.analyzer.impl.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.setup.region.HitRegions
import com.project.analyzer.hud.setup.region.internal.InMemoryHitRegions
import com.project.analyzer.impl.setup.OverlayRegionAutoUpdater
import com.project.analyzer.impl.setup.OverlayRegionController
import com.project.analyzer.impl.setup.WindowsGameWindowTracker
import com.project.analyzer.impl.setup.WindowsOverlayMode
import kotlinx.coroutines.launch
import java.awt.Color

@Composable
fun OverlayWindow(
    onCloseRequest: () -> Unit,
    panels: Set<HudPanel>,
    visible: Boolean = true,
    gameTitles: List<String> = listOf("Assetto Corsa"),
    modifier: Modifier = Modifier,
    state: WindowState = rememberWindowState()
) {
    Window(
        visible = visible,
        onCloseRequest = onCloseRequest,
        title = "HUD Overlay",
        transparent = true,
        undecorated = true,
        resizable = false,
        alwaysOnTop = true,
        focusable = true,
        state = state
    ) {

        LaunchedEffect(Unit) {
            window.background = Color(0, 0, 0, 0)
        }

        val hitRegions: HitRegions = remember { InMemoryHitRegions() }
        val regionController = remember { OverlayRegionController(hitRegions) }
        val scope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            regionController.attach(window)
            WindowsOverlayMode.applyBaseStyles(window)

            val autoUpdater = OverlayRegionAutoUpdater(window, hitRegions, regionController)
            autoUpdater.start(scope)

            scope.launch {
                WindowsGameWindowTracker(
                    overlayWindow = window,
                    titleContainsAny = gameTitles
                ).startTracking()
            }

            onDispose {

            }
        }

        HudHost(
            panels = panels,
            modifier = modifier.fillMaxSize()
        )
    }
}
