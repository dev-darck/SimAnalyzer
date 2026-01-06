package com.project.analyzer.impl.setup

import com.project.analyzer.hud.setup.region.HitRegions
import java.awt.Window
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

internal class OverlayRegionController(private val hitRegions: HitRegions) {

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
