package com.project.analyzer.impl.setup.game

import androidx.compose.ui.unit.IntRect
import com.project.analyzer.game.impl.GameDetector
import com.project.analyzer.game.impl.GameWindowInfo
import com.project.analyzer.game.impl.WS_EX_APPWINDOW
import com.project.analyzer.game.impl.WS_EX_NOACTIVATE
import com.project.analyzer.game.impl.WS_EX_TOOLWINDOW
import com.project.analyzer.game.impl.user32Ex
import com.project.analyzer.impl.setup.WindowsOverlayRegion
import com.project.analyzer.impl.setup.region.HitRegions
import com.project.analyzer.utils.NsRateLimiter
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.SwingUtilities
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class OverlayController(
    private val gameDetector: GameDetector,
    private val hitRegions: HitRegions,
    private val coroutineDispatcher: CoroutineDispatcher,
) {

    private val user32 = User32.INSTANCE

    private val _state = MutableStateFlow(OverlayState())
    val state: StateFlow<OverlayState> = _state.asStateFlow()

    private var overlayWindow: Window? = null
    private var overlayHwnd: HWND? = null

    private var trackingJob: Job? = null
    private var hitRegionsJob: Job? = null
    private var clickThroughJob: Job? = null
    private var boundsGuardJob: Job? = null

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
    private val logger = LoggerFactory.getLogger(OverlayController::class.java.name)
    private val invalidHwndLimiter = NsRateLimiter(2.seconds.inWholeNanoseconds)

    fun attach(window: Window, scope: CoroutineScope) {
        overlayWindow = window

        runOnEdt {
            window.background = java.awt.Color(0, 0, 0, 0)
            window.isVisible = false
            WindowsOverlayRegion.resetToFullWindow(window)
            applyOverlayStyles(window)

            overlayHwnd = currentOverlayHwnd()
            gameDetector.setOverlayHwnd(overlayHwnd)

            attachResizeGuard(window)
        }

        startTracking(scope)
        startHitRegionsObserver(scope)
        startClickThroughController(scope)
        startBoundsGuard(scope)

        logger.info("Attach to window ${window.name} overlayHwnd=$overlayHwnd")
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
            ensureTopmostAndShow()
        }

        val captureResult = user32Ex.SetCapture(hwnd)
        val lastErr = Native.getLastError()
        logger.info("OverlayController.beginDrag: SetCapture result=$captureResult lastError=$lastErr")
    }

    fun endDrag() {
        if (!_state.value.isDragging) return

        _state.update { it.copy(isDragging = false) }

        val releaseResult = user32Ex.ReleaseCapture()
        val lastErr = Native.getLastError()
        logger.info("OverlayController.endDrag: ReleaseCapture result=$releaseResult lastError=$lastErr")

        val window = overlayWindow ?: return

        runOnEdt {
            WindowsOverlayRegion.apply(window, hitRegions.snapshot())
            ensureTopmostAndShow()
        }
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
                window.isVisible = false
            }
        }

        logger.info("Detach from window ${overlayWindow?.name} overlayHwnd=$overlayHwnd")

        expectedBounds = null
        lastClickThrough = null
        gameDetector.setOverlayHwnd(null)
        overlayWindow = null
        overlayHwnd = null

        _state.update { OverlayState() }
    }

    fun setInputLocked(locked: Boolean) {
        if (inputLocked == locked) return
        inputLocked = locked
        lastClickThrough = null
        ignoreForegroundUntilNs = System.nanoTime() + INPUT_LOCK_FOCUS_GRACE_NS
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

        if (!window.isVisible) {
            logger.info(
                "Enable full screen window to bind it to monitor size monitor id ${gameInfo.monitor.id} ${gameInfo.monitor.bounds}",
            )
            WindowsOverlayRegion.resetToFullWindow(window)
            window.isVisible = true
        }

        ensureTopmostAndShow()

        if (!_state.value.isDragging) {
            WindowsOverlayRegion.apply(window, hitRegions.snapshot())
        }

        _state.update {
            it.copy(
                isVisible = true,
                bounds = expected,
                gameInfo = gameInfo,
            )
        }
    }

    private fun hideOverlay(window: Window) {
        if (_state.value.isDragging) endDrag()

        expectedBounds = null

        if (window.isVisible) {
            WindowsOverlayRegion.resetToFullWindow(window)
            window.isVisible = false
        }

        _state.update {
            OverlayState(
                isVisible = false,
                bounds = null,
                gameInfo = null,
                isDragging = false,
            )
        }
    }

    private fun startHitRegionsObserver(scope: CoroutineScope) {
        hitRegionsJob?.cancel()
        hitRegionsJob = scope.launch {
            hitRegions.observeChanges().collect { regions ->
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
                updateClickThroughState()
                delay(CLICK_THROUGH_POLL_MS)
            }
        }
    }

    private suspend fun updateClickThroughState() {
        val window = overlayWindow ?: return
        val hwnd = currentOverlayHwnd() ?: return
        if (!window.isVisible) return
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
            while (true) {
                guardBounds()
                delay(BOUNDS_GUARD_POLL_MS)
            }
        }
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
    }

    private fun ensureTopmostAndShow() {
        val hwnd = currentOverlayHwnd() ?: return
        if (!ensureValidHwnd(hwnd, "ensureTopmostAndShow")) return
        user32.ShowWindow(hwnd, WinUser.SW_SHOWNOACTIVATE)
        val result = user32.SetWindowPos(
            hwnd,
            HWND(Pointer.createConstant(-1)),
            0,
            0,
            0,
            0,
            WinUser.SWP_NOMOVE or
                WinUser.SWP_NOSIZE or
                WinUser.SWP_NOACTIVATE or
                WinUser.SWP_SHOWWINDOW,
        )

        val err = Native.getLastError()
        logger.info("Set app under apps: SetWindowPos ok=$result lastError=$err hwnd=${hwnd.pointer}")
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
        if (!valid && invalidHwndLimiter.shouldLog(System.nanoTime())) {
            logger.warn("OverlayController.$tag: invalid HWND=${hwnd?.pointer}")
        }
        return valid
    }

    companion object {

        private const val LOCK_HANDLE_SIZE_PX = 32
        private val CLICK_THROUGH_POLL_MS = 16.milliseconds.inWholeMilliseconds
        private val BOUNDS_GUARD_POLL_MS = 150.milliseconds.inWholeMilliseconds
        private val INPUT_LOCK_FOCUS_GRACE_NS = 1200.milliseconds.inWholeNanoseconds
    }
}
