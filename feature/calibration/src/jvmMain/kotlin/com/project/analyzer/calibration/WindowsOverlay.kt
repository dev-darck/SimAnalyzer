
package com.project.analyzer.calibration

import com.project.analyzer.calibration.presentation.overlay.IntRect
import com.sun.jna.Native
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Component
import java.awt.Window
import kotlin.math.roundToInt

object WindowsOverlayMode {
    private const val WS_EX_NOACTIVATE = 0x08000000
    private const val WS_EX_TOOLWINDOW = 0x00000080
    private const val WS_EX_APPWINDOW = 0x00040000

    fun applyBaseStyles(component: Component) {
        val hwnd = WinDef.HWND(Native.getComponentPointer(component))
        val u = User32.INSTANCE

        var exStyle = u.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)

        exStyle = exStyle or WinUser.WS_EX_LAYERED
        exStyle = exStyle or WS_EX_NOACTIVATE

        exStyle = exStyle or WS_EX_TOOLWINDOW
        exStyle = exStyle and WS_EX_APPWINDOW.inv()

        exStyle = exStyle and WinUser.WS_EX_TRANSPARENT.inv()

        u.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle)
        u.SetLayeredWindowAttributes(hwnd, 0, 255.toByte(), WinUser.LWA_ALPHA)
    }
}

object WindowsOverlayRegion {

    fun apply(window: Window, rectsLogical: List<IntRect>) {
        val hwnd = WinDef.HWND(Native.getComponentPointer(window))

        val tx = window.graphicsConfiguration.defaultTransform
        val sx = tx.scaleX.toFloat()
        val sy = tx.scaleY.toFloat()

        val gdi = GDI32.INSTANCE
        val user32 = User32.INSTANCE

        val mainRgn = gdi.CreateRectRgn(0, 0, 0, 0)

        rectsLogical.forEach { r ->
            val l = (r.left * sx).roundToInt()
            val t = (r.top * sy).roundToInt()
            val rr = (r.right * sx).roundToInt()
            val bb = (r.bottom * sy).roundToInt()

            val tmp = gdi.CreateRectRgn(l, t, rr, bb)
            gdi.CombineRgn(mainRgn, mainRgn, tmp, 2)
            gdi.DeleteObject(tmp)
        }

        user32.SetWindowRgn(hwnd, mainRgn, true)
    }

    fun applyFullWindow(window: Window) {
        val hwnd = WinDef.HWND(Native.getComponentPointer(window))
        val tx = window.graphicsConfiguration.defaultTransform
        val sx = tx.scaleX.toFloat()
        val sy = tx.scaleY.toFloat()

        val wPhys = (window.width * sx).toInt().coerceAtLeast(1)
        val hPhys = (window.height * sy).toInt().coerceAtLeast(1)

        val gdi = GDI32.INSTANCE
        val user32 = User32.INSTANCE

        val rgn = gdi.CreateRectRgn(0, 0, wPhys, hPhys)
        user32.SetWindowRgn(hwnd, rgn, true)
    }
}
