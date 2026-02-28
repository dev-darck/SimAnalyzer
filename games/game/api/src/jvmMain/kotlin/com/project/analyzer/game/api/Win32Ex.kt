package com.project.analyzer.game.api

import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

public interface Win32Ex : StdCallLibrary {

    public fun GetCapture(): WinDef.HWND?
    public fun SetCapture(hWnd: WinDef.HWND): WinDef.HWND?
    public fun ClientToScreen(hwnd: WinDef.HWND, pt: WinDef.POINT): Boolean
    public fun ReleaseCapture(): Boolean
    public fun IsIconic(hwnd: WinDef.HWND): Boolean
}

public val user32Ex: Win32Ex = Native.load("user32", Win32Ex::class.java, W32APIOptions.DEFAULT_OPTIONS)

public const val WS_EX_TOOLWINDOW: Int = 0x00000080
public const val WS_EX_APPWINDOW: Int = 0x00040000
public const val WS_EX_NOACTIVATE: Int = 0x08000000
