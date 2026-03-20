package com.project.analyzer.game.impl

import com.project.analyzer.game.api.GameConfig
import com.project.analyzer.game.api.GameWindowDetector
import com.project.analyzer.game.api.GameWindowInfo
import com.project.analyzer.game.api.GraphicsDeviceInfo
import com.project.analyzer.game.api.user32Ex
import com.project.analyzer.game.impl.display.DisplayDeviceSnapshot
import com.project.analyzer.game.impl.display.DisplayTopology
import com.project.analyzer.game.impl.model.WindowMetadata
import com.sun.jna.Pointer
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
import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class GameDetector(
    private val configs: List<GameConfig>,
    private val pollIntervalMs: Long = 200.milliseconds.inWholeMilliseconds,
    private val requireForeground: Boolean = true,
    private val coroutineDispatcher: CoroutineDispatcher,
) : GameWindowDetector {

    private val user32 = User32.INSTANCE
    private val kernel32 = Kernel32.INSTANCE
    private val psapi = Psapi.INSTANCE

    private val titleBuffer = CharArray(512)
    private val pathBuffer = CharArray(1024)
    private val classBuffer = CharArray(256)

    private val requiresTitleMatch = configs.any { it.titlePatterns.isNotEmpty() }
    private val requiresProcessMatch = configs.any { it.processNames.isNotEmpty() }
    private val requiresClassMatch = configs.any { it.windowClassNames.isNotEmpty() }

    private val processNameCache = LinkedHashMap<Int, String>(PROCESS_CACHE_LIMIT)
    private val classNameCache = LinkedHashMap<Long, String>(CLASS_CACHE_LIMIT)

    @Volatile
    private var overlayHwnd: HWND? = null

    @Volatile
    private var displayTopology: DisplayTopology? = null

    override fun setOverlayHwnd(hwnd: HWND?) {
        overlayHwnd = hwnd
    }

    override fun observeGameWindow(): Flow<GameWindowInfo?> = flow {
        var cachedGameHwnd: HWND? = null
        var lastEmittedInfo: GameWindowInfo? = null
        var lostFocusCounter = 0
        val maxLostFocusCount = 3

        while (currentCoroutineContext().isActive) {
            var gameInfo = if (cachedGameHwnd != null) checkWindow(cachedGameHwnd) else null

            if (gameInfo == null && requireForeground) {
                gameInfo = checkForegroundWindow()
            }

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
            } else if (lastEmittedInfo != null) {
                lostFocusCounter++
                if (lostFocusCounter >= maxLostFocusCount) {
                    cachedGameHwnd = null
                    lastEmittedInfo = null
                    emit(null)
                }
            }

            delay(pollIntervalMs)
        }
    }.flowOn(coroutineDispatcher)

    private fun checkForegroundWindow(): GameWindowInfo? {
        val foreground = user32.GetForegroundWindow() ?: return null
        if (overlayHwnd?.pointer == foreground.pointer) return null
        return checkWindow(foreground)
    }

    private fun checkWindow(hwnd: HWND): GameWindowInfo? {
        if (!user32.IsWindow(hwnd) || !user32.IsWindowVisible(hwnd)) return null
        if (requireForeground && !isGameOrOverlayForeground(hwnd)) return null
        return getWindowInfo(hwnd)
    }

    private fun scanAllVisibleWindows(): GameWindowInfo? {
        var found: GameWindowInfo? = null

        user32.EnumWindows({ hwnd, _ ->
            if (!user32.IsWindowVisible(hwnd)) return@EnumWindows true
            if (overlayHwnd?.pointer == hwnd.pointer) return@EnumWindows true
            if (requireForeground && !isGameOrOverlayForeground(hwnd)) return@EnumWindows true

            val info = getWindowInfo(hwnd)
            if (info != null) {
                found = info
                return@EnumWindows false
            }
            true
        }, null)

        return found
    }

    private fun isGameOrOverlayForeground(gameHwnd: HWND): Boolean {
        if (user32Ex.IsIconic(gameHwnd)) return false

        val foreground = user32.GetForegroundWindow() ?: return false
        val foregroundPointer = foreground.pointer
        val overlayPointer = overlayHwnd?.pointer
        return foregroundPointer == gameHwnd.pointer || (overlayPointer != null && foregroundPointer == overlayPointer)
    }

    private fun getWindowInfo(hwnd: HWND): GameWindowInfo? {
        val metadata = getWindowMetadata(hwnd) ?: return null
        if (!matchesGameConfig(metadata)) return null

        val bounds = getWindowBounds(hwnd) ?: return null
        val monitor = findMonitorForWindow(bounds)
        val displayBounds = findDisplayBoundsForWindow(bounds)
        val isFullscreen = isWindowFullscreen(bounds, displayBounds)

        return GameWindowInfo(
            hwnd = hwnd,
            title = metadata.title,
            processName = metadata.processName,
            className = metadata.className,
            bounds = bounds,
            isFullscreen = isFullscreen,
            monitor = monitor,
        )
    }

    private fun getWindowMetadata(hwnd: HWND): WindowMetadata? {
        val title = if (requiresTitleMatch) getWindowTitle(hwnd) else ""
        val processName = if (requiresProcessMatch) getProcessNameCached(hwnd) ?: return null else ""
        val className = if (requiresClassMatch) getWindowClassNameCached(hwnd) else ""
        return WindowMetadata(title = title, processName = processName, className = className)
    }

    private fun matchesGameConfig(metadata: WindowMetadata): Boolean = configs.any { config ->
        val titleOk = config.titlePatterns.isEmpty() || config.matchesTitle(metadata.title)
        val processOk = config.processNames.isEmpty() || config.matchesProcessName(metadata.processName)
        val classOk = config.windowClassNames.isEmpty() || config.matchesClassName(metadata.className)
        titleOk && processOk && classOk
    }

    private fun getWindowTitle(hwnd: HWND): String {
        val len = user32.GetWindowText(hwnd, titleBuffer, titleBuffer.size)
        return if (len > 0) String(titleBuffer, 0, minOf(len, titleBuffer.size)) else ""
    }

    private fun getWindowClassNameCached(hwnd: HWND): String {
        val key = Pointer.nativeValue(hwnd.pointer)
        classNameCache[key]?.let { return it }

        val className = getWindowClassName(hwnd)
        classNameCache.putBounded(key, className, CLASS_CACHE_LIMIT)
        return className
    }

    private fun getWindowClassName(hwnd: HWND): String {
        val len = user32.GetClassName(hwnd, classBuffer, classBuffer.size)
        return if (len > 0) String(classBuffer, 0, minOf(len, classBuffer.size)) else ""
    }

    private fun getProcessNameCached(hwnd: HWND): String? {
        val pid = getWindowPid(hwnd)
        if (pid == 0) return null

        processNameCache[pid]?.let { return it }
        val processName = getProcessName(pid) ?: return null
        processNameCache.putBounded(pid, processName, PROCESS_CACHE_LIMIT)
        return processName
    }

    private fun getWindowPid(hwnd: HWND): Int {
        val pidRef = IntByReference()
        user32.GetWindowThreadProcessId(hwnd, pidRef)
        return pidRef.value
    }

    private fun getProcessName(pid: Int): String? {
        val process = kernel32.OpenProcess(
            WinNT.PROCESS_QUERY_INFORMATION or WinNT.PROCESS_VM_READ,
            false,
            pid,
        ) ?: return null

        return try {
            val len = psapi.GetModuleFileNameExW(process, null, pathBuffer, pathBuffer.size)
            if (len <= 0) return null

            val fullPath = String(pathBuffer, 0, len)
            fullPath.substringAfterLast("\\")
        } finally {
            kernel32.CloseHandle(process)
        }
    }

    private fun getWindowBounds(hwnd: HWND): Rectangle? {
        val clientPx = getClientBoundsPhysical(hwnd) ?: return null
        val monitorHandle = user32.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST)
        val monitorInfo = WinUser.MONITORINFO().apply { cbSize = size() }
        if (!user32.GetMonitorInfo(monitorHandle, monitorInfo).booleanValue()) return null

        val rcMonitor = monitorInfo.rcMonitor
        val monitorPhysicalBounds = Rectangle(
            rcMonitor.left,
            rcMonitor.top,
            rcMonitor.right - rcMonitor.left,
            rcMonitor.bottom - rcMonitor.top,
        )

        val topology = currentDisplayTopology()
        val display = topology.devices
            .maxByOrNull { intersectionArea(it.physicalBounds, monitorPhysicalBounds) }
            ?: topology.defaultDevice()

        val xUser = display.userBounds.x + (clientPx.x - display.physicalBounds.x) / display.scaleX
        val yUser = display.userBounds.y + (clientPx.y - display.physicalBounds.y) / display.scaleY
        val widthUser = clientPx.width / display.scaleX
        val heightUser = clientPx.height / display.scaleY

        return Rectangle(
            xUser.roundToInt(),
            yUser.roundToInt(),
            widthUser.roundToInt(),
            heightUser.roundToInt(),
        )
    }

    private fun findMonitorForWindow(windowBounds: Rectangle): GraphicsDeviceInfo {
        val topology = currentDisplayTopology()
        val display = topology.devices
            .maxByOrNull { intersectionArea(it.userBounds, windowBounds) }
            ?: topology.defaultDevice()

        return GraphicsDeviceInfo(
            id = display.id,
            bounds = display.userBounds,
            isDefault = display.isDefault,
        )
    }

    private fun findDisplayBoundsForWindow(windowBounds: Rectangle): Rectangle {
        val intersectingBounds = currentDisplayTopology()
            .devices
            .map { it.userBounds }
            .filter { intersectionArea(it, windowBounds) > 0 }

        if (intersectingBounds.isEmpty()) {
            return findMonitorForWindow(windowBounds).bounds
        }

        return intersectingBounds.reduce(Rectangle::union)
    }

    private fun getClientBoundsPhysical(hwnd: HWND): Rectangle? {
        val rect = WinDef.RECT()
        if (!user32.GetClientRect(hwnd, rect)) return null

        val point = POINT(0, 0)
        if (!user32Ex.ClientToScreen(hwnd, point)) return null

        return Rectangle(point.x, point.y, rect.right - rect.left, rect.bottom - rect.top)
    }

    private fun isWindowFullscreen(windowBounds: Rectangle, displayBounds: Rectangle): Boolean {
        val tolerance = 15
        return abs(windowBounds.x - displayBounds.x) <= tolerance &&
            abs(windowBounds.y - displayBounds.y) <= tolerance &&
            abs(windowBounds.width - displayBounds.width) <= tolerance &&
            abs(windowBounds.height - displayBounds.height) <= tolerance
    }

    private fun currentDisplayTopology(): DisplayTopology {
        val nowNs = System.nanoTime()
        val cached = displayTopology
        if (cached != null && nowNs - cached.updatedAtNs <= DISPLAY_TOPOLOGY_TTL_NS) {
            return cached
        }

        val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val defaultDeviceId = graphicsEnvironment.defaultScreenDevice.iDstring
        val screenDevices = graphicsEnvironment.screenDevices
        val displayDevices: List<GraphicsDevice> = if (screenDevices.isNotEmpty()) {
            screenDevices.toList()
        } else {
            listOf(graphicsEnvironment.defaultScreenDevice)
        }
        val devices = displayDevices.map { device ->
            val configuration = device.defaultConfiguration
            val userBounds = configuration.bounds
            val transform = configuration.defaultTransform
            val scaleX = transform.scaleX.takeIf { it > 0.0 } ?: 1.0
            val scaleY = transform.scaleY.takeIf { it > 0.0 } ?: 1.0

            DisplayDeviceSnapshot(
                id = device.iDstring,
                userBounds = Rectangle(userBounds),
                physicalBounds = Rectangle(
                    (userBounds.x * scaleX).roundToInt(),
                    (userBounds.y * scaleY).roundToInt(),
                    (userBounds.width * scaleX).roundToInt(),
                    (userBounds.height * scaleY).roundToInt(),
                ),
                scaleX = scaleX,
                scaleY = scaleY,
                isDefault = device.iDstring == defaultDeviceId,
            )
        }

        return DisplayTopology(devices = devices, updatedAtNs = nowNs).also {
            displayTopology = it
        }
    }

    private companion object {

        const val PROCESS_CACHE_LIMIT: Int = 32
        const val CLASS_CACHE_LIMIT: Int = 128
        val DISPLAY_TOPOLOGY_TTL_NS: Long = 2_000.milliseconds.inWholeNanoseconds
    }
}

private fun intersectionArea(first: Rectangle, second: Rectangle): Int {
    val intersection = first.intersection(second)
    return if (intersection.isEmpty) 0 else intersection.width * intersection.height
}

private fun <K, V> LinkedHashMap<K, V>.putBounded(key: K, value: V, maxSize: Int) {
    if (size >= maxSize && !containsKey(key)) {
        val oldestKey = entries.firstOrNull()?.key
        if (oldestKey != null) {
            remove(oldestKey)
        }
    }
    put(key, value)
}
