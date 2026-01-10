package com.project.analyzer.impl.setup.game

import com.project.analyzer.impl.setup.WindowsOverlayRegion
import com.project.analyzer.impl.setup.region.HitRegions
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.awt.Rectangle
import java.awt.Window
import javax.swing.SwingUtilities

class OverlayController(
    private val gameDetector: GameDetector,
    private val hitRegions: HitRegions,
    private val coroutineDispatcher: CoroutineDispatcher
) {

    private val _state = MutableStateFlow(OverlayState())
    val state: StateFlow<OverlayState> = _state.asStateFlow()
    private val user32 = User32.INSTANCE
    private var overlayWindow: Window? = null
    private var overlayHwnd: HWND? = null
    private var trackingJob: Job? = null
    private var hitRegionsJob: Job? = null
    private var isDragging = false
    private var lastBounds: Rectangle? = null

    fun attach(window: Window, scope: CoroutineScope) {
        overlayWindow = window
        overlayHwnd = HWND(Native.getComponentPointer(window))

        gameDetector.setOverlayHwnd(overlayHwnd)

        applyOverlayStyles(window)
        startTracking(scope)
        startHitRegionsObserver(scope)
    }

    fun detach() {
        trackingJob?.cancel()
        trackingJob = null
        hitRegionsJob?.cancel()
        hitRegionsJob = null
        gameDetector.setOverlayHwnd(null)
        overlayWindow = null
        overlayHwnd = null
        lastBounds = null
    }

    private fun startHitRegionsObserver(scope: CoroutineScope) {
        hitRegionsJob = scope.launch {
            hitRegions.observeChanges().collect { regions ->
                val window = overlayWindow ?: return@collect
                if (!isDragging && window.isVisible) {
                    SwingUtilities.invokeLater {
                        if (regions.isEmpty()) {
                            WindowsOverlayRegion.applyFullWindow(window)
                        } else {
                            WindowsOverlayRegion.apply(window, regions)
                        }
                    }
                }
            }
        }
    }

    fun beginDrag() {
        if (!isDragging) {
            isDragging = true
            _state.value = _state.value.copy(isDragging = true)

            overlayWindow?.let { window ->
                SwingUtilities.invokeLater {
                    removeTransparentStyle()
                    applyFullWindowRegion(window)
                }
            }
        }
    }

    fun endDrag() {
        if (isDragging) {
            isDragging = false
            _state.value = _state.value.copy(isDragging = false)

            overlayWindow?.let { window ->
                SwingUtilities.invokeLater {
                    applyHitRegions(window)
                }
            }
        }
    }

    private fun startTracking(scope: CoroutineScope) {
        trackingJob = scope.launch(coroutineDispatcher) {
            gameDetector.observeGameWindow().collect { gameInfo ->
                handleGameWindowChange(gameInfo)
            }
        }
    }

    private fun handleGameWindowChange(gameInfo: GameWindowInfo?) {
        val window = overlayWindow ?: return
        val hwnd = overlayHwnd ?: return


        if (gameInfo != null) {
            val bounds = gameInfo.bounds
            val boundsChanged = lastBounds != bounds

            SwingUtilities.invokeLater {

                if (boundsChanged) {
                    window.setBounds(bounds.x, bounds.y, bounds.width, bounds.height)
                    lastBounds = bounds
                }

                if (!window.isVisible) {
                    window.isVisible = true
                    WindowsOverlayRegion.applyFullWindow(window)
                }

                ensureTopmost(hwnd)
            }

            _state.value = OverlayState(
                isVisible = true,
                bounds = bounds,
                gameInfo = gameInfo,
                isDragging = isDragging
            )
        } else {
            if (window.isVisible) {
                SwingUtilities.invokeLater {
                    window.isVisible = false
                }
            }

            _state.value = OverlayState(
                isVisible = false,
                bounds = null,
                gameInfo = null,
                isDragging = isDragging
            )
        }
    }

    private fun ensureTopmost(hwnd: HWND) {
        user32.SetWindowPos(
            hwnd,
            HWND(Pointer.createConstant(-1)), // HWND_TOPMOST
            0, 0, 0, 0,
            WinUser.SWP_NOMOVE or WinUser.SWP_NOSIZE or WinUser.SWP_NOACTIVATE
        )
    }

    private fun applyOverlayStyles(window: Window) {
        val hwnd = HWND(Native.getComponentPointer(window))

        var exStyle = user32.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)

        exStyle = exStyle or WinUser.WS_EX_LAYERED
        exStyle = exStyle or WS_EX_TOOLWINDOW
        exStyle = exStyle and WS_EX_APPWINDOW.inv()
        exStyle = exStyle or WS_EX_NOACTIVATE

        user32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle)
        user32.SetLayeredWindowAttributes(hwnd, 0, 255.toByte(), WinUser.LWA_ALPHA)
    }

    private fun removeTransparentStyle() {
        val hwnd = overlayHwnd ?: return
        var exStyle = user32.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)
        exStyle = exStyle and WinUser.WS_EX_TRANSPARENT.inv()
        user32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle)
    }

    private fun applyHitRegions(window: Window) {
        val regions = hitRegions.snapshot()
        if (regions.isEmpty()) {
            WindowsOverlayRegion.apply(window, emptyList())
        } else {
            WindowsOverlayRegion.apply(window, regions)
        }
    }

    private fun applyFullWindowRegion(window: Window) {
        WindowsOverlayRegion.applyFullWindow(window)
    }

    private companion object {

        const val WS_EX_TOOLWINDOW = 0x00000080
        const val WS_EX_APPWINDOW = 0x00040000
        const val WS_EX_NOACTIVATE = 0x08000000
    }
}
