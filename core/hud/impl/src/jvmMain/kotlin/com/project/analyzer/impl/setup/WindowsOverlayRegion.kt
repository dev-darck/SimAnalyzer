package com.project.analyzer.impl.setup

import androidx.compose.ui.unit.IntRect
import com.sun.jna.Native
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import java.awt.Window
import kotlin.math.roundToInt

internal object WindowsOverlayRegion {

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
