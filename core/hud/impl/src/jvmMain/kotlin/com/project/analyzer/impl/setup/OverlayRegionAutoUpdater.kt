package com.project.analyzer.impl.setup

import com.project.analyzer.hud.setup.region.HitRegions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Window
import javax.swing.SwingUtilities

internal class OverlayRegionAutoUpdater(
    private val window: Window,
    private val hitRegions: HitRegions,
    private val controller: OverlayRegionController
) {

    fun start(scope: CoroutineScope): Job {
        return scope.launch {
            var lastSeenVer = hitRegions.version()
            var lastAppliedVer = -1L
            var lastChangeMs = System.currentTimeMillis()

            while (isActive) {
                val v = hitRegions.version()
                if (v != lastSeenVer) {
                    lastSeenVer = v
                    lastChangeMs = System.currentTimeMillis()
                }

                val stable = (System.currentTimeMillis() - lastChangeMs) >= 200L

                if (stable && !controller.isDragging() && v != lastAppliedVer) {
                    lastAppliedVer = v
                    val rects = hitRegions.snapshot()
                    SwingUtilities.invokeLater {
                        WindowsOverlayRegion.apply(window, rects)
                    }
                }

                delay(50)
            }
        }
    }
}
