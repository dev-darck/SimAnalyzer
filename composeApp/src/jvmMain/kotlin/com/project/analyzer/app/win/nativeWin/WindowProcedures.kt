package com.project.analyzer.app.win.nativeWin

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.awt.ComposeWindow
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTBOTTOM
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTBOTTOMLEFT
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTBOTTOMRIGHT
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTCLIENT
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTCLOSE
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTLEFT
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTMAXBUTTON
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTMINBUTTON
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTRIGHT
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTTOP
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTTOPLEFT
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTTOPRIGHT
import com.project.analyzer.app.win.nativeWin.WinUserConst.HTTRANSPARENT
import com.project.analyzer.app.win.nativeWin.WinUserConst.WM_LBUTTONDOWN
import com.project.analyzer.app.win.nativeWin.WinUserConst.WM_LBUTTONUP
import com.project.analyzer.app.win.nativeWin.WinUserConst.WM_MOUSEMOVE
import com.project.analyzer.app.win.nativeWin.WinUserConst.WM_NCCALCSIZE
import com.project.analyzer.app.win.nativeWin.WinUserConst.WM_NCHITTEST
import com.project.analyzer.app.win.nativeWin.WinUserConst.WM_NCLBUTTONDOWN
import com.project.analyzer.app.win.nativeWin.WinUserConst.WM_NCLBUTTONUP
import com.project.analyzer.app.win.nativeWin.WinUserConst.WM_NCMOUSEMOVE
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.utils.logger.logger
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinDef.UINT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import org.jetbrains.skiko.SkiaLayer
import java.awt.Window

typealias WindowProcedure = WinUser.WindowProc

internal class ComposeWindowProcedure(
    window: Window,
    private val hitTest: (x: Float, y: Float) -> Int,
    private val onWindowInsetUpdate: (WindowInsets) -> Unit,
) : WindowProcedure {

    private val logger = logger()
    private val windowPointer = (window as? ComposeWindow)?.windowHandle?.let(::Pointer)
        ?: Native.getWindowPointer(window)

    val windowHandle = HWND(windowPointer)

    private var hitResult = HTCLIENT

    private val margins = WindowMargins(
        leftBorderWidth = -1,
        topBorderHeight = -1,
        rightBorderWidth = -1,
        bottomBorderHeight = -1,
    )

    private val defaultWindowProcedure: LONG_PTR =
        User32Extend.instance?.setWindowLong(windowHandle, WinUser.GWL_WNDPROC, this) ?: LONG_PTR(0)

    @Volatile
    private var disposed = false

    private var dpi = UINT(0)
    private var width = 0
    private var height = 0
    private var frameX = 0
    private var frameY = 0
    private var edgeX = 0
    private var edgeY = 0
    private var padding = 0
    private var isMaximized = User32Extend.instance?.isWindowInMaximized(windowHandle) == true

    private val skiaLayerProcedure = (window as? ComposeWindow)?.findSkiaLayer()?.let {
        SkiaLayerWindowProcedure(
            skiaLayer = it,
            hitTest = { x, y ->
                updateWindowInfo()

                val horizontalPadding = frameX
                val verticalPadding = frameY

                hitResult = when {
                    isMaximized -> hitTest(x, y)
                    x <= horizontalPadding && y > verticalPadding && y < height - verticalPadding -> HTLEFT
                    x <= horizontalPadding && y <= verticalPadding -> HTTOPLEFT
                    x <= horizontalPadding -> HTBOTTOMLEFT
                    y <= verticalPadding && x > horizontalPadding && x < width - horizontalPadding -> HTTOP
                    y <= verticalPadding && x <= horizontalPadding -> HTTOPLEFT
                    y <= verticalPadding -> HTTOPRIGHT
                    x >= width - horizontalPadding && y > verticalPadding && y < height - verticalPadding -> HTRIGHT
                    x >= width - horizontalPadding && y <= verticalPadding -> HTTOPRIGHT
                    x >= width - horizontalPadding -> HTBOTTOMRIGHT
                    y >= height - verticalPadding && x > horizontalPadding && x < width - horizontalPadding -> HTBOTTOM
                    y >= height - verticalPadding && x <= horizontalPadding -> HTBOTTOMLEFT
                    y >= height - verticalPadding -> HTBOTTOMRIGHT
                    else -> hitTest(x, y)
                }

                hitResult
            },
        )
    }

    init {
        enableResizability()
        setWindowBackground()
        enableBorderAndShadow()
    }

    override fun callback(hWnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT {
        val user32 = User32Extend.instance ?: return LRESULT(0)

        return when (uMsg) {
            WM_NCCALCSIZE -> {
                if (wParam.toInt() == 0) {
                    callDefaultProc(user32, hWnd, uMsg, wParam, lParam)
                } else {
                    dpi = user32.GetDpiForWindow(hWnd)
                    frameX = user32.GetSystemMetricsForDpi(WinUser.SM_CXFRAME, dpi)
                    frameY = user32.GetSystemMetricsForDpi(WinUser.SM_CYFRAME, dpi)
                    edgeX = user32.GetSystemMetricsForDpi(WinUser.SM_CXEDGE, dpi)
                    edgeY = user32.GetSystemMetricsForDpi(WinUser.SM_CYEDGE, dpi)
                    padding = user32.GetSystemMetricsForDpi(WinUser.SM_CXPADDEDBORDER, dpi)
                    isMaximized = user32.isWindowInMaximized(hWnd)

                    val insets = WindowInsets(
                        left = if (isMaximized) frameX + padding else 0,
                        right = if (isMaximized) frameX + padding else 0,
                        top = if (isMaximized) frameY + padding else 0,
                        bottom = if (isMaximized) frameY + padding else 0,
                    )
                    onWindowInsetUpdate(insets)

                    LRESULT(0)
                }
            }

            WM_NCHITTEST -> LRESULT(hitResult.toLong())

            else -> {
                if (uMsg == WM_NCMOUSEMOVE) {
                    skiaLayerProcedure?.let { user32.PostMessage(it.contentHandle, uMsg, wParam, lParam) }
                }
                callDefaultProc(user32, hWnd, uMsg, wParam, lParam)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException") // JNA may throw Error; we fall back to DefWindowProc
    private fun callDefaultProc(
        user32: User32Extend,
        hWnd: HWND,
        uMsg: Int,
        wParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT = try {
        user32.CallWindowProc(defaultWindowProcedure, hWnd, uMsg, wParam, lParam)
    } catch (e: Error) {
        User32.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam)
    }

    private fun updateWindowInfo() {
        User32Extend.instance?.apply {
            dpi = GetDpiForWindow(windowHandle)
            frameX = GetSystemMetricsForDpi(WinUser.SM_CXFRAME, dpi)
            frameY = GetSystemMetricsForDpi(WinUser.SM_CYFRAME, dpi)

            val rect = RECT()
            if (GetWindowRect(windowHandle, rect)) {
                rect.read()
                width = rect.right - rect.left
                height = rect.bottom - rect.top
            }
            rect.clear()
        }
    }

    private fun enableResizability() {
        User32Extend.instance?.updateWindowStyle(windowHandle) { oldStyle ->
            (oldStyle or WinUser.WS_CAPTION or WinUser.WS_CLIPCHILDREN) and WinUser.WS_SYSMENU.inv()
        }
    }

    private fun setWindowBackground() {
        val gdi32 = runCatching { NativeLibrary.getInstance("gdi32") }.getOrNull() ?: return
        val user32 = User32Extend.instance ?: return

        val createBrush = runCatching { gdi32.getFunction("CreateSolidBrush") }.getOrNull() ?: return

        val bgColorBGR = 0x00201E1B
        val hBrush = runCatching { createBrush.invokeLong(arrayOf(bgColorBGR)) }.getOrNull() ?: return

        runCatching {
            user32.SetClassLongPtr(windowHandle, -10, Pointer(hBrush))
        }.onSuccess {
            logger.debug { "Window background brush set" }
        }
    }

    private fun enableBorderAndShadow() {
        val dwm = runCatching { NativeLibrary.getInstance("dwmapi") }.getOrNull()
            ?: run {
                logger.debug { "dwmapi not available" }
                return
            }

        val extend = runCatching { dwm.getFunction("DwmExtendFrameIntoClientArea") }.getOrNull()
        extend?.let {
            runCatching { it.invoke(arrayOf(windowHandle, margins)) }
                .onSuccess { logger.debug { "DwmExtendFrameIntoClientArea applied (shadow enabled)" } }
                .onFailure { logger.debug { "DwmExtendFrameIntoClientArea failed: ${it.message}" } }
        }

        val setAttribute = runCatching { dwm.getFunction("DwmSetWindowAttribute") }.getOrNull()
        if (setAttribute != null) {
            val darkMode = IntByReference(1)
            runCatching { setAttribute.invoke(arrayOf(windowHandle, 20, darkMode.pointer, 4)) }
                .onSuccess { logger.debug { "DWMWA_USE_IMMERSIVE_DARK_MODE enabled" } }

            val cornerPref = IntByReference(2) // DWMWCP_ROUND
            runCatching { setAttribute.invoke(arrayOf(windowHandle, 33, cornerPref.pointer, 4)) }
                .onSuccess { logger.debug { "DWMWA_WINDOW_CORNER_PREFERENCE set to ROUND" } }

            val borderColor = IntByReference(0xFFFFFFFE.toInt()) // DWMWA_COLOR_NONE
            runCatching { setAttribute.invoke(arrayOf(windowHandle, 34, borderColor.pointer, 4)) }
                .onSuccess { logger.debug { "DWMWA_BORDER_COLOR set to NONE" } }
        }
    }

    @Suppress("NestedBlockDepth")
    fun dispose() {
        if (disposed) return
        disposed = true

        skiaLayerProcedure?.dispose()

        GDI32.INSTANCE.DeleteObject(windowHandle)

        if (defaultWindowProcedure.toLong() > 0) {
            User32Extend.instance?.let { user32 ->
                try {
                    if (Platform.is64Bit()) {
                        user32.SetWindowLongPtr(windowHandle, WinUser.GWL_WNDPROC, defaultWindowProcedure)
                    } else {
                        user32.SetWindowLong(windowHandle, WinUser.GWL_WNDPROC, defaultWindowProcedure)
                    }
                } catch (_: Throwable) {
                }
            }
        }

        LeakCanaryRuntime.watch(this, "ComposeWindowProcedure")
    }
}

internal class SkiaLayerWindowProcedure(skiaLayer: SkiaLayer, private val hitTest: (x: Float, y: Float) -> Int) :
    WindowProcedure {

    private val windowHandle = HWND(Pointer(skiaLayer.windowHandle))
    internal val contentHandle = HWND(skiaLayer.canvas.let(Native::getComponentPointer))
    private val defaultWindowProcedure: LONG_PTR =
        User32Extend.instance?.setWindowLong(contentHandle, WinUser.GWL_WNDPROC, this)
            ?: LONG_PTR(0)
    private var hitResult = HTCLIENT

    @Volatile
    private var disposed = false

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun callback(hwnd: HWND, uMsg: Int, wParam: WPARAM, lParam: LPARAM): LRESULT {
        val user32 = User32Extend.instance

        if (disposed || user32 == null || defaultWindowProcedure.toLong() <= 0) {
            return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam)
        }

        return when (uMsg) {
            WM_NCHITTEST -> {
                hitResult = lParam.useMousePoint { x, y -> hitTest(x.toFloat(), y.toFloat()) }
                when (hitResult) {
                    HTCLIENT, HTMAXBUTTON, HTMINBUTTON, HTCLOSE -> LRESULT(hitResult.toLong())
                    else -> LRESULT(HTTRANSPARENT.toLong())
                }
            }

            WM_NCMOUSEMOVE -> {
                user32.SendMessage(contentHandle, WM_MOUSEMOVE, wParam, lParam)
                LRESULT(0)
            }

            WM_NCLBUTTONDOWN -> {
                user32.SendMessage(contentHandle, WM_LBUTTONDOWN, wParam, lParam)
                LRESULT(0)
            }

            WM_NCLBUTTONUP -> {
                user32.SendMessage(contentHandle, WM_LBUTTONUP, wParam, lParam)
                LRESULT(0)
            }

            else -> {
                try {
                    user32.CallWindowProc(defaultWindowProcedure, hwnd, uMsg, wParam, lParam)
                } catch (e: Error) {
                    User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam)
                }
            }
        }
    }

    private inline fun <T> LPARAM.useMousePoint(crossinline block: (x: Int, y: Int) -> T): T {
        val lParamValue = toInt()
        val x = lParamValue.lowWord.toShort().toInt()
        val y = lParamValue.highWord.toShort().toInt()

        val point = POINT(x, y)
        User32Extend.instance?.ScreenToClient(windowHandle, point)
        point.read()

        val result = block(point.x, point.y)
        point.clear()
        return result
    }

    @Suppress("NestedBlockDepth")
    fun dispose() {
        if (disposed) return
        disposed = true

        if (defaultWindowProcedure.toLong() > 0) {
            User32Extend.instance?.let { user32 ->
                try {
                    if (Platform.is64Bit()) {
                        user32.SetWindowLongPtr(contentHandle, WinUser.GWL_WNDPROC, defaultWindowProcedure)
                    } else {
                        user32.SetWindowLong(contentHandle, WinUser.GWL_WNDPROC, defaultWindowProcedure)
                    }
                } catch (_: Throwable) {
                }
            }
        }

        LeakCanaryRuntime.watch(this, "SkiaLayerWindowProcedure")
    }
}
