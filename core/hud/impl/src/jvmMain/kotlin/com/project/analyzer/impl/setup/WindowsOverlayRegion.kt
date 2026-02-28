package com.project.analyzer.impl.setup

import androidx.compose.ui.unit.IntRect
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import java.awt.Window
import kotlin.math.roundToInt

private const val RGN_OR = 2

internal object WindowsOverlayRegion {

    private val gdi32 = GDI32.INSTANCE
    private val user32 = User32.INSTANCE

    fun apply(window: Window, rectsLogical: List<IntRect>) {
        if (rectsLogical.isEmpty()) {
            setEmptyRegion(window)
            return
        }

        val hwnd = resolveHwnd(window) ?: return

        val rc = WinDef.RECT()
        if (!user32.GetWindowRect(hwnd, rc)) return
        val physW = (rc.right - rc.left).coerceAtLeast(1)
        val physH = (rc.bottom - rc.top).coerceAtLeast(1)

        val logW = window.width.coerceAtLeast(1)
        val logH = window.height.coerceAtLeast(1)

        val sx = physW.toFloat() / logW.toFloat()
        val sy = physH.toFloat() / logH.toFloat()

        val mainRgn = gdi32.CreateRectRgn(0, 0, 0, 0)

        rectsLogical.forEach { r ->
            val l = (r.left * sx).roundToInt()
            val t = (r.top * sy).roundToInt()
            val rr = (r.right * sx).roundToInt()
            val bb = (r.bottom * sy).roundToInt()

            val tmp = gdi32.CreateRectRgn(l, t, rr, bb)
            gdi32.CombineRgn(mainRgn, mainRgn, tmp, RGN_OR)
            gdi32.DeleteObject(tmp)
        }

        val ok = user32.SetWindowRgn(hwnd, mainRgn, true)
        if (ok == 0) gdi32.DeleteObject(mainRgn)
    }

    private fun setEmptyRegion(window: Window) {
        val hwnd = resolveHwnd(window) ?: return
        val empty = gdi32.CreateRectRgn(0, 0, 0, 0)
        val ok = user32.SetWindowRgn(hwnd, empty, true)
        if (ok == 0) gdi32.DeleteObject(empty)
    }

    fun resetToFullWindow(window: Window) {
        val hwnd = resolveHwnd(window) ?: return
        user32.SetWindowRgn(hwnd, null, true)
    }

    private fun resolveHwnd(window: Window): WinDef.HWND? {
        if (!window.isDisplayable) return null
        val ptr = Native.getComponentPointer(window)
        if (ptr == Pointer.NULL) return null
        val hwnd = WinDef.HWND(ptr)
        return if (user32.IsWindow(hwnd)) hwnd else null
    }
}
