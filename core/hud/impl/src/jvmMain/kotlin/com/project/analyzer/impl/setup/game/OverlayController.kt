package com.project.analyzer.impl.setup.game

import androidx.compose.ui.unit.IntRect
import com.project.analyzer.game.api.GameWindowDetector
import com.project.analyzer.game.api.GameWindowInfo
import com.project.analyzer.game.api.WS_EX_APPWINDOW
import com.project.analyzer.game.api.WS_EX_NOACTIVATE
import com.project.analyzer.game.api.WS_EX_TOOLWINDOW
import com.project.analyzer.game.api.user32Ex
import com.project.analyzer.impl.setup.WindowsOverlayRegion
import com.project.analyzer.impl.setup.region.HitRegions
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.ptr.IntByReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.SwingUtilities
import kotlin.time.Duration.Companion.milliseconds

class OverlayController(
    private val gameDetector: GameWindowDetector,
    private val hitRegions: HitRegions,
    private val coroutineDispatcher: CoroutineDispatcher,
) {

    private val logger = logger()

    private val user32 = User32.INSTANCE

    private val _state = MutableStateFlow(OverlayState())
    val state: StateFlow<OverlayState> = _state.asStateFlow()

    private var overlayWindow: Window? = null
    private var overlayHwnd: HWND? = null

    private var trackingJob: Job? = null
    private var hitRegionsJob: Job? = null
    private var clickThroughJob: Job? = null
    private var boundsGuardJob: Job? = null
    private val clickThroughSignals = Channel<Unit>(capacity = Channel.CONFLATED)
    private val boundsGuardSignals = Channel<Unit>(capacity = Channel.CONFLATED)

    @Volatile
    private var expectedBounds: Rectangle? = null

    @Volatile
    private var lastClickThrough: Boolean? = null

    @Volatile
    private var inputLocked: Boolean = false

    @Volatile
    private var isApplyingBounds: Boolean = false

    @Volatile
    private var ignoreForegroundUntilNs: Long = 0L

    private var resizeListener: ComponentAdapter? = null

    fun attach(window: Window, scope: CoroutineScope) {
        overlayWindow = window

        runOnEdt {
            window.background = java.awt.Color(0, 0, 0, 0)
            applyOverlayStyles(window)
            WindowsOverlayRegion.apply(window, emptyList())

            overlayHwnd = currentOverlayHwnd()
            gameDetector.setOverlayHwnd(overlayHwnd)

            attachResizeGuard(window)
        }

        startTracking(scope)
        startHitRegionsObserver(scope)
        startClickThroughController(scope)
        startBoundsGuard(scope)

        logger.debug { "Attach to window ${window.name} overlayHwnd=$overlayHwnd" }
    }

    fun onComposeWindowVisibilityChanged(isVisible: Boolean) {
        signalClickThroughRefresh()
        signalBoundsGuardRefresh()
        if (!isVisible) return

        val window = overlayWindow ?: return
        if (_state.value.isDragging) return

        runOnEdt {
            expectedBounds?.let { applyBoundsIfNeeded(window, it) }
            if (!_state.value.isVisible) return@runOnEdt
            WindowsOverlayRegion.apply(window, hitRegions.snapshot())
            ensureTopmost()
        }

        // Compose applies window visibility asynchronously; refresh again after the EDT sync so
        // click-through state is recomputed against the actually visible window.
        signalClickThroughRefresh()
        signalBoundsGuardRefresh()
    }

    fun beginDrag() {
        if (_state.value.isDragging) return

        _state.update { it.copy(isDragging = true) }

        val window = overlayWindow ?: return
        val hwnd = currentOverlayHwnd() ?: return
        if (!ensureValidHwnd(hwnd, "beginDrag")) return

        runOnEdt {
            setMouseTransparent(hwnd, enabled = false)
            WindowsOverlayRegion.resetToFullWindow(window)
            ensureTopmost()
        }
        signalClickThroughRefresh()
        signalBoundsGuardRefresh()

        val captureResult = user32Ex.SetCapture(hwnd)
        val lastErr = Native.getLastError()
        logger.debug { "SetCapture result=$captureResult lastError=$lastErr" }
    }

    fun endDrag() {
        if (!_state.value.isDragging) return

        _state.update { it.copy(isDragging = false) }

        val releaseResult = user32Ex.ReleaseCapture()
        val lastErr = Native.getLastError()
        logger.debug { "ReleaseCapture result=$releaseResult lastError=$lastErr" }

        val window = overlayWindow ?: return

        runOnEdt {
            WindowsOverlayRegion.apply(window, hitRegions.snapshot())
            ensureTopmost()
        }
        signalClickThroughRefresh()
        signalBoundsGuardRefresh()
    }

    fun detach() {
        trackingJob?.cancel()
        trackingJob = null
        hitRegionsJob?.cancel()
        hitRegionsJob = null
        clickThroughJob?.cancel()
        clickThroughJob = null
        boundsGuardJob?.cancel()
        boundsGuardJob = null

        overlayWindow?.let { window ->
            runOnEdt {
                detachResizeGuard(window)
            }
        }

        logger.debug { "Detach from window ${overlayWindow?.name} overlayHwnd=$overlayHwnd" }

        val closingHwnd = overlayHwnd
        expectedBounds = null
        lastClickThrough = null
        gameDetector.setOverlayHwnd(null)
        overlayWindow = null
        overlayHwnd = null

        _state.update { OverlayState() }
        signalClickThroughRefresh()
        signalBoundsGuardRefresh()

        if (closingHwnd != null) {
            LeakCanaryRuntime.watch(closingHwnd, "OverlayController.hwnd")
        }
    }

    fun setInputLocked(locked: Boolean) {
        if (inputLocked == locked) return
        inputLocked = locked
        lastClickThrough = null
        ignoreForegroundUntilNs = System.nanoTime() + INPUT_LOCK_FOCUS_GRACE_NS
        signalClickThroughRefresh()
        requestGameForeground()
    }

    private fun startTracking(scope: CoroutineScope) {
        trackingJob?.cancel()
        trackingJob = scope.launch(coroutineDispatcher) {
            gameDetector.observeGameWindow().collectLatest { gameInfo ->
                handleGameWindowChange(gameInfo)
            }
        }
    }

    private suspend fun handleGameWindowChange(gameInfo: GameWindowInfo?) = withContext(Dispatchers.Swing) {
        val window = overlayWindow ?: return@withContext

        if (gameInfo == null) {
            hideOverlay(window)
            return@withContext
        }

        val now = System.nanoTime()
        val isForeground = isGameOrOverlayForeground(gameInfo.hwnd)
        if (!isForeground && now >= ignoreForegroundUntilNs) {
            hideOverlay(window)
            return@withContext
        }

        val expected = gameInfo.bounds

        expectedBounds = expected
        applyBoundsIfNeeded(window, expected)
        _state.update {
            it.copy(
                isVisible = true,
                bounds = expected,
                gameInfo = gameInfo,
            )
        }

        if (!window.isVisible) {
            logger.debug {
                "Enable full screen window to bind it to monitor size monitor id ${gameInfo.monitor.id} ${gameInfo.monitor.bounds}"
            }
            // Safe default for the hidden/first-show path: hidden overlay must not intercept
            // clicks even if click-through refresh races with Compose window visibility.
            WindowsOverlayRegion.apply(window, emptyList())
            currentOverlayHwnd()?.let { setMouseTransparent(it, enabled = true) }
            lastClickThrough = true
        }

        if (window.isVisible) {
            ensureTopmost()
        }

        if (window.isVisible && !_state.value.isDragging) {
            WindowsOverlayRegion.apply(window, hitRegions.snapshot())
        }
        signalClickThroughRefresh()
        signalBoundsGuardRefresh()
    }

    private fun hideOverlay(window: Window) {
        if (_state.value.isDragging) endDrag()

        expectedBounds = null
        hitRegions.clear()
        lastClickThrough = true

        if (window.isVisible) {
            WindowsOverlayRegion.apply(window, emptyList())
            currentOverlayHwnd()?.let { setMouseTransparent(it, enabled = true) }
        }

        _state.update {
            OverlayState(
                isVisible = false,
                bounds = null,
                gameInfo = null,
                isDragging = false,
            )
        }
        signalClickThroughRefresh()
        signalBoundsGuardRefresh()
    }

    private fun startHitRegionsObserver(scope: CoroutineScope) {
        hitRegionsJob?.cancel()
        hitRegionsJob = scope.launch {
            hitRegions.observeChanges().collect { regions ->
                signalClickThroughRefresh()
                val window = overlayWindow ?: return@collect
                if (!window.isVisible) return@collect
                if (_state.value.isDragging) return@collect

                withContext(Dispatchers.Swing) {
                    WindowsOverlayRegion.apply(window, regions)
                }
            }
        }
    }

    private fun startClickThroughController(scope: CoroutineScope) {
        clickThroughJob?.cancel()
        clickThroughJob = scope.launch(coroutineDispatcher) {
            while (isActive) {
                if (shouldPollClickThroughWithMouse()) {
                    updateClickThroughState()
                    withTimeoutOrNull(CLICK_THROUGH_POLL_MS) {
                        clickThroughSignals.receive()
                    }
                } else {
                    clickThroughSignals.receive()
                    updateClickThroughState()
                }
            }
        }
        signalClickThroughRefresh()
    }

    private suspend fun updateClickThroughState() {
        val window = overlayWindow ?: return
        val hwnd = currentOverlayHwnd() ?: return
        if (!window.isVisible) {
            if (lastClickThrough != true) {
                lastClickThrough = true
                withContext(Dispatchers.Swing) {
                    setMouseTransparent(hwnd, enabled = true)
                }
            }
            return
        }
        val regions = hitRegions.snapshot()

        if (inputLocked) {
            val mouse = MouseInfo.getPointerInfo()?.location ?: return
            val wx = mouse.x - window.x
            val wy = mouse.y - window.y
            val inside = wx in 0 until window.width && wy in 0 until window.height
            val overUnlockHandle = inside && isOverLockHandle(wx, wy, regions)
            val shouldBeClickThrough = !_state.value.isDragging && !overUnlockHandle
            if (lastClickThrough != shouldBeClickThrough) {
                lastClickThrough = shouldBeClickThrough
                withContext(Dispatchers.Swing) {
                    setMouseTransparent(hwnd, enabled = shouldBeClickThrough)
                }
            }
            return
        }
        val shouldBeClickThrough = regions.isEmpty()

        if (lastClickThrough != shouldBeClickThrough) {
            lastClickThrough = shouldBeClickThrough
            withContext(Dispatchers.Swing) {
                setMouseTransparent(hwnd, enabled = shouldBeClickThrough)
            }
        }
    }

    private fun isOverLockHandle(x: Int, y: Int, regions: List<IntRect>): Boolean {
        val size = LOCK_HANDLE_SIZE_PX
        return regions.any { rect ->
            val left = (rect.right - size).coerceAtLeast(rect.left)
            val top = rect.top
            val right = rect.right
            val bottom = (rect.top + size).coerceAtMost(rect.bottom)
            x in left until right && y in top until bottom
        }
    }

    private fun startBoundsGuard(scope: CoroutineScope) {
        boundsGuardJob?.cancel()
        boundsGuardJob = scope.launch(coroutineDispatcher) {
            while (isActive) {
                if (shouldRunBoundsGuardFallback()) {
                    guardBounds()
                    withTimeoutOrNull(BOUNDS_GUARD_POLL_MS) {
                        boundsGuardSignals.receive()
                    }
                } else {
                    boundsGuardSignals.receive()
                    guardBounds()
                }
            }
        }
        signalBoundsGuardRefresh()
    }

    private suspend fun guardBounds() {
        val window = overlayWindow ?: return
        val expected = expectedBounds ?: return
        if (!window.isVisible) return
        if (_state.value.isDragging) return

        val actual = window.bounds
        if (actual == expected) return

        withContext(Dispatchers.Swing) {
            applyBoundsIfNeeded(window, expected)
        }
    }

    private fun applyBoundsIfNeeded(window: Window, expected: Rectangle) {
        val actual = window.bounds
        if (actual == expected) return
        if (isApplyingBounds) return

        isApplyingBounds = true
        try {
            window.setBounds(expected.x, expected.y, expected.width, expected.height)
            window.validate()
        } finally {
            isApplyingBounds = false
        }
    }

    private fun attachResizeGuard(window: Window) {
        detachResizeGuard(window)

        resizeListener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) = onWindowMutated(window)
            override fun componentMoved(e: ComponentEvent) = onWindowMutated(window)
        }.also { window.addComponentListener(it) }
    }

    private fun detachResizeGuard(window: Window) {
        resizeListener?.let { window.removeComponentListener(it) }
        resizeListener = null
    }

    private fun onWindowMutated(window: Window) {
        if (isApplyingBounds) return
        if (_state.value.isDragging) return

        val expected = expectedBounds ?: return
        val actual = window.bounds
        if (actual == expected) return

        applyBoundsIfNeeded(window, expected)
        signalBoundsGuardRefresh()
    }

    private fun shouldPollClickThroughWithMouse(): Boolean {
        val window = overlayWindow ?: return false
        return inputLocked && window.isVisible
    }

    private fun shouldRunBoundsGuardFallback(): Boolean {
        val window = overlayWindow ?: return false
        return expectedBounds != null && window.isVisible && !_state.value.isDragging
    }

    private fun signalClickThroughRefresh() {
        clickThroughSignals.trySend(Unit)
    }

    private fun signalBoundsGuardRefresh() {
        boundsGuardSignals.trySend(Unit)
    }

    private fun ensureTopmost() {
        val hwnd = currentOverlayHwnd() ?: return
        if (!ensureValidHwnd(hwnd, "ensureTopmost")) return
        val result = user32.SetWindowPos(
            hwnd,
            HWND(Pointer.createConstant(-1)),
            0,
            0,
            0,
            0,
            WinUser.SWP_NOMOVE or
                WinUser.SWP_NOSIZE or
                WinUser.SWP_NOACTIVATE,
        )

        val err = Native.getLastError()
        if (result) return

        if (err == ERROR_INVALID_WINDOW_HANDLE) {
            logger.atDebug(RATE_LIMITED) {
                message = "ensureTopmost: stale HWND ignored (lastError=$err hwnd=${hwnd.pointer})"
            }
            return
        }

        logger.atWarn(RATE_LIMITED) {
            message = "Set app under apps failed: SetWindowPos ok=$result lastError=$err hwnd=${hwnd.pointer}"
        }
    }

    private fun requestGameForeground() {
        val hwnd = _state.value.gameInfo?.hwnd ?: return
        if (!ensureValidHwnd(hwnd, "requestGameForeground")) return
        user32.SetForegroundWindow(hwnd)
    }

    private fun setMouseTransparent(hwnd: HWND, enabled: Boolean) {
        if (!ensureValidHwnd(hwnd, "setMouseTransparent")) return
        var exStyle = user32.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)
        val oldStyle = exStyle

        exStyle = if (enabled) {
            exStyle or WinUser.WS_EX_TRANSPARENT
        } else {
            exStyle and WinUser.WS_EX_TRANSPARENT.inv()
        }

        if (oldStyle != exStyle) {
            user32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle)
            user32.SetWindowPos(
                hwnd,
                null,
                0,
                0,
                0,
                0,
                WinUser.SWP_NOMOVE or WinUser.SWP_NOSIZE or WinUser.SWP_NOZORDER or WinUser.SWP_NOACTIVATE,
            )
        }
    }

    private fun applyOverlayStyles(window: Window) {
        val hwnd = resolveHwnd(window) ?: return
        if (!ensureValidHwnd(hwnd, "applyOverlayStyles")) return
        var exStyle = user32.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)

        exStyle = exStyle or WinUser.WS_EX_LAYERED
        exStyle = exStyle or WS_EX_TOOLWINDOW
        exStyle = exStyle and WS_EX_APPWINDOW.inv()
        exStyle = exStyle or WS_EX_NOACTIVATE
        exStyle = exStyle or WinUser.WS_EX_TRANSPARENT

        user32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle)
        disableDwmWindowShadow(hwnd)
    }

    private fun disableDwmWindowShadow(hwnd: HWND) {
        val dwm = runCatching { NativeLibrary.getInstance("dwmapi") }.getOrNull() ?: return
        val setAttribute = runCatching { dwm.getFunction("DwmSetWindowAttribute") }.getOrNull() ?: return

        // Suppress DWM shadow/non-client rendering for transparent overlay windows.
        val ncRenderingDisabled = IntByReference(DWMNCRP_DISABLED)
        runCatching {
            setAttribute.invoke(arrayOf(hwnd, DWMWA_NCRENDERING_POLICY, ncRenderingDisabled.pointer, 4))
        }

        // Best-effort cleanup for Win11 border/corner halo.
        val noBorderColor = IntByReference(DWMWA_COLOR_NONE)
        runCatching {
            setAttribute.invoke(arrayOf(hwnd, DWMWA_BORDER_COLOR, noBorderColor.pointer, 4))
        }

        val noRoundedCorners = IntByReference(DWMWCP_DONOTROUND)
        runCatching {
            setAttribute.invoke(arrayOf(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, noRoundedCorners.pointer, 4))
        }
    }

    private fun isGameOrOverlayForeground(gameHwnd: HWND): Boolean {
        if (user32Ex.IsIconic(gameHwnd)) return false
        val foreground = user32.GetForegroundWindow() ?: return false
        val fgPtr = foreground.pointer
        val overlayPtr = overlayHwnd?.pointer
        return fgPtr == gameHwnd.pointer || (overlayPtr != null && fgPtr == overlayPtr)
    }

    private inline fun runOnEdt(crossinline block: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            block()
        } else {
            SwingUtilities.invokeAndWait { block() }
        }
    }

    private fun currentOverlayHwnd(): HWND? {
        val w = overlayWindow ?: return null
        if (!w.isDisplayable) return null
        val hwnd = resolveHwnd(w) ?: return null
        if (!isValidHwnd(hwnd)) return null
        if (overlayHwnd?.pointer != hwnd.pointer) {
            overlayHwnd = hwnd
            gameDetector.setOverlayHwnd(hwnd)
        }
        return hwnd
    }

    private fun resolveHwnd(window: Window): HWND? {
        if (!window.isDisplayable) return null
        val ptr = Native.getComponentPointer(window)
        if (ptr == Pointer.NULL) return null
        return HWND(ptr)
    }

    private fun isValidHwnd(hwnd: HWND?): Boolean {
        if (hwnd == null) return false
        if (hwnd.pointer == Pointer.NULL) return false
        return user32.IsWindow(hwnd)
    }

    private fun ensureValidHwnd(hwnd: HWND?, tag: String): Boolean {
        val valid = isValidHwnd(hwnd)
        if (!valid) {
            logger.atWarn(RATE_LIMITED) {
                message = "$tag: invalid HWND=${hwnd?.pointer}"
            }
        }
        return valid
    }

    companion object {

        private const val LOCK_HANDLE_SIZE_PX = 32
        private val CLICK_THROUGH_POLL_MS = 16.milliseconds.inWholeMilliseconds
        private val BOUNDS_GUARD_POLL_MS = 500.milliseconds.inWholeMilliseconds
        private val INPUT_LOCK_FOCUS_GRACE_NS = 1200.milliseconds.inWholeNanoseconds
        private const val DWMWA_NCRENDERING_POLICY = 2
        private const val DWMWA_WINDOW_CORNER_PREFERENCE = 33
        private const val DWMWA_BORDER_COLOR = 34
        private const val DWMNCRP_DISABLED = 1
        private const val DWMWCP_DONOTROUND = 1
        private const val DWMWA_COLOR_NONE = 0xFFFFFFFE.toInt()
        private const val ERROR_INVALID_WINDOW_HANDLE = 1400
    }
}
