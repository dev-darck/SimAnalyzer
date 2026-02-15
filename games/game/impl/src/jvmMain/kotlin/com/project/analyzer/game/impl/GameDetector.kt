package com.project.analyzer.game.impl

import com.project.analyzer.game.api.GameConfig
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Psapi
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.POINT
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import kotlin.math.abs
import kotlin.math.roundToInt

class GameDetector(
    private val configs: List<GameConfig>,
    private val pollIntervalMs: Long = 200L,
    private val requireForeground: Boolean = true,
    private val coroutineDispatcher: CoroutineDispatcher
) {

    private val user32 = User32.INSTANCE
    private val kernel32 = Kernel32.INSTANCE
    private val psapi = Psapi.INSTANCE

    private val titleBuffer = CharArray(512)
    private val pathBuffer = CharArray(1024)
    private val classBuffer = CharArray(256)

    @Volatile
    private var overlayHwnd: HWND? = null

    fun setOverlayHwnd(hwnd: HWND?) {
        overlayHwnd = hwnd
    }

    fun observeGameWindow(): Flow<GameWindowInfo?> = flow {
        var cachedGameHwnd: HWND? = null
        var lastEmittedInfo: GameWindowInfo? = null
        var lostFocusCounter = 0
        val maxLostFocusCount = 3

        while (currentCoroutineContext().isActive) {
            var gameInfo = if (cachedGameHwnd != null) checkWindow(cachedGameHwnd) else null

            if (gameInfo == null) {
                gameInfo = scanAllVisibleWindows()
            }

            if (gameInfo != null) {
                cachedGameHwnd = gameInfo.hwnd
                lostFocusCounter = 0

                val boundsChanged = lastEmittedInfo?.bounds != gameInfo.bounds
                val hwndChanged = lastEmittedInfo?.hwnd?.pointer != gameInfo.hwnd.pointer

                if (lastEmittedInfo == null || boundsChanged || hwndChanged) {
                    lastEmittedInfo = gameInfo
                    emit(gameInfo)
                }
            } else {
                if (lastEmittedInfo != null) {
                    lostFocusCounter++
                    if (lostFocusCounter >= maxLostFocusCount) {
                        cachedGameHwnd = null
                        lastEmittedInfo = null
                        emit(null)
                    }
                }
            }

            delay(pollIntervalMs)
        }
    }.flowOn(coroutineDispatcher)

    private fun checkWindow(hwnd: HWND): GameWindowInfo? {
        if (!user32.IsWindow(hwnd) || !user32.IsWindowVisible(hwnd)) return null

        if (requireForeground && !isGameOrOverlayForeground(hwnd)) return null

        val info = getWindowInfo(hwnd) ?: return null
        return if (matchesGameConfig(info)) info else null
    }

    private fun scanAllVisibleWindows(): GameWindowInfo? {
        var found: GameWindowInfo? = null

        user32.EnumWindows({ hwnd, _ ->
            if (user32.IsWindowVisible(hwnd)) {
                if (!requireForeground || isGameOrOverlayForeground(hwnd)) {
                    val info = getWindowInfo(hwnd)
                    if (info != null && matchesGameConfig(info)) {
                        found = info
                        return@EnumWindows false
                    }
                }
            }
            true
        }, null)

        return found
    }

    private fun isGameOrOverlayForeground(gameHwnd: HWND): Boolean {
        if (user32Ex.IsIconic(gameHwnd)) return false

        val foreground = user32.GetForegroundWindow() ?: return false
        val fgPtr = foreground.pointer
        val gamePtr = gameHwnd.pointer

        return fgPtr == gamePtr || (overlayHwnd != null && fgPtr == overlayHwnd!!.pointer)
    }

    private fun getWindowInfo(hwnd: HWND): GameWindowInfo? {
        val title = getWindowTitle(hwnd)
        if (title.isBlank()) return null

        val processName = getProcessName(hwnd) ?: return null
        val className = getWindowClassName(hwnd)

        val bounds = getWindowBounds(hwnd) ?: return null
        val monitor = findMonitorForWindow(bounds)
        val isFullscreen = isWindowFullscreen(bounds, monitor)

        return GameWindowInfo(hwnd, title, processName, className, bounds, isFullscreen, monitor)
    }

    private fun matchesGameConfig(info: GameWindowInfo): Boolean = configs.any { config ->
        config.matches(info)
    }

    private fun getWindowTitle(hwnd: HWND): String {
        val len = user32.GetWindowText(hwnd, titleBuffer, titleBuffer.size)
        return if (len > 0) String(titleBuffer, 0, minOf(len, titleBuffer.size)) else ""
    }

    private fun getWindowClassName(hwnd: HWND): String {
        val len = user32.GetClassName(hwnd, classBuffer, classBuffer.size)
        return if (len > 0) String(classBuffer, 0, minOf(len, classBuffer.size)) else ""
    }

    private fun getProcessName(hwnd: HWND): String? {
        val pidRef = IntByReference()
        user32.GetWindowThreadProcessId(hwnd, pidRef)
        val pid = pidRef.value
        if (pid == 0) return null

        val hProcess = kernel32.OpenProcess(
            WinNT.PROCESS_QUERY_INFORMATION or WinNT.PROCESS_VM_READ,
            false,
            pid
        ) ?: return null

        return try {
            val len = psapi.GetModuleFileNameExW(hProcess, null, pathBuffer, pathBuffer.size)
            if (len > 0) {
                val fullPath = String(pathBuffer, 0, len)
                fullPath.substringAfterLast("\\")
            } else null
        } finally {
            kernel32.CloseHandle(hProcess)
        }
    }

    private fun getWindowBounds(hwnd: HWND): Rectangle? {
        val clientPx = getClientBoundsPhysical(hwnd) ?: return null
        val hmon = user32.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST)
        val mi = WinUser.MONITORINFO()
        mi.cbSize = mi.size()
        if (!user32.GetMonitorInfo(hmon, mi).booleanValue()) return null

        val m = mi.rcMonitor
        val monitorPx = Rectangle(m.left, m.top, m.right - m.left, m.bottom - m.top)

        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val defaultDevice = ge.defaultScreenDevice

        fun devicePhysicalBounds(device: GraphicsDevice): Rectangle {
            val gc = device.defaultConfiguration
            val ub = gc.bounds
            val tx = gc.defaultTransform
            return Rectangle(
                (ub.x * tx.scaleX).roundToInt(),
                (ub.y * tx.scaleY).roundToInt(),
                (ub.width * tx.scaleX).roundToInt(),
                (ub.height * tx.scaleY).roundToInt()
            )
        }

        val bestDevice = ge.screenDevices.maxByOrNull { d ->
            val pb = devicePhysicalBounds(d)
            val inter = pb.intersection(monitorPx)
            if (inter.isEmpty) 0 else inter.width * inter.height
        } ?: defaultDevice

        val gc = bestDevice.defaultConfiguration
        val monitorUser = gc.bounds
        val tx = gc.defaultTransform

        val xUser = monitorUser.x + (clientPx.x - monitorPx.x) / tx.scaleX
        val yUser = monitorUser.y + (clientPx.y - monitorPx.y) / tx.scaleY
        val wUser = clientPx.width / tx.scaleX
        val hUser = clientPx.height / tx.scaleY

        return Rectangle(xUser.roundToInt(), yUser.roundToInt(), wUser.roundToInt(), hUser.roundToInt())
    }

    private fun findMonitorForWindow(windowBounds: Rectangle): GraphicsDeviceInfo {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val devices = ge.screenDevices

        val best = devices.maxByOrNull { d ->
            val b = d.defaultConfiguration.bounds
            val intersection = b.intersection(windowBounds)
            if (intersection.isEmpty) 0 else intersection.width * intersection.height
        } ?: ge.defaultScreenDevice

        return GraphicsDeviceInfo(
            id = best.iDstring,
            bounds = best.defaultConfiguration.bounds,
            isDefault = best == ge.defaultScreenDevice
        )
    }

    private fun getClientBoundsPhysical(hwnd: HWND): Rectangle? {
        val rc = WinDef.RECT()
        if (!user32.GetClientRect(hwnd, rc)) return null
        val pt = POINT(0, 0)
        if (!user32Ex.ClientToScreen(hwnd, pt)) return null
        return Rectangle(pt.x, pt.y, rc.right - rc.left, rc.bottom - rc.top)
    }

    private fun isWindowFullscreen(windowBounds: Rectangle, monitor: GraphicsDeviceInfo): Boolean {
        val tolerance = 15
        return abs(windowBounds.x - monitor.bounds.x) <= tolerance &&
            abs(windowBounds.y - monitor.bounds.y) <= tolerance &&
            abs(windowBounds.width - monitor.bounds.width) <= tolerance &&
            abs(windowBounds.height - monitor.bounds.height) <= tolerance
    }
}
