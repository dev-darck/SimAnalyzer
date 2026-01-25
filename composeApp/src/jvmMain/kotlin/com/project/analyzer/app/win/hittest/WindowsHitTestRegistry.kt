package com.project.analyzer.app.win.hittest

import androidx.compose.runtime.Stable
import com.project.analyzer.app.win.nativeWin.WinUserConst
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

@Stable
class WindowsHitTestRegistry {

    @Volatile
    private var captionBar: IntRect? = null

    @Volatile
    private var minBtn: IntRect? = null

    @Volatile
    private var maxBtn: IntRect? = null

    @Volatile
    private var closeBtn: IntRect? = null

    private val excludes = ConcurrentHashMap<String, IntRect>()

    private val lastSampleRef = AtomicReference(HitTestSample())

    fun setCaptionBar(rect: IntRect) {
        captionBar = rect
    }

    fun clearCaptionBar() {
        captionBar = null
    }

    fun setMinButton(rect: IntRect) {
        minBtn = rect
    }

    fun clearMinButton() {
        minBtn = null
    }

    fun setMaxButton(rect: IntRect) {
        maxBtn = rect
    }

    fun clearMaxButton() {
        maxBtn = null
    }

    fun setCloseButton(rect: IntRect) {
        closeBtn = rect
    }

    fun clearCloseButton() {
        closeBtn = null
    }

    fun setExclude(key: String, rect: IntRect) {
        excludes[key] = rect
    }

    fun clearExclude(key: String) {
        excludes.remove(key)
    }

    fun hitTest(x: Float, y: Float): Int {
        val xi = x.roundToInt()
        val yi = y.roundToInt()

        fun commit(code: Int, reason: String) {
            val sample = HitTestSample(
                x = xi,
                y = yi,
                code = code,
                codeName = codeName(code),
                reason = reason,
                timeMs = System.currentTimeMillis()
            )
            lastSampleRef.set(sample)
        }

        closeBtn?.let {
            if (it.contains(xi, yi)) {
                commit(WinUserConst.HTCLOSE, "closeBtn"); return WinUserConst.HTCLOSE
            }
        }
        maxBtn?.let {
            if (it.contains(xi, yi)) {
                commit(WinUserConst.HTMAXBUTTON, "maxBtn"); return WinUserConst.HTMAXBUTTON
            }
        }
        minBtn?.let {
            if (it.contains(xi, yi)) {
                commit(WinUserConst.HTMINBUTTON, "minBtn"); return WinUserConst.HTMINBUTTON
            }
        }

        for ((k, r) in excludes.entries) {
            if (r.contains(xi, yi)) {
                commit(WinUserConst.HTCLIENT, "exclude:$k"); return WinUserConst.HTCLIENT
            }
        }

        captionBar?.let {
            if (it.contains(xi, yi)) {
                commit(WinUserConst.HTCAPTION, "captionBar"); return WinUserConst.HTCAPTION
            }
        }

        commit(WinUserConst.HTCLIENT, "client")
        return WinUserConst.HTCLIENT
    }

    private fun codeName(code: Int): String = when (code) {
        WinUserConst.HTCLIENT -> "HTCLIENT"
        WinUserConst.HTCAPTION -> "HTCAPTION"
        WinUserConst.HTMINBUTTON -> "HTMINBUTTON"
        WinUserConst.HTMAXBUTTON -> "HTMAXBUTTON"
        WinUserConst.HTCLOSE -> "HTCLOSE"
        WinUserConst.HTLEFT -> "HTLEFT"
        WinUserConst.HTRIGHT -> "HTRIGHT"
        WinUserConst.HTTOP -> "HTTOP"
        WinUserConst.HTTOPLEFT -> "HTTOPLEFT"
        WinUserConst.HTTOPRIGHT -> "HTTOPRIGHT"
        WinUserConst.HTBOTTOM -> "HTBOTTOM"
        WinUserConst.HTBOTTOMLEFT -> "HTBOTTOMLEFT"
        WinUserConst.HTBOTTOMRIGHT -> "HTBOTTOMRIGHT"
        WinUserConst.HTTRANSPARENT -> "HTTRANSPARENT"
        else -> "HT($code)"
    }
}

data class IntRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    fun contains(x: Int, y: Int): Boolean = x in left..<right && y >= top && y < bottom
}

data class HitTestSample(
    val x: Int = 0,
    val y: Int = 0,
    val code: Int = WinUserConst.HTCLIENT,
    val codeName: String = "HTCLIENT",
    val reason: String = "init",
    val timeMs: Long = 0L
)
