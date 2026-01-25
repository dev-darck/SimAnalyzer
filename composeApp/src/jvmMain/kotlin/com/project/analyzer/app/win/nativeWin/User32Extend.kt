package com.project.analyzer.app.win.nativeWin

import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.UINT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WindowProc
import com.sun.jna.win32.W32APIOptions

@Suppress("FunctionName")
internal interface User32Extend : User32 {

    fun SetWindowLong(hWnd: HWND, nIndex: Int, wndProc: WindowProc): LONG_PTR
    fun SetWindowLongPtr(hWnd: HWND, nIndex: Int, wndProc: WindowProc): LONG_PTR

    fun SetWindowLong(hWnd: HWND, nIndex: Int, dwNewLong: LONG_PTR): LONG_PTR
    fun SetWindowLongPtr(hWnd: HWND, nIndex: Int, dwNewLong: LONG_PTR): LONG_PTR

    fun SetClassLongPtr(hWnd: HWND, nIndex: Int, dwNewLong: Pointer): LONG_PTR
    fun CallWindowProc(
        proc: LONG_PTR,
        hWnd: HWND,
        uParam: Int,
        wParam: WPARAM,
        lParam: LPARAM
    ): LRESULT

    fun GetSystemMetricsForDpi(nIndex: Int, dpi: UINT): Int
    fun GetDpiForWindow(hWnd: HWND): UINT
    fun ScreenToClient(hWnd: HWND, lpPoint: POINT): Boolean

    companion object {

        val instance: User32Extend? by lazy {
            runCatching {
                Native.load("user32", User32Extend::class.java, W32APIOptions.DEFAULT_OPTIONS)
            }.getOrNull()
        }
    }
}

internal fun User32Extend.setWindowLong(hWnd: HWND, nIndex: Int, procedure: WindowProcedure): LONG_PTR {
    return if (Platform.is64Bit()) {
        SetWindowLongPtr(hWnd, nIndex, procedure)
    } else {
        SetWindowLong(hWnd, nIndex, procedure)
    }
}

internal fun User32.isWindowInMaximized(hWnd: HWND): Boolean {
    val placement = WinUser.WINDOWPLACEMENT()
    val ok = GetWindowPlacement(hWnd, placement).booleanValue() && placement.showCmd == WinUser.SW_SHOWMAXIMIZED
    placement.clear()
    return ok
}

internal fun User32.updateWindowStyle(hWnd: HWND, block: (oldStyle: Int) -> Int) {
    val oldStyle = GetWindowLong(hWnd, WinUser.GWL_STYLE)
    SetWindowLong(hWnd, WinUser.GWL_STYLE, block(oldStyle))
}

internal val Int.lowWord: Int get() = (this and 0xFFFF)
internal val Int.highWord: Int get() = (this shr 16) and 0xFFFF
