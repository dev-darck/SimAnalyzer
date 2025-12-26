package com.project.analyzer.calibration

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.awt.Window
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

class WindowsGameWindowTracker(
    private val overlayWindow: Window,
    private val titleContainsAny: List<String>,
    private val pollMs: Long = 150L
) {

    data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {

        val width: Int get() = right - left
        val height: Int get() = bottom - top
    }

    suspend fun startTracking() {
        val user32 = User32.INSTANCE
        var gameHwnd: HWND? = null

        withContext(Dispatchers.IO) {
            var lastVisible: Boolean? = null
            var lastX = Int.MIN_VALUE
            var lastY = Int.MIN_VALUE
            var lastW = Int.MIN_VALUE
            var lastH = Int.MIN_VALUE

            val overlayHwnd = HWND(Native.getComponentPointer(overlayWindow))

            while (isActive) {
                if (gameHwnd == null || !user32.IsWindow(gameHwnd)) gameHwnd = findGameWindow(titleContainsAny)

                val hwnd = gameHwnd
                if (hwnd != null) {
                    val fg = user32.GetForegroundWindow()
                    val visible = (fg != null && (fg == hwnd || fg == overlayHwnd))

                    val rect = getWindowRect(hwnd)

                    SwingUtilities.invokeLater {
                        val newVisible = visible && rect != null
                        if (lastVisible != newVisible) {
                            overlayWindow.isVisible = newVisible
                            lastVisible = newVisible
                        }

                        if (newVisible) {
                            val tx = overlayWindow.graphicsConfiguration.defaultTransform
                            val sx = tx.scaleX.toFloat()
                            val sy = tx.scaleY.toFloat()

                            val x = (rect.left / sx).roundToInt()
                            val y = (rect.top / sy).roundToInt()
                            val w = (rect.width / sx).roundToInt()
                            val h = (rect.height / sy).roundToInt()

                            if (x != lastX || y != lastY || w != lastW || h != lastH) {
                                overlayWindow.setBounds(x, y, w, h)
                                lastX = x
                                lastY = y
                                lastW = w
                                lastH = h
                            }
                        }
                    }
                } else {
                    SwingUtilities.invokeLater { overlayWindow.isVisible = false }
                }

                delay(pollMs)
            }
        }
    }

    private fun getWindowRect(hwnd: HWND): Rect? {
        val r = WinDef.RECT()
        val ok = User32.INSTANCE.GetWindowRect(hwnd, r)
        if (!ok) return null
        return Rect(r.left, r.top, r.right, r.bottom)
    }

    private fun findGameWindow(titleContainsAny: List<String>): HWND? {
        val user32 = User32.INSTANCE
        var found: HWND? = null

        user32.EnumWindows(
            /* lpEnumFunc = */
            WinUser.WNDENUMPROC { hWnd, _ ->
                if (!user32.IsWindowVisible(hWnd)) return@WNDENUMPROC true

                val title = getWindowTitle(hWnd)
                if (title.isNotBlank() && titleContainsAny.any { title.contains(it, ignoreCase = true) }) {
                    found = hWnd
                    return@WNDENUMPROC false
                }
                true
            },
            /* data = */
            null
        )

        return found
    }

    private fun getWindowTitle(hwnd: HWND): String {
        val buf = CharArray(512)
        val len = User32.INSTANCE.GetWindowText(hwnd, buf, buf.size)
        return if (len > 0) String(buf, 0, len) else ""
    }
}
