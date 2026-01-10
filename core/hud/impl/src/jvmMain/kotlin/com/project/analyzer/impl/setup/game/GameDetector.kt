package com.project.analyzer.impl.setup.game

import com.project.analyzer.api.di.IO
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Psapi
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinDef.HWND
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
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import kotlin.math.abs

class GameDetector(
    private val configs: List<GameConfig>,
    private val pollIntervalMs: Long = 150L,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher
) {

    private val user32 = User32.INSTANCE
    private val kernel32 = Kernel32.INSTANCE
    private val psapi = Psapi.INSTANCE

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
        var isFirstEmit = true

        while (currentCoroutineContext().isActive) {
            val gameInfo = findGameWindow(cachedGameHwnd)

            if (gameInfo != null) {
                cachedGameHwnd = gameInfo.hwnd
                lostFocusCounter = 0

                val shouldEmit = isFirstEmit ||
                    lastEmittedInfo == null ||
                    lastEmittedInfo.hwnd.pointer != gameInfo.hwnd.pointer ||
                    lastEmittedInfo.bounds != gameInfo.bounds

                if (shouldEmit) {
                    lastEmittedInfo = gameInfo
                    emit(gameInfo)
                    isFirstEmit = false
                }
            } else {
                if (lastEmittedInfo != null) {
                    lostFocusCounter++
                    if (lostFocusCounter >= maxLostFocusCount) {
                        cachedGameHwnd = null
                        lastEmittedInfo = null
                        emit(null)
                    }
                } else if (isFirstEmit) {
                    emit(null)
                    isFirstEmit = false
                }
            }

            delay(pollIntervalMs)
        }
    }.flowOn(ioDispatcher)

    private fun findGameWindow(cachedHwnd: HWND?): GameWindowInfo? {
        if (cachedHwnd != null && user32.IsWindow(cachedHwnd) && user32.IsWindowVisible(cachedHwnd)) {
            val info = getWindowInfo(cachedHwnd)
            if (info != null && matchesGameConfig(info) && isGameActive(cachedHwnd)) {
                return info
            }
        }

        var foundWindow: GameWindowInfo? = null

        user32.EnumWindows(
            WinUser.WNDENUMPROC { hwnd, _ ->
                if (!user32.IsWindowVisible(hwnd)) return@WNDENUMPROC true

                val info = getWindowInfo(hwnd)
                if (info != null && matchesGameConfig(info) && isGameActive(hwnd)) {
                    foundWindow = info
                    return@WNDENUMPROC false
                }
                true
            },
            null
        )

        return foundWindow
    }

    private fun isGameActive(gameHwnd: HWND): Boolean {
        val foreground = user32.GetForegroundWindow() ?: return false

        val foregroundPointer = foreground.pointer

        if (foregroundPointer == gameHwnd.pointer) return true

        val overlay = overlayHwnd
        if (overlay != null && foregroundPointer == overlay.pointer) return true

        if (overlay == null) {
            return user32.IsWindow(gameHwnd) && user32.IsWindowVisible(gameHwnd)
        }

        return false
    }

    private fun getWindowInfo(hwnd: HWND): GameWindowInfo? {
        val title = getWindowTitle(hwnd)
        if (title.isBlank()) return null

        val processName = getProcessName(hwnd) ?: return null
        val bounds = getWindowBounds(hwnd) ?: return null
        val monitor = findMonitorForWindow(bounds)
        val isFullscreen = isWindowFullscreen(bounds, monitor)

        return GameWindowInfo(
            hwnd = hwnd,
            title = title,
            processName = processName,
            bounds = bounds,
            isFullscreen = isFullscreen,
            monitor = monitor
        )
    }

    private fun matchesGameConfig(info: GameWindowInfo): Boolean = configs.any { config ->
        val titleMatch = config.titlePatterns.isEmpty() ||
            config.titlePatterns.any { info.title.contains(it, ignoreCase = true) }

        val processMatch = config.processNames.isEmpty() ||
            config.processNames.any { info.processName.equals(it, ignoreCase = true) }

        titleMatch && processMatch
    }

    private fun getWindowTitle(hwnd: HWND): String {
        val buf = CharArray(512)
        val len = user32.GetWindowText(hwnd, buf, buf.size)
        return if (len > 0) String(buf, 0, len) else ""
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
            val path = CharArray(1024)
            val len = psapi.GetModuleFileNameExW(hProcess, null, path, path.size)
            if (len > 0) {
                val fullPath = String(path, 0, len)
                fullPath.substringAfterLast("\\")
            } else null
        } finally {
            kernel32.CloseHandle(hProcess)
        }
    }

    private fun getWindowBounds(hwnd: HWND): Rectangle? {
        val rect = WinDef.RECT()
        if (!user32.GetWindowRect(hwnd, rect)) return null
        return Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top)
    }

    private fun findMonitorForWindow(windowBounds: Rectangle): GraphicsDeviceInfo {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val defaultDevice = ge.defaultScreenDevice

        val bestDevice = ge.screenDevices.maxByOrNull { device ->
            val monitorBounds = device.defaultConfiguration.bounds
            val intersection = monitorBounds.intersection(windowBounds)
            if (intersection.isEmpty) 0 else intersection.width * intersection.height
        } ?: defaultDevice

        return GraphicsDeviceInfo(
            id = bestDevice.iDstring,
            bounds = bestDevice.defaultConfiguration.bounds,
            isDefault = bestDevice == defaultDevice
        )
    }

    private fun isWindowFullscreen(windowBounds: Rectangle, monitor: GraphicsDeviceInfo): Boolean {
        val tolerance = 10
        return abs(windowBounds.x - monitor.bounds.x) <= tolerance &&
            abs(windowBounds.y - monitor.bounds.y) <= tolerance &&
            abs(windowBounds.width - monitor.bounds.width) <= tolerance &&
            abs(windowBounds.height - monitor.bounds.height) <= tolerance
    }
}
