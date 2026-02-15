package com.project.analyzer.game.impl

import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

interface User32Ex : StdCallLibrary {

    fun GetCapture(): WinDef.HWND?
    fun SetCapture(hWnd: WinDef.HWND): WinDef.HWND?
    fun ClientToScreen(hwnd: WinDef.HWND, pt: WinDef.POINT): Boolean
    fun ReleaseCapture(): Boolean
    fun IsIconic(hwnd: WinDef.HWND): Boolean
}

val user32Ex: User32Ex =
    Native.load("user32", User32Ex::class.java, W32APIOptions.DEFAULT_OPTIONS)

const val WS_EX_TOOLWINDOW = 0x00000080
const val WS_EX_APPWINDOW = 0x00040000
const val WS_EX_NOACTIVATE = 0x08000000
