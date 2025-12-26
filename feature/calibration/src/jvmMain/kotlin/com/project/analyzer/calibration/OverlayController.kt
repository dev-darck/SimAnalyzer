package com.project.analyzer.calibration

import com.project.analyzer.calibration.presentation.overlay.HitRegions
import java.awt.Window
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OverlayRegionController(private val hitRegions: HitRegions) {
    @Volatile
    private var window: Window? = null
    private val dragging = AtomicBoolean(false)

    fun attach(w: Window) {
        window = w
    }

    fun beginDrag() {
        val w = window ?: return
        if (dragging.compareAndSet(false, true)) {
            SwingUtilities.invokeLater {
                WindowsOverlayRegion.applyFullWindow(w)
            }
        }
    }

    fun endDrag() {
        val w = window ?: return
        if (dragging.compareAndSet(true, false)) {
            val rects = hitRegions.snapshot()
            SwingUtilities.invokeLater {
                WindowsOverlayRegion.apply(w, rects)
            }
        }
    }

    fun isDragging(): Boolean = dragging.get()
}

class OverlayRegionAutoUpdater(
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
