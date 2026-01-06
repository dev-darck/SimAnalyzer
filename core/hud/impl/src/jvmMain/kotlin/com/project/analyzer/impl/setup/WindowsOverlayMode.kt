package com.project.analyzer.impl.setup

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Component

internal object WindowsOverlayMode {

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
